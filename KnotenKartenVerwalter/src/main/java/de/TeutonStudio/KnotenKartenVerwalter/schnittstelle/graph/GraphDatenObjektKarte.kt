package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import android.R.attr.scaleX
import android.R.attr.scaleY
import android.R.attr.translationX
import android.R.attr.translationY
import android.graphics.RectF
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntSize
import de.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDatenAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDatenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDatenKarte
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDatenKnoten
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDatenVerbindung
//import de.TeutonStudio.KnotenKartenVerwalter.daten.auswahl.AuswahlDaten
//import de.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteZustand
//import de.TeutonStudio.KnotenKartenVerwalter.daten.knoten.AnschlussKnotenDaten
//import de.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.VerbindungDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphCanvasObjekt.Companion.Composable as VComposable
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjekt.Companion.Composable as KComposable
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjektKnoten.Companion.anschlüsseNachIDEhe
//import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.GraphKnotenObjekt.Companion.anschlüsseNachIDEhe
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.erzeugeKnoten
//import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.Verbindung
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.VerbindungFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.erzeugeVerbindung

typealias Zustand = GraphDatenObjektKarte.GraphDatenObjektKarteZustand
typealias Auswahl = GraphDatenObjektKarte.GraphDatenObjektKarteAuswahl
typealias Kontext = GraphDatenObjektKarte.GraphDatenObjektKarteKontext

interface GraphDatenObjektKarte<D: GraphDatenKarte>: GraphDatenObjekt<D> {
    abstract val knotenFabrik: KnotenFabrik
    abstract val verbindungFabrik: VerbindungFabrik

    val ctx: Kontext
    val zustand: Zustand
    val auswahl: Auswahl
    abstract val pseudoVerbindung: MutableState<GraphDatenObjektVerbindung<*>?>

    val knoten get() = GraphCache(daten.knoten) { d: GraphDatenKnoten ->
        knotenFabrik.erzeugeKnoten(graph,d,this).apply { registriere() }
    }.erhalte()

    val verbindungen get() = GraphCache(daten.verbindungen) { d: GraphDatenVerbindung ->
        knoten.anschlüsseNachIDEhe(d.ids)?.let { (a1,a2) -> verbindungFabrik.erzeugeVerbindung(
            graph,d,
            derivedStateOf { a1.pos },
            derivedStateOf { a2.pos },
        )?.apply {
            startKante = a1.daten.kante
            endeKante = a2.daten.kante
        } }?.apply { registriere() }
    }.erhalte()

    val anschlüsse get() = knoten.flatMap { it.anschlüsse }

    @Composable public override fun BoxScope.Darstellung() {
        Box(Modifier.fillMaxSize().graphicsLayer {
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

    public fun erhalteAuswahl() = listOf(erhalteKnotenAuswahl(),erhalteVerbindungAuswahl(),erhalteAnschlussAuswahl()).flatten()
    public fun erhalteKnotenAuswahl() = knoten.filter { it.daten.id in auswahl.knotenIds }
    public fun erhalteVerbindungAuswahl() = verbindungen.filter { it.daten.id in auswahl.verbindungIds }
    public fun erhalteAnschlussAuswahl() = anschlüsse.filter { it.daten.id in auswahl.anschlussIds }

    fun verschiebeKnoten(id: String, panDelta: Offset) {
        val knoten = knoten.filter { it.daten.id in auswahl.knotenIds }.filterIsInstance<GraphDaten.bewegbareGD>()
        if (knoten.isEmpty()) TODO("${id} ist kein [GraphDaten.bewegbareGD]")
        knoten.first().verschiebeKnoten(panDelta)
    }

    class GraphDatenObjektKarteKontext {
        val objektDatenId = null as GraphDatenId?
        val pos = BildschirmPosition.Zero

    }
    class GraphDatenObjektKarteZustand {
        private val dimension = mutableStateOf(IntSize.Zero)
        private val zoom = mutableFloatStateOf(1f)
        private val pos = mutableStateOf(Offset.Zero)

        public fun erhaltePos(): Offset = pos.value
        public fun erhalteZoom(): Float = zoom.floatValue

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
    class GraphDatenObjektKarteAuswahl {
        val knotenIds get() = auswahl.getOrElse("knoten",{ emptyList() })
        val verbindungIds get() = auswahl.getOrElse("verbindung",{ emptyList() })
        val anschlussIds get() = auswahl.getOrElse("anschluss",{ emptyList() })

        val istLeer get() = auswahl.isEmpty()
        val istEinzel get() = auswahl.size == 1
        val istMulti get() = auswahl.size > 1

        public fun wähleKnoten(ids: String) { knotenIds.plus(ids) }
        public fun wähleVerbindung(ids: String) { verbindungIds.plus(ids) }
        public fun wähleAnschluss(ids: String) { anschlussIds.plus(ids) }
        public fun enthält(objekt: GraphDatenObjekt<*>) = when {
            objekt is GraphDatenObjektKnoten<*> -> knotenIds
            objekt is GraphDatenObjektVerbindung<*> -> verbindungIds
            objekt is GraphDatenObjektAnschluss<*> -> anschlussIds
            else -> listOf()
        }.contains(objekt.daten.id)
        public fun leereAuswahl() { auswahl.clear() }
        private val auswahl: SnapshotStateMap<String, Iterable<String>> = mutableStateMapOf()
    }

    public companion object {
        public fun Iterable<GraphDatenObjektKnoten<*>>.sichtbar() = filter { it.istImViewport() }
        public fun Iterable<GraphDatenObjektVerbindung<*>>.sichtbar() = filter { it.istImViewport() }
    }
}