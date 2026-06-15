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
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.GraphAnschlussObjekt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.GraphKartenObjekt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.GraphKnotenObjekt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.GraphVerbindungObjekt

/** Allgemeiner Vertrag eines darstellbaren Graphobjekts. */
typealias GraphObjekt = GraphDatenObjekt<out GraphDaten>

/**
 * Beschreibt die gemeinsame Brücke zwischen fachlichen [GraphDaten] und ihrer Darstellung im [Graph].
 * Spezialisierte Graphobjekte werden über [GraphKartenObjekt], [GraphKnotenObjekt], [GraphAnschlussObjekt] und [GraphVerbindungObjekt] modelliert.
 *
 * Standardimplementierungen werden von den Basistypen Karte, Knoten, Anschluss und Verbindung bereitgestellt.
 * Eigene Implementierungen sollen diese Basistypen erweitern, statt den gesamten Systemvertrag direkt neu zu implementieren.
 *
 * Das Interface koordiniert Registrierung, Auswahl, Kontextfenster, Inspector-Darstellung und Koordinatenumrechnung.
 */
interface GraphDatenObjekt<D: GraphDaten>{
    public val graph: Graph
    public val daten: D
    public fun registriere() = also { graph.inhalt.add(it) }

    var layoutCoordinates: MutableState<LayoutCoordinates?>

    /** Reagiert auf einen einfachen Klick im lokalen Bildschirmkoordinatenraum. */
    public open fun beiKlick(klickPos: Offset)

    /** Reagiert auf ein Halten und bereitet typischerweise das Kontextfenster vor. */
    public open fun beiHalten(klickPos: Offset)

    /** Reagiert auf Transformationsgesten des Graphobjekts. */
    public open fun beiTransform(centroid: Offset, zoomDelta: Float, panDelta: Offset, rotationChange: Float)

    @Composable public open fun Modifier.modiInputEvent() = vorher().position().transform().tapping()

    /** Liefert den äußeren Modifier, bevor Eingabe-Modifier ergänzt werden. */
    @Composable public fun Modifier.vorher() = zIndex(1f)
    @Composable public fun Modifier.transform() = transformable(rememberTransformableState(::beiTransform))
    @Composable public fun Modifier.tapping() = pointerInput(daten.id) { detectTapGestures(onTap = ::beiKlick,onLongPress = ::beiHalten) }
    @Composable public fun Modifier.position() = onGloballyPositioned { layoutCoordinates.value = it }


    /**
     * Erstellt die lokale Compose-Darstellung dieses Graphobjekts.
     * Sie wird innerhalb der zugehörigen Kartenebene eingebunden.
     *
     * @param modifier äußerer Modifier der Darstellung
     */
    @Composable public fun zuComposable(modifier: Modifier = Modifier.Companion) = Box(modifier = modifier.vorher().modiInputEvent()) { erhalteDarstellung() }

    /**
     * Erstellt den Inhalt des Graphobjekts innerhalb einer Box.
     * Die Darstellung wird von [zuComposable] in die Kartenebene eingesetzt.
     *
     * @receiver BoxScope der lokalen Darstellung
     */
    @Composable public fun BoxScope.erhalteDarstellung()

    /**
     * Erstellt das Kontextfenster dieses Graphobjekts.
     * Es wird von der Karte an der gespeicherten Kontextposition geöffnet.
     *
     * @param pos Position des Kontextfensters im Bildschirmkoordinatenraum
     */
    @Composable public fun erhalteKontextFenster(pos: BildschirmPosition = graph.karte.ctx.second)

    /**
     * Erstellt die Inspector-Darstellung dieses Graphobjekts.
     * Sie wird vom Graphsystem für das aktuell ausgewählte Objekt eingebunden.
     */
    @Composable public fun erhalteInspektor()


    /** Gibt an, ob die Karte das Kontextfenster dieses Objekts öffnet. */
    public val öffneKontext get() = derivedStateOf { graph.karte.ctx.first == daten.id }

    /** Gibt an, ob dieses Objekt in der Kartenauswahl enthalten ist. */
    public val istSelektiert get() = derivedStateOf { graph.karte.zustand.auswahl.value.enthält(this) }

    public fun erhalteAnschluss(knotenId: String,anschlussId: String): Anschluss<out AnschlussDaten>? = graph.karte.knoten.find { it.daten.id == knotenId }!!.anschlüsse.find { it.daten.id == anschlussId }
    public fun erhalteAnschlussMann(id: IDEhe): Anschluss<out AnschlussDaten>? = erhalteAnschluss(id.knotenIdMann,id.anschlussIdMann)
    public fun erhalteAnschlussWeib(id: IDEhe): Anschluss<out AnschlussDaten>? = erhalteAnschluss(id.knotenIdWeib,id.anschlussIdWeib)

    public fun BildschirmPosition.zuGraph(zustand: KarteZustand = graph.karte.zustand): KartenPosition = zustand.zuKarte(this)
    public fun KartenPosition.zuBild(zustand: KarteZustand = graph.karte.zustand): BildschirmPosition = zustand.zuBild(this)
    public fun BildschirmPosition.zuDelta(zustand: KarteZustand = graph.karte.zustand): KartenPosition = this.toOffset() / zustand.zoom
    public fun BildschirmPosition.zuKnoten(knoten: Knoten, zustand: KarteZustand = graph.karte.zustand): KnotenPosition = this.toOffset() - knoten.daten.position

    public fun erstelleVerbindung(von: Anschluss<out AnschlussDaten>, zu: Anschluss<out AnschlussDaten>) = Unit

}
