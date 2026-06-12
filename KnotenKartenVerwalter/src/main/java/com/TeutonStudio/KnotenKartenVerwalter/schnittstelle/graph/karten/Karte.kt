package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.TeutonStudio.KnotenKartenVerwalter.AuswahlÄndern
import com.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import com.TeutonStudio.KnotenKartenVerwalter.KartenAktualisierung
import com.TeutonStudio.KnotenKartenVerwalter.KontextAktionAusführen
import com.TeutonStudio.KnotenKartenVerwalter.VerbindungErstellen
import com.TeutonStudio.KnotenKartenVerwalter.daten.AuswahlDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.AuswahlDaten.Companion.zuAuswahl
import com.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteZustand
import com.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenAnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.printLogCat
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphCache
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphObjekt
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten.Companion.zuComposable
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenFabrik
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.erzeugeKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.Verbindung
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.Verbindung.Companion.zuComposable
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.VerbindungFabrik
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.erzeugeVerbindung


sealed class Karte(
    graph: Graph,
    daten: KarteDaten,
): GraphObjekt<KarteDaten>(graph,daten) {
    abstract val zustand: KarteZustand
    abstract val knotenFabrik: KnotenFabrik
    abstract val verbindungFabrik: VerbindungFabrik
    abstract val pseudoVerbindung: MutableState<Verbindung?>
    abstract val aktualisierung: KartenAktualisierung
    abstract val onVerbindungErstellen: VerbindungErstellen
    abstract val onKontextAktion: KontextAktionAusführen
    abstract val onAuswahlÄndern: AuswahlÄndern

    private val VERBINDUNG_TREFFER_RADIUS = 50f

    val knoten by GraphCache(daten.knoten) { d: KnotenDaten ->
        knotenFabrik.erzeugeKnoten(graph,d,this).apply { registriere() }
    }

    val verbindungen by GraphCache(daten.verbindungen) { d: VerbindungDaten ->
        val knotenMann = knoten.find { it.daten.id == d.ids.knotenIdMann }
        val knotenWeib = knoten.find { it.daten.id == d.ids.knotenIdWeib }
        if (knotenMann == null && knotenWeib == null) null
        val anschlussMann = knotenMann!!.anschlüsse.find { it.daten.id == d.ids.anschlussIdMann }
        val anschlussWeib = knotenWeib!!.anschlüsse.find { it.daten.id == d.ids.anschlussIdWeib }
        if (anschlussMann == null && anschlussWeib == null) null
        verbindungFabrik.erzeugeVerbindung(graph,d,
            derivedStateOf { knotenMann.erhalteAnschlussPos(anschlussMann!!.daten.id) },
            derivedStateOf { knotenWeib.erhalteAnschlussPos(anschlussWeib!!.daten.id) },
        )?.apply {
            startKante = anschlussMann!!.daten.kante
            endeKante = anschlussWeib!!.daten.kante
            registriere()
        }
    }

    val anschlüsse get() = knoten.flatMap { it.anschlüsse }

    @Composable
    override fun zuComposable(modifier: Modifier) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clipToBounds()
                .draggable2D(
                    state = rememberDraggable2DState {
                        zustand.verschiebe(it)
                        onAuswahlÄndern(AuswahlDaten.LEER)
                        graph.keinKontext()
                    }
                )
                .pointerInput(daten.id) {
                    detectTapGestures(
                        onTap = {
                            // TODO herausfinden, wie ich it. tranformieren muss
                            val kartePos = zustand.erhalteUntransformiert(it.round())
                            val v = graph.erhalteVerbindungNachPos(kartePos)?.apply {
                                printLogCat(first, second, second.getDistanceSquared())
                                if (second.getDistanceSquared() < VERBINDUNG_TREFFER_RADIUS) {
                                    graph.wähle(first.daten.zuAuswahl())
                                } else {
                                    graph.wähle()
                                }
                            }
                            if (v == null) {
                                graph.wähle()
                            }
                            graph.keinKontext()
                        },
                        onLongPress = {
                            val karteCTX = { graph.ctx = daten.id to it.round() }
                            val kartePos = zustand.erhalteUntransformiert(it.round())
                            val v = graph.erhalteVerbindungNachPos(kartePos)?.apply {
                                printLogCat(first, second, second.getDistanceSquared())
                                if (second.getDistanceSquared() < VERBINDUNG_TREFFER_RADIUS) {
                                    graph.wähle(first.daten.zuAuswahl())
                                    graph.ctx = first.daten.id to it.round()
                                    return@detectTapGestures
                                } else {
                                    karteCTX()
                                }
                            }
                            if (v == null) karteCTX()
                        },
                    )
                }
        ) {
            verbindungen.zuComposable()
            pseudoVerbindung.value?.zuComposable()
            knoten.zuComposable()
            if (öffneKontext.value) erhalteKontextFenster(graph.ctx.second)
            verbindungen.forEach {
                if (it.öffneKontext.value) it.erhalteKontextFenster(graph.ctx.second)
            }
        }
    }

    @Composable
    override fun erhalteKontextFenster(
        pos: BildschirmPosition
    ) {
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
                    Text("Kontextfenster der Karte")
                }
            }
        }
    }

}
