package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.TeilgraphErsetzungsPlan
import de.TeutonStudio.MathematikRechenSystem.kern.*
import java.util.UUID

/** Strukturformeln werden mit bestehenden Rechnern gekapselt, ohne neue Knotenarten. */
data class FormelKartenEntwurf(val karte:KartenDaten,val wurzel:KnotenId)
fun erzeugeFormelKarte(f:GebundeneFormel,position:GraphPunkt=GraphPunkt.Zero):FormelKartenEntwurf {
    FormelBearbeitungsEntwurf(f).geprüft()
    if(f.quantoren.isNotEmpty()) {
        val k=konfiguriereGebundeneKnotenFormel(ZahlenRechnerKnotenVorlagen.standard.erzeuge(position),f)
        return FormelKartenEntwurf(KartenDaten(name="Formel",knoten=listOf(k)),k.id)
    }
    val knoten=mutableListOf<KnotenDaten>();val kanten=mutableListOf<VerbindungDaten>();val nachAusdruck=mutableMapOf<String,KnotenDaten>()
    val quellen=f.quellen.associateBy {it.id};val quellKnoten=mutableMapOf<String,KnotenDaten>()
    fun quelle(q:FormelVariablenQuelle):KnotenDaten = quellKnoten.getOrPut(q.id) {
        val ast=FormelAusdruck.Variable(UUID.randomUUID().toString(),q.anzeigeName,typ=q.typ)
        konfiguriereGebundeneKnotenFormel(ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt(position.x,position.y+knoten.size*180f)),GebundeneFormel(ast,if(f.wurzel is FormelAusdruck.Variable && f.variablenVorkommen[f.wurzel.id]==q.id) f.quellen else listOf(q),mapOf(ast.id to q.id),annahmen=if(f.wurzel is FormelAusdruck.Variable && f.variablenVorkommen[f.wurzel.id]==q.id) f.annahmen else emptySet())).also {knoten+=it}
    }
    fun baue(e:FormelAusdruck):KnotenDaten = nachAusdruck.getOrPut(e.id) {
        if(e is FormelAusdruck.Variable) quelle(quellen.getValue(f.variablenVorkommen.getValue(e.id)))
        else {
            val kinder=if(e is FormelAusdruck.Operation) e.argumente.sortedBy {it.position}.map {baue(it.ausdruck)} else emptyList()
            val synthetisch=if(e is FormelAusdruck.Operation) e.argumente.sortedBy {it.position}.mapIndexed { i,a -> FormelVariablenQuelle("verbindung:${e.id}:$i","Eingang ${i+1}",FormelVariablenArt.EXTERN,a.ausdruck.typ,
                    methodenSignatur=when(val teil=a.ausdruck) {is FormelAusdruck.Variable -> quellen[f.variablenVorkommen[teil.id]]?.methodenSignatur;is FormelAusdruck.Literal -> (teil.wert as? SignaturtragendeMethode)?.signatur;else -> null}) } else emptyList()
            val ast=if(e is FormelAusdruck.Operation) e.copy(argumente=e.argumente.sortedBy {it.position}.mapIndexed { i,a -> a.copy(ausdruck=FormelAusdruck.Variable("eingang:${e.id}:$i",synthetisch[i].anzeigeName,typ=a.ausdruck.typ)) }) else e
            val original=if(e.id==f.wurzel.id) f.quellen else emptyList()
            original.forEach(::quelle)
            val vorkommen=if(ast is FormelAusdruck.Operation) ast.argumente.mapIndexed {i,a -> a.ausdruck.id to synthetisch[i].id}.toMap() else emptyMap()
            val lokal=GebundeneFormel(ast,original+synthetisch,vorkommen,annahmen=if(e.id==f.wurzel.id) f.annahmen else emptySet())
            val k=konfiguriereGebundeneKnotenFormel(ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt(position.x+300f*(1+kinder.size),position.y+knoten.size*180f)),lokal)
            (original.map {it.id to quelle(it)}+synthetisch.mapIndexed {i,q -> q.id to kinder[i]}).forEach { (id,kind) ->
                kanten+=VerbindungDaten(von=AnschlussVerweis(kind.id,kind.anschlüsse.single {it.richtung==AnschlussRichtung.Ausgang}.id),zu=AnschlussVerweis(k.id,k.anschlüsse.single {it.semantischeId==id}.id))
            }
            knoten+=k;k
        }
    }
    val root=baue(f.wurzel)
    return FormelKartenEntwurf(KartenDaten(name="Formelkarte",knoten=knoten,verbindungen=kanten),root.id)
}

