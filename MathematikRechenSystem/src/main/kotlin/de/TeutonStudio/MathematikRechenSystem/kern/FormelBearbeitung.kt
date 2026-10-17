package de.TeutonStudio.MathematikRechenSystem.kern

import java.util.UUID
import de.TeutonStudio.TypSystem.*

/** Persistierbarer semantischer Entwurf. Namen sind keine Bindungsschlüssel. */
class FormelBearbeitungsEntwurf(start: GebundeneFormel) {
    var formel: GebundeneFormel = start
        private set

    fun mitWurzel(wurzel: FormelAusdruck, neueBindungen:Map<String,String> = emptyMap(), neuImport:Boolean=false) {
        val quellen = formel.quellen.toMutableList()
        val vorkommen = linkedMapOf<String,String>()
        fun besuche(e: FormelAusdruck) {
            when(e) {
                is FormelAusdruck.Variable -> {
                    val alt = (neueBindungen[e.id] ?: if(neuImport) null else formel.variablenVorkommen[e.id])?.let { id -> quellen.singleOrNull { it.id==id } }
                    val kandidaten = quellen.filter { it.anzeigeName==e.name }
                    val q = alt ?: kandidaten.singleOrNull() ?: if(kandidaten.isEmpty())
                        FormelVariablenQuelle(UUID.randomUUID().toString(),e.name,FormelVariablenArt.FREI,e.typ).also { quellen += it }
                        else error("Für '${e.name}' muss eine eindeutige Quelle ausgewählt werden.")
                    vorkommen[e.id]=q.id
                }
                is FormelAusdruck.Operation -> e.argumente.forEach { besuche(it.ausdruck) }
                else -> Unit
            }
        }
        besuche(wurzel)
        formel=formel.copy(wurzel=wurzel,quellen=quellen,variablenVorkommen=vorkommen)
    }
    fun importiere(latex: String) {
        val neu=FormelLatexCodec.importiere(latex,formel.quellen)
        require(neu is FormelLatexImportErgebnis.Erfolg) { (neu as FormelLatexImportErgebnis.Fehler).nachricht }
        require(formel.quantoren.isEmpty()) { "Gebundene Gültigkeitsbereiche müssen strukturell bearbeitet werden." }
        mitWurzel(neu.ausdruck,neuImport=true)
    }
    fun quelle(q: FormelVariablenQuelle) {
        val alt=formel.quellen.singleOrNull { it.id==q.id }
        lateinit var symbol:MathematischesObjekt
        fun ersetzeAst(e:FormelAusdruck):FormelAusdruck = when(e) {
            is FormelAusdruck.Variable -> if(formel.variablenVorkommen[e.id]==q.id) e.copy(name=q.anzeigeName,latex=q.anzeigeName,typ=q.typ) else e
            is FormelAusdruck.Operation -> e.copy(argumente=e.argumente.map { it.copy(ausdruck=ersetzeAst(it.ausdruck)) },bedingungen=e.bedingungen.map {ersetze(it,mapOf(q.id to symbol))})
            is FormelAusdruck.Literal -> e.copy(wert=ersetze(e.wert,mapOf(q.id to symbol)))
            else -> e
        }
        symbol=when(q.rolle) {
            FormelSymbolRolle.METHODE -> MethodenVariable(q.anzeigeName,q.id,requireNotNull(q.methodenSignatur))
            FormelSymbolRolle.WERT,FormelSymbolRolle.KONSTANTE -> Variable(q.anzeigeName,bindungsId=q.id)
            else -> AllgemeinerParameter(q.anzeigeName,bindungsId=q.id)
        }
        formel=formel.copy(wurzel=ersetzeAst(formel.wurzel),
            quellen=if(alt==null) formel.quellen+q else formel.quellen.map { if(it.id==q.id) q else it },
            annahmen=formel.annahmen.map { ersetze(it,mapOf(q.id to symbol)) }.toSet(),
            quantoren=if(q.art==FormelVariablenArt.GEBUNDEN) formel.quantoren else formel.quantoren.filterNot {it.quelleId==q.id})
    }
    fun entferneQuelle(id:String) {
        require(id !in formel.variablenVorkommen.values && formel.quantoren.none { it.quelleId==id } && formel.praedikate.none { id in it.argumentQuellenIds }) { "Die Quelle wird noch verwendet." }
        require(formel.annahmen.none { a -> a.enthalteneMethodenParameter().any { it.bindungsId==id } }) { "Die Quelle wird in einer Voraussetzung verwendet." }
        formel=formel.copy(quellen=formel.quellen.filterNot { it.id==id })
    }
    fun verschiebeQuelle(id:String,delta:Int) {
        val liste=formel.quellen.toMutableList();val i=liste.indexOfFirst { it.id==id };val j=i+delta
        require(i>=0 && j in liste.indices)
        val q=liste.removeAt(i);liste.add(j,q);formel=formel.copy(quellen=liste)
    }
    fun fügeVoraussetzungHinzu(links:String,relation:String,rechts:String) {
        val l=FormelLatexCodec.importiereGebunden(links,formel.quellen).werteAus()
        val r=FormelLatexCodec.importiereGebunden(rechts,formel.quellen).werteAus()
        val a=when(relation) {"=" -> Gleichheit(l.objekt,r.objekt);"≠" -> Ungleichheit(l.objekt,r.objekt);else -> Vergleich(l.objekt as ZahlAusdruck,when(relation) {"<" -> VergleichsArt.Kleiner;"≤" -> VergleichsArt.KleinerGleich;">" -> VergleichsArt.Größer;"≥" -> VergleichsArt.GrößerGleich;else -> error("Unbekannter Vergleich.")},r.objekt as ZahlAusdruck)}
        voraussetzungen(formel.annahmen+l.bedingungen+r.bedingungen+a)
    }
    fun bindeQuelle(id:String,quantor:FormelQuantorArt,geltungsbereich:String) {
        require(formel.quellen.single {it.id==id}.art==FormelVariablenArt.GEBUNDEN)
        val alt=formel.quantoren.singleOrNull {it.quelleId==id}
        formel=formel.copy(quantoren=formel.quantoren.filterNot {it.quelleId==id}+FormelQuantorBindung(alt?.bindungsId ?: UUID.randomUUID().toString(),quantor,id,geltungsbereich))
    }
    fun übernehme(f:GebundeneFormel) { FormelBearbeitungsEntwurf(f).geprüft();formel=f }
    fun voraussetzungen(a:Set<Aussage>) { formel=formel.copy(annahmen=a) }
    fun geprüft():GebundeneFormel {
        require(FormelAusdruckPruefer.pruefe(formel.wurzel)==FormelPruefung.Gueltig) { "Die Formel ist unvollständig." }
        val p=FormelBindungsPruefer.pruefe(formel)
        require(p==FormelBindungsPruefung.Gueltig) { (p as FormelBindungsPruefung.Ungueltig).gruende.joinToString("\n") }
        formel.quellen.forEach { q -> require(q.rolle!=FormelSymbolRolle.METHODE || q.methodenSignatur!=null) { "Methodensignatur für '${q.anzeigeName}' fehlt." } }
        val typen=prüfeFormelTypen(formel)
        require(typen.isEmpty()) {typen.joinToString("\n")}
        return formel
    }
    companion object {
        fun aus(wurzel:FormelAusdruck):FormelBearbeitungsEntwurf = FormelBearbeitungsEntwurf(GebundeneFormel(wurzel,emptyList(),emptyMap())).also { it.mitWurzel(wurzel) }
    }
}

