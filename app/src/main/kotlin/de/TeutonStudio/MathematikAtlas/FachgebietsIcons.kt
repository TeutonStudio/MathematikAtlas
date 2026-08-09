package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

internal data class FachgebietsIconFarben(
    val hintergrundStart: Color,
    val hintergrundEnde: Color,
    val innenflaeche: Color,
    val mengeLinks: Color,
    val mengeRechts: Color,
    val schnitt: Color,
    val kontur: Color,
    val element: Color,
)

@Composable
private fun fachgebietsIconFarben() = FachgebietsIconFarben(
    hintergrundStart = MaterialTheme.colorScheme.primaryContainer,
    hintergrundEnde = MaterialTheme.colorScheme.secondaryContainer,
    innenflaeche = MaterialTheme.colorScheme.surface,
    mengeLinks = MaterialTheme.colorScheme.primary,
    mengeRechts = MaterialTheme.colorScheme.secondary,
    schnitt = MaterialTheme.colorScheme.tertiary,
    kontur = MaterialTheme.colorScheme.onSurfaceVariant,
    element = MaterialTheme.colorScheme.onPrimaryContainer,
)

internal fun hatFachgebietsIcon(id: String): Boolean = id == "mengenlehre"

@Composable
internal fun FachgebietsIcon(id: String, modifier: Modifier = Modifier) {
    if (id != "mengenlehre") return
    val farben = fachgebietsIconFarben()
    Canvas(modifier) {
        val min = size.minDimension
        val rand = min * 0.07f
        val radius = min * 0.12f
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(farben.hintergrundStart, farben.hintergrundEnde),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
            cornerRadius = CornerRadius(radius, radius),
        )
        drawRoundRect(
            color = farben.innenflaeche.copy(alpha = .88f),
            topLeft = Offset(rand, rand),
            size = Size(size.width - 2 * rand, size.height - 2 * rand),
            cornerRadius = CornerRadius(radius * .72f, radius * .72f),
        )
        val kreisRadius = min * .255f
        val links = Offset(size.width * .39f, size.height * .50f)
        val rechts = Offset(size.width * .61f, size.height * .50f)
        drawCircle(farben.mengeLinks.copy(alpha = .36f), kreisRadius, links)
        drawCircle(farben.mengeRechts.copy(alpha = .36f), kreisRadius, rechts)
        drawOval(
            color = farben.schnitt.copy(alpha = .50f),
            topLeft = Offset(size.width * .43f, size.height * .31f),
            size = Size(size.width * .14f, size.height * .38f),
        )
        drawCircle(farben.kontur, kreisRadius, links, style = Stroke(min * .018f))
        drawCircle(farben.kontur, kreisRadius, rechts, style = Stroke(min * .018f))
        val punktRadius = min * .026f
        listOf(
            Offset(size.width * .27f, size.height * .43f),
            Offset(size.width * .34f, size.height * .58f),
            Offset(size.width * .50f, size.height * .49f),
            Offset(size.width * .66f, size.height * .39f),
            Offset(size.width * .72f, size.height * .59f),
        ).forEach { drawCircle(farben.element, punktRadius, it) }
    }
}
