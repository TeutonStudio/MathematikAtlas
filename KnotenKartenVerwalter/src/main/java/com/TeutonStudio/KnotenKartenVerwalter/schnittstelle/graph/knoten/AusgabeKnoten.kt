package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten

import com.TeutonStudio.KnotenKartenVerwalter.daten.knoten.AusgabeDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.Karte

open class AusgabeKnoten(_graph: Graph, daten: AusgabeDaten, besitzer: Karte): BasisKnoten(_graph,daten,besitzer) {
    public companion object {
        public const val KNOTEN_ART: KnotenArt = "ausgabe"
    }

}