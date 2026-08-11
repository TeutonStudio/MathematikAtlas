package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import kotlin.math.ceil

/**
 * Kleine Typgrafik neben einem Handle. Mehrere Segmente werden wie die bereits
 * vorhandenen Mehrfarbenanschlüsse diagonal gestreift, der Kurztext bleibt als
 * mathematische/DOM-spezifische Typnotation darüber lesbar.
 */
@Composable
fun TypMiniGrafik(
    descriptor: TypVisualDescriptor,
    farbeFürSegment: @Composable (TypVisualSegment) -> Color,
    modifier: Modifier = Modifier,
) {
    val segmente = descriptor.segmente.ifEmpty {
        listOf(TypVisualSegment(descriptor.kurztext, descriptor.kurztext))
    }
    val farben = segmente.map(farbeFürSegment)
    Box(
        modifier = modifier
            .height(18.dp)
            .widthIn(min = 24.dp, max = 92.dp)
            .clip(RoundedCornerShape(5.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            if (farben.size == 1) {
                drawRect(farben.single())
            } else {
                val band = 8.dp.toPx()
                val diagonal = size.width + size.height
                rotate(-45f, center) {
                    val anzahl = ceil(diagonal / band).toInt() + 4
                    repeat(anzahl) { index ->
                        val links = (index - 2) * band
                        drawRect(
                            color = farben[index % farben.size],
                            topLeft = Offset(links, -diagonal),
                            size = androidx.compose.ui.geometry.Size(band, diagonal * 3f),
                        )
                    }
                }
            }
            drawRect(MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
        }
        Text(
            text = descriptor.kurztext,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}
