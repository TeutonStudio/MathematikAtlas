package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import android.graphics.RectF
import androidx.compose.runtime.State
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphPosition
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Kante
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphCanvasObjekt.Companion.overlaps

interface GraphDatenObjektVerbindung<D: GraphDatenVerbindung>: GraphDatenObjekt<D>, GraphCanvasObjekt {
    public abstract var startKante: Kante
    public abstract val start: State<GraphPosition>
    public abstract var endeKante: Kante
    public abstract val ende: State<GraphPosition>

    public override val zeichnung: DrawScope.() -> Unit
        get() = { drawPath(
            path = erhaltePfad(),
            color = when {
                istSelektiert.value -> graph.selektiertFarbe
                daten.fehler != null -> Color(0xFFDC2626)
                else -> Color(0xFF475569)
            },
            style = Stroke(width = if (istSelektiert.value) 8f else 3f, cap = StrokeCap.Round),
        ) }

    override fun beiKlick(klickPos: Offset) {}

    override fun beiHalten(klickPos: Offset) {}

    override fun beiTransform(
        centroid: Offset,
        zoomDelta: Float,
        panDelta: Offset,
        rotationChange: Float
    ) {}

    public fun istImViewport(viewport: RectF = graph.karte.zustand.erhalteViewportRect()): Boolean = listOf(start.value, ende.value).let { p ->
        val puffer = 80f; RectF(
            p.minOf { it.x } - puffer,
            p.minOf { it.y } - puffer,
            p.maxOf { it.x } + puffer,
            p.maxOf { it.y } + puffer,
        )
    }.overlaps(viewport)

    // TODO herausfinden wie Path und abstand sich in einer Pfad klasse vereinheitlichen lassen
    public fun erhaltePfad(): Path
    public fun abstand(pos: Offset): Offset

    public companion object {

        public fun Offset.dot(other: Offset): Float = x * other.x + y * other.y
        public fun Rect.diagonale(): Offset = Offset(width,height)
        public operator fun Rect.times(other: Float): Offset = diagonale() * other
    }
}