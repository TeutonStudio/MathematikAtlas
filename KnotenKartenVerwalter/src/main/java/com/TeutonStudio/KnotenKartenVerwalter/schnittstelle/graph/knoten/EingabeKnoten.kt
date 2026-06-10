package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten

import com.TeutonStudio.KnotenKartenVerwalter.daten.knoten.EingabeDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.Karte

open class EingabeKnoten(_graph: Graph, daten: EingabeDaten, besitzer: Karte): BasisKnoten(_graph,daten,besitzer) {
    public companion object {
        public const val KNOTEN_ART: KnotenArt = "eingabe"
    }

}