package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
fun KnotenAuswertungszeitFußzeile(dauerNanos: Long?) {
    val dauer = dauerNanos ?: return
    val formatiert = formatiereAuswertungsDauerNanos(dauer)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
