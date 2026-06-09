package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.composable

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.VerbindungDaten
import kotlin.math.abs
import kotlin.math.max


/**
 * Gemeinsame Canvas-Umgebung für eine oder mehrere Zeichenfunktionen.
 */
@Composable
public fun VerbindungUmgebung(
    modifier: Modifier = Modifier,
    vararg inhalt: DrawScope.() -> Unit,
) { Canvas(modifier = modifier) { inhalt.forEach { it() } } }

/**
 * Listenvariante der gemeinsamen Canvas-Umgebung.
 */
@Composable
public fun VerbindungUmgebung(
    modifier: Modifier = Modifier,
    inhalt: List<DrawScope.() -> Unit>,
) { Canvas(modifier = modifier) { inhalt.forEach { it() } } }

/**
 *
 */
public fun DrawScope.VerbindungPfad(daten: VerbindungDaten, start: Offset, ende: Offset): Unit {
    val kontrollAbstand = max(48f, abs(ende.x - start.x) / 2f)
    drawPath( // TODO abhängig von kante machen
        path = Path().apply {
            moveTo(start.x, start.y)
            cubicTo(
                start.x + kontrollAbstand,
                start.y,
                ende.x - kontrollAbstand,
                ende.y,
                ende.x,
                ende.y,
            )
        },
        color = when {
            daten.fehler != null -> Color(0xFFDC2626)
            daten.ausgewaehlt -> Color(0xFF2563EB)
            else -> Color(0xFF475569)
        },
        style = Stroke(width = 3f, cap = StrokeCap.Round),
    )
}