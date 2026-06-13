package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.zIndex
import de.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.VerbindungDaten
import de.TeutonStudio.KnotenKartenVerwalter.overlaps
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjekt

internal interface GraphVerbindungObjekt<V: VerbindungDaten>: GraphDatenObjekt<V> {
    public abstract var startKante: AnschlussKante // TODO herausfinden ob State oder var besser ist
    public abstract val start: State<KartenPosition>
    public abstract var endeKante: AnschlussKante // TODO herausfinden ob State oder var besser ist
    public abstract val ende: State<KartenPosition>


    @Composable override fun Modifier.modifier() = this.zIndex(-1f)

    @Composable override fun zuComposable(modifier: Modifier) = Canvas(modifier = Modifier.modifier()) { zeichnung() }
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

    public abstract fun abstand(pos: KartenPosition): Offset

    public abstract fun erhaltePfad(): Path


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
        public fun Iterable<Verbindung>.zuComposable(/*modifier: Modifier = Modifier*/) {
            if (this.count() == 0) return
            Canvas(modifier = Modifier.zIndex(-1f)) { forEach { verbindung -> verbindung.zeichnung(this) } }
        }

        public fun Iterable<Verbindung>.sichtbar() = filter { it.istImViewport() }
    }
}