package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen

import androidx.compose.runtime.State
import com.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import com.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph

typealias VerbindungArt = String
typealias VerbindungFabrik = Map<VerbindungArt,VerbindungKonstruktor>
typealias VerbindungKonstruktor = (graph: Graph, daten: VerbindungDaten, start: State<KartenPosition>, ende: State<KartenPosition>) -> Verbindung

public fun VerbindungFabrik.erzeugeVerbindung(
    graph: Graph, daten: VerbindungDaten,
    start: State<KartenPosition>, ende: State<KartenPosition>,
): Verbindung? = this[daten.klasse]?.invoke(graph,daten,start,ende)

@Suppress("UNCHECKED_CAST")
val BasisVerbindungFabrik: VerbindungFabrik = mapOf(
    BasisVerbindung.VERBINDUNG_ART to ::BasisVerbindung as VerbindungKonstruktor,
    BezierVerbindung.VERBINDUNG_ART to ::BezierVerbindung as VerbindungKonstruktor
)