/** Fachliche Typprüfung vor der atomaren Übernahme; unbekannte Typen werden nicht erfunden. */
fun prüfeFormelTypen(f:GebundeneFormel):List<String> {
    val fehler=mutableListOf<String>();val quellen=f.quellen.associateBy {it.id}
    val strukturtasten=StrukturFormelFamilie.entries.flatMap {StrukturFormelTastatur.fuer(it)}.associateBy {it.operatorId}
    val zahlentasten=FormelTastatur.standard.filter {it.operatorId!=null}.associateBy {it.operatorId}
    fun atom(t:FormelTyp):de.TeutonStudio.TypSystem.TypAusdruck = de.TeutonStudio.TypSystem.TypAusdruck.Atom(when(t) {FormelTyp.ZAHL -> MathematischeTypen.Zahl;FormelTyp.MENGE -> MathematischeTypen.Menge;FormelTyp.AUSSAGE -> MathematischeTypen.Aussage;FormelTyp.METHODE -> MathematischeTypen.Methode;FormelTyp.TUPEL -> MathematischeTypen.Tupel;FormelTyp.VEKTOR -> MathematischeTypen.Vektor;FormelTyp.MATRIX -> MathematischeTypen.Matrix;FormelTyp.TENSOR -> MathematischeTypen.Tensor;else -> MathematischeTypen.Objekt})
    fun typ(e:FormelAusdruck):de.TeutonStudio.TypSystem.TypAusdruck {
        if(e is FormelAusdruck.Variable) {val q=quellen[f.variablenVorkommen[e.id]];return q?.methodenSignatur?.typAusdruck ?: q?.vollstaendigerTyp?.takeUnless {it==de.TeutonStudio.TypSystem.TypAusdruck.Unbekannt} ?: atom(q?.typ ?: e.typ)}
        if(e is FormelAusdruck.Literal) return when(val w=e.wert) {
            is Methode -> w.methodenTypAusdruck()
            is Tupel -> de.TeutonStudio.TypSystem.TypAusdruck.Parameterisiert(MathematischeTypen.Tupel,w.elemente.mapIndexed {i,v ->typ(FormelAusdruck.Literal("literal:$i",v,FormelTyp.OBJEKT))})
            is RationaleZahl -> RationaleZahlen.elementTypAusdruck()
            is ZahlAusdruck -> atom(FormelTyp.ZAHL)
            is Aussage -> atom(FormelTyp.AUSSAGE)
            is MengenAusdruck -> atom(FormelTyp.MENGE)
            else -> atom(e.typ)
        }
        if(e is FormelAusdruck.Operation) {
            val a=e.argumente.sortedBy {it.position}.map {it.ausdruck}
            if(e.operatorId=="formel.gruppierung") return a.singleOrNull()?.let(::typ) ?: de.TeutonStudio.TypSystem.TypAusdruck.Unbekannt
            if(e.operatorId=="tupel.konstruktion") return de.TeutonStudio.TypSystem.TypAusdruck.Parameterisiert(MathematischeTypen.Tupel,a.map(::typ))
            if(e.operatorId=="methode.anwenden") return (a.firstOrNull()?.let(::typ) as? de.TeutonStudio.TypSystem.TypAusdruck.Parameterisiert)?.takeIf {it.konstruktor==MathematischeTypen.Methode}?.argumente?.getOrNull(1) ?: de.TeutonStudio.TypSystem.TypAusdruck.Unbekannt
            if(e.operatorId=="tupel.projektion") {
                val index=(a.getOrNull(1) as? FormelAusdruck.Literal)?.wert as? RationaleZahl
                val i=index?.let {runCatching {it.zähler.intValueExact()-1}.getOrNull()}
                if(index?.nenner==java.math.BigInteger.ONE && i!=null) return (a.firstOrNull()?.let(::typ) as? de.TeutonStudio.TypSystem.TypAusdruck.Parameterisiert)?.takeIf {it.konstruktor==MathematischeTypen.Tupel}?.argumente?.getOrNull(i) ?: de.TeutonStudio.TypSystem.TypAusdruck.Unbekannt
            }
        }
        return atom(e.typ)
    }
    fun verlange(e:FormelAusdruck,t:de.TeutonStudio.TypSystem.TypAusdruck) {val p=MathematischeTypen.typSystem.prüfe(typ(e),t);if(p is de.TeutonStudio.TypSystem.TypPrüfung.Inkompatibel && MathematischeTypen.typSystem.prüfe(t,typ(e))!=de.TeutonStudio.TypSystem.TypPrüfung.Kompatibel) fehler+="Inkompatibler Formeltyp: ${p.grund}"}
    fun besuche(e:FormelAusdruck) {
        if(e !is FormelAusdruck.Operation) return
        val a=e.argumente.sortedBy {it.position}.map {it.ausdruck}
        when(e.operatorId) {
            "methode.anwenden" -> {
                if(a.isEmpty()) fehler+="Methodenausdruck fehlt."
                else {
                    verlange(a[0],atom(FormelTyp.METHODE))
                    val t=typ(a[0]) as? de.TeutonStudio.TypSystem.TypAusdruck.Parameterisiert
                    val args=(t?.takeIf {it.konstruktor==MathematischeTypen.Methode}?.argumente?.firstOrNull() as? de.TeutonStudio.TypSystem.TypAusdruck.Parameterisiert)?.argumente
                    if(args!=null) {if(args.size!=a.size-1) fehler+="Falsche Argumentanzahl der Methodenanwendung." else args.zip(a.drop(1)).forEach {(t,v) ->verlange(v,t)}}
                }
            }
            "tupel.projektion" -> if(a.size!=2) fehler+="Tupelprojektion benötigt Tupel und Index." else {verlange(a[0],atom(FormelTyp.TUPEL));verlange(a[1],atom(FormelTyp.ZAHL))}
            "tupel.konstruktion","formel.gruppierung" -> Unit
            else -> {
                val taste=strukturtasten[e.operatorId]
                if(taste!=null && taste.argumente.size==a.size) taste.argumente.zip(a).forEach {(t,v) ->if(t.typ!=FormelTyp.OBJEKT) verlange(v,atom(t.typ))}
                else if(e.operatorId in zahlentasten) a.forEach {verlange(it,atom(FormelTyp.ZAHL))}
            }
        }
        a.forEach(::besuche)
    }
    besuche(f.wurzel)
    return fehler.distinct()
}

