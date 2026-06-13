package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

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
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import com.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import com.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import com.TeutonStudio.KnotenKartenVerwalter.KnotenPosition
import com.TeutonStudio.KnotenKartenVerwalter.daten.GraphDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteZustand
import com.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.IDEhe
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.Anschluss
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten

typealias GraphObjekt = GraphDatenObjekt<out GraphDaten>

interface GraphDatenObjekt<D: GraphDaten>{
    public val graph: Graph
    public val daten: D
    public fun registriere() = also { graph.inhalt.add(it) }

    var layoutCoordinates: MutableState<LayoutCoordinates?>

    @Composable open fun Modifier.modifier() = vorher().position().transform().tapping()
    @Composable public fun Modifier.vorher() = this
    @Composable public fun Modifier.transform() = transformable(rememberTransformableState(::beiTransform))
    @Composable public fun Modifier.tapping() = pointerInput(daten.id) { detectTapGestures(onTap = ::beiKlick,onLongPress = ::beiHalten) }
    @Composable public fun Modifier.position() = onGloballyPositioned { layoutCoordinates.value = it }

    @Composable
    open fun zuComposable(modifier: Modifier = Modifier.Companion) = Box(
        modifier = modifier.modifier()
    ) { erhalteDarstellung() }

    public open fun beiKlick(klickPos: Offset) {}
    public open fun beiHalten(klickPos: Offset) {}
    public open fun beiTransform(centroid: Offset, zoomDelta: Float, panDelta: Offset, rotationChange: Float) {}

    @Composable abstract fun BoxScope.erhalteDarstellung()
    @Composable abstract fun erhalteKontextFenster(pos: BildschirmPosition = graph.karte.ctx.second)
    @Composable open fun erhalteInspektor() {}

    public val öffneKontext get() = derivedStateOf { graph.karte.ctx.first == daten.id }
    public val istSelektiert get() = derivedStateOf { graph.karte.zustand.auswahl.value.enthält(this) }

    public fun erhalteAnschluss(knotenId: String,anschlussId: String): Anschluss<out AnschlussDaten>? = graph.karte.knoten.find { it.daten.id == knotenId }!!.anschlüsse.find { it.daten.id == anschlussId }
    public fun erhalteAnschlussMann(id: IDEhe): Anschluss<out AnschlussDaten>? = erhalteAnschluss(id.knotenIdMann,id.anschlussIdMann)
    public fun erhalteAnschlussWeib(id: IDEhe): Anschluss<out AnschlussDaten>? = erhalteAnschluss(id.knotenIdWeib,id.anschlussIdWeib)

    //    public fun KartenPosition.zuBildAusKnoten(zustand: KarteZustand = graph.karte.zustand): BildschirmPosition = zustand.zuBildAusKnoten(this).round()
    public fun BildschirmPosition.zuKarte(zustand: KarteZustand = graph.karte.zustand): KartenPosition = zustand.zuKarte(this)
    public fun KartenPosition.zuBild(zustand: KarteZustand = graph.karte.zustand): BildschirmPosition = zustand.zuBild(this)
    public fun BildschirmPosition.zuDelta(zustand: KarteZustand = graph.karte.zustand): KartenPosition = this.toOffset() / zustand.zoom
    public fun BildschirmPosition.zuKnoten(
        knoten: Knoten,
        zustand: KarteZustand = graph.karte.zustand,
    ): KnotenPosition = this.toOffset() - knoten.daten.position


    // Auf dem Graph wird von einem Anschluss aus gezogen
    fun planeVerbindung(a: Anschluss<out AnschlussDaten>) = Unit
    // Auf dem Graph wird eine gezogene Verbindung auf einem Anschluss dieses Knoten losgelassen
    // von ist dabei der Anschluss von dem gezogen wurde und nach der auf dem fallen gelassen wurde
    fun erstelleVerbindung(von: Anschluss<out AnschlussDaten>, zu: Anschluss<out AnschlussDaten>) = Unit

}
