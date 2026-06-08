package com.TeutonStudio.KnotenKartenVerwalter

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntRect
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
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

public fun <K, V> Iterable<Pair<K, V>>.toMutableMap() = this.toMap().toMutableMap()
public fun <T> Pair<T,T>.enthält(value: T): Boolean = first == value || second == value
public fun <T> Pair<T,T>.toSet(): Set<T> = setOf(first,second)

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
typealias Rechteck = Rect

public fun Rechteck(breite: Float, tiefe: Float, position: KartenPosition): Rechteck= Rect(position,position+ Offset(breite,tiefe))

/**
 *
 */
public fun KartenPosition.zuBild(ansicht: AnsichtsfensterDaten): BildschirmPosition {
    val zoom = ansicht.erhalteZoomfaktor().coerceAtLeast(0.01f)
    val verschiebung = ansicht.erhalteVerschiebung()

    return IntOffset(
        x = (x * zoom + verschiebung.x).roundToInt(),
        y = (y * zoom + verschiebung.y).roundToInt(),
    )
}


/**
 *
 */
public fun BildschirmPosition.zuKarte(ansicht: AnsichtsfensterDaten): KartenPosition {
    val zoom = ansicht.erhalteZoomfaktor().coerceAtLeast(0.01f)
    val verschiebung = ansicht.erhalteVerschiebung()

    return Offset(
        x = (x - verschiebung.x) / zoom,
        y = (y - verschiebung.y) / zoom,
    )
}


/**
 *
 */
public fun KartenPosition.aufKnoten(daten: KnotenDaten): Boolean = daten.dimension.contains(this)

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

/*private fun Modifier.alignFürKante(
    kante: AnschlussKante,
    skalierung: Float,
): Modifier = when (kante) {
    AnschlussKante.Links ->
        this.align(Alignment.CenterStart)
            .fillMaxHeight()
            .offset(x = (-5f * skalierung).dp)

    AnschlussKante.Rechts ->
        this.align(Alignment.CenterEnd)
            .fillMaxHeight()
            .offset(x = (5f * skalierung).dp)

    AnschlussKante.Oben ->
        this.align(Alignment.TopCenter)
            .fillMaxWidth()
            .offset(y = (-5f * skalierung).dp)

    AnschlussKante.Unten ->
        this.align(Alignment.BottomCenter)
            .fillMaxWidth()
            .offset(y = (5f * skalierung).dp)
}*/

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
typealias KnotenAnschlüsse = MutableMap<AnschlussDaten,Int>
/**
 * Der Modifier für einen Anschluss, abhängig von Anschluss und index
 */
typealias AnschlussModifier = (AnschlussDaten, Int) -> Modifier

// public fun KnotenAnschlüsse.filterKante(kante: AnschlussKante): KnotenAnschlüsse = this.filter { (daten,idx) -> daten.kante == kante }.toMutableMap()
public fun Map<Anschluss,Int>.filterKante(kante: AnschlussKante): Map<Anschluss,Int> = this.filter { (a,idx) -> a.daten.kante == kante }.toMutableMap()
public fun KnotenAnschlüsse.filterRichtung(richtung: AnschlussRichtung): KnotenAnschlüsse = this.filter { (daten,idx) -> when (richtung) {
    AnschlussRichtung.Eingang -> daten is EingangDaten
    AnschlussRichtung.Ausgang -> daten is AusgangDaten
} }.toMutableMap()

// Verbindung

typealias idReferenz = Pair<Pair<String,String>,Pair<String,String>>

public fun idReferenz.erhalteKnotenIds(): Pair<String,String> = this.first
public fun idReferenz.erhalteAnschlussIds(): Pair<String,String> = this.second

public fun idReferenz.hatKnotenId(id: String): Boolean = this.erhalteKnotenIds().enthält(id)
public fun idReferenz.istVerbunden(daten: KnotenDaten): Boolean = this.hatKnotenId(daten.id)

public fun idReferenz.hatAnschlussId(id: String): Boolean = this.erhalteAnschlussIds().enthält(id)
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

@Composable @JvmName("IterVerbindungen2Composable")
public fun Iterable<Verbindung>.zuComposable(modifier: (VerbindungDaten) -> Modifier) = this.forEach { it.zuComposable(modifier(it.daten)) }

// Knoten

@Composable @JvmName("IterKnoten2Composable")
public fun Iterable<Knoten>.zuComposable(
    modifierKnoten: (KnotenDaten) -> Modifier,
    modifierAnschluss: (KnotenDaten) -> AnschlussModifier,
) = this.forEach { it.zuComposable(modifierKnoten(it.daten),modifierAnschluss(it.daten),1f) }

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

public fun KarteZustand.verschiebe(delta: Offset) { ansicht = Triple(ansicht.first,ansicht.second + delta.x, ansicht.third + delta.y) }
public fun KarteZustand.zoome(delta: Float) { ansicht = Triple(ansicht.first + delta,ansicht.second,ansicht.third) }
public fun KarteZustand.transformiere(verschiebung: Offset,zoom: Float) { ansicht = Triple(ansicht.first + zoom,ansicht.second + verschiebung.x, ansicht.third + verschiebung.y) }

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
    val kartePos = pos.zuKarte(this.ansicht) // TODO umrechnung der Bildschirm position [pos] abhängig von ansicht zoom und ansicht verschiebung.
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
typealias KnotenKonstruktor = (daten: KnotenDaten,besitzer: Karte) -> Knoten

public fun KnotenFabrik.erzeugeKnoten(daten: KnotenDaten,besitzer: Karte): Knoten? = this[daten.klasse]?.invoke(daten,besitzer)

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
