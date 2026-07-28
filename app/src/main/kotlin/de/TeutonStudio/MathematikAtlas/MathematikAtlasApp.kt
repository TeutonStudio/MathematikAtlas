package de.TeutonStudio.MathematikAtlas

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenKartenEditor
import kotlinx.coroutines.delay

private sealed interface GraphKontext {
    data class Knoten(val id: KnotenId) : GraphKontext
    data class Verbindung(val id: VerbindungsId) : GraphKontext
}

@Composable
fun MathematikAtlasApp(zustand: AtlasZustand) {
    val context = LocalContext.current
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use { w -> w.write(zustand.speicher.exportiere(zustand.editor.karte)) } }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> zustand.importiere(r.readText()) } }
    }
    var graphKontext by remember { mutableStateOf<GraphKontext?>(null) }

    LaunchedEffect(zustand.editor.karte) {
        zustand.aktualisiereAuswertung()
        delay(650)
        zustand.speichereAktuell()
    }

    // Ab Android 15 kann der Inhalt standardmäßig bis unter die Systemleisten
    // reichen. Die gesamte Arbeitsfläche erhält deshalb den Navigationsleisten-
    // Inset; so bleiben insbesondere die unteren Inspektoraktionen erreichbar.
    Row(
        Modifier.fillMaxSize()
            .navigationBarsPadding()
            .background(MaterialTheme.colorScheme.background),
    ) {
        VerwaltungsFenster(zustand, Modifier.width(280.dp).fillMaxHeight())
        VerticalDivider()
        Column(Modifier.weight(1f).fillMaxHeight()) {
            WerkzeugLeiste(zustand, onImport = { import.launch(arrayOf("application/json", "text/plain")) }, onExport = { export.launch("${zustand.editor.karte.name}.json") })
            HorizontalDivider()
            Box(Modifier.weight(1f)) {
                KnotenKartenEditor(
                    zustand = zustand.editor,
                    modifier = Modifier.fillMaxSize(),
                    rendererFür = zustand::rendererFür,
                    farbeFürAnschluss = { anschluss -> anschlussFarbe(anschluss.art.wert) },
                    beiHintergrundKontext = { zustand.öffneKnotenAuswahl(it) },
                    beiKnotenKontext = { graphKontext = GraphKontext.Knoten(it.id) },
                    beiVerbindungKontext = { graphKontext = GraphKontext.Verbindung(it.id) },
                    beiVerbindungAufHintergrund = { start, position -> zustand.öffneKnotenAuswahl(position, start) },
                    beiKnotenDoppelklick = { it.kartenVerweis?.let(zustand::öffne) },
                )
                zustand.knotenAuswahlPosition?.let { KnotenAuswahlDialog(zustand, it) }
                graphKontext?.let { KontextDialog(zustand, it) { graphKontext = null } }
            }
        }
        VerticalDivider()
        Inspektor(zustand, Modifier.width(310.dp).fillMaxHeight())
    }
}

