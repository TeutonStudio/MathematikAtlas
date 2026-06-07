package com.TeutonStudio.KnotenKartenVerwalter

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.toOffset
import com.TeutonStudio.KnotenKartenVerwalter.daten.AuswahlDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.AusgangDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.EingangDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KarteZustand
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Anschluss
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BasisAusgang
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BasisEingang
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BasisKarte
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphObjekt
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Karte
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.KartenKontextAktion
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Knoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Verbindung
import kotlin.math.roundToInt

fun <T> Pair<T,T>.enthält(value: T): Boolean = first == value || second == value

// Geometrie

/**
 * Position in der Karte
 */
typealias KartenPosition = Offset
/**
 * Position im Knoten
 */
typealias KnotenPosition = Offset
/**
 * Position auf dem Bildschirm
 */
typealias BildschirmPosition = IntOffset
/**
 * Dimensionen eines Rechteck
 */
typealias Rechteck = IntSize

/**
 *
 */
public fun KartenPosition.zuBild(daten: AnsichtsfensterDaten): BildschirmPosition {
    return TODO("Formel für Karte zu Bildschirm in bezug auf positionen definieren")
}

/**
 *
 */
public fun BildschirmPosition.zuKarte(ansicht: AnsichtsfensterDaten): KartenPosition {
    return TODO("Formel für Bildschirm zu Karte in bezug auf positionen deifnieren")
}

/**
 *
 */
public fun KartenPosition.aufKnoten(daten: KnotenDaten): Boolean {
    val abstand = daten.dimension.center.toOffset()
    val eckeMin = daten.position - abstand
    val eckeMax = daten.position + abstand
    val inX = this.x in eckeMin.x..eckeMax.x
    val inY = this.y in eckeMin.y..eckeMax.y
    return inX && inY
}

/**
 *
 */
public fun KartenPosition.aufVerbindung(daten: VerbindungDaten,knoten: Pair<Knoten,Knoten>): Boolean {
    return TODO("formel zu ermittelung der entfernung zur Verbindung zur pos definieren")
}

public fun KnotenPosition.zuKarte(daten: KnotenDaten): KartenPosition {
    return this.zuKarte(daten.position)
}

public fun KnotenPosition.zuKarte(zentrum: KartenPosition): KartenPosition {
    return TODO("definieren, ob der lokale Knotenraum andere skalierung hat als der der Karte.")
}


/**
 * Rechnet die Weltposition eines Knotens in eine Bildschirmposition um.
 *
 * Diese Hilfsfunktion wird von älterem UI-Code verwendet. Neue Kartenlogik nutzt
 * zusätzlich die Transformationsfunktionen in `schnittstelle/Karte.kt`.
 */
public fun BildschirmPosition.zuIntOffset(zustand: KarteZustand): IntOffset = IntOffset(
    x = (this.x * zustand.ansicht.erhalteZoomfaktor() + zustand.ansicht.erhalteVerschiebung().x).roundToInt(),
    y = (this.y * zustand.ansicht.erhalteZoomfaktor() + zustand.ansicht.erhalteVerschiebung().y).roundToInt(),
)

/**
 * Kurzform für eine reine Verschiebung ohne expliziten Zoom.
 */
/*public fun BildschirmPosition.zuIntOffset(verschiebung: Offset): IntOffset = zuIntOffset(
    KarteZustand(verschiebung = verschiebung),
)*/

// Anschluss

/**
 * Seite eines Anschlusses am Knoten.
 *
 * Eingänge liegen links am Knotenrahmen, Ausgänge rechts. Diese Richtung wird
 * sowohl für das Rendering als auch für die Validierung neuer Verbindungen
 * verwendet.
 */
enum class AnschlussRichtung {
    Eingang,
    Ausgang,
}

public fun AnschlussRichtung?.istEingang(): Boolean = this == AnschlussRichtung.Eingang
public fun AnschlussRichtung?.istAusgang(): Boolean = this == AnschlussRichtung.Ausgang

public fun Anschluss.istEingang(): Boolean = this is BasisEingang
public fun Anschluss.istAusgang(): Boolean = this is BasisAusgang