fun bereichFürTyp(t:TypAusdruck):MengenAusdruck = when(t) {
    is TypAusdruck.Atom -> when(t.id) {MathematischeTypen.Reell -> ReelleZahlen;MathematischeTypen.Komplex -> KomplexeZahlen;MathematischeTypen.Rational -> RationaleZahlen;MathematischeTypen.Ganz -> GanzeZahlen;MathematischeTypen.Natuerlich -> NatürlicheZahlen;MathematischeTypen.Aussage -> WahrheitsMenge;else -> FehlendeObermenge("Typ")}
    is TypAusdruck.Parameterisiert -> when(t.konstruktor) {
        MathematischeTypen.Tupel -> Tupelraum(t.argumente.map(::bereichFürTyp))
        MathematischeTypen.Methode -> {require(t.argumente.size==2);Abbildungsmenge(bereichFürTyp(t.argumente[1]),bereichFürTyp(t.argumente[0]))}
        MathematischeTypen.SpaltenVektor,MathematischeTypen.ZeilenVektor -> {require(t.argumente.size==2);Vektorraum(if(t.konstruktor==MathematischeTypen.SpaltenVektor) VektorOrientierung.Spalte else VektorOrientierung.Zeile,(t.argumente[1] as TypAusdruck.Literal).wert.toInt(),bereichFürTyp(t.argumente[0]))}
        MathematischeTypen.Matrix -> {
            require(t.argumente.size == 3)
            val n = (t.argumente[1] as? TypAusdruck.Literal)?.wert?.toIntOrNull()
            val m = (t.argumente[2] as? TypAusdruck.Literal)?.wert?.toIntOrNull()
            // Ein Typliteral enthält keine Quellbindung. Daraus keine neue Variable erfinden.
            if (n == null || m == null) FehlendeObermenge("Symbolische Matrixdimension ohne Quellenvertrag")
            else Matrizenraum(n, m, bereichFürTyp(t.argumente[0]))
        }
        MathematischeTypen.Tensor -> {require(t.argumente.size>1);Tensorraum(bereichFürTyp(t.argumente[0]),t.argumente.drop(1).map {RationaleZahl.parse((it as TypAusdruck.Literal).wert)})}
        MathematischeTypen.Menge -> Potenzmenge(bereichFürTyp(t.argumente.single()))
        else -> FehlendeObermenge("Strukturtyp")
    }
    is TypAusdruck.Vereinigung -> Vereinigung(t.alternativen.map(::bereichFürTyp))
    else -> FehlendeObermenge("Unbekannter Typ")
}

