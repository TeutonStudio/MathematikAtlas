package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert

import androidx.compose.runtime.State
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenKarte
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenKnoten
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphPosition
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKarte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKnoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Zustand
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindete
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.veränderung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.wählte


typealias KartenArt = String
typealias KartenFabrik = Map<KartenArt,KartenKonstruktor>
typealias KartenKonstruktor = (Graph,GraphDatenKarte,Zustand,veränderung,verbindete,wählte) -> GraphDatenObjektKarte<*>

public fun KartenFabrik.erzeugeKarte(
    graph: Graph, daten: GraphDatenKarte,
    zustand: Zustand,
    veränderung: veränderung,
    verbindete: verbindete,
    wählte: wählte,
): GraphDatenObjektKarte<*> {
    val klasse = daten.klasse ?: BasisObjektKarte.KARTEN_ART
    val konstruktor = this[klasse] ?: error("Keine Kartenklasse '$klasse'. Bekannte Klassen: ${keys.joinToString()}")

    return konstruktor(
        graph,
        daten,
        zustand,
        veränderung,
        verbindete,
        wählte,
    )
}
@Suppress("UNCHECKED_CAST")
val BasisKartenFabrik: KartenFabrik = mapOf(
    BasisObjektKarte.KARTEN_ART to ::BasisObjektKarte as KartenKonstruktor,
)

typealias KnotenArt = String
typealias KnotenFabrik = Map<KnotenArt,KnotenKonstruktor>
typealias KnotenKonstruktor = (graph: Graph, daten: GraphDatenKnoten, besitzer: GraphDatenObjektKarte<*>) -> GraphDatenObjektKnoten<*>

public fun KnotenFabrik.erzeugeKnoten(
    graph: Graph, daten: GraphDatenKnoten,
    besitzer: GraphDatenObjektKarte<*>,
): GraphDatenObjektKnoten<*>? = this[daten.klasse]?.invoke(graph,daten,besitzer)

@Suppress("UNCHECKED_CAST")
val BasisKnotenFabrik: KnotenFabrik = mapOf(
    BasisObjektKnoten.KNOTEN_ART to ::BasisObjektKnoten as KnotenKonstruktor,
//    EingabeKnoten.KNOTEN_ART to ::EingabeKnoten as KnotenKonstruktor,
//    AusgabeKnoten.KNOTEN_ART to ::AusgabeKnoten as KnotenKonstruktor,
)

typealias AnschlussArt = String
typealias AnschlussFabrik = Map<AnschlussArt,AnschlussKonstruktor>
typealias AnschlussKonstruktor = (graph: Graph, daten: GraphDatenAnschluss, besitzer: GraphDatenObjektKnoten<*>) -> GraphDatenObjektAnschluss<*>

public fun AnschlussFabrik.erzeugeAnschluss(
    graph: Graph, daten: GraphDatenAnschluss,
    besitzer: GraphDatenObjektKnoten<*>,
): GraphDatenObjektAnschluss<*>? = this[daten.klasse]?.invoke(graph,daten,besitzer)

@Suppress("UNCHECKED_CAST")
val BasisAnschlussFabrik: AnschlussFabrik = mapOf(
    BasisObjektAnschluss.ANSCHLUSS_ART to ::BasisObjektAnschluss as AnschlussKonstruktor,
//    BasisEingang.ANSCHLUSS_ART to ::BasisEingang as AnschlussKonstruktor,
//    BasisAusgang.ANSCHLUSS_ART to ::BasisAusgang as AnschlussKonstruktor,
)

typealias VerbindungArt = String
typealias VerbindungFabrik = Map<VerbindungArt,VerbindungKonstruktor>
typealias VerbindungKonstruktor = (Graph, GraphDatenVerbindung, start: State<GraphPosition>, ende: State<GraphPosition>) -> GraphDatenObjektVerbindung<*>

public fun VerbindungFabrik.erzeugeVerbindung(
    graph: Graph, daten: GraphDatenVerbindung,
    start: State<GraphPosition>, ende: State<GraphPosition>,
): GraphDatenObjektVerbindung<*>? = this[daten.klasse]?.invoke(graph,daten,start,ende)

@Suppress("UNCHECKED_CAST")
val BasisVerbindungFabrik: VerbindungFabrik = mapOf(
    BasisObjektVerbindung.VERBINDUNG_ART to ::BasisObjektVerbindung as VerbindungKonstruktor,
    BezierObjektVerbindung.VERBINDUNG_ART to ::BezierObjektVerbindung as VerbindungKonstruktor
)
