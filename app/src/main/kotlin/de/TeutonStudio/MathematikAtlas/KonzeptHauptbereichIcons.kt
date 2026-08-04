package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private const val PFEILSPITZEN_WINKEL = PI / 7.0

/**
 * Symbolisiert eine Basis aus zwei Vektoren. Die parallelen Hilfslinien deuten
 * das von den Basisvektoren aufgespannte Parallelogramm an.
 */
@Composable
internal fun LineareAlgebraIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(modifier) {
        val kantenlänge = size.minDimension
        val versatz = Offset(
            x = (size.width - kantenlänge) / 2f,
            y = (size.height - kantenlänge) / 2f,
        )

        fun punkt(x: Float, y: Float): Offset = Offset(
            x = versatz.x + x * kantenlänge,
            y = versatz.y + y * kantenlänge,
        )

        val ursprung = punkt(0.22f, 0.78f)
        val ersterVektor = punkt(0.72f, 0.70f)
        val zweiterVektor = punkt(0.42f, 0.24f)
        val summe = Offset(
            x = ersterVektor.x + zweiterVektor.x - ursprung.x,
            y = ersterVektor.y + zweiterVektor.y - ursprung.y,
        )
        val strichbreite = kantenlänge * 0.07f
        val hilfslinienBreite = kantenlänge * 0.04f
        val pfeilspitzenLänge = kantenlänge * 0.14f

        drawLine(
            color = tint.copy(alpha = 0.3f),
            start = ersterVektor,
            end = summe,
            strokeWidth = hilfslinienBreite,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint.copy(alpha = 0.3f),
            start = zweiterVektor,
            end = summe,
            strokeWidth = hilfslinienBreite,
            cap = StrokeCap.Round,
        )
        zeichneVektor(ursprung, ersterVektor, tint, strichbreite, pfeilspitzenLänge)
        zeichneVektor(ursprung, zweiterVektor, tint, strichbreite, pfeilspitzenLänge)
        drawCircle(
            color = tint,
            radius = kantenlänge * 0.055f,
            center = ursprung,
        )
    }
}

private fun DrawScope.zeichneVektor(
    start: Offset,
    ende: Offset,
    farbe: Color,
    strichbreite: Float,
    pfeilspitzenLänge: Float,
) {
    val winkel = atan2(
        (ende.y - start.y).toDouble(),
        (ende.x - start.x).toDouble(),
    )
    val linkeSpitze = Offset(
        x = ende.x - pfeilspitzenLänge * cos(winkel - PFEILSPITZEN_WINKEL).toFloat(),
        y = ende.y - pfeilspitzenLänge * sin(winkel - PFEILSPITZEN_WINKEL).toFloat(),
    )
    val rechteSpitze = Offset(
        x = ende.x - pfeilspitzenLänge * cos(winkel + PFEILSPITZEN_WINKEL).toFloat(),
        y = ende.y - pfeilspitzenLänge * sin(winkel + PFEILSPITZEN_WINKEL).toFloat(),
    )

    drawLine(farbe, start, ende, strichbreite, StrokeCap.Round)
    drawLine(farbe, ende, linkeSpitze, strichbreite, StrokeCap.Round)
    drawLine(farbe, ende, rechteSpitze, strichbreite, StrokeCap.Round)
}

@Preview(name = "Konzepticon – Lineare Algebra", showBackground = true)
@Composable
private fun LineareAlgebraIconVorschau() {
    MaterialTheme {
        Surface {
            Box(Modifier.padding(16.dp)) {
                LineareAlgebraIcon(Modifier.size(64.dp))
            }
        }
    }
}
