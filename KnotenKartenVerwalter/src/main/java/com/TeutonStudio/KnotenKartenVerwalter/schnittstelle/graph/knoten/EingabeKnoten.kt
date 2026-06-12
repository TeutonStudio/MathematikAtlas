package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten

import com.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenEingabeDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.Karte

open class EingabeKnoten(graph: Graph, daten: KnotenEingabeDaten, besitzer: Karte): BasisKnoten(graph,daten,besitzer) {
    public companion object {
        public const val KNOTEN_ART: KnotenArt = "eingabe"
    }

}