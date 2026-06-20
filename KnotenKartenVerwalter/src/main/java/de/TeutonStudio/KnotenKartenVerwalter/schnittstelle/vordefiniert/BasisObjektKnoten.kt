package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntSize
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenKnoten
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphPosition
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKarte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKnoten

class BasisObjektKnoten(
    override val graph: Graph,
    override val daten: GraphDatenKnoten,
    override val besitzer: GraphDatenObjektKarte<*>,
) : GraphDatenObjektKnoten<GraphDatenKnoten> {
    override val layoutCoordinates: MutableState<LayoutCoordinates?> = mutableStateOf(null)

    @Composable
    override fun BoxScope.Darstellung() {
        TODO("Not yet implemented")
    }

    @Composable
    override fun BoxScope.KontextFenster(pos: IntSize) {
        TODO("Not yet implemented")
    }

    @Composable
    override fun BoxScope.Inspektor() {
        TODO("Not yet implemented")
    }

    override val anschlussFabrik: AnschlussFabrik = BasisAnschlussFabrik
    override fun definiereVerbindung() {
        TODO("Not yet implemented")
    }

    public companion object {
        public const val KNOTEN_ART = "default"
    }
}