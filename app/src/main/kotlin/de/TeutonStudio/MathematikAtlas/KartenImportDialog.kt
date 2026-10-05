package de.TeutonStudio.MathematikAtlas

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import de.TeutonStudio.MathematikAtlas.speicher.SichererKartenAbruf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun KartenImportDialog(zustand: AtlasZustand, schließen: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var adresse by remember { mutableStateOf("") }
    var fehler by remember { mutableStateOf<String?>(null) }
    var lädt by remember { mutableStateOf(false) }

    fun importiere(laden: suspend () -> ByteArray) {
        scope.launch {
            lädt = true
            fehler = null
            runCatching { laden() }
                .mapCatching { inhalt -> zustand.importiere(inhalt) }
                .onSuccess { schließen() }
                .onFailure { fehler = it.message ?: "Import fehlgeschlagen." }
            lädt = false
        }
    }

    val datei = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            importiere {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(it)?.use { eingabe ->
                        eingabe.readBytesBegrenzt(SichererKartenAbruf.MAXIMALE_GRÖSSE)
                    } ?: error("Die Quelldatei konnte nicht geöffnet werden.")
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!lädt) schließen() },
        title = { Text("Karte oder Sammlung importieren") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(LocalAtlasAbstände.current.standard)) {
                OutlinedButton(
                    onClick = { datei.launch(arrayOf("application/json", "text/plain", KartenExportFormat.MATLAS.mimeType, "application/zip")) },
                    enabled = !lädt,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Datei auswählen") }
                OutlinedTextField(
                    value = adresse,
                    onValueChange = { adresse = it; fehler = null },
                    label = { Text("HTTPS-Adresse") },
                    singleLine = true,
                    enabled = !lädt,
                    isError = fehler != null,
                    supportingText = { fehler?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { importiere { withContext(Dispatchers.IO) { SichererKartenAbruf.lade(adresse) } } },
                    enabled = adresse.isNotBlank() && !lädt,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (lädt) "Import wird geprüft…" else "Von Link importieren") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = schließen, enabled = !lädt) { Text("Abbrechen") } },
    )
}

private fun java.io.InputStream.readBytesBegrenzt(maximal: Int): ByteArray {
    val ausgabe = java.io.ByteArrayOutputStream()
    val puffer = ByteArray(8192)
    var gesamt = 0
    while (true) {
        val gelesen = read(puffer)
        if (gelesen < 0) break
        gesamt += gelesen
        require(gesamt <= maximal) { "Die Importdatei ist größer als 16 MiB." }
        ausgabe.write(puffer, 0, gelesen)
    }
    return ausgabe.toByteArray()
}
