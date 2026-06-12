package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.IDEhe
import com.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.fillMaxKante
import com.TeutonStudio.KnotenKartenVerwalter.filterKante
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjekt
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.BezierVerbindung
import kotlin.collections.component1
import kotlin.collections.component2

typealias GraphAnschluss = Anschluss<out AnschlussDaten>

sealed class Anschluss<D: AnschlussDaten>(
    override val graph: Graph,
    override val daten: D,
): GraphAnschlussObjekt<D> {
//    public val karte get() = besitzer.besitzer
//    public abstract val besitzer: Knoten
    //    public var partner: Anschluss?
    val pos get() = besitzer.erhalteAnschlussPos(daten.id)

    private var dragPos by mutableStateOf(Offset.Zero)
    private var dragZiel by mutableStateOf<Anschluss<out AnschlussDaten>?>(null)

    public val modifier get() = Modifier
        .size(5.dp)
        .background(Color.Black, CircleShape)
        .pointerInput(daten.id) {
            detectDragGestures(
                onDragStart = ::beiVerbindungZiehenStart,
                onDrag = ::beiVerbindungZiehenDelta,
                onDragEnd = ::beiVerbindungZiehenEnde,
                onDragCancel = ::beiVerbindungZiehenAbbruch,
            )
        }

    @Composable
    override fun Modifier.modifier(): Modifier = tapping()
        .size(5.dp)
        .background(Color.Black, CircleShape)
        .pointerInput(daten.id) {
            detectDragGestures(
                onDragStart = ::beiVerbindungZiehenStart,
                onDrag = ::beiVerbindungZiehenDelta,
                onDragEnd = ::beiVerbindungZiehenEnde,
                onDragCancel = ::beiVerbindungZiehenAbbruch,
            )
        }

    override fun beiKlick(klickPos: Offset) {
//                        graph.wähle(daten.zuAuswahl())
        karte.keinKontext()
    }

    override fun beiHalten(klickPos: Offset) {
        karte.ctx = daten.id to klickPos.round()
//                        graph.wähle(daten.zuAuswahl())
    }

    override fun beiTransform(
        centroid: Offset,
        zoomDelta: Float,
        panDelta: Offset,
        rotationChange: Float
    ) {}

    @Composable
    override fun BoxScope.erhalteDarstellung() {}

    @Composable
    override fun erhalteKontextFenster(pos: BildschirmPosition) {
        Box(
            modifier = Modifier
                .offset { pos }
//                .onSizeChanged { fensterGröße = it }
                .background(Color.White, RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(8.dp))
                .padding(vertical = 4.dp),
        ) {
            Card() {
                Column {
                    Text("Kontextfenster des Anschluss")
                }
            }
        }
    }

    public open fun beiVerbindungZiehenStart(klickPos: Offset) {
        karte.keinKontext()
        karte.wähle()
        dragPos = pos

        karte.pseudoVerbindung.value = BezierVerbindung(
            graph, VerbindungDaten(
                "pseudo",
                IDEhe(
                    besitzer.daten.id,
                    besitzer.daten.id,
                    daten.id,
                    daten.id,
                ),
            ),
            derivedStateOf { pos },
            derivedStateOf { dragZiel?.pos ?: dragPos }
        ).apply {
            startKante = this@Anschluss.daten.kante
            endeKante = AnschlussKante.Links
        }
    }
    public open fun beiVerbindungZiehenDelta(change: PointerInputChange, dragAmount:Offset) {
        change.consume()
        dragPos += dragAmount.round().zuDelta()
        if (graph.erhalteAnschlussNachPos(dragPos)?.apply {
                val bedingung = second.getDistanceSquared() < 500f && erlaubtVerbindung(first)
                if (bedingung) {
                    dragZiel = first
                    karte.pseudoVerbindung.value?.endeKante = first.daten.kante
                    return
                } else dragZiel = null
            } == null) dragZiel = null
    }
    public open fun beiVerbindungZiehenEnde() {
        dragZiel?.let { karte.definiereVerbindung(this@Anschluss,it) }
        karte.pseudoVerbindung.value = null
    }
    public open fun beiVerbindungZiehenAbbruch() {
        karte.pseudoVerbindung.value = null
    }


    public fun abstand(anschluss: Anschluss<out AnschlussDaten>): Offset = anschluss.pos - pos

    public fun istSelbst(zielBesitzer: Knoten?): Boolean = (besitzer.daten.id == zielBesitzer?.daten?.id) ?: false
    public open fun erlaubtVerbindung(anschluss: Anschluss<out AnschlussDaten>): Boolean = anschluss != this


    public open fun istEingang(): Boolean = false
    public open fun istAusgang(): Boolean = false

    public companion object {
        @Composable
        public fun Map<Anschluss<out AnschlussDaten>,Int>.zuLeiste(kante: AnschlussKante, leisteModifier: Modifier = Modifier) {
            val listeComposable = this.filterKante(kante).map { (anschluss,idx) -> @Composable { anschluss.zuComposable() } }
            if (kante.istVertikal()) Column(
                modifier = leisteModifier.fillMaxKante(kante),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) { listeComposable.forEach { it() } }
            else if (kante.istHorizontal()) Row(
                modifier = leisteModifier.fillMaxKante(kante),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) { listeComposable.forEach { it() } }
            else TODO("Sollte nicht passieren")
        }

        public fun Iterable<Anschluss<out AnschlussDaten>>.findeNachId(id:String) = find { it.daten.id == id }
    }
}