/**
 * Kante eines Knotens, an der ein Anschluss liegt.
 */
enum class AnschlussKante {
    Links,
    Rechts,
    Oben,
    Unten,
}

public fun AnschlussKante.istVertikal(): Boolean = this == AnschlussKante.Links || this == AnschlussKante.Rechts
public fun AnschlussKante.istHorizontal(): Boolean = this == AnschlussKante.Oben || this == AnschlussKante.Unten

/**
 * Anschlüsse eines Knotens und ihre Sortierung
 */
typealias KnotenAnschlüsse = Map<AnschlussDaten,Int>
/**
 * Der Modifier für einen Anschluss, abhängig von Anschluss und index
 */
typealias AnschlussModifier = (AnschlussDaten, Int) -> Modifier

public fun KnotenAnschlüsse.filterKante(kante: AnschlussKante): KnotenAnschlüsse = this.filter { (daten,idx) -> daten.kante == kante }
public fun KnotenAnschlüsse.filterRichtung(richtung: AnschlussRichtung): KnotenAnschlüsse = this.filter { (daten,idx) -> when (richtung) {
    AnschlussRichtung.Eingang -> daten is EingangDaten
    AnschlussRichtung.Ausgang -> daten is AusgangDaten
} }

// Verbindung

typealias idReferenz = Pair<Pair<String,String>,Pair<String,String>>

public fun idReferenz.erhalteKnotenIds(): Pair<String,String> = this.first
public fun idReferenz.erhalteAnschlussIds(): Pair<String,String> = this.second

private fun idReferenz.hatKnotenId(id: String): Boolean = this.erhalteKnotenIds().enthält(id)
public fun idReferenz.istVerbunden(daten: KnotenDaten): Boolean = this.hatKnotenId(daten.id)

private fun idReferenz.hatAnschlussId(id: String): Boolean = this.erhalteAnschlussIds().enthält(id)
public fun idReferenz.istVerbunden(daten: AnschlussDaten): Boolean = this.hatAnschlussId(daten.id)

public fun idReferenz.hatGleichenKnoten(other: idReferenz): Boolean {
    val knotenIds = other.erhalteKnotenIds()
    return this.hatKnotenId(knotenIds.first) || this.hatKnotenId(knotenIds.second)
}

public fun idReferenz.hatGleichenAnschluss(other: idReferenz): Boolean {
    val anschlussIds1 = this.erhalteAnschlussIds()
    val anschlussIds2 = other.erhalteAnschlussIds()
    val ersterAnschluss = anschlussIds1.first == anschlussIds2.first || anschlussIds1.first == anschlussIds2.second
    val zweiterAnschluss = anschlussIds1.second == anschlussIds2.first || anschlussIds1.second == anschlussIds2.second
    return  (ersterAnschluss || zweiterAnschluss) && hatGleichenKnoten(other)
}

// Knoten


// Karten

/**
 * Persistierter Viewport einer Karte.
 *
 * `x` und `y` beschreiben die Verschiebung des Weltkoordinatensystems in
 * Bildschirmkoordinaten. `zoom` beschreibt den Skalierungsfaktor zwischen Welt-
 * und Bildschirmkoordinaten.
 */
typealias AnsichtsfensterDaten = Triple<Float,Float,Float>

public fun AnsichtsfensterDaten(zoom: Float, verschiebung: Offset): AnsichtsfensterDaten = Triple(zoom,verschiebung.x,verschiebung.y)
public fun StandardAnsicht(): AnsichtsfensterDaten = Triple(1f,0f,0f)

public fun AnsichtsfensterDaten.erhalteVerschoben(von: BildschirmPosition): BildschirmPosition = TODO()
public fun AnsichtsfensterDaten.erhalteVerschiebung(): Offset = Offset(this.second,this.third)
public fun AnsichtsfensterDaten.erhalteZoomfaktor(): Float = first

/**
 *
 */
