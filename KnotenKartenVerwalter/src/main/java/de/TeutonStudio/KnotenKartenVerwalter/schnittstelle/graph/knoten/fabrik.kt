package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.AnschlussKnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenAnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.Karte

typealias KnotenArt = String
typealias KnotenFabrik = Map<KnotenArt,KnotenKonstruktor>
typealias KnotenKonstruktor = (graph: Graph, daten: AnschlussKnotenDaten, besitzer: Karte) -> Knoten

public fun KnotenFabrik.erzeugeKnoten(graph: Graph, daten: AnschlussKnotenDaten, besitzer: Karte): Knoten? = this[daten.klasse]?.invoke(graph,daten,besitzer)

@Suppress("UNCHECKED_CAST")
val BasisKnotenFabrik: KnotenFabrik = mapOf(
    BasisKnoten.KNOTEN_ART to ::BasisKnoten as KnotenKonstruktor,
    EingabeKnoten.KNOTEN_ART to ::EingabeKnoten as KnotenKonstruktor,
    AusgabeKnoten.KNOTEN_ART to ::AusgabeKnoten as KnotenKonstruktor,
)
