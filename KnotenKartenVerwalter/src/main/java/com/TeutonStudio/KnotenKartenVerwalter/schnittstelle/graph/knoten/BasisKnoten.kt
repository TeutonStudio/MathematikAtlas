package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.AnschlussFabrik
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.BasisAnschlussFabrik
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.Karte

open class BasisKnoten(
    _graph: Graph,
    override val daten: KnotenDaten,
    override val besitzer: Karte,
): Knoten(_graph) {
    override val anschlussFabrik: AnschlussFabrik = BasisAnschlussFabrik


    @Composable
    override fun Textzeile() {
        Text("Knoten Textzeile")
    }

    @Composable
    override fun Fußzeile() {
        Text("Knoten Fußzeile")
    }


    public companion object {
        public const val KNOTEN_ART: KnotenArt = "default"
    }
}