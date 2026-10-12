package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * Blender-artige Laufzeitanzeige in einem eigenen unteren Rahmen.
 *
 * Der Editor positioniert diese Diagnose weiterhin außerhalb des logischen
 * Knotenrechtecks. Dadurch bleiben Knotengröße, Handles, Edges, Trefferfläche und
 * MiniMap unverändert, während die Zeitangabe nicht mehr direkt auf dem
 * Graphhintergrund liegt.
 */
@Composable
fun KnotenAuswertungszeitFußzeile(dauerNanos: Long?, ausgewählt: Boolean = false) {
    val dauer = dauerNanos ?: return
    val formatiert = formatiereAuswertungsDauerNanos(dauer)
    val form = RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp)
    val rahmenFarbe = if (ausgewählt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val rahmenBreite = if (ausgewählt) 3.dp else 1.dp
    Surface(
        modifier = Modifier.fillMaxWidth().drawWithContent {
            drawContent()
            // Die Knotenunterkante bildet bereits den oberen Abschluss.
            val breite = rahmenBreite.toPx()
            val inset = breite / 2
            val rechts = (size.width - inset).coerceAtLeast(inset)
            val unten = (size.height - inset).coerceAtLeast(inset)
            val radius = (minOf(4.dp.toPx(), size.width / 2, size.height / 2) - inset).coerceAtLeast(0f)
            val rahmen = Path().apply {
                moveTo(inset, 0f)
                lineTo(inset, unten - radius)
                if (radius > 0f) arcTo(Rect(inset, unten - 2 * radius, inset + 2 * radius, unten), 180f, -90f, false)
                lineTo(rechts - radius, unten)
                if (radius > 0f) arcTo(Rect(rechts - 2 * radius, unten - 2 * radius, rechts, unten), 90f, -90f, false)
                lineTo(rechts, 0f)
            }
            drawPath(rahmen, rahmenFarbe, style = Stroke(breite))
        },
        shape = form,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = "Δzeit  $formatiert",
                modifier = Modifier.semantics {
                    contentDescription = "Letzte Auswertungsdauer $formatiert"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

fun formatiereAuswertungsDauerNanos(dauerNanos: Long): String {
    val dauer = dauerNanos.coerceAtLeast(0L)
    return when {
        dauer < 1_000L -> "<1 µs"
        dauer < 1_000_000L -> String.format(Locale.ROOT, "%.0f µs", dauer / 1_000.0).replace('.', ',')
        dauer < 1_000_000_000L -> String.format(Locale.ROOT, "%.1f ms", dauer / 1_000_000.0).replace('.', ',')
        else -> String.format(Locale.ROOT, "%.2f s", dauer / 1_000_000_000.0).replace('.', ',')
    }
}

/** X-Abstände der Übergänge von den unteren Rundungen zur geraden Unterkante. */
internal fun unterkantenEinzüge(outline: Outline): Pair<Float, Float> = when (outline) {
    is Outline.Rounded -> outline.roundRect.bottomLeftCornerRadius.x to outline.roundRect.bottomRightCornerRadius.x
    else -> 0f to 0f
}
