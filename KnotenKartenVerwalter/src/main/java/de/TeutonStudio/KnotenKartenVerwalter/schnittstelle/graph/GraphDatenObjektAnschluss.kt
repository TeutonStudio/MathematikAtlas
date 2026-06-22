package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphPosition

interface GraphDatenObjektAnschluss<D: GraphDatenAnschluss>: GraphDatenObjekt<D> {
    public val besitzer: GraphDatenObjektKnoten<*>
    public val karte get() = besitzer.besitzer

    public val pos get() = erhaltePos() ?: Offset.Zero

    public val istEingang get() = false
    public val istAusgang get() = false

    private fun erhaltePos(): GraphPosition? = layoutCoordinates.value?.let { anschlussCoordinates ->
        besitzer.layoutCoordinates.value?.localPositionOf(
            anschlussCoordinates,
            anschlussCoordinates.size.center.toOffset(),
        )?.let { lokalePosition -> besitzer.daten.position + lokalePosition }
    }

    @Composable public override fun Modifier.vorher(): Modifier = size(5.dp).background(Color.Black, CircleShape)

    /** Kombiniert Tap- und Drag-Gesten für das Verbindungsziehen. */
    @Composable public override fun Modifier.modiInputEvent(): Modifier = vorher().tapping().position().pointerInput(daten.id) {
        detectDragGestures(
            orientationLock = null,
            shouldAwaitTouchSlop = { false },
            onDragStart = ::beiVerbindungZiehenStart,
            onDrag = ::beiVerbindungZiehenDelta,
            onDragEnd = ::beiVerbindungZiehenEnde,
            onDragCancel = ::beiVerbindungZiehenAbbruch,
        )
    }

    public fun beiVerbindungZiehenStart(start: PointerInputChange,change: PointerInputChange,klickPos: Offset)
    public fun beiVerbindungZiehenDelta(change: PointerInputChange, dragAmount:Offset)
    public fun beiVerbindungZiehenEnde(change: PointerInputChange)
    public fun beiVerbindungZiehenAbbruch()

    interface gerichteterGDOA<D: GraphDatenAnschluss.gerichteteGDA>: GraphDatenObjektAnschluss<D> {

        public override val istEingang get() = daten.istEingang
        public override val istAusgang get() = daten.istAusgang

    }
/*    interface {

    }*/

    public companion object {

        public fun Iterable<GraphDatenObjektAnschluss<*>>.findMann(ids: GraphDatenVerbindung.IDEhe) = find { it.daten.id == ids.anschlussIdMann }
        public fun Iterable<GraphDatenObjektAnschluss<*>>.findWeib(ids: GraphDatenVerbindung.IDEhe) = find { it.daten.id == ids.anschlussIdWeib }
    }
}