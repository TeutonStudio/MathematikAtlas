package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.dp
import com.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import com.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.overlaps
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjekt

sealed class Verbindung(
    override val graph: Graph,
    override val daten: VerbindungDaten,
): GraphDatenObjekt<VerbindungDaten> {
    override var layoutCoordinates: LayoutCoordinates? = null
    public abstract var startKante: AnschlussKante // TODO herausfinden ob State oder var besser ist
    public abstract val start: State<KartenPosition>
    public abstract var endeKante: AnschlussKante // TODO herausfinden ob State oder var besser ist
    public abstract val ende: State<KartenPosition>

    @Composable override fun Modifier.modifier() = this

    @Composable
    override fun zuComposable(modifier: Modifier) = Canvas(modifier = modifier) { zeichnung() }
    @Composable override fun BoxScope.erhalteDarstellung() = TODO("Nicht benötigt für Verbindung")

    public val zeichnung: DrawScope.() -> Unit
        get() = {
            drawPath(
                path = erhaltePfad(),
                color = when {
                    istSelektiert.value -> graph.selektiertFarbe
                    daten.fehler != null -> Color(0xFFDC2626)
                    else -> Color(0xFF475569)
                },
                style = Stroke(width = if (istSelektiert.value) 8f else 3f, cap = StrokeCap.Round),
            )
        }

    public abstract fun erhaltePfad(): Path

    public abstract fun abstand(pos: KartenPosition): Offset

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
                    Text("löschen",Modifier.clickable() { graph.karte.vernichteVerbindung(this@Verbindung) })
                }
            }
        }
    }

    fun istImViewport(viewport: RectF = graph.karte.zustand.erhalteViewportRect()): Boolean = listOf(start.value, ende.value).let { p ->
            val puffer = 80f
            RectF(
                p.minOf { it.x } - puffer,
                p.minOf { it.y } - puffer,
                p.maxOf { it.x } + puffer,
                p.maxOf { it.y } + puffer,
            )
        }.overlaps(viewport)


    public companion object {
        @Composable
        public fun Iterable<Verbindung>.zuComposable(modifier: Modifier = Modifier) {
            if (this.count() == 0) return
            Canvas(modifier = modifier) { forEach { verbindung -> verbindung.zeichnung(this) } }
        }

        public fun Iterable<Verbindung>.sichtbar() = filter { it.istImViewport() }
    }
}