@Composable
private fun KontextDialog(zustand: AtlasZustand, kontext: GraphKontext, schließen: () -> Unit) {
    val id = when (kontext) {
        is GraphKontext.Knoten -> kontext.id.wert
        is GraphKontext.Verbindung -> kontext.id.wert
    }
    AlertDialog(
        onDismissRequest = schließen,
        title = { Text("ID: $id", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (kontext) {
                    is GraphKontext.Knoten -> {
                        Text("Knoten", style = MaterialTheme.typography.labelLarge)
                        Button(
                            onClick = {
                                zustand.editor.wähleKnoten(kontext.id)
                                zustand.editor.dupliziereAuswahl()
                                schließen()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Duplizieren") }
                        OutlinedButton(
                            onClick = {
                                zustand.editor.wähleKnoten(kontext.id)
                                zustand.editor.isoliereAusgewähltenKnoten()
                                schließen()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Isolieren") }
                        Text("Entfernt alle Verbindungen dieses Knotens.", style = MaterialTheme.typography.bodySmall)
                        Button(
                            onClick = {
                                zustand.editor.wähleKnoten(kontext.id)
                                zustand.editor.löscheAuswahl()
                                schließen()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        ) { Text("Löschen") }
                    }
                    is GraphKontext.Verbindung -> {
                        Text("Verbindung", style = MaterialTheme.typography.labelLarge)
                        Button(
                            onClick = {
                                zustand.editor.wähleVerbindung(kontext.id)
                                zustand.editor.löscheAuswahl()
                                schließen()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        ) { Text("Löschen") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = schließen) { Text("Schließen") } },
    )
}

@Composable
private fun WerkzeugLeiste(zustand: AtlasZustand, onImport: () -> Unit, onExport: () -> Unit) {
    var umbenennenGeöffnet by remember(zustand.editor.karte.id) { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        zustand.brotkrumen.forEachIndexed { index, ref ->
            val name = zustand.speicher.lade(ref)?.name ?: ref.kartenId.wert.take(8)
            TextButton(onClick = { zustand.geheZuBrotkrume(index) }) { Text(name) }
            if (index < zustand.brotkrumen.lastIndex) Text("›", color = MaterialTheme.colorScheme.outline)
        }
        Spacer(Modifier.weight(1f))
        Text("v${zustand.editor.karte.version}", style = MaterialTheme.typography.labelMedium)
        TextButton(onClick = { umbenennenGeöffnet = true }) { Text("Karte umbenennen") }
        TextButton(onClick = zustand.editor::rückgängig, enabled = zustand.editor.kannRückgängig()) { Text("Rückgängig") }
        TextButton(onClick = zustand.editor::wiederholen, enabled = zustand.editor.kannWiederholen()) { Text("Wiederholen") }
        TextButton(onClick = onImport) { Text("Import") }
        TextButton(onClick = onExport) { Text("Export") }
        Button(onClick = zustand::speichereAktuell) { Text("Speichern") }
    }
    if (umbenennenGeöffnet) {
        NameÄndernDialog(
            titel = "Karte umbenennen",
            aktuellerName = zustand.editor.karte.name,
            schließen = { umbenennenGeöffnet = false },
            bestätigen = { name ->
                zustand.ersetzeKarteMitAuswahl(zustand.editor.karte.copy(name = name))
                zustand.speichereAktuell()
                umbenennenGeöffnet = false
            },
        )
    }
}

@Composable
internal fun NameÄndernDialog(
    titel: String,
    aktuellerName: String,
    schließen: () -> Unit,
    bestätigen: (String) -> Unit,
) {
    var text by remember(aktuellerName) { mutableStateOf(aktuellerName) }
    val bereinigterName = text.trim()
    AlertDialog(
        onDismissRequest = schließen,
        title = { Text(titel) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Name") },
                singleLine = true,
                isError = bereinigterName.isEmpty(),
                supportingText = {
                    if (bereinigterName.isEmpty()) Text("Der Name darf nicht leer sein.")
                },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { bestätigen(bereinigterName) },
                enabled = bereinigterName.isNotEmpty(),
            ) { Text("Übernehmen") }
        },
        dismissButton = { TextButton(onClick = schließen) { Text("Abbrechen") } },
    )
}

internal fun AtlasZustand.ersetzeKarteMitAuswahl(neueKarte: KartenDaten) {
    val knoten = editor.ausgewählterKnoten
    val verbindung = editor.ausgewählteVerbindung
    editor.ersetzeKarte(neueKarte, historieLeeren = false)
    when {
        knoten != null -> editor.wähleKnoten(knoten)
        verbindung != null -> editor.wähleVerbindung(verbindung)
    }
}

internal fun anschlussFarbe(id: String) = when {
    id.startsWith("mathematik.geometrie.") -> androidx.compose.ui.graphics.Color(0xFF0891B2)
    id == "mathematik.zahl" -> androidx.compose.ui.graphics.Color(0xFF2563EB)
    id == "mathematik.aussage" -> androidx.compose.ui.graphics.Color(0xFF7C3AED)
    id == "mathematik.menge" -> androidx.compose.ui.graphics.Color(0xFF059669)
    id in setOf("mathematik.vektor", "mathematik.matrix") -> androidx.compose.ui.graphics.Color(0xFFEA580C)
    id == "mathematik.funktion" -> androidx.compose.ui.graphics.Color(0xFFDB2777)
    else -> androidx.compose.ui.graphics.Color(0xFF475569)
}