public fun KarteZustand.erhalteNachBildPos(
    pos: BildschirmPosition,
    knoten: Iterable<Knoten>,
    verbindung: Iterable<Verbindung>,
): GraphObjekt? {
    val kartePos = pos.zuKarte(this.ansicht)
        KartenPosition.Zero // TODO umrechnung der Bildschirm position [pos] abhängig von ansicht zoom und ansicht verschiebung.
    var auswahl: GraphObjekt? = null
    knoten.forEach {
        if (kartePos.aufKnoten(it.daten)) auswahl = it
    }
    verbindung.forEach {
        val k = knoten.filter { k -> it.daten.ids.hatKnotenId(k.daten.id) }
        if (kartePos.aufVerbindung(it.daten,k[0] to k[1])) auswahl = it
    }

    return auswahl
}

/**
 * Callback fuer eine geaenderte Knotenposition in Weltkoordinaten.
 */
typealias KartenAktualisierung = (knotenId: String, position: KartenPosition) -> Unit

/**
 * Callback, wenn durch Anschluss-Drag eine neue Verbindung entstanden ist.
 */
typealias VerbindungErstellen = (verbindung: VerbindungDaten) -> Unit

/**
 * Callback fuer Aktionen aus dem Kontextmenue der Karte.
 */
typealias KontextAktionAusführen = (aktion: KartenKontextAktion) -> Unit

/**
 * Callback fuer kontrollierte Auswahl von Knoten und Verbindungen.
 */
typealias AuswahlÄndern = (auswahl: AuswahlDaten) -> Unit



/// Fabriken

// Verbindung

typealias VerbindungArt = String
typealias VerbindungFabrik = Map<VerbindungArt,VerbindungKonstruktor>
typealias VerbindungKonstruktor = (daten:VerbindungDaten,von: Anschluss?, zu: Anschluss?, start: KartenPosition, ende: KartenPosition) -> Verbindung

public fun VerbindungFabrik.erzeugeVerbindung(
    daten: VerbindungDaten,
    anschlüsse: Pair<Anschluss,Anschluss>? = null,
    positionen: Pair<KartenPosition, KartenPosition> = Offset.Zero to Offset.Zero
): Verbindung? = this[daten.klasse]?.invoke(daten, anschlüsse?.first, anschlüsse?.second,positionen.first,positionen.second)

// Anschluss

typealias AnschlussArt = String
typealias AnschlussFabrik = Map<AnschlussArt,AnschlussKonstruktor>
typealias AnschlussKonstruktor = (daten: AnschlussDaten, besitzer: Knoten) -> Anschluss

public fun AnschlussFabrik.erzeugeAnschluss(daten: AnschlussDaten, besitzer: Knoten): Anschluss? = this[daten.klasse]?.invoke(daten,besitzer)

// Knoten

typealias KnotenArt = String
typealias KnotenFabrik = Map<KnotenArt,KnotenKonstruktor>
typealias KnotenKonstruktor = (KnotenDaten) -> Knoten

public fun KnotenFabrik.erzeugeKnoten(daten: KnotenDaten): Knoten? = this[daten.klasse]?.invoke(daten)

// Karten

typealias KartenArt = String
typealias KartenFabrik = Map<KartenArt,KartenKonstruktor>
typealias KartenKonstruktor = (
    daten: KarteDaten,
    zustand: KarteZustand,
    aktualisierung: KartenAktualisierung,
    onVerbindungErstellen: VerbindungErstellen,
    onKontextAktion: KontextAktionAusführen,
    onAuswahlÄndern: AuswahlÄndern,
) -> Karte

fun KartenFabrik.erzeugeKarte(
    daten: KarteDaten,
    zustand: KarteZustand,
    aktualisierung: KartenAktualisierung,
    onVerbindungErstellen: VerbindungErstellen,
    onKontextAktion: KontextAktionAusführen,
    onAuswahlÄndern: AuswahlÄndern,
): Karte {
    val klasse = daten.klasse ?: BasisKarte.KARTEN_ART

    val konstruktor = this[klasse]
        ?: error("Keine Kartenklasse '$klasse'. Bekannte Klassen: ${keys.joinToString()}")

    return konstruktor(
        daten,
        zustand,
        aktualisierung,
        onVerbindungErstellen,
        onKontextAktion,
        onAuswahlÄndern,
    )
}
