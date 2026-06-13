package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse

import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten


typealias AnschlussArt = String
typealias AnschlussFabrik = Map<AnschlussArt,AnschlussKonstruktor>
typealias AnschlussKonstruktor = (graph: Graph, daten: AnschlussDaten, besitzer: Knoten) -> Anschluss<out AnschlussDaten>

public fun AnschlussFabrik.erzeugeAnschluss(graph: Graph, daten: AnschlussDaten, besitzer: Knoten): Anschluss<out AnschlussDaten>? = this[daten.klasse]?.invoke(graph,daten,besitzer)

@Suppress("UNCHECKED_CAST")
val BasisAnschlussFabrik: AnschlussFabrik = mapOf(
    BasisAnschluss.ANSCHLUSS_ART to ::BasisAnschluss as AnschlussKonstruktor,
    BasisEingang.ANSCHLUSS_ART to ::BasisEingang as AnschlussKonstruktor,
    BasisAusgang.ANSCHLUSS_ART to ::BasisAusgang as AnschlussKonstruktor,
)