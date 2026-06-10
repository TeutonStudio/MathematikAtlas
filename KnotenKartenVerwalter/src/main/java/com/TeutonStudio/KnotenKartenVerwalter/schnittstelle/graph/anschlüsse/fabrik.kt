package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse

import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten


typealias AnschlussArt = String
typealias AnschlussFabrik = Map<AnschlussArt,AnschlussKonstruktor>
typealias AnschlussKonstruktor = (graph: Graph, daten: AnschlussDaten, besitzer: Knoten) -> Anschluss

public fun AnschlussFabrik.erzeugeAnschluss(graph: Graph, daten: AnschlussDaten, besitzer: Knoten): Anschluss? = this[daten.klasse]?.invoke(graph,daten,besitzer)

@Suppress("UNCHECKED_CAST")
val BasisAnschlussFabrik: AnschlussFabrik = mapOf(
    BasisAnschluss.ANSCHLUSS_ART to ::BasisAnschluss as AnschlussKonstruktor,
    BasisEingang.ANSCHLUSS_ART to ::BasisEingang as AnschlussKonstruktor,
    BasisAusgang.ANSCHLUSS_ART to ::BasisAusgang as AnschlussKonstruktor,
)