package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import com.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenAnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.knoten.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.erhalteSize
import com.TeutonStudio.KnotenKartenVerwalter.fillMaxKante
import com.TeutonStudio.KnotenKartenVerwalter.offsetKante
import com.TeutonStudio.KnotenKartenVerwalter.radius
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphCache
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphObjekt
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.Anschluss
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.Anschluss.Companion.zuLeiste
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.AnschlussFabrik
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.alignment
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.erzeugeAnschluss
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.wertFür
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.Karte
import kotlin.collections.toMap

sealed class Knoten(
    graph: Graph,
    daten: KnotenDaten,
): GraphObjekt<KnotenDaten>(graph,daten) {
    public abstract val besitzer: Karte
    public abstract val anschlussFabrik: AnschlussFabrik

    public val anschlüsse by GraphCache(daten.anschlüsse) { d: AnschlussDaten ->
        anschlussFabrik.erzeugeAnschluss(graph,d,this).apply { registriere() }
    }

    @Composable
    public override fun zuComposable(modifierKnoten: Modifier) {
        Box(modifier = Modifier
            .offset { daten.position.zuBild() }
            .size(with(LocalDensity.current) { (daten.erhalteSize() * zoomFaktor()).toDpSize() })
            .draggable2D(
                enabled = daten.beweglich,
                state = rememberDraggable2DState {
                    graph.verschiebeKnoten(daten.id,it)
                    graph.wähle(daten.zuAuswahl())
                    graph.keinKontext()
                }, )
            .pointerInput(daten.id) {
                detectTapGestures(
                    onLongPress = {
                        graph.ctx = daten.id to it.zuBildAusKnoten()
                        graph.wähle(daten.zuAuswahl())
                    },
                    onTap = {
                        graph.wähle(daten.zuAuswahl())
                        graph.keinKontext()
                    },
                ) }
        ) {
            Inhalt(modifierKnoten)
            AnschlussKante.entries.forEach { kante ->
                val modi = Modifier
                    .fillMaxKante(kante)
                    .offsetKante(kante, radius(kante))
                Box(
                    modifier = modi.align(kante.alignment()), //.offset(x = (-5f * skalierung).dp),
                    contentAlignment = Alignment.Center,
                ) { anschlüsse.map { it to (daten.anschlussIdx[it.daten.id] ?: 0) }.toMap().zuLeiste(kante) }
            }
            if (öffneKontext.value) erhalteKontextFenster(graph.ctx.second)
            anschlüsse.forEach {
                if (it.öffneKontext.value) it.erhalteKontextFenster(erhalteAnschlussPos(it.daten.id).zuBildAusKnoten().zuKnoten(this@Knoten).round())
            }
        }
    }

    @Composable public fun Inhalt(modifier: Modifier) = Card(modifier = modifier, border = if (istSelektiert) BorderStroke(4.dp,graph.selektiertFarbe) else null) {Column(Modifier.padding(15.dp)) { Kopfzeile(); Textzeile(); Fußzeile() }}

    @Composable public fun Kopfzeile() = Text(daten.name)
    @Composable public abstract fun Textzeile()
    @Composable public abstract fun Fußzeile()

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
                    Text("Kontextfenster des Knoten")
                }
            }
        }
    }

    private fun relAnteilKante(anschlüsse: Iterable<AnschlussDaten>, aId: String, kante: AnschlussKante): Float {
        val sorter = compareBy<AnschlussDaten> { it.id }
        val anschluesseAnKante = anschlüsse.filter { it.kante == kante }.sortedWith(sorter)
        val indexAnKante = anschluesseAnKante.indexOfFirst { it.id == aId }.coerceAtLeast(0)
        val anzahlAnKante = anschluesseAnKante.size.coerceAtLeast(1)
        return (indexAnKante + 1f) / (anzahlAnKante + 1f)
    }

    public fun erhalteAnschlussPos(aId: String): KartenPosition {
        val kante = daten.anschlüsse.find { it.id == aId }?.kante ?: AnschlussKante.Rechts
        val anteil = relAnteilKante(daten.anschlüsse,aId,kante)

        return Offset(
            x = kante.wertFür(
                daten.position.x,
                daten.position.x + daten.dimension.width,
                daten.position.x + daten.dimension.width * anteil,
                daten.position.x + daten.dimension.width * anteil
            ),
            y = kante.wertFür(
                daten.position.y + daten.dimension.height * anteil,
                daten.position.y + daten.dimension.height * anteil,
                daten.position.y,
                daten.position.y + daten.dimension.height
            ),
        )
    }

    private fun zoomFaktor(): Float = graph.karte.zustand.zoom.coerceAtLeast(0.01f)

    public companion object {
        @Composable
        public fun Iterable<Knoten>.zuComposable(
            modifierKnoten: (KnotenDaten) -> Modifier = { d -> Modifier}
        ) = forEach { it.zuComposable(modifierKnoten(it.daten)) }
    }

}
