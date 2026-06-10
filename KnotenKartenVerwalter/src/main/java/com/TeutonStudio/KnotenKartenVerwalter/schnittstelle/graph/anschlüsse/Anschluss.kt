package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import com.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.IDEhe
import com.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.fillMaxKante
import com.TeutonStudio.KnotenKartenVerwalter.filterKante
import com.TeutonStudio.KnotenKartenVerwalter.idReferenz
import com.TeutonStudio.KnotenKartenVerwalter.pos
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphObjekt
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.BasisVerbindung
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.BezierVerbindung
import kotlin.collections.component1
import kotlin.collections.component2

sealed class Anschluss(
    _graph: Graph
): GraphObjekt(_graph) {
    public abstract override val daten: AnschlussDaten
    public abstract val besitzer: Knoten
//    public var partner: Anschluss?

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
                detectTapGestures(
                    onLongPress = {
                        graph.ctx = daten.id to it.round()
//                        graph.wähle(daten.zuAuswahl())
                    },
                    onTap = {
//                        graph.wähle(daten.zuAuswahl())
                        graph.keinKontext()
                    },
                )
                detectDragGestures(
                    onDragStart = {
                        graph.keinKontext()
                        graph.wähle()

                        val start = derivedStateOf { erhaltePosition() }

                        _dragPos = start.value
                        dragPos.value = _dragPos

                        val ende = derivedStateOf { dragPos.value }

                        val vDaten = VerbindungDaten(
                            "pseudo",
                            IDEhe(besitzer.daten.id,besitzer.daten.id,daten.id,daten.id),
                        )
                        graph.karte.pseudoVerbindung.value = BezierVerbindung(graph, vDaten, start, ende).apply {
                            startKante = this@Anschluss.daten.kante
                            endeKante = AnschlussKante.Links
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val kartePos = graph.karte.zustand.erhalteUntransformiert(change.position.round())
                        val nA = graph.erhalteVerbindungNachKlick(kartePos)
//                        val dist = (nA.erhaltePosition().zuBild(graph.karte.zustand).toOffset() - change.position).getDistanceSquared()
//                        if (nA.second < 2) println(nA.daten.label)
                        _dragPos += dragAmount
//                        _dragPos = change.position
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
        ) { }
    }

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

    public fun erhaltePosition(): KartenPosition = (besitzer.daten to daten).pos()

    public abstract fun erlaubtVerbindung(daten: Anschluss): Boolean

    public fun istSelbst(zielBesitzer: Knoten?): Boolean = (besitzer.daten.id == zielBesitzer?.daten?.id) ?: false

    public companion object {
        @Composable
        public fun Map<Anschluss,Int>.zuLeiste(kante: AnschlussKante, leisteModifier: Modifier = Modifier) {
            val listeComposable = this.filterKante(kante).map { (anschluss,idx) -> @Composable { anschluss.zuComposable(/*modifier(anschluss.daten,idx)*/) } }
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
    }
}

