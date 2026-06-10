package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

// Compose
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussFabrik
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussKonstruktor
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussRichtung
import com.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import com.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import com.TeutonStudio.KnotenKartenVerwalter.KnotenAnschlüsse
import com.TeutonStudio.KnotenKartenVerwalter.KnotenFabrik
import com.TeutonStudio.KnotenKartenVerwalter.KnotenKonstruktor

// Daten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.AusgangDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.EingangDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.idReferenz
import com.TeutonStudio.KnotenKartenVerwalter.istAusgang
import com.TeutonStudio.KnotenKartenVerwalter.istEingang
import com.TeutonStudio.KnotenKartenVerwalter.pos

// Composables
import com.TeutonStudio.KnotenKartenVerwalter.zuBild
import com.TeutonStudio.KnotenKartenVerwalter.zuKarte
import kotlin.collections.component1
import kotlin.collections.filter

@Suppress("UNCHECKED_CAST")
val BasisAnschlussFabrik: AnschlussFabrik = mapOf(
    BasisAnschluss.ANSCHLUSS_ART to ::BasisAnschluss as AnschlussKonstruktor,
    BasisEingang.ANSCHLUSS_ART to ::BasisEingang as AnschlussKonstruktor,
    BasisAusgang.ANSCHLUSS_ART to ::BasisAusgang as AnschlussKonstruktor,
)

/**
 * Standardgröße und Außenabstand eines Anschlusses.
 *
 * Der Modifier wird von `Knoten.kt` erweitert, wenn ein Anschluss zusätzlich als
 * Drag-Startpunkt für Verbindungen dienen soll.
 */
val AnschlussModifierStandard = Modifier
    .padding(vertical = 4.dp)
    .size(10.dp)



/**
 * Anschluss als Graphobjekt und Elternklasse aller Anschlüsse
 */
abstract class Anschluss(
    _graph: Graph
): GraphObjekt(_graph) {
    public abstract override val daten: AnschlussDaten
    public abstract val besitzer: Knoten
//    public var partner: Anschluss?

    public fun erhaltePosition(): KartenPosition = (besitzer.daten to daten).pos()

    public abstract fun erlaubtVerbindung(daten: Anschluss): Boolean
//    public fun erstelleVerbindung(zu: Anschluss)

    public fun istSelbst(zielBesitzer: Knoten?): Boolean = (besitzer.daten.id == zielBesitzer?.daten?.id) ?: false
}

/**
 * Standard für Anschlüsse
 */
open class BasisAnschluss(
    _graph: Graph,
    override val daten: AnschlussDaten,
    override val besitzer: Knoten,
//    override var partner: Anschluss? = null,
): Anschluss(_graph) {
    val radius
        get() = 5.dp

    private var _dragPos: Offset by mutableStateOf(Offset.Zero)
    private val dragPos: MutableState<Offset> = mutableStateOf(Offset.Zero)

    @Composable
    override fun zuComposable(modifier: Modifier) {
        val farbe = Color.Black
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.size(radius).background(farbe, CircleShape).pointerInput(daten.id) {
                detectDragGestures(
                    onDragStart = {
                        graph.keinKontext()

                        val start = derivedStateOf { erhaltePosition() }

                        _dragPos = start.value
                        dragPos.value = _dragPos

                        val ende = derivedStateOf { dragPos.value }

                        val vDaten = VerbindungDaten(
                            "pseudo",
                            idReferenz(besitzer.daten to daten, besitzer.daten to daten),
                        )
                        graph.karte.pseudoVerbindung.value = BasisVerbindung(graph,vDaten,start,ende).apply {
                            startKante = this@BasisAnschluss.daten.kante
                            endeKante = AnschlussKante.Links
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val nA = graph.inhalt.filterIsInstance<Anschluss>().filter { it.besitzer != besitzer }.minBy {
                            (it.erhaltePosition().zuBild(graph.karte.zustand).toOffset() - change.position).getDistanceSquared()
                        }
                        val dist = (nA.erhaltePosition().zuBild(graph.karte.zustand).toOffset() - change.position).getDistanceSquared()
                        if (dist < 2) println(nA.daten.label)
                        _dragPos += dragAmount
                        dragPos.value = _dragPos
                    },
                    onDragEnd = {
                        // TODO finalisieren
                        graph.karte.pseudoVerbindung.value = null
                    },
                    onDragCancel = {
                        graph.karte.pseudoVerbindung.value = null
                    },
                )
            }
        ) {
            if (öffneKontext.value) erhalteKontextFenster(graph.ctx.second)
        }
    }

    @Composable
    override fun erhalteKontextFenster(pos: BildschirmPosition) {
        TODO("Not yet implemented")
    }

    override fun erlaubtVerbindung(daten: Anschluss): Boolean = !istSelbst(daten.besitzer)

    public companion object {
        public const val ANSCHLUSS_ART = "default"
    }
}

/**
 * Ein Anschluss, der sich nur mit einem Ausgang verbinden lässt.
 * Wenn Verbindung bereits besteht, wird für die neue verbindung die alte gelöscht.
 */
open class BasisEingang(
    _graph: Graph,
    override val daten: EingangDaten,
    override val besitzer: Knoten,
): BasisAnschluss(_graph,daten, besitzer) {

    override fun erlaubtVerbindung(daten: Anschluss): Boolean = super.erlaubtVerbindung(daten) && daten.istAusgang()

    public companion object {
        public const val ANSCHLUSS_ART = "input"
    }
}

/**
 * Ein Anschluss, der sich nur mit Eingängen verbinden lässt
 */
open class BasisAusgang(
    _graph: Graph,
    override val daten: AusgangDaten,
    override val besitzer: Knoten,
): BasisAnschluss(_graph,daten, besitzer) {

    override fun erlaubtVerbindung(daten: Anschluss): Boolean = super.erlaubtVerbindung(daten) && daten.istEingang()

    public companion object {
        public const val ANSCHLUSS_ART = "output"
    }
}
