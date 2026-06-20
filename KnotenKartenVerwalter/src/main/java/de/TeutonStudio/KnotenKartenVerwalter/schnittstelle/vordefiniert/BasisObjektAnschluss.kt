package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntSize
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphPosition
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKnoten

open class BasisObjektAnschluss(
    override val graph: Graph,
    override val daten: GraphDatenAnschluss,
    override val besitzer: GraphDatenObjektKnoten<*>,
): GraphDatenObjektAnschluss<GraphDatenAnschluss> {
    override val layoutCoordinates = mutableStateOf<LayoutCoordinates?>(null)
    override fun beiKlick(klickPos: Offset) {
        TODO("Not yet implemented")
    }

    override fun beiHalten(klickPos: Offset) {
        TODO("Not yet implemented")
    }

    override fun beiTransform(
        centroid: Offset,
        zoomDelta: Float,
        panDelta: Offset,
        rotationChange: Float
    ) {
        TODO("Not yet implemented")
    }

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

    override fun beiVerbindungZiehenStart(
        start: PointerInputChange,
        change: PointerInputChange,
        klickPos: Offset
    ) {
        TODO("Not yet implemented")
    }

    override fun beiVerbindungZiehenDelta(
        change: PointerInputChange,
        dragAmount: Offset
    ) {
        TODO("Not yet implemented")
    }

    override fun beiVerbindungZiehenEnde(change: PointerInputChange) {
        TODO("Not yet implemented")
    }

    override fun beiVerbindungZiehenAbbruch() {
        TODO("Not yet implemented")
    }

    public companion object {
        public const val ANSCHLUSS_ART = "default"
    }
}
