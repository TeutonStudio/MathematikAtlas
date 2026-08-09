package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import java.util.Locale

/** Blender-artige, zurückhaltende Laufzeitanzeige unterhalb eines Knotens. */
@Composable
fun KnotenAuswertungszeitFußzeile(dauerNanos: Long?) {
    val dauer = dauerNanos ?: return
    val formatiert = formatiereAuswertungsDauerNanos(dauer)
    Text(
        text = "Δt  $formatiert",
        modifier = Modifier
            .padding(start = 4.dp)
            .semantics { contentDescription = "Letzte Auswertungsdauer $formatiert" },
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
    )
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
