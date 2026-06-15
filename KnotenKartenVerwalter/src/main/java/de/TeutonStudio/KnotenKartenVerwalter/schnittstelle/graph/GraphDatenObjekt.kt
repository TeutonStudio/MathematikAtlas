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
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.zIndex
import de.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import de.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import de.TeutonStudio.KnotenKartenVerwalter.KnotenPosition
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteZustand
import de.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.IDEhe
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.Anschluss
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten

/**
 * Das GraphDatenObjekt ist die Brücke zwischen der abstrakten daten [GraphDaten] zum unt dem Graph durch [GraphDatenObjekt]
 */
typealias GraphObjekt = GraphDatenObjekt<out GraphDaten>

/**
 *
 * Bei jeder Neuzeichnung durch @Composable wird [layoutCoordinates] aktuelisiert
 *
 */
interface GraphDatenObjekt<D: GraphDaten>{
    public val graph: Graph
    public val daten: D
    public fun registriere() = also { graph.inhalt.add(it) }

    var layoutCoordinates: MutableState<LayoutCoordinates?>

    /*
    Wird durchgeführt bevor, durch den [Graph] [zuComposable] geöffnet wird
     */
    public open fun beiKlick(klickPos: Offset)
    /*
    Wird durchgeführt bevor, durch den [Graph] [erhalteKontextFenster] geöffnet wird
     */
    public open fun beiHalten(klickPos: Offset)
    public open fun beiTransform(centroid: Offset, zoomDelta: Float, panDelta: Offset, rotationChange: Float)

    @Composable open fun Modifier.modiInputEvent() = vorher().position().transform().tapping()
    /*
    Der erste Modifier vor den [modiInputEvent] InputeEvent modifieren angewendet wird.
     */
    @Composable public fun Modifier.vorher() = zIndex(1f)
    @Composable public fun Modifier.transform() = transformable(rememberTransformableState(::beiTransform))
    @Composable public fun Modifier.tapping() = pointerInput(daten.id) { detectTapGestures(onTap = ::beiKlick,onLongPress = ::beiHalten) }
    @Composable public fun Modifier.position() = onGloballyPositioned { layoutCoordinates.value = it }


    @Composable fun zuComposable(modifier: Modifier = Modifier.Companion) = Box(modifier = modifier.vorher().modiInputEvent()) { erhalteDarstellung() }

    /**
     * Die Lokale @Composable welt des [GraphObjekt].
     */
    @Composable fun BoxScope.erhalteDarstellung()

    /**
     * Die Lokale @Composable welt des [GraphObjekt], in der KontextUmgebung, wird durch die [Karte] [beiHalten] geöffnet.
     */
    @Composable fun erhalteKontextFenster(pos: BildschirmPosition = graph.karte.ctx.second)

    /**
     * Die Lokale @Composable welt des [GraphObjekt], in der InspektorUmgebung, wird durch die [Karte] [beiKlick], [beiHalten] und [beiTransform] geöffnet.
     */
    @Composable fun erhalteInspektor()


    /*
    Wird von der [Karte] genutz um [erhalteKontextFenster] zu öffnen.
     */
    public val öffneKontext get() = derivedStateOf { graph.karte.ctx.first == daten.id }
    /*
    Wird von der [Karte] genutz um [erhalteInspektor] zu öffnen
     */
    public val istSelektiert get() = derivedStateOf { graph.karte.zustand.auswahl.value.enthält(this) }

    public fun erhalteAnschluss(knotenId: String,anschlussId: String): Anschluss<out AnschlussDaten>? = graph.karte.knoten.find { it.daten.id == knotenId }!!.anschlüsse.find { it.daten.id == anschlussId }
    public fun erhalteAnschlussMann(id: IDEhe): Anschluss<out AnschlussDaten>? = erhalteAnschluss(id.knotenIdMann,id.anschlussIdMann)
    public fun erhalteAnschlussWeib(id: IDEhe): Anschluss<out AnschlussDaten>? = erhalteAnschluss(id.knotenIdWeib,id.anschlussIdWeib)

    //    public fun KartenPosition.zuBildAusKnoten(zustand: KarteZustand = graph.karte.zustand): BildschirmPosition = zustand.zuBildAusKnoten(this).round()
    public fun BildschirmPosition.zuGraph(zustand: KarteZustand = graph.karte.zustand): KartenPosition = zustand.zuKarte(this)
    public fun KartenPosition.zuBild(zustand: KarteZustand = graph.karte.zustand): BildschirmPosition = zustand.zuBild(this)
    public fun BildschirmPosition.zuDelta(zustand: KarteZustand = graph.karte.zustand): KartenPosition = this.toOffset() / zustand.zoom
    public fun BildschirmPosition.zuKnoten(knoten: Knoten, zustand: KarteZustand = graph.karte.zustand): KnotenPosition = this.toOffset() - knoten.daten.position


    // Auf dem Graph wird von einem Anschluss aus gezogen
    // fun planeVerbindung(a: Anschluss<out AnschlussDaten>) = Unit
    // Auf dem Graph wird eine gezogene Verbindung auf einem Anschluss dieses Knoten losgelassen
    // von ist dabei der Anschluss von dem gezogen wurde und nach der auf dem fallen gelassen wurde
    fun erstelleVerbindung(von: Anschluss<out AnschlussDaten>, zu: Anschluss<out AnschlussDaten>) = Unit

}
