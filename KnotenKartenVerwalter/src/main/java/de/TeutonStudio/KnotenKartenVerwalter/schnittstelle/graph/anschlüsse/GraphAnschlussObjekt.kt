package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import de.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.IDEhe
import de.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.VerbindungDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjekt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.BezierVerbindung

interface GraphAnschlussObjekt<D: AnschlussDaten>: GraphDatenObjekt<D> {
    public val besitzer: Knoten
    public val karte get() = besitzer.besitzer

    val pos get() = erhaltePos() ?: Offset.Zero // besitzer.erhalteAnschlussPos(daten.id)

    private fun erhaltePos(): KartenPosition? =
        layoutCoordinates.value?.let { anschlussCoordinates ->
            besitzer.layoutCoordinates.value?.localPositionOf(
                sourceCoordinates = anschlussCoordinates,
                relativeToSource = anschlussCoordinates.size.center.toOffset(),
            )?.let { lokalePosition ->
                besitzer.daten.position + lokalePosition
            }
        }

    @Composable
    override fun Modifier.vorher(): Modifier = size(5.dp).background(Color.Black, CircleShape)

    @Composable
    override fun Modifier.modifier(): Modifier = vorher().tapping().position()
        .pointerInput(daten.id) {
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


    public open fun istEingang(): Boolean = false
    public open fun istAusgang(): Boolean = false

}