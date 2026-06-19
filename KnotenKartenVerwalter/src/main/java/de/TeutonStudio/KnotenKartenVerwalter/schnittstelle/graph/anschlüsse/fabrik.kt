package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse

//import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDatenAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BasisObjektAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKnoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektVerbindung
//import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten


/** Schlüssel einer Anschlussimplementierung in der [AnschlussFabrik]. */
typealias AnschlussArt = String

/** Fabrikvertrag für Anschlussimplementierungen. */
typealias AnschlussFabrik = Map<AnschlussArt,AnschlussKonstruktor>

/** Konstruktorfunktion eines konkreten [Anschluss]. */
typealias AnschlussKonstruktor = (graph: Graph, daten: GraphDatenAnschluss, besitzer: GraphDatenObjektKnoten<*>) -> GraphDatenObjektAnschluss<*>

/**
 * Erzeugt den zur Datenklasse passenden Anschluss.
 *
 * @receiver Fabrikzuordnung der bekannten Anschlussarten
 */
public fun AnschlussFabrik.erzeugeAnschluss(graph: Graph, daten: GraphDatenAnschluss, besitzer: GraphDatenObjektKnoten<*>): GraphDatenObjektAnschluss<*>? = this[daten.klasse]?.invoke(graph,daten,besitzer)

@Suppress("UNCHECKED_CAST")
val BasisAnschlussFabrik: AnschlussFabrik = mapOf(
    BasisObjektAnschluss.ANSCHLUSS_ART to ::BasisObjektAnschluss as AnschlussKonstruktor,
//    BasisEingang.ANSCHLUSS_ART to ::BasisEingang as AnschlussKonstruktor,
//    BasisAusgang.ANSCHLUSS_ART to ::BasisAusgang as AnschlussKonstruktor,
)
