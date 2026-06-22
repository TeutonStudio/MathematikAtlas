package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphPosition
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Kante
import kotlin.math.roundToInt

interface GraphDatenObjektAnschluss<D: GraphDatenAnschluss>: GraphDatenObjekt<D> {
    public val besitzer: GraphDatenObjektKnoten<*>
    public val karte get() = besitzer.besitzer

    public val pos get() = erhaltePos()

    public val istEingang get() = false
    public val istAusgang get() = false

    public var dragPos: MutableState<Offset>
    public var dragZiel: MutableState<GraphDatenObjektAnschluss<*>?>

    override fun beiKlick(klickPos: Offset) {
        karte.auswahl.wähleAnschluss(daten.id)
    }

    override fun beiHalten(klickPos: Offset) {
        karte.ctx.pos = klickPos.round()
        karte.ctx.objektDatenId = daten.id
    }

    override fun beiTransform(
        centroid: Offset,
        zoomDelta: Float,
        panDelta: Offset,
        rotationChange: Float
    ) {} // Nicht genutzt

    private fun erhaltePos(): GraphPosition =
        layoutCoordinates.value?.let { anschlussCoordinates ->
            besitzer.layoutCoordinates.value?.localPositionOf(
                sourceCoordinates = anschlussCoordinates,
                relativeToSource = anschlussCoordinates.size.center.toOffset(),
            )?.let { lokalePosition -> besitzer.daten.position + lokalePosition }
        } ?: Offset.Zero

    @Composable public override fun Modifier.vorher(): Modifier =
        /*offset {
            val durchmesser = 10
            val radius = durchmesser / 2
            val knotenGröße = besitzer.layoutCoordinates.value?.size
            val breite = knotenGröße?.width ?: besitzer.daten.breite.roundToInt()
            val tiefe = knotenGröße?.height ?: besitzer.daten.tiefe.roundToInt()
            val anteil = anschlussAnteil()

            when (daten.kante) {
                Kante.Links -> IntOffset(-radius, (tiefe * anteil).roundToInt() - radius)
                Kante.Rechts -> IntOffset(breite - radius, (tiefe * anteil).roundToInt() - radius)
                Kante.Oben -> IntOffset((breite * anteil).roundToInt() - radius, -radius)
                Kante.Unten -> IntOffset((breite * anteil).roundToInt() - radius, tiefe - radius)
            }
        }.*/size(10.dp).background(Color.Black, CircleShape)

    private fun anschlussAnteil(): Float {
        val gleicheKante = besitzer.daten.anschlüsse
            .filter { it.kante == daten.kante }
            .sortedBy { besitzer.daten.anschlussIdx[it.id] ?: Int.MAX_VALUE }
        val index = gleicheKante.indexOfFirst { it.id == daten.id }
            .takeIf { it >= 0 }
            ?: 0

        return (index + 1).toFloat() / (gleicheKante.size + 1).coerceAtLeast(1).toFloat()
    }

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

    public fun erhaltePseudoVerbindung(): GraphDatenObjektVerbindung<*>

    public fun beiVerbindungZiehenStart(start: PointerInputChange,change: PointerInputChange,klickPos: Offset) {
        start.consume()
        karte.ctx.objektDatenId = null
        karte.auswahl.wähleAnschluss(daten.id)
        dragPos.value = pos

        karte.pseudoVerbindung.value = erhaltePseudoVerbindung()
    }
    public fun beiVerbindungZiehenDelta(change: PointerInputChange, dragAmount:Offset) {
        change.consume()
        dragPos.value += dragAmount
        if (karte.erhalteAnschlussNachPos(dragPos.value)?.apply {
                val bedingung = second.getDistanceSquared() < 500f / karte.zustand.erhalteZoom() && daten.erlaubeVerbindung(first.daten)
                if (bedingung) {
                    dragZiel.value = first
                    karte.pseudoVerbindung.value?.endeKante = first.daten.kante
                    return
                } else dragZiel.value = null
            } == null) dragZiel.value = null
    }
    public fun beiVerbindungZiehenEnde(change: PointerInputChange) {
        dragZiel.value?.let { karte.definiereVerbindung(this,it) }
        karte.pseudoVerbindung.value = null
    }
    public fun beiVerbindungZiehenAbbruch() {
        karte.pseudoVerbindung.value = null
    }

    interface gerichteterGDOA<D: GraphDatenAnschluss.gerichteteGDA>: GraphDatenObjektAnschluss<D> {
        // TODO falls eingang, alle alten verbindung löschen, wenn neue erstellt wird
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
