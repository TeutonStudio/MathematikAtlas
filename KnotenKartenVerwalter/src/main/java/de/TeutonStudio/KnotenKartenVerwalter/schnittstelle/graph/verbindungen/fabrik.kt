package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen

import androidx.compose.runtime.State
import de.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import de.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.VerbindungDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph

/** Schlüssel einer Verbindungsimplementierung in der [VerbindungFabrik]. */
typealias VerbindungArt = String

/** Fabrikvertrag für Verbindungsimplementierungen. */
typealias VerbindungFabrik = Map<VerbindungArt,VerbindungKonstruktor>

/** Konstruktorfunktion einer konkreten [Verbindung]. */
typealias VerbindungKonstruktor = (graph: Graph, daten: VerbindungDaten, start: State<KartenPosition>, ende: State<KartenPosition>) -> Verbindung

/**
 * Erzeugt die zur Datenklasse passende Verbindung.
 *
 * @receiver Fabrikzuordnung der bekannten Verbindungsarten
 */
public fun VerbindungFabrik.erzeugeVerbindung(
    graph: Graph, daten: VerbindungDaten,
    start: State<KartenPosition>, ende: State<KartenPosition>,
): Verbindung? = this[daten.klasse]?.invoke(graph,daten,start,ende)

@Suppress("UNCHECKED_CAST")
val BasisVerbindungFabrik: VerbindungFabrik = mapOf(
    BasisVerbindung.VERBINDUNG_ART to ::BasisVerbindung as VerbindungKonstruktor,
    BezierVerbindung.VERBINDUNG_ART to ::BezierVerbindung as VerbindungKonstruktor
)
