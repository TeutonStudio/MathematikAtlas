package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.zIndex

/**
 * TODO KDoc: Vertrag und Seiteneffekte dieser Schnittstelle klären.
 */
interface GraphCanvasObjekt: GraphObjekt {
    public val zeichnung: DrawScope.() -> Unit

    public companion object {

        public fun RectF.overlaps(other: RectF) = left <= other.right && right >= other.left && top <= other.bottom && bottom >= other.top
        @Composable public fun Iterable<GraphCanvasObjekt>.Composable() = Canvas(Modifier.fillMaxSize().zIndex(0f)) {
            forEach { it.zeichnung(this) }
        }
    }
}