/** Rekonstruktion folgt Verbindungen und semantischen Portidentitäten, niemals Grafikpositionen. */
fun karteAlsGebundeneFormel(karte:KartenDaten,wurzel:KnotenId):GebundeneFormel {
    val cache=mutableMapOf<KnotenId,GebundeneFormel>();val aktiv=mutableSetOf<KnotenId>()
    fun baue(id:KnotenId):GebundeneFormel {
        cache[id]?.let {return it}
        require(aktiv.add(id)) {"Zyklischer Formelgraph."}
        val k=karte.knoten.single {it.id==id}
        val f=formelAusBestehendemKnoten(k)
        if(f.quantoren.isNotEmpty()) {require(id==wurzel && karte.verbindungen.none {it.zu.knotenId==id});return f}
        val qs=linkedMapOf<String,FormelVariablenQuelle>();val vorkommen=linkedMapOf<String,String>();val annahmen=linkedSetOf<Aussage>()
        val ersetzt=mutableMapOf<String,GebundeneFormel>();val werte=mutableMapOf<String,MathematischesObjekt>()
        fun quelle(q:FormelVariablenQuelle) {val vorher=qs.putIfAbsent(q.id,q);require(vorher==null || vorher==q) {"Widersprüchliche Quellenidentität."}}
        for(q in f.quellen) {
            val port=k.anschlüsse.singleOrNull {it.semantischeId==q.id} ?: k.anschlüsse.singleOrNull {it.name==q.anzeigeName && it.richtung==AnschlussRichtung.Eingang}
            val verbindung=karte.verbindungen.singleOrNull {it.zu.knotenId==id && it.zu.anschlussId==port?.id}
            if(verbindung==null) quelle(q)
            else {
                val kind=baue(verbindung.von.knotenId);ersetzt[q.id]=kind
                kind.quellen.forEach(::quelle);vorkommen+=kind.variablenVorkommen;annahmen+=kind.annahmen
                val aus=kind.werteAus(operation={op,args ->
                    val bed=mutableSetOf<Aussage>()
                    val w=werteFormelOperationAus(op.operatorId,op.argumente.sortedBy {it.position}.map {it.rollenId}.zip(args).toMap(),k,bed) as? MathematischesObjekt ?: error("Nichtmathematischer Formelwert.")
                    FormelKernErgebnis(w,bed)
                })
                werte[q.id]=aus.objekt;annahmen+=aus.bedingungen
            }
        }
        fun ast(e:FormelAusdruck):FormelAusdruck = when(e) {
            is FormelAusdruck.Variable -> {
                val q=f.variablenVorkommen.getValue(e.id)
                ersetzt[q]?.wurzel ?: e.copy(id="${id.wert}:${e.id}").also {vorkommen[it.id]=q}
            }
            is FormelAusdruck.Operation -> e.copy(id="${id.wert}:${e.id}",argumente=e.argumente.map {it.copy(ausdruck=ast(it.ausdruck))},bedingungen=e.bedingungen.map {ersetze(it,werte)})
            is FormelAusdruck.Literal -> e.copy(id="${id.wert}:${e.id}",wert=ersetze(e.wert,werte))
            is FormelAusdruck.Platzhalter -> error("Der Graph enthält einen offenen Ausdruck.")
        }
        annahmen+=f.annahmen.map {ersetze(it,werte)}
        val wurzelAst=ast(f.wurzel)
        val ids=mutableSetOf<String>()
        fun variablen(e:FormelAusdruck) {if(e is FormelAusdruck.Variable) ids+=e.id;if(e is FormelAusdruck.Operation) e.argumente.forEach {variablen(it.ausdruck)}}
        variablen(wurzelAst)
        val result=GebundeneFormel(wurzelAst,qs.values.toList(),vorkommen.filterKeys {it in ids},annahmen=annahmen)
        FormelBearbeitungsEntwurf(result).geprüft();aktiv.remove(id);cache[id]=result;return result
    }
    return baue(wurzel)
}

