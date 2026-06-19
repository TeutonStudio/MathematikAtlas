package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen

import androidx.compose.runtime.State
import de.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BasisObjektVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BezierObjektVerbindung
//import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BezierVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
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
    BasisObjektVerbindung.VERBINDUNG_ART to ::BasisObjektVerbindung as VerbindungKonstruktor,
    BezierObjektVerbindung.VERBINDUNG_ART to ::BezierObjektVerbindung as VerbindungKonstruktor
)
