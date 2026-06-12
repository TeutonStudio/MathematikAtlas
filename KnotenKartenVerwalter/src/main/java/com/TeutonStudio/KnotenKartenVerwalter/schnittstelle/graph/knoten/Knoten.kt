package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten

import android.graphics.RectF
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import com.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.daten.knoten.AnschlussKnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.fillMaxKante
import com.TeutonStudio.KnotenKartenVerwalter.offsetKante
import com.TeutonStudio.KnotenKartenVerwalter.radius
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphCache
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.Anschluss.Companion.zuLeiste
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.erzeugeAnschluss

sealed class Knoten(
    override val graph: Graph,
    override val daten: AnschlussKnotenDaten,
): GraphKnotenObjekt<AnschlussKnotenDaten> {
    override var layoutCoordinates: LayoutCoordinates? = null
    public val anschlüsse by GraphCache(daten.anschlüsse) { d: AnschlussDaten ->
        anschlussFabrik.erzeugeAnschluss(graph,d,this).apply { registriere() }
    }

    override fun definiereVerbindung() {
        println("Verbindung gezogen")
    }

    @Composable
    override fun BoxScope.erhalteDarstellung() {
        Inhalt()
        AnschlussKante.entries.forEach { kante ->
            val modi = Modifier
                .fillMaxKante(kante)
                .offsetKante(kante, radius(kante))
            Box(
                modifier = modi.align(kante.alignment()),
                contentAlignment = Alignment.Center,
            ) { anschlüsse.associateWith { (daten.anschlussIdx[it.daten.id] ?: 0) }.zuLeiste(kante) }
        }
    }

    @Composable
    override fun erhalteInspektor() {
        Card(Modifier.padding(25.dp)) {
            Column(Modifier.padding(15.dp)) {
                Text("Inpektor: ${daten.name}")
            }
        }
    }

    @Composable public fun Inhalt() = Card(modifier = Modifier, border = if (istSelektiert.value) BorderStroke(4.dp,graph.selektiertFarbe) else null) {Column(Modifier.padding(15.dp)) { Kopfzeile(); Textzeile(); Fußzeile() }}

    @Composable public fun Kopfzeile() = Text(daten.name)
    @Composable public abstract fun Textzeile()
    @Composable public abstract fun Fußzeile()

    @Composable
    override fun erhalteKontextFenster(pos: BildschirmPosition) {
        Box(
            modifier = Modifier
                .offset { pos }
                .padding(vertical = 4.dp),
        ) {
            Card() {
                Column(Modifier.padding(5.dp),horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("id: ${daten.id}",Modifier.scale(.9f),Color.Gray)
                    Text("löschen",Modifier.clickable() { graph.karte.vernichteKnoten(this@Knoten) })
                    Text("duplizieren",Modifier.clickable() { graph.karte.dupliziereKnoten(this@Knoten) })
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

//    public fun KartenPosition.zuBildAusKnoten(zustand: KarteZustand = graph.karte.zustand): BildschirmPosition = (this + daten.position).round()

    public companion object {
        @Composable public fun Iterable<Knoten>.zuComposable() = forEach { it.zuComposable() }
    }

}
