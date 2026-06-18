package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen

import androidx.compose.runtime.State
import de.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.VerbindungDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektVerbindung

/** Schlüssel einer Verbindungsimplementierung in der [VerbindungFabrik]. */
typealias VerbindungArt = String

/** Fabrikvertrag für Verbindungsimplementierungen. */
typealias VerbindungFabrik = Map<VerbindungArt,VerbindungKonstruktor>

/** Konstruktorfunktion einer konkreten [Verbindung]. */
typealias VerbindungKonstruktor = (graph: Graph, daten: GraphDatenVerbindung, start: State<KartenPosition>, ende: State<KartenPosition>) -> GraphDatenObjektVerbindung<*>

/**
 * Erzeugt die zur Datenklasse passende Verbindung.
 *
 * @receiver Fabrikzuordnung der bekannten Verbindungsarten
 */
public fun VerbindungFabrik.erzeugeVerbindung(
    graph: Graph, daten: GraphDatenVerbindung,
    start: State<KartenPosition>, ende: State<KartenPosition>,
): GraphDatenObjektVerbindung<*>? = this[daten.klasse]?.invoke(graph,daten,start,ende)

@Suppress("UNCHECKED_CAST")
val BasisVerbindungFabrik: VerbindungFabrik = mapOf(
    BasisVerbindung.VERBINDUNG_ART to ::BasisVerbindung as VerbindungKonstruktor,
    BezierVerbindung.VERBINDUNG_ART to ::BezierVerbindung as VerbindungKonstruktor
)
