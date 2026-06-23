package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import android.graphics.RectF
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenKnoten
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Kante
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphCanvasObjekt.Companion.overlaps
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.AnschlussFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.erzeugeAnschluss

interface GraphDatenObjektKnoten<D: GraphDatenKnoten>: GraphDatenObjekt<D>, GraphDatenObjekt.Vergrößerbar<D> {
    public abstract val besitzer: GraphDatenObjektKarte<*>
    public abstract val anschlussFabrik: AnschlussFabrik
    override val vergrößerbarZoom: Float get() = besitzer.zustand.erhalteZoom()


    val anschlüsse get() = graph.anschlüsse.filter { it.besitzer.daten.id == daten.id }

    /** Positioniert und skaliert den Knoten innerhalb der Kartenebene. */
    @Composable public override fun Modifier.vorher(): Modifier = vergrößerbareGröße()
        .offset { daten.position.round() }.zIndex(1f)
//        .width(with(LocalDensity.current) { daten.breite.toDp() })
//        .height(with(LocalDensity.current) { daten.tiefe.toDp() })
        .apply { if (istSelektiert.value) { border(2.dp, graph.selektiertFarbe, RoundedCornerShape(8.dp)) } }

    @Composable public override fun Modifier.modiInputEvent(): Modifier =
        vorher()
            .position()
            .tapping()
            .pointerInput(daten.id) {
                detectDragGestures(
                    onDragStart = {
                        besitzer.auswahl.wähleKnoten(daten.id)
                        besitzer.ctx.objektDatenId = null
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        beiTransform(Offset.Zero, 1f, dragAmount, 0f)
                    },
                )
            }

    @Composable public override fun Modifier.position(): Modifier =
        onGloballyPositioned {
            layoutCoordinates.value = it
            daten.breite = it.size.width.toFloat()
            daten.tiefe = it.size.height.toFloat()
        }


    public override fun beiKlick(klickPos: Offset) {
        besitzer.auswahl.wähleKnoten(daten.id)
        besitzer.ctx.objektDatenId = null
    }
    public override fun beiHalten(klickPos: Offset) {
        besitzer.auswahl.wähleKnoten(daten.id)
        besitzer.ctx.pos = klickPos.round()
        besitzer.ctx.objektDatenId = daten.id
    }
    public override fun beiTransform(centroid: Offset, zoomDelta: Float, panDelta: Offset, rotationChange: Float) {
        besitzer.verschiebeKnoten(daten.id, panDelta / besitzer.zustand.erhalteZoom())
        besitzer.auswahl.wähleKnoten(daten.id)
        besitzer.ctx.objektDatenId = null
    }

    @Composable public override fun ComposableStandard() = Box(objektModifier) {
        Darstellung()
        if (istSelektiert.value) { VergrößerBereiche() }
        Kante.entries.forEach { k ->
            KnotenKante(k,Modifier.offset(x = if (k == Kante.Links) (-5).dp else 5.dp).zIndex(2f),
                anschlüsse.filter { it.daten.kante == k }.map { { Box(it.objektModifier) } }
            )
        }
    }

    @Composable
    private fun BoxScope.KnotenKante(kante: Kante, modifier: Modifier, inhalt: Iterable<@Composable () -> Unit>) {
        if (kante.istVertikal) {
            Column(modifier.align(kante.alignment()),Arrangement.SpaceEvenly,Alignment.CenterHorizontally) {
                inhalt.forEach { it() }
            }
        }
        if (kante.istHorizontal) {
            Row(modifier.align(kante.alignment()),Arrangement.SpaceEvenly,Alignment.CenterVertically) {
                inhalt.forEach { it() }
            }
        }
    }

    @Composable
    public fun BoxScope.StandardKontextFenster(pos: androidx.compose.ui.unit.IntOffset = besitzer.ctx.pos) {
        Card(Modifier
            .offset { pos }
            .padding(4.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text(daten.name)
                Text("Position: ${daten.position.x.toInt()}, ${daten.position.y.toInt()}")
                Text("Anschlüsse: ${daten.anschlüsse.size}")
            }
        }
    }

    /** Prüft, ob der Knoten den sichtbaren Kartenbereich überschneidet. */
    public fun istImViewport(viewport: RectF = besitzer.zustand.erhalteViewportRect()): Boolean = RectF(
        daten.position.x,
        daten.position.y,
        daten.position.x + daten.breite,
        daten.position.y + daten.tiefe,
    ).overlaps(viewport)

    public fun planeVerbindung(vonAnschluss: GraphDatenObjektAnschluss<*>, vonKnoten: GraphDatenObjektKnoten<*>) {}
    public fun verwerfeGeplanteVerbindung() {}
    public fun definiereVerbindung() {}
    public fun definiereVerbindung(mann: GraphDatenObjektAnschluss<*>, weib: GraphDatenObjektAnschluss<*>) {}

//    public fun GraphPosition.zuBildAusKnoten(): BildschirmPosition = (this + daten.position).round()

    public companion object {
        public fun Iterable<GraphDatenObjektKnoten<*>>.sichtbar() = filter { it.istImViewport() }

        public fun <D: GraphDatenObjekt<*>> Iterable<D>.findeNachId(id: String) = find { it.daten.id == id }
        public fun Iterable<GraphDatenObjektKnoten<*>>.anschlussNachId(idKnoten: String, idAnschluss: String) = (findeNachId(idKnoten) as GraphDatenObjektKnoten<*>).anschlussNachId(idAnschluss)

        public fun GraphDatenObjektKnoten<*>.anschlussNachId(id: String) = anschlüsse.findeNachId(id)
        public fun Iterable<GraphDatenObjektKnoten<*>>.anschlüsseNachIDEhe(ids: GraphDatenVerbindung.IDEhe) =
            anschlussNachId(ids.knotenIdMann, ids.anschlussIdMann)?.let { aM ->
                anschlussNachId(ids.knotenIdWeib, ids.anschlussIdWeib)?.let { aW ->
                    aM to aW
                }
            }
    }
}
