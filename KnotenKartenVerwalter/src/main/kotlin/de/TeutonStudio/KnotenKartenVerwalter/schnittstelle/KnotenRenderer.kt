package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten

interface KnotenRenderer {
    @Composable fun Inhalt(knoten: KnotenDaten, ausgewählt: Boolean)
}

object StandardKnotenRenderer : KnotenRenderer {
    @Composable override fun Inhalt(knoten: KnotenDaten, ausgewählt: Boolean) {
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(knoten.name, style = MaterialTheme.typography.titleMedium)
            Text(knoten.art, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            knoten.parameter.entries.take(3).forEach { (schlüssel, wert) ->
                Text("$schlüssel: $wert", style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
    }
}
