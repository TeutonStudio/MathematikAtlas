package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import android.graphics.RectF
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenKarte
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenKnoten
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphCanvasObjekt.Companion.Composable as VComposable
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjekt.Companion.Composable as KComposable
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKnoten.Companion.anschlüsseNachIDEhe
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphHintergrund.RasterArt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphHintergrund.RasterTesselation
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.KnotenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.VerbindungFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.erzeugeKnoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.erzeugeVerbindung

typealias Zustand = GraphDatenObjektKarte.GraphDatenObjektKarteZustand
typealias Auswahl = GraphDatenObjektKarte.GraphDatenObjektKarteAuswahl
typealias Kontext = GraphDatenObjektKarte.GraphDatenObjektKarteKontext

interface GraphDatenObjektKarte<D: GraphDatenKarte>: GraphDatenObjekt<D> {
    abstract val knotenFabrik: KnotenFabrik
    abstract val verbindungFabrik: VerbindungFabrik

    val ctx: Kontext
    val zeigeÜbersicht get() = true
    val zeigeKontrollLeiste get() = true
    val zustand: Zustand
    val auswahl: Auswahl
    abstract val pseudoVerbindung: MutableState<GraphDatenObjektVerbindung<*>?>

    val knoten get() = graph.knoten
    val verbindungen get() = graph.verbindungen
    val anschlüsse get() = graph.anschlüsse


    @Composable public override fun BoxScope.Darstellung() {
        Box(Modifier.fillMaxSize().onSizeChanged {
            zustand.setzeDimension(it)
            daten.breite = it.width.toFloat()
            daten.tiefe = it.height.toFloat()
        }.graphicsLayer {
            translationX = zustand.erhaltePos().x
            translationY = zustand.erhaltePos().y
            scaleX = zustand.erhalteZoom()
            scaleY = zustand.erhalteZoom()
            transformOrigin = TransformOrigin(0f, 0f)
        } ) {
            knoten.sichtbar().KComposable(/*Modifier.zIndex(1f)*/)
            verbindungen.sichtbar().VComposable(/*Modifier.zIndex(-1f)*/)
            pseudoVerbindung.value?.apply { listOf(this).VComposable() }
        }
        graph.inhalt.filterIsInstance<GraphDatenObjekt<*>>().forEach { if (it.öffneKontext.value) it.ComposableKontext() }
        if (auswahl.istEinzel) { Box(Modifier.align(Alignment.CenterEnd)) { erhalteAuswahl().first().ComposableInspektor() } }
    }

    override fun beiKlick(klickPos: Offset) {
        auswahl.leereAuswahl()
    }

    override fun beiHalten(klickPos: Offset) {
        ctx.pos = klickPos.round()
        ctx.objektDatenId = daten.id
    }

    override fun beiTransform(
        centroid: Offset,
        zoomDelta: Float,
        panDelta: Offset,
        rotationChange: Float
    ) {
        zustand.transformiere(panDelta,zoomDelta)
    }

    public fun erhalteVerbindungNachPos(pos: Offset): Pair<GraphDatenObjektVerbindung<*>,Offset>? = verbindungen.map { it to it.abstand(pos) }.minByOrNull { it.second.getDistanceSquared() }
    public fun erhalteAnschlussNachPos(pos: Offset): Pair<GraphDatenObjektAnschluss<*>,Offset>? = anschlüsse.map { it to it.pos - pos  }.minByOrNull { it.second.getDistanceSquared() }

    public fun sichtbarerWeltBereich(): Rect? {
        if (daten.dimension.width <= 0 || daten.dimension.height <= 0) { return null }

        val sichererZoom = zustand.erhalteZoom().coerceAtLeast(0.0001f)

        return Rect(
            left = -zustand.erhaltePos().x / sichererZoom,
            top = -zustand.erhaltePos().y / sichererZoom,
            right = (daten.dimension.width - zustand.erhaltePos().x) / sichererZoom,
            bottom = (daten.dimension.height - zustand.erhaltePos().y) / sichererZoom,
        )
    }
    public fun inhaltsGrenzen(puffer: Float = 0f): Rect? {
        val grenzen = knoten.map { it.daten.dimension }.reduceOrNull { bisher, nächstes ->
            Rect(
                left = minOf(bisher.left,nächstes.left),
                top = minOf(bisher.top,nächstes.top),
                right = maxOf(bisher.right,nächstes.right),
                bottom = maxOf(bisher.bottom,nächstes.bottom),
            )
        } ?: return null

        val sichererPuffer = puffer.coerceAtLeast(0f)

        return Rect(
            left = grenzen.left - sichererPuffer,
            top = grenzen.top - sichererPuffer,
            right = grenzen.right + sichererPuffer,
            bottom = grenzen.bottom + sichererPuffer,
        )
    }
    public fun definiereVerbindung(mann: GraphDatenObjektAnschluss<*>, weib: GraphDatenObjektAnschluss<*>) /*= daten.verbindungen.add(VerbindungDaten(mann,weib,"",null)).apply {
        if (weib.daten is AusgangDaten && mann.besitzer is PullObjekt) (mann.besitzer as PullObjekt).aktualisiereCache()
        if (mann.daten is AusgangDaten && weib.besitzer is PullObjekt) (weib.besitzer as PullObjekt).aktualisiereCache()
        mann.besitzer.definiereVerbindung()
        weib.besitzer.definiereVerbindung()
        keinKontext()
    }*/