/** Importiert nur nachweisbar bekannte Quellen/Operatoren; unbekannte Knoten bleiben unangetastet. */
private fun formelAusBestehendemKnoten(k:KnotenDaten):GebundeneFormel {
    ladeGebundeneKnotenFormel(k)?.let {return it}
    if(k.art=="mathematik.zahl") {
        val w=RationaleZahl.parse(k.parameter["wert"] ?: error("Zahlwert fehlt."))
        return GebundeneFormel(FormelAusdruck.Literal("wert",w,FormelTyp.ZAHL),emptyList(),emptyMap())
    }
    if(k.art=="mathematik.variable") {
        val name=k.parameter["name"] ?: "x"
        val raum=when(k.parameter["werteVorrat"]) {"R" -> ReelleZahlen;"Q" -> RationaleZahlen;"Z" -> GanzeZahlen;"N" -> NatürlicheZahlen;"C" -> KomplexeZahlen;else -> null}
        require(k.parameter["variable.struktur"]==null || k.parameter["variable.struktur"]=="zahl") {"Diese strukturierte Quelle benötigt ihren vollständigen Formelkontext."}
        val q=FormelVariablenQuelle(k.id.wert,name,FormelVariablenArt.FREI,FormelTyp.ZAHL,raum)
        return GebundeneFormel(FormelAusdruck.Variable("wert",name,typ=FormelTyp.ZAHL),listOf(q),mapOf("wert" to q.id))
    }
    val op=UniversellerZahlenOperator.vonIdOderNull(k.parameter[ZAHLENRECHNER_OPERATOR])
    val taste=FormelTastatur.standard.singleOrNull {it.operatorId==op?.stabileId} ?: error("Knoten '${k.name}' besitzt keinen unterstützten strukturellen Formelvertrag.")
    val eingänge=k.anschlüsse.filter {it.richtung==AnschlussRichtung.Eingang}.sortedBy {it.reihenfolge}
    require(eingänge.size==taste.argumentRollen.size) {"Die Operatoranschlüsse sind nicht eindeutig abbildbar."}
    val qs=eingänge.map {FormelVariablenQuelle(it.semantischeId ?: "${k.id.wert}:${it.id.wert}",it.name,FormelVariablenArt.EXTERN,FormelTyp.ZAHL)}
    val ast=FormelAusdruck.Operation("operator",requireNotNull(taste.operatorId),qs.mapIndexed {i,q -> FormelArgument(taste.argumentRollen[i],i,FormelAusdruck.Variable("argument:$i",q.anzeigeName,typ=q.typ))},taste.ergebnisTyp)
    return GebundeneFormel(ast,qs,qs.indices.associate {"argument:$it" to qs[it].id})
}

fun planeFormelKnotenEntfalten(karte:KartenDaten,k:KnotenDaten):TeilgraphErsetzungsPlan {
    val f=requireNotNull(ladeGebundeneKnotenFormel(k));val neu=erzeugeFormelKarte(f,k.position)
    val root=neu.karte.knoten.single {it.id==neu.wurzel};val aus=k.anschlüsse.single {it.richtung==AnschlussRichtung.Ausgang}
    val rootAus=root.anschlüsse.single {it.richtung==AnschlussRichtung.Ausgang}
    val externe=karte.verbindungen.filter {it.zu.knotenId==k.id || it.von.knotenId==k.id}.map {v ->
        if(v.von.knotenId==k.id) v.copy(von=AnschlussVerweis(root.id,rootAus.id)) else {
            val qid=k.anschlüsse.single {it.id==v.zu.anschlussId}.semantischeId ?: error("Die historische Anschlussbindung muss zuerst übernommen werden.")
            val ziel=neu.karte.knoten.singleOrNull {n -> n.anschlüsse.any {it.semantischeId==qid} && ladeGebundeneKnotenFormel(n)?.wurzel is FormelAusdruck.Variable} ?: error("Quellanschluss nicht eindeutig abbildbar.")
            v.copy(zu=AnschlussVerweis(ziel.id,ziel.anschlüsse.single {it.semantischeId==qid}.id))
        }
    }
    return TeilgraphErsetzungsPlan.aus(karte,setOf(k.id),neu.karte.knoten,neu.karte.verbindungen+externe)
}
fun planeFormelKnotenZusammenfassen(karte:KartenDaten,wurzel:KnotenDaten):TeilgraphErsetzungsPlan {
    val ids=linkedSetOf<KnotenId>()
    fun besuche(id:KnotenId) {if(!ids.add(id)) return;karte.verbindungen.filter {it.zu.knotenId==id}.forEach {besuche(it.von.knotenId)}}
    besuche(wurzel.id)
    require(karte.verbindungen.none {it.von.knotenId in ids && it.von.knotenId!=wurzel.id && it.zu.knotenId !in ids}) {"Ein innerer Knoten wird außerhalb der Formel verwendet."}
    val f=karteAlsGebundeneFormel(karte,wurzel.id)
    val k=konfiguriereGebundeneKnotenFormel(wurzel,f)
    return TeilgraphErsetzungsPlan.aus(karte,ids,listOf(k),karte.verbindungen.filter {it.von.knotenId==wurzel.id && it.zu.knotenId !in ids})
}
