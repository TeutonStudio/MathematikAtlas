package de.TeutonStudio.MathematikKnoten

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Ein Rahmen für alle Fachpaletten und beide Plattformen. Übernahme betrifft den gesamten Entwurf. */
@Composable
internal fun FormelEditorDialogRahmen(
    titel: String,
    beschreibung: String,
    zeichnung: FormelZeichnungsEntwurf,
    gültig: Boolean,
    schließen: () -> Unit,
    übernehmen: () -> Unit,
    vorschau: @Composable () -> Unit,
    inhalt: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = schließen, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.windowInsetsPadding(WindowInsets.safeDrawing).padding(12.dp)
            .fillMaxWidth(.95f).fillMaxHeight(.94f).widthIn(max = 1180.dp),
            shape = MaterialTheme.shapes.extraLarge, tonalElevation = 8.dp) {
            Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(titel, style = MaterialTheme.typography.titleLarge)
                        Text(beschreibung, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = schließen) { Text("Schließen") }
                }
                // Endliche sichtbare Bodyhöhe; die Fachpaletten begrenzen ihre eigenen Lazy-Grids.
                Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormelEingabeModi(zeichnung)
                    vorschau()
                    inhalt()
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = schließen) { Text("Abbrechen") }
                    Button(onClick = übernehmen, enabled = gültig) { Text("Formel übernehmen") }
                }
            }
        }
    }
}
