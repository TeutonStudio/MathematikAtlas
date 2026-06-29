package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.zIndex
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDaten.Companion.toOffset
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenKarte
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphCanvasObjekt.Companion.Composable as VComposable
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjekt.Companion.Composable as KComposable
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphHintergrund.RasterArt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphHintergrund.RasterTesselation
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.KnotenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.VerbindungFabrik

typealias Zustand = GraphDatenObjektKarte.GraphDatenObjektKarteZustand
typealias Auswahl = GraphDatenObjektKarte.GraphDatenObjektKarteAuswahl
typealias Kontext = GraphDatenObjektKarte.GraphDatenObjektKarteKontext

interface GraphDatenObjektKarte<D: GraphDatenKarte>: GraphDatenObjekt<D>, GraphDatenObjekt.Kontext<D> {
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

//    private fun kontextObjekte(): List<GraphDatenObjekt<*>> = listOf(listOf(this), knoten, verbindungen, anschlüsse).flatten()

    override val objektModifier: Modifier @Composable get() =  Modifier.modiInputEvent().clipToBounds()

    @Composable public override fun BoxScope.Darstellung() {
        Box(Modifier.fillMaxSize().onSizeChanged {
                zustand.setzeDimension(it)
                daten.breite = it.width.toFloat()
                daten.tiefe = it.height.toFloat()
            }
            .graphicsLayer {
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
        with(ctx) { KontextComposable(graph.inhalt) }
        with(auswahl) { InspektorComposable(graph.inhalt) }
//        kontextObjekte().forEach { if (it.öffneKontext.value) it.ComposableKontext() }
//        erhalteAuswahl().singleOrNull()?.let {
//            Box(Modifier.align(Alignment.CenterEnd)) { it.ComposableInspektor() }
//        }
    }

    override fun beiKlick(klickPos: Offset) {
        erhalteVerbindungNachPos(klickPos)?.apply { val (v,o) = this
            if (o.getDistanceSquared() < 50f) {
                auswahl.wähleVerbindung(v.daten.id)
                ctx.keinKontext()
                return
            }
        }

        auswahl.leereAuswahl()
        ctx.keinKontext()
    }

    override fun beiHalten(klickPos: Offset) {
        erhalteVerbindungNachPos(klickPos)?.apply { val (v,o) = this
            if (o.getDistanceSquared() < 50f) {
                auswahl.wähleVerbindung(v.daten.id)
                ctx.wähle(klickPos,v.daten)
                return
            }
        }
        auswahl.leereAuswahl()
        ctx.wähle(klickPos,daten)
    }

    override fun beiTransform(
        centroid: Offset,
        zoomDelta: Float,
        panDelta: Offset,
        rotationChange: Float
    ) {
        ctx.keinKontext()
        zustand.transformiere(panDelta,zoomDelta)
    }

    public fun erhalteVerbindungNachPos(pos: Offset): Pair<GraphDatenObjektVerbindung<*>,Offset>? { erhalteVerbindungNachPosPseudo(pos)?.apply { println(second.getDistanceSquared()); return this }; return null }
    public fun erhalteVerbindungNachPosPseudo(pos: Offset): Pair<GraphDatenObjektVerbindung<*>,Offset>? = verbindungen.map { it to it.abstand(pos) }.minByOrNull { it.second.getDistanceSquared() }
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

    public fun planeVerbindung(vonAnschluss: GraphDatenObjektAnschluss<*>,vonKnoten: GraphDatenObjektKnoten<*>) {
        knoten.forEach { it.planeVerbindung(vonAnschluss,vonKnoten) }
    }
    public fun verwerfeGeplanteVerbindung() {
        knoten.forEach { it.verwerfeGeplanteVerbindung() }
    }
    public fun definiereVerbindung(mann: GraphDatenObjektAnschluss<*>, weib: GraphDatenObjektAnschluss<*>) {
        knoten.forEach { it.definiereVerbindung(mann,weib) }
    }



    /*= daten.verbindungen.add(VerbindungDaten(mann,weib,"",null)).apply {
        if (weib.daten is AusgangDaten && mann.besitzer is PullObjekt) (mann.besitzer as PullObjekt).aktualisiereCache()
        if (mann.daten is AusgangDaten && weib.besitzer is PullObjekt) (weib.besitzer as PullObjekt).aktualisiereCache()
//        mann.besitzer.definiereVerbindung()
        weib.besitzer.definiereVerbindung()
        keinKontext()
    }*/

//    public fun erhalteAuswahl() = listOf(erhalteKnotenAuswahl(),erhalteVerbindungAuswahl(),erhalteAnschlussAuswahl()).flatten()
//    public fun erhalteKnotenAuswahl() = knoten.filter { it.daten.id in auswahl.knotenIds }
//    public fun erhalteVerbindungAuswahl() = verbindungen.filter { it.daten.id in auswahl.verbindungIds }
//    public fun erhalteAnschlussAuswahl() = anschlüsse.filter { it.daten.id in auswahl.anschlussIds }

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
        var objektDatenId by mutableStateOf(null as GraphDatenId?)
        var pos by mutableStateOf(IntOffset.Zero)

        @Composable public fun BoxScope.KontextComposable(graphInhalt: Iterable<GraphObjekt>) {
            graphInhalt
                .filterIsInstance<GraphDatenObjekt.Kontext<*>>()
                .forEach { if (it.öffneKontext.value) Box(Modifier.zIndex(30f)) { it.ComposableKontext() } }
        }
        public fun keinKontext() { objektDatenId = null }
        public fun wähle(klickPos: Offset, daten: GraphDaten) { pos = klickPos.round(); objektDatenId = daten.id}
        public fun wähle(klickPos: IntOffset, daten: GraphDaten) { pos = klickPos; objektDatenId = daten.id}
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
        public fun bildschirmZuWelt(position: Offset): Offset =
            (position - pos.value) / zoom.floatValue.coerceAtLeast(0.0001f)

        public fun verschiebe(delta: Offset) { pos.value += delta }
        public fun zoome(delta: Float) { zoom.floatValue = (zoom.floatValue * delta).coerceIn(MIN_ZOOM,MAX_ZOOM) }
        public fun transformiere(verschiebung: Offset,zoom: Float) { verschiebe(verschiebung); zoome(zoom) }
        public fun zentriereAuf(weltPosition: Offset) { pos.value = dimension.value.center.toOffset() - weltPosition * zoom.floatValue }

        public fun erhalteViewportRect(
            breite: Float = dimension.value.width.toFloat(),
            tiefe: Float = dimension.value.height.toFloat(),
            puffer: Float = 200f,
        ) = Rect(
            -pos.value / zoom.floatValue - puffer.toOffset(),
            (Offset(breite,tiefe) - pos.value) / zoom.floatValue + puffer.toOffset()
        )
        public companion object {
            public const val MIN_ZOOM = 0.05f
            public const val MAX_ZOOM = 5f
        }
    }
    data class GraphDatenObjektKarteAuswahl(private val auswahl: SnapshotStateMap<String, List<String>> = mutableStateMapOf()) {
        private val mehrfachAuswahl = mutableStateOf(false)

        val knotenIds get() = auswahl.getOrElse("knoten",{ emptyList() })
        val verbindungIds get() = auswahl.getOrElse("verbindung",{ emptyList() })
        val anschlussIds get() = auswahl.getOrElse("anschluss",{ emptyList() })
        val istMehrfachAuswahl get() = mehrfachAuswahl.value

        val anzahl get() = auswahl.values.sumOf { it.size }
        val istLeer get() = anzahl == 0
        val istEinzel get() = anzahl == 1
        val istMulti get() = anzahl > 1

        @Composable public fun BoxScope.InspektorComposable(graphInhalt: Iterable<GraphObjekt>) {
            Box(Modifier.align(Alignment.CenterEnd).zIndex(20f)) {
                graphInhalt
                    .filterIsInstance<GraphDatenObjekt.Inspektor<*>>()
                    .forEach { if (it.istSelektiert.value) it.ComposableInspektor() }
            }
        }

        private fun wähle(schlüssel: String, ids: Array<out String>) {
            if (!istMehrfachAuswahl) leereAuswahl()
            auswahl[schlüssel] = (auswahl[schlüssel].orEmpty() + ids).distinct()
        }

        public fun setzeMehrfachAuswahl(aktiv: Boolean) {
            mehrfachAuswahl.value = aktiv
        }
        public fun wechsleMehrfachAuswahl() {
            setzeMehrfachAuswahl(!istMehrfachAuswahl)
        }
        public fun wähleKnoten(vararg ids: String) { wähle("knoten", ids) }
        public fun wähleVerbindung(vararg ids: String) { wähle("verbindung", ids) }
        public fun wähleAnschluss(vararg ids: String) { wähle("anschluss", ids) }
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

        private const val VERBINDUNG_TREFFER_RADIUS = 14f
    }
}
