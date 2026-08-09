package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class KartenExportFormat(
    val anzeigeName: String,
    val endung: String,
    val beschreibung: String,
    val verfügbar: Boolean,
) {
    JSON("JSON", ".json", "Rohe, kompatible Karten-JSON für Bearbeitung und Debugging.", true),
    MATLAS(".matlas", ".matlas", "Versioniertes Kartenpaket. Wird mit dem Container-Writer aus #198 aktiviert.", false),
}

internal fun normalisiereExportDateiname(name: String, format: KartenExportFormat): String {
    val basis = name.trim().ifBlank { "Karte" }
        .removeSuffix(".json")
        .removeSuffix(".matlas")
    return basis + format.endung
}

@Composable
internal fun KartenExportFormatDialog(
    schließen: () -> Unit,
    exportieren: (KartenExportFormat) -> Unit,
) {
    var format by remember { mutableStateOf(KartenExportFormat.JSON) }
    AlertDialog(
        onDismissRequest = schließen,
        title = { Text("Karte exportieren") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                KartenExportFormat.entries.forEach { kandidat ->
                    Surface(
                        onClick = { if (kandidat.verfügbar) format = kandidat },
                        enabled = kandidat.verfügbar,
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = if (format == kandidat) 3.dp else 0.dp,
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            RadioButton(
                                selected = format == kandidat,
                                onClick = { if (kandidat.verfügbar) format = kandidat },
                                enabled = kandidat.verfügbar,
                            )
                            Column {
                                Text(kandidat.anzeigeName, style = MaterialTheme.typography.titleSmall)
                                Text(kandidat.beschreibung, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { exportieren(format) }, enabled = format.verfügbar) { Text("Exportieren") }
        },
        dismissButton = { TextButton(onClick = schließen) { Text("Abbrechen") } },
    )
}
