package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import de.TeutonStudio.TypSystem.AnschlussVertrag

const val FORMEL_KONTEXT_EIGENSCHAFT = "formel.kontext"
const val FORMEL_QUELLEN_PORTS = "formel.quellenPorts"

/** Fehlende historische Angaben bleiben unbekannt, beschädigte neue Daten werden nicht überschrieben. */
fun ladeGebundeneKnotenFormel(k:KnotenDaten):GebundeneFormel? {
    val roh=(k.eigenschaften[FORMEL_KONTEXT_EIGENSCHAFT] as? KnotenEigenschaft.Text)?.wert
    if(roh!=null) return FormelKontextCodec.dekodieren(roh)
    val ast=ladeStrukturRechnerFormel(k) ?: (k.eigenschaften[FORMEL_AST_EIGENSCHAFT] as? KnotenEigenschaft.Text)?.wert?.let(StrukturFormelCodec::dekodieren) ?: return null
    return FormelBearbeitungsEntwurf.aus(ast).formel
}
fun konfiguriereGebundeneKnotenFormel(k:KnotenDaten,f:GebundeneFormel,zeichnung:KnotenEigenschaft?=null):KnotenDaten {
    FormelBearbeitungsEntwurf(f).geprüft()
    require(k.art==ZAHLENRECHNER_ART || StrukturRechnerKnotenFamilie.fuerKnotenArt(k.art)!=null)
    val alt=ladeGebundeneKnotenFormel(k)
    val zuordnung=(k.eigenschaften[FORMEL_QUELLEN_PORTS] as? KnotenEigenschaft.Objekt)?.felder.orEmpty().mapValues { (_,v) -> (v as KnotenEigenschaft.Text).wert }
    val benutzt=mutableSetOf<AnschlussId>()
    val portNamen=mutableSetOf<String>()
    val eingänge=f.quellen.filter { it.art!=FormelVariablenArt.GEBUNDEN }.mapIndexed { i,q ->
        val port=k.anschlüsse.firstOrNull {it.semantischeId==q.id} ?: k.anschlüsse.firstOrNull { it.id.wert==zuordnung[q.id] } ?: if(alt?.quellen?.any { it.id==q.id }==true || alt==null) k.anschlüsse.firstOrNull { it.name==q.anzeigeName && it.richtung==AnschlussRichtung.Eingang && it.id !in benutzt } else null
        val grundName=if(f.quellen.count {it.anzeigeName==q.anzeigeName}>1) "${q.anzeigeName} · ${f.quellen.indexOf(q)+1}" else q.anzeigeName
        var portName=grundName;var suffix=1
        while(!portNamen.add(portName)) portName="$grundName · ${suffix++}"
        val art=anschlussFürFormelTyp(q.typ)
        (port ?: AnschlussDaten(name=q.anzeigeName,richtung=AnschlussRichtung.Eingang,kante=AnschlussKante.Links,art=art)).copy(
            name=portName,semantischeId=q.id,art=art,reihenfolge=i,kannSichErweitern=false,dynamischErzeugt=false,
            vertrag=AnschlussVertrag(q.methodenSignatur?.typAusdruck ?: q.vollstaendigerTyp),zulässigeArten=emptySet()).also { benutzt+=it.id }
    }
    val ausgang=(k.anschlüsse.firstOrNull { it.richtung==AnschlussRichtung.Ausgang } ?: AnschlussDaten(name="wert",richtung=AnschlussRichtung.Ausgang,kante=AnschlussKante.Rechts,art=anschlussFürFormelTyp(f.wurzel.typ))).copy(name="wert",art=anschlussFürFormelTyp(f.wurzel.typ),artFolgtEingang=null,artVereinigtEingänge=emptyList(),zulässigeArten=emptySet())
    val familie=StrukturRechnerKnotenFamilie.fuerKnotenArt(k.art)
    val latex=FormelLatexCodec.exportiere(f.wurzel)
    return k.copy(name="Formel",anschlüsse=eingänge+ausgang,
        parameter=k.parameter+mapOf((if(k.art==ZAHLENRECHNER_ART) ZAHLENRECHNER_OPERATOR else RECHNER_OPERATOR_PARAMETER) to (familie?.formelOperatorId ?: ZAHLENRECHNER_FORMEL_ID),
            ZAHLENRECHNER_FORMEL_LATEX to latex,STRUKTUR_RECHNER_FORMEL_AUSDRUCK to StrukturFormelCodec.kodieren(f.wurzel),
            STRUKTUR_RECHNER_FORMEL_LATEX to latex),
        eigenschaften=k.eigenschaften+mapOf(FORMEL_QUELLEN_PORTS to KnotenEigenschaft.Objekt(f.quellen.filter {it.art!=FormelVariablenArt.GEBUNDEN}.zip(eingänge).associate { (q,p) -> q.id to KnotenEigenschaft.Text(p.id.wert) }),FORMEL_KONTEXT_EIGENSCHAFT to KnotenEigenschaft.Text(FormelKontextCodec.kodieren(f)),FORMEL_AST_EIGENSCHAFT to KnotenEigenschaft.Text(StrukturFormelCodec.kodieren(f.wurzel)),FORMEL_INTERPRETATION_EIGENSCHAFT to KnotenEigenschaft.Text(latex))+listOfNotNull(zeichnung?.let { FORMEL_ZEICHNUNG_EIGENSCHAFT to it }).toMap())
}
fun anschlussFürFormelTyp(t:FormelTyp):AnschlussArtId = when(t) {
    FormelTyp.ZAHL -> MathematikAnschlussArten.Zahl.id;FormelTyp.MENGE -> MathematikAnschlussArten.Menge.id;FormelTyp.AUSSAGE -> MathematikAnschlussArten.Aussage.id
    FormelTyp.TUPEL -> MathematikAnschlussArten.Tupel.id;FormelTyp.VEKTOR -> MathematikAnschlussArten.SpaltenVektor.id;FormelTyp.MATRIX -> MathematikAnschlussArten.Matrix.id;FormelTyp.TENSOR -> MathematikAnschlussArten.Tensor.id
    FormelTyp.METHODE -> MathematikAnschlussArten.Methode.id;else -> MathematikAnschlussArten.Objekt.id
}
fun werteGebundeneKnotenFormelAus(k:KnotenAuswertungsKontext):KnotenAuswertungsErgebnis {
    val f=requireNotNull(ladeGebundeneKnotenFormel(k.knoten)) { "Formelkontext fehlt." }
    val ports=(k.knoten.eigenschaften[FORMEL_QUELLEN_PORTS] as? KnotenEigenschaft.Objekt)?.felder.orEmpty()
    val eingangsQuellen=k.eingänge.values.flatMap { w ->
        val bekannte=w.gebundeneFormel?.quellen.orEmpty()
        val parameter=w.mathematischesObjekt("Formeleingang").freieMethodenParameter()
        bekannte+w.variablenQuellen.mapNotNull { q ->
            val kandidaten=parameter.filter { it.name==q.name }
            val id=q.bindungsId ?: kandidaten.singleOrNull()?.bindungsId ?: q.knotenId.wert
            bekannte.singleOrNull {it.id==id} ?: FormelVariablenQuelle(id,q.name,
                if(q.alsMethodenParameter) FormelVariablenArt.FREI else FormelVariablenArt.EXTERN,
                formelTypFürTyp(q.werteVorrat.elementTypAusdruck()),q.werteVorrat,
                methodenSignatur=(kandidaten.singleOrNull() as? MethodenVariable)?.signatur)
        }
    }
    val bindungen=f.quellen.filter { it.art!=FormelVariablenArt.GEBUNDEN }.mapNotNull { q ->
        val portId=(ports[q.id] as? KnotenEigenschaft.Text)?.wert
        val name=(k.knoten.anschlüsse.singleOrNull {it.semantischeId==q.id} ?: k.knoten.anschlüsse.singleOrNull {it.id.wert==portId})?.name ?: q.anzeigeName
        k.eingänge[name]?.let { w ->
            val original=w.mathematischesObjekt("Formeleingang ${q.anzeigeName}")
            val alt=original.freieMethodenParameter().filter {it.bindungsId==null}
            val normalisierung=alt.associate { p ->
                val quelle=eingangsQuellen.filter {it.anzeigeName==p.name}.distinct().singleOrNull()
                    ?: error("Historische Eingangsvariable ${p.name} ist nicht eindeutig gebunden.")
                require(p is Variable) { "Die historische strukturierte Eingangsbindung benötigt eine explizite Identität." }
                p.name to p.copy(bindungsId=quelle.id)
            }
            q.id to ersetze(original,normalisierung)
        }
    }.toMap()
    val historischeBindungen=k.eingänge.values.flatMap {it.mathematischesObjekt("Formeleingang").freieMethodenParameter()+it.annahmen.flatMap {a -> a.freieMethodenParameter()}}
        .filterIsInstance<Variable>().filter {it.bindungsId==null}.associate {p ->
            val q=eingangsQuellen.filter {it.anzeigeName==p.name}.distinct().singleOrNull() ?: error("Historische Annahme ist nicht eindeutig gebunden: ${p.name}.")
            p.name to p.copy(bindungsId=q.id)
        }
    val eingangsAnnahmen=k.eingänge.values.flatMap {it.annahmen}.map {ersetze(it,historischeBindungen)}.toSet()
    val r=f.copy(annahmen=f.annahmen+eingangsAnnahmen).werteAus(bindungen) { op,a ->
        val bedingungen=mutableSetOf<Aussage>()
        val w=werteFormelOperationAus(op.operatorId,op.argumente.sortedBy {it.position}.map {it.rollenId}.zip(a).toMap(),k.knoten,bedingungen) as? MathematischesObjekt ?: error("Der Operator liefert keinen mathematischen Formelwert.")
        FormelKernErgebnis(w,bedingungen)
    }
    val freieIds=(r.objekt.freieMethodenParameter()+r.bedingungen.flatMap {it.freieMethodenParameter()}).mapNotNull {it.bindungsId}.toSet()
    val deklarationen=(f.quellen+eingangsQuellen).groupBy {it.id}
    deklarationen.forEach { (id,qs) -> require(qs.map {it.copy(anzeigeName="Quelle")}.distinct().size==1) { "Widersprüchliche Formelquelle $id." } }
    val freie=deklarationen.values.map {it.last()}.filter {
        (it.id in freieIds || (it.id !in bindungen && it in f.quellen)) && it.art!=FormelVariablenArt.GEBUNDEN
    }
    require(freieIds.all {id -> freie.any {it.id==id}}) { "Freie Eingangsvariable ohne vollständigen Quellenvertrag." }
    val quellen=freie.mapIndexed {i,q -> VariablenQuelle(k.knoten.id,q.anzeigeName,q.werteVorrat ?: FehlendeObermenge(q.typ.name),alsMethodenParameter=q.art==FormelVariablenArt.FREI,bindungsId=q.id,reihenfolge=i) }+k.eingänge.values.flatMap {it.variablenQuellen}
    val snapshot=GebundeneFormel(FormelAusdruck.Literal(f.wurzel.id,r.objekt,f.wurzel.typ,r.übersetzungen),freie,emptyMap(),annahmen=r.bedingungen)
    return KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(r.objekt,r.bedingungen,variablenQuellen=quellen.distinct(),übersetzungen=r.übersetzungen,gebundeneFormel=snapshot)),eingänge=k.eingänge,
        formelAusgaben=setOf("wert"),bezeugteAusgaben=if(f.wurzel is FormelAusdruck.Literal) setOf("wert") else emptySet())
}
