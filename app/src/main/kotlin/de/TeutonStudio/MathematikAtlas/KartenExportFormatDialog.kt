package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.MathematikKnoten.MatlasKartenContainer

enum class KartenExportFormat(
    val anzeigeName: String,
    val endung: String,
    val mimeType: String,
    val beschreibung: String,
) {
    JSON(
        "JSON",
        ".json",
        "application/json",
        "Rohe, kompatible Karten-JSON für Bearbeitung und Debugging.",
    ),
    MATLAS(
        ".matlas",
        MatlasKartenContainer.DATEI_ENDUNG,
        MatlasKartenContainer.MIME_TYPE,
        "Versioniertes Kartenpaket mit Manifest, Prüfsumme und Karten-JSON.",
    ),
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
                        onClick = { format = kandidat },
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = if (format == kandidat) 3.dp else 0.dp,
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            RadioButton(
                                selected = format == kandidat,
                                onClick = { format = kandidat },
                            )
                            Column {
                                Text(kandidat.anzeigeName, style = MaterialTheme.typography.titleSmall)
                                Text(kandidat.beschreibung, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "Dateiendung: ${kandidat.endung}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { exportieren(format) }) { Text("Exportieren") }
        },
        dismissButton = { TextButton(onClick = schließen) { Text("Abbrechen") } },
    )
}
