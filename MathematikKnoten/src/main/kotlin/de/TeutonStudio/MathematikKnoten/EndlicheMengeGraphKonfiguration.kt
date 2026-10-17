package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister
import de.TeutonStudio.TypSystem.*
import de.TeutonStudio.MathematikRechenSystem.kern.MathematischeTypen

const val ENDLICHE_MENGE_ART = "mathematik.endlicheMenge"
const val ENDLICHE_MENGE_EINGABE = "eingabeModus"
const val ENDLICHE_MENGE_LOKAL = "lokal"
const val ENDLICHE_MENGE_GRAPH = "graph"
const val ENDLICHE_MENGE_ANZAHL = "endlicheMenge.anzahl"

fun endlicheMengeEingabe(k:KnotenDaten):String = (k.parameter[ENDLICHE_MENGE_EINGABE] ?: ENDLICHE_MENGE_LOKAL).also {
    require(it in setOf(ENDLICHE_MENGE_LOKAL,ENDLICHE_MENGE_GRAPH)) { "Unbekannte Mengeneingabe '$it'." }
}
fun endlicheMengeGraphAnzahl(k:KnotenDaten):Int {
    val roh=k.parameter[ENDLICHE_MENGE_ANZAHL] ?: error("Die Anzahl der Graph-Elemente fehlt.")
    return (roh.toIntOrNull() ?: error("Ungültige Anzahl der Graph-Elemente '$roh'.")).also {
        require(it in 0..64) { "0 bis 64 Graph-Elemente sind zulässig." }
    }
}

/** Ein expliziter Moduswechsel; Verbindungs-/Lokalwertverluste prüft der vorhandene KonfigurationsPlan. */
fun konfiguriereEndlicheMenge(k:KnotenDaten,modus:String,anzahl:Int=1):KnotenDaten {
    require(k.art==ENDLICHE_MENGE_ART && modus in setOf(ENDLICHE_MENGE_LOKAL,ENDLICHE_MENGE_GRAPH))
    require(anzahl in 0..64)
    require(k.anschlüsse.none {it.richtung==AnschlussRichtung.Neutral}) { "Neutrale Zusatzanschlüsse sind nicht als Mengenelemente umsetzbar." }
    val ausgang=k.anschlüsse.singleOrNull {it.richtung==AnschlussRichtung.Ausgang && it.name=="menge"}
        ?: error("Der Mengenausgang fehlt oder ist mehrdeutig.")
    require(k.anschlüsse.count {it.richtung==AnschlussRichtung.Ausgang}==1 &&
        ausgang.belegung==AnschlussBelegung.Einzeln && !ausgang.kannSichErweitern && ausgang.sammelAnschluss==null &&
        k.anschlüsse.map {it.id}.distinct().size==k.anschlüsse.size) { "Der Anschlussvertrag der endlichen Menge ist mehrdeutig." }
    val eingänge=if(modus==ENDLICHE_MENGE_LOKAL) emptyList() else List(anzahl) {i ->
        val name="element${i+1}"
        val vorhandener=k.anschlüsse.singleOrNull {it.richtung==AnschlussRichtung.Eingang && it.name==name}
        (vorhandener ?: AnschlussDaten(name=name,richtung=AnschlussRichtung.Eingang,kante=AnschlussKante.Links,
                art=MathematikAnschlussArten.Objekt.id,reihenfolge=i)).alsGraphElementEingang()
    }
    require(eingänge.none {it.belegung!=AnschlussBelegung.Einzeln || it.kannSichErweitern || it.sammelAnschluss!=null}) { "Graph-Elemente benötigen einzelne Pflichtanschlüsse." }
    val neu = k.copy(parameter=k.parameter+(ENDLICHE_MENGE_EINGABE to modus)+if(modus==ENDLICHE_MENGE_GRAPH) mapOf(ENDLICHE_MENGE_ANZAHL to anzahl.toString()) else emptyMap(),
        anschlüsse=eingänge+ausgang)
    prüfeEndlicheMengeForm(neu)
    return neu
}

/** Such- und Definitionsvorlage für denselben vorhandenen Konstruktor. */
fun endlicheMengeGraphVorlage(anzahl: Int = 1): KnotenVorlage {
    val basis = MathematikKnotenVorlagen.EndlicheMenge
    val k = konfiguriereEndlicheMenge(basis.erzeuge(GraphPunkt.Zero), ENDLICHE_MENGE_GRAPH, anzahl)
    return basis.copy(name = if (anzahl == 1) "Einzelmenge" else "Endliche Menge aus Graph-Elementen",
        anschlüsse = k.anschlüsse, standardParameter = k.parameter, standardEigenschaften = k.eigenschaften)
}

