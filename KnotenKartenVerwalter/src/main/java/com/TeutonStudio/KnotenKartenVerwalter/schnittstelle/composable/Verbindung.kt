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
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.VerbindungDaten
import kotlin.math.abs
import kotlin.math.hypot
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
public fun DrawScope.VerbindungPfad(
    daten: VerbindungDaten,
    start: Offset,
    ende: Offset,
    startKante: AnschlussKante,
    endeKante: AnschlussKante,
): Unit {
    val dx = ende.x - start.x
    val dy = ende.y - start.y

    val distanz = hypot(dx.toDouble(), dy.toDouble()).toFloat()

    val kontrollAbstand = max(
        48f,
        distanz * 0.35f,
    ).coerceAtMost(240f)

    val startRichtung = startKante.tangente()
    val endeRichtung = endeKante.tangente()

    val c1 = start + startRichtung * kontrollAbstand
    val c2 = ende + endeRichtung * kontrollAbstand

    drawPath(
        path = Path().apply {
            moveTo(start.x, start.y)
            cubicTo(
                c1.x,
                c1.y,
                c2.x,
                c2.y,
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

private fun AnschlussKante.tangente(): Offset = when (this) {
    AnschlussKante.Links -> Offset(1f, 0f)
    AnschlussKante.Rechts -> Offset(-1f, 0f)
    AnschlussKante.Oben -> Offset(0f, -1f)
    AnschlussKante.Unten -> Offset(0f, 1f)
}