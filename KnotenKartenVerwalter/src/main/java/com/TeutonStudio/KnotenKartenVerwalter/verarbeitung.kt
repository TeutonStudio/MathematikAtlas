package com.TeutonStudio.KnotenKartenVerwalter

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import com.TeutonStudio.KnotenKartenVerwalter.daten.AuswahlDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.AnsichtsfensterDaten
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
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Karte
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.KartenKontextAktion
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.KartenTreffer
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Knoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Verbindung
import kotlin.math.roundToInt

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
typealias Rechteck = Offset

/**
 *
 */
public fun KartenPosition.zuBild(daten: AnsichtsfensterDaten): BildschirmPosition {
    return TODO("Formel für Karte zu Bildschrim in bezug auf positionen definieren")
}

/**
 * Rechnet die Weltposition eines Knotens in eine Bildschirmposition um.
 *
 * Diese Hilfsfunktion wird von älterem UI-Code verwendet. Neue Kartenlogik nutzt
 * zusätzlich die Transformationsfunktionen in `schnittstelle/Karte.kt`.
 */
public fun BildschirmPosition.zuIntOffset(zustand: KarteZustand): IntOffset = IntOffset(
    x = (this.x * zustand.zoom + zustand.verschiebung.x).roundToInt(),
    y = (this.y * zustand.zoom + zustand.verschiebung.y).roundToInt(),
)

/**
 * Kurzform für eine reine Verschiebung ohne expliziten Zoom.
 */
public fun BildschirmPosition.zuIntOffset(verschiebung: Offset): IntOffset = zuIntOffset(
    KarteZustand(verschiebung = verschiebung),
)

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

public fun idReferenz.hatGleichenKnoten(other: idReferenz): Boolean {
    val knotenIds1 = this.erhalteKnotenIds()
    val knotenIds2 = other.erhalteKnotenIds()
    val ersterKnoten = knotenIds1.first == knotenIds2.first || knotenIds1.first == knotenIds2.second
    val zweiterKnoten = knotenIds1.second == knotenIds2.first || knotenIds1.second == knotenIds2.second
    return ersterKnoten || zweiterKnoten
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
typealias KartenKonstruktor = (KarteDaten) -> Karte

public fun KartenFabrik.erzeugeKarte(daten: KarteDaten): Karte? = this[daten.klasse]?.invoke(daten)