    public fun erhalteAuswahl() = listOf(erhalteKnotenAuswahl(),erhalteVerbindungAuswahl(),erhalteAnschlussAuswahl()).flatten()
    public fun erhalteKnotenAuswahl() = knoten.filter { it.daten.id in auswahl.knotenIds }
    public fun erhalteVerbindungAuswahl() = verbindungen.filter { it.daten.id in auswahl.verbindungIds }
    public fun erhalteAnschlussAuswahl() = anschlüsse.filter { it.daten.id in auswahl.anschlussIds }

    fun verschiebeKnoten(id: String, panDelta: Offset) {
        val zielKnoten = knoten.filter {
            if (id in auswahl.knotenIds) {
                it.daten.id in auswahl.knotenIds
            } else {
                it.daten.id == id
            }
        }

        zielKnoten.mapNotNull { it.daten as? GraphDaten.bewegbareGD }.forEach { it.verschiebeKnoten(panDelta) }
    }

    class GraphDatenObjektKarteKontext {
        var objektDatenId = null as GraphDatenId?
        var pos = IntOffset.Zero

    }
    class GraphDatenObjektKarteZustand {
        private val dimension = mutableStateOf(IntSize.Zero)
        private val zoom = mutableFloatStateOf(1f)
        private val pos = mutableStateOf(Offset.Zero)
        private val einstellung = mutableStateOf(Pair(RasterArt.Punkte,RasterTesselation.Trigon))

        public fun erhaltePos() = pos.value
        public fun erhalteZoom() = zoom.floatValue
        public fun erhalteArt() = einstellung.value.first
        public fun erhalteTesselation() = einstellung.value.second
        public fun setzeDimension(neueDimension: IntSize) { dimension.value = neueDimension }

        public fun verschiebe(delta: Offset) { pos.value += delta }
        public fun zoome(delta: Float) { zoom.floatValue = (zoom.floatValue * delta).coerceIn(MIN_ZOOM,MAX_ZOOM) }
        public fun transformiere(verschiebung: Offset,zoom: Float) { verschiebe(verschiebung); zoome(zoom) }

        public fun erhalteViewportRect(
            breite: Float = dimension.value.width.toFloat(),
            höhe: Float = dimension.value.height.toFloat(),
            puffer: Float = 200f,
        ) = RectF(
            -pos.value.x / zoom.floatValue - puffer,
            -pos.value.y / zoom.floatValue - puffer,
            (breite - pos.value.x) / zoom.floatValue + puffer,
            (höhe - pos.value.y) / zoom.floatValue + puffer,
        )

        public companion object {
            public const val MIN_ZOOM = 0.05f
            public const val MAX_ZOOM = 5f
        }
    }
    data class GraphDatenObjektKarteAuswahl( private val auswahl: SnapshotStateMap<String, List<String>> = mutableStateMapOf()) {
        val knotenIds get() = auswahl.getOrElse("knoten",{ emptyList() })
        val verbindungIds get() = auswahl.getOrElse("verbindung",{ emptyList() })
        val anschlussIds get() = auswahl.getOrElse("anschluss",{ emptyList() })

        val istLeer get() = auswahl.isEmpty()
        val istEinzel get() = auswahl.size == 1
        val istMulti get() = auswahl.size > 1

        public fun wähleKnoten(vararg ids: String) { auswahl["knoten"]?.plus(ids) }
        public fun wähleVerbindung(vararg ids: String) { auswahl["verbindung"]?.plus(ids) }
        public fun wähleAnschluss(vararg ids: String) { auswahl["anschluss"]?.plus(ids) }
        public fun enthält(objekt: GraphDatenObjekt<*>) = when {
            objekt is GraphDatenObjektKnoten<*> -> knotenIds
            objekt is GraphDatenObjektVerbindung<*> -> verbindungIds
            objekt is GraphDatenObjektAnschluss<*> -> anschlussIds
            else -> listOf()
        }.contains(objekt.daten.id)
        public fun leereAuswahl() { auswahl.clear() }
    }

    public companion object {
        @JvmName("IterKnoten2inViewport")
        public fun Iterable<GraphDatenObjektKnoten<*>>.sichtbar() = filter { it.istImViewport() }
        @JvmName("IterVerbindung2inViewport")
        public fun Iterable<GraphDatenObjektVerbindung<*>>.sichtbar() = filter { it.istImViewport() }
    }
}
