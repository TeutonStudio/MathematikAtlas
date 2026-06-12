package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten

import com.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenAusgabeDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.Karte

open class AusgabeKnoten(graph: Graph, daten: KnotenAusgabeDaten, besitzer: Karte): BasisKnoten(graph,daten,besitzer) {
    public companion object {
        public const val KNOTEN_ART: KnotenArt = "ausgabe"
    }

}