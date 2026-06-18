package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

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
        @Composable public fun Iterable<GraphCanvasObjekt>.Composable(/*modifier: Modifier = Modifier*/) = forEach { Canvas(Modifier.fillMaxSize().zIndex(-1f)) { it.zeichnung } }

    }
}
