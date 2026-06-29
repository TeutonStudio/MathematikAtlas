package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import android.graphics.RectF
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDaten.Companion.maxOffset
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDaten.Companion.minOffset
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDaten.Companion.toOffset
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphPosition
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Kante
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphCanvasObjekt.Companion.overlaps

interface GraphDatenObjektVerbindung<D: GraphDatenVerbindung>:
    GraphDatenObjekt<D>,
    GraphCanvasObjekt,
    GraphDatenObjekt.Kontext<D>,
    GraphDatenObjekt.Inspektor<D> {
    public abstract var startKante: Kante
    public abstract val start: State<GraphPosition>
    public abstract var endeKante: Kante
    public abstract val ende: State<GraphPosition>

    override fun DrawScope.zeichne() { drawPath(
        path = erhaltePfad(),
        color = when {
            istSelektiert.value -> graph.selektiertFarbe
            daten.fehler != null -> Color(0xFFDC2626)
            else -> Color(0xFF475569)
        },
        style = Stroke(width = if (istSelektiert.value) 8f else 3f, cap = StrokeCap.Round),
    ) }

    override fun beiKlick(klickPos: Offset) {
        graph.karte.auswahl.wähleVerbindung(daten.id)
        graph.karte.ctx.keinKontext()
    }
    override fun beiHalten(klickPos: Offset) {
        graph.karte.auswahl.wähleVerbindung(daten.id)
        graph.karte.ctx.wähle(klickPos,daten)
    }
    override fun beiTransform(centroid: Offset, zoomDelta: Float, panDelta: Offset, rotationChange: Float) {}

    public fun istImViewport(viewport: Rect = graph.karte.zustand.erhalteViewportRect()): Boolean = listOf(start.value, ende.value).let { p -> 80f.let { puffer -> Rect(p.minOffset(puffer), p.maxOffset(puffer)) } }.overlaps(viewport)

    // TODO herausfinden wie Path und abstand sich in einer Pfad klasse vereinheitlichen lassen
    public fun erhaltePfad(): Path
    public fun abstand(pos: Offset): Offset

    @Composable
    override fun BoxScope.KontextFenster(pos: IntOffset) {
        StandardKontextFenster(pos)
    }

    @Composable
    override fun BoxScope.Inspektor() {
        StandardInspektor()
    }

    @Composable
    public fun BoxScope.StandardKontextFenster(pos: IntOffset = graph.karte.ctx.pos) {
        Card(Modifier.offset { pos }.padding(4.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text(daten.label ?: daten.id)
                Text("Von: ${daten.ids.knotenIdMann}.${daten.ids.anschlussIdMann}")
                Text("Nach: ${daten.ids.knotenIdWeib}.${daten.ids.anschlussIdWeib}")
            }
        }
    }

    @Composable
    public fun BoxScope.StandardInspektor() {
        Card(Modifier.padding(12.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text(daten.label ?: daten.id)
                Text("Von: ${daten.ids.knotenIdMann}.${daten.ids.anschlussIdMann}")
                Text("Nach: ${daten.ids.knotenIdWeib}.${daten.ids.anschlussIdWeib}")
                daten.fehler?.let { Text("Fehler: $it") }
            }
        }
    }

    public companion object {

        public fun Offset.dot(other: Offset): Float = x * other.x + y * other.y
        public fun Rect.diagonale(): Offset = Offset(width,height)
        public operator fun Rect.times(other: Float): Offset = diagonale() * other
    }
}
