package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.zIndex
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenVerbindung

//typealias GraphObjekt = GraphDatenObjekt<out GraphDaten>

interface GraphDatenObjekt<D: GraphDaten>: GraphObjekt {
    public val daten: D
//    public val graph: Graph
//    public fun registriere() = also { graph.inhalt.add(it) }

    public val layoutCoordinates: MutableState<LayoutCoordinates?>
    public val objektModifier @Composable get() = Modifier.modiInputEvent()

    public open fun beiKlick(klickPos: Offset)
    public open fun beiHalten(klickPos: Offset)
    public open fun beiTransform(centroid: Offset, zoomDelta: Float, panDelta: Offset, rotationChange: Float)

    @Composable public open fun Modifier.modiInputEvent() = vorher().position().transform().tapping()
    @Composable public open fun Modifier.vorher() = zIndex(1f)

    @Composable public fun Modifier.transform() = transformable(rememberTransformableState(::beiTransform))
    @Composable public fun Modifier.tapping() = pointerInput(daten.id) { detectTapGestures(onTap = ::beiKlick,onLongPress = ::beiHalten) }
    @Composable public fun Modifier.position() = onGloballyPositioned { layoutCoordinates.value = it }


    @Composable public open fun BoxScope.Darstellung()
    @Composable public open fun BoxScope.KontextFenster(pos: IntOffset = graph.karte.ctx.pos)
    @Composable public open fun BoxScope.Inspektor()

    @Composable public fun ComposableStandard() = Box(objektModifier) {
        Darstellung()
        if (this@GraphDatenObjekt is GraphDatenObjektKnoten<*>) {
            anschlüsse.forEach { anschluss ->
                Box(anschluss.objektModifier)
            }
        }
    }
    @Composable public fun ComposableKontext() = Box() { KontextFenster() }
    @Composable public fun ComposableInspektor() = Box() { Inspektor() }

    public val öffneKontext get() = derivedStateOf { graph.karte.ctx.objektDatenId == daten.id }
    public val istSelektiert get() = derivedStateOf { graph.karte.auswahl.enthält(this) }

    public fun erhalteAnschluss(knotenId: String,anschlussId: String) = graph.karte.knoten.find { it.daten.id == knotenId }!!.anschlüsse.find { it.daten.id == anschlussId }
    public fun erhalteAnschlussMann(id: GraphDatenVerbindung.IDEhe) = erhalteAnschluss(id.knotenIdMann,id.anschlussIdMann)
    public fun erhalteAnschlussWeib(id: GraphDatenVerbindung.IDEhe) = erhalteAnschluss(id.knotenIdWeib,id.anschlussIdWeib)

//    public fun BildschirmPosition.zuGraph(zustand: Zustand = graph.karte.zustand): KartenPosition = zustand.zuKarte(this)
//    public fun KartenPosition.zuBild(zustand: Zustand = graph.karte.zustand): BildschirmPosition = zustand.zuBild(this)
//    public fun BildschirmPosition.zuDelta(zustand: Zustand = graph.karte.zustand): KartenPosition = this.toOffset() / zustand.erhalteZoom()
//    public fun BildschirmPosition.zuKnoten(knoten: GraphDatenObjekt<GraphDaten.bewegbareGD>, zustand: Zustand = graph.karte.zustand): KnotenPosition = this.toOffset() - knoten.daten.position

    public fun erstelleVerbindung(von: GraphDatenObjektAnschluss<*>, zu: GraphDatenObjektAnschluss<*>) = Unit

    public companion object {
        @Composable public fun Iterable<GraphDatenObjekt<*>>.Composable(/*modifier: Modifier = Modifier*/) = forEach { it.ComposableStandard() }

    }
}
