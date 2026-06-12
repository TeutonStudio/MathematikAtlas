package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import com.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import com.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Graph
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphObjekt
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.AnschlussKante

sealed class Verbindung(
    graph: Graph,
    daten: VerbindungDaten,
): GraphObjekt<VerbindungDaten>(graph,daten) {
    public abstract var startKante: AnschlussKante // TODO herausfinden ob State oder var besser ist
    public abstract val start: State<KartenPosition>
    public abstract var endeKante: AnschlussKante // TODO herausfinden ob State oder var besser ist
    public abstract val ende: State<KartenPosition>

    @Composable
    override fun zuComposable(modifier: Modifier) = Canvas(modifier = modifier) { zeichnung() }

    public val zeichnung: DrawScope.() -> Unit
        get() = {
            drawPath(
                path = erhaltePfad(),
                color = when {
                    istSelektiert -> graph.selektiertFarbe
                    daten.fehler != null -> Color(0xFFDC2626)
                    else -> Color(0xFF475569)
                },
                style = Stroke(width = if (istSelektiert) 8f else 3f, cap = StrokeCap.Round),
            )
        }

    public abstract fun erhaltePfad(): Path

    public abstract fun abstand(pos: KartenPosition): Offset

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
                    Text("Kontextfenster der Verbindung")
                }
            }
        }
    }

    public companion object {
        @Composable
        public fun Iterable<Verbindung>.zuComposable(modifier: Modifier = Modifier) {
            if (this.count() == 0) return
            Canvas(modifier = modifier) { forEach { verbindung -> verbindung.zeichnung(this) } }
        }
    }
}