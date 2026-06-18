package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import android.graphics.RectF
import androidx.compose.runtime.State
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.toOffset
import de.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDatenVerbindung
import de.TeutonStudio.KnotenKartenVerwalter.daten.Kante
import de.TeutonStudio.KnotenKartenVerwalter.overlaps

interface GraphDatenObjektVerbindung<D: GraphDatenVerbindung>: GraphDatenObjekt<D>, GraphCanvasObjekt {
    public abstract var startKante: Kante
    public abstract val start: State<KartenPosition>
    public abstract var endeKante: Kante
    public abstract val ende: State<KartenPosition>

    public override val zeichnung: DrawScope.() -> Unit
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

    public fun istImViewport(viewport: RectF = graph.karte.zustand.erhalteViewportRect()): Boolean = listOf(start.value, ende.value).let { p ->
        val puffer = 80f
        RectF(
            p.minOf { it.x } - puffer,
            p.minOf { it.y } - puffer,
            p.maxOf { it.x } + puffer,
            p.maxOf { it.y } + puffer,
        )
    }.overlaps(viewport)

    public fun erhaltePfad(): Path
}