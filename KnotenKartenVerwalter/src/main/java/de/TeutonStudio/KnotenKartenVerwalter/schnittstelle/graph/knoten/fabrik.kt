package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDatenAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDatenKnoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BasisObjektKnoten
//import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.AnschlussKnotenDaten
//import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenAnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKarte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKnoten
//import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.Karte

/** Schlüssel einer Knotenimplementierung in der [KnotenFabrik]. */
typealias KnotenArt = String

/** Fabrikvertrag für Knotenimplementierungen. */
typealias KnotenFabrik = Map<KnotenArt,KnotenKonstruktor>

/** Konstruktorfunktion eines konkreten [Knoten]. */
typealias KnotenKonstruktor = (graph: Graph, daten: GraphDatenKnoten, besitzer: GraphDatenObjektKarte<*>) -> GraphDatenObjektKnoten<*>

/**
 * Erzeugt den zur Datenklasse passenden Knoten.
 *
 * @receiver Fabrikzuordnung der bekannten Knotenarten
 */
public fun KnotenFabrik.erzeugeKnoten(graph: Graph, daten: GraphDatenKnoten, besitzer: GraphDatenObjektKarte<*>): GraphDatenObjektKnoten<*>? = this[daten.klasse]?.invoke(graph,daten,besitzer)

@Suppress("UNCHECKED_CAST")
val BasisKnotenFabrik: KnotenFabrik = mapOf(
    BasisObjektKnoten.KNOTEN_ART to ::BasisObjektKnoten as KnotenKonstruktor,
//    EingabeKnoten.KNOTEN_ART to ::EingabeKnoten as KnotenKonstruktor,
//    AusgabeKnoten.KNOTEN_ART to ::AusgabeKnoten as KnotenKonstruktor,
)
