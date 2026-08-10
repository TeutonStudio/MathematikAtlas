package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath

internal enum class FachgebietsIconId {
    Mengenlehre,
}

internal data class FachgebietsIconFarben(
    val außenStart: Color,
    val außenEnde: Color,
    val innenStart: Color,
    val innenEnde: Color,
    val rahmen: Color,
    val mengeLinksStart: Color,
    val mengeLinksEnde: Color,
    val mengeRechtsStart: Color,
    val mengeRechtsEnde: Color,
    val schnittStart: Color,
    val schnittEnde: Color,
    val elementAußen: Color,
    val elementLinks: Color,
    val elementSchnitt: Color,
    val elementRechts: Color,
)

@Composable
private fun fachgebietsIconFarben() = FachgebietsIconFarben(
    außenStart = MaterialTheme.colorScheme.surfaceContainerHigh,
    außenEnde = MaterialTheme.colorScheme.primaryContainer,
    innenStart = MaterialTheme.colorScheme.surfaceContainerLowest,
    innenEnde = MaterialTheme.colorScheme.surfaceContainer,
    rahmen = MaterialTheme.colorScheme.onSurfaceVariant,
    mengeLinksStart = MaterialTheme.colorScheme.primaryContainer,
    mengeLinksEnde = MaterialTheme.colorScheme.primary,
    mengeRechtsStart = MaterialTheme.colorScheme.secondaryContainer,
    mengeRechtsEnde = MaterialTheme.colorScheme.secondary,
    schnittStart = MaterialTheme.colorScheme.tertiaryContainer,
    schnittEnde = MaterialTheme.colorScheme.tertiary,
    elementAußen = MaterialTheme.colorScheme.onSurface,
    elementLinks = MaterialTheme.colorScheme.onPrimaryContainer,
    elementSchnitt = MaterialTheme.colorScheme.onTertiaryContainer,
    elementRechts = MaterialTheme.colorScheme.onSecondaryContainer,
)

internal fun fachgebietsIconId(id: String): FachgebietsIconId? = when (id) {
    "mengenlehre" -> FachgebietsIconId.Mengenlehre
    else -> null
}

internal fun hatFachgebietsIcon(id: String): Boolean = fachgebietsIconId(id) != null

@Composable
internal fun FachgebietsIcon(id: String, modifier: Modifier = Modifier) {
    when (fachgebietsIconId(id)) {
        FachgebietsIconId.Mengenlehre -> MengenlehreFachgebietsIcon(modifier)
        null -> Unit
    }
}

@Composable
internal fun MengenlehreFachgebietsIcon(modifier: Modifier = Modifier) {
    val farben = fachgebietsIconFarben()
    Canvas(modifier) {
        val min = size.minDimension
        val außenRand = min * 0.055f
        val innenRand = min * 0.105f
        val außenRadius = min * 0.12f
        val innenRadius = min * 0.075f

        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(farben.außenStart, farben.außenEnde),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
            cornerRadius = CornerRadius(außenRadius, außenRadius),
        )
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(farben.innenStart, farben.innenEnde),
                start = Offset(size.width * .15f, size.height * .1f),
                end = Offset(size.width * .85f, size.height * .9f),
            ),
            topLeft = Offset(innenRand, innenRand),
            size = Size(size.width - 2 * innenRand, size.height - 2 * innenRand),
            cornerRadius = CornerRadius(innenRadius, innenRadius),
        )
        drawRoundRect(
            color = farben.rahmen,
            topLeft = Offset(innenRand, innenRand),
            size = Size(size.width - 2 * innenRand, size.height - 2 * innenRand),
            cornerRadius = CornerRadius(innenRadius, innenRadius),
            style = Stroke(min * .012f),
        )

        val kreisRadius = min * .245f
        val links = Offset(size.width * .39f, size.height * .51f)
        val rechts = Offset(size.width * .61f, size.height * .51f)
        val linksPfad = kreisPfad(links, kreisRadius)
        val rechtsPfad = kreisPfad(rechts, kreisRadius)

        drawCircle(
            brush = Brush.linearGradient(
                listOf(farben.mengeLinksStart, farben.mengeLinksEnde),
                start = Offset(links.x - kreisRadius, links.y - kreisRadius),
                end = Offset(links.x + kreisRadius, links.y + kreisRadius),
            ),
            radius = kreisRadius,
            center = links,
            alpha = .48f,
        )
        drawCircle(
            brush = Brush.linearGradient(
                listOf(farben.mengeRechtsStart, farben.mengeRechtsEnde),
                start = Offset(rechts.x - kreisRadius, rechts.y - kreisRadius),
                end = Offset(rechts.x + kreisRadius, rechts.y + kreisRadius),
            ),
            radius = kreisRadius,
            center = rechts,
            alpha = .48f,
        )

        // Die Schnittfläche ist tatsächlich A∩B und keine optische Ellipsenannäherung.
        clipPath(linksPfad) {
            clipPath(rechtsPfad) {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(farben.schnittStart, farben.schnittEnde),
                        start = Offset(size.width * .42f, size.height * .28f),
                        end = Offset(size.width * .58f, size.height * .74f),
                    ),
                    alpha = .78f,
                )
            }
        }

        drawCircle(farben.rahmen, kreisRadius, links, style = Stroke(min * .015f))
        drawCircle(farben.rahmen, kreisRadius, rechts, style = Stroke(min * .015f))

        val punktRadius = min * .024f
        val punkte = listOf(
            Triple(Offset(size.width * .22f, size.height * .27f), farben.elementAußen, "außen"),
            Triple(Offset(size.width * .78f, size.height * .73f), farben.elementAußen, "außen"),
            Triple(Offset(size.width * .29f, size.height * .45f), farben.elementLinks, "links"),
            Triple(Offset(size.width * .34f, size.height * .61f), farben.elementLinks, "links"),
            Triple(Offset(size.width * .50f, size.height * .50f), farben.elementSchnitt, "schnitt"),
            Triple(Offset(size.width * .66f, size.height * .41f), farben.elementRechts, "rechts"),
            Triple(Offset(size.width * .71f, size.height * .60f), farben.elementRechts, "rechts"),
        )
        punkte.forEach { (position, farbe, _) ->
            drawCircle(farbe, punktRadius, position)
        }

        // Ein subtiler innerer Lichtsaum bleibt ebenfalls vollständig themebasiert.
        drawRoundRect(
            color = farben.rahmen.copy(alpha = .22f),
            topLeft = Offset(außenRand, außenRand),
            size = Size(size.width - 2 * außenRand, size.height - 2 * außenRand),
            cornerRadius = CornerRadius(außenRadius * .82f, außenRadius * .82f),
            style = Stroke(min * .008f),
        )
    }
}

private fun kreisPfad(mitte: Offset, radius: Float): Path = Path().apply {
    addOval(
        Rect(
            left = mitte.x - radius,
            top = mitte.y - radius,
            right = mitte.x + radius,
            bottom = mitte.y + radius,
        ),
    )
}