/** Der Konstruktor liefert eine Menge, unabhängig von den Arten seiner vollständigen Elemente. */
fun prüfeEndlicheMengeForm(k: KnotenDaten) {
    require(k.anschlüsse.map {it.id}.distinct().size == k.anschlüsse.size) { "Anschlussidentitäten sind nicht eindeutig." }
    require(k.anschlüsse.filter {it.richtung==AnschlussRichtung.Eingang}.all {
        it.belegung==AnschlussBelegung.Einzeln && !it.kannSichErweitern && !it.dynamischErzeugt && it.sammelAnschluss==null
    }) { "Graph-Elemente benötigen feste einzelne Pflichtanschlüsse." }
    val ausgang = k.anschlüsse.singleOrNull { it.richtung == AnschlussRichtung.Ausgang }
        ?: error("Genau ein Mengenausgang ist erforderlich.")
    require(ausgang.name == "menge" && k.anschlüsse.none { it.richtung == AnschlussRichtung.Neutral } &&
        ausgang.belegung == AnschlussBelegung.Einzeln && !ausgang.kannSichErweitern && ausgang.sammelAnschluss == null) {
        "Der Ausgangsvertrag der endlichen Menge ist ungültig."
    }
    val register = AnschlussArtRegister(MathematikAnschlussArten.alle)
    fun mengenArt(art: AnschlussArtId) = register.istUnterart(art, MathematikAnschlussArten.Menge.id)
    require((setOf(ausgang.art) + ausgang.zulässigeArten).all(::mengenArt)) { "Der Ausgang 'menge' muss eine Menge deklarieren." }
    fun mengenTyp(typ: TypAusdruck): Boolean = when (typ) {
        TypAusdruck.Unbekannt -> true
        is TypAusdruck.Atom -> MathematischeTypen.istAtomUntertyp(typ.id, MathematischeTypen.Menge)
        is TypAusdruck.Parameterisiert -> typ.konstruktor == MathematischeTypen.Menge && typ.argumente.size == 1
        is TypAusdruck.Vereinigung -> typ.alternativen.all(::mengenTyp)
        else -> false
    }
    require(mengenTyp(ausgang.vertrag.typ)) { "Der semantische Ausgangstyp widerspricht der Menge." }
    val inputs = k.anschlüsse.filter { it.richtung == AnschlussRichtung.Eingang }.associateBy { it.name }
    fun mengenRolle(name: String) = inputs[name]?.let { mengenArt(it.art) && mengenTyp(it.vertrag.typ) } == true
    require(ausgang.artFolgtEingang?.let(::mengenRolle) != false && ausgang.artVereinigtEingänge.all(::mengenRolle) &&
        ausgang.artAbbildungVonEingang?.abbildung?.values?.all(::mengenArt) != false &&
        ausgang.artPriorisiertEingänge?.prioritäten?.all(::mengenArt) != false) { "Eine Ausgangsartregel widerspricht dem festen Mengenvertrag." }
    val regel = ausgang.typInferenz
    val gültig = when (regel) {
        null -> true
        is TypInferenzRegel.FolgtEingang -> mengenRolle(regel.eingang)
        is TypInferenzRegel.GemeinsameOberart -> regel.eingänge.all(::mengenRolle)
        is TypInferenzRegel.Vereinigung -> regel.eingänge.all(::mengenRolle)
        is TypInferenzRegel.AbbildungVonEingang -> regel.abbildung.values.all(::mengenTyp)
        is TypInferenzRegel.Priorisierung -> regel.prioritäten.all(::mengenTyp)
        else -> false
    }
    require(gültig) { "Die Ausgangstypregel ist für den festen Mengenvertrag nicht nachgewiesen." }
}

/** Breite historische Objektports erlauben mathematische Methoden über die vorhandene Laufzeit-Capability. */
internal fun AnschlussDaten.alsGraphElementEingang(): AnschlussDaten =
    if(art==MathematikAnschlussArten.Objekt.id && zulässigeArten.isEmpty() && vertrag.typ==TypAusdruck.Unbekannt &&
        vertrag.anforderungen.isEmpty() && typInferenz==null && artFolgtEingang==null && artVereinigtEingänge.isEmpty() &&
        artAbbildungVonEingang==null && artPriorisiertEingänge==null)
        copy(art=MathematikAnschlussArten.AtlasWert.id,zulässigeArten=setOf(MathematikAnschlussArten.Objekt.id,MathematikAnschlussArten.Methode.id))
    else this
