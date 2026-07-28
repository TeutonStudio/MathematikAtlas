package de.TeutonStudio.MathematikAtlas

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenKartenEditor
import de.TeutonStudio.KnotenKartenVerwalter.zustand.*
import kotlinx.coroutines.delay

private sealed interface GraphKontext {
    data class Knoten(val id: KnotenId) : GraphKontext
    data class Knotengruppe(val knotenIds: Set<KnotenId>) : GraphKontext
    data class Verbindung(val id: VerbindungsId) : GraphKontext
    data class Anschluss(val ref: AnschlussVerweis) : GraphKontext
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
                    beiKnotenKontext = { knoten ->
                        graphKontext = if (zustand.editor.auswahlModus == AuswahlModus.Gruppe) {
                            GraphKontext.Knotengruppe(zustand.editor.ausgewählteKnoten + knoten.id)
                        } else GraphKontext.Knoten(knoten.id)
                    },
                    beiVerbindungKontext = { graphKontext = GraphKontext.Verbindung(it.id) },
                    beiAnschlussKontext = { graphKontext = GraphKontext.Anschluss(it) },
                    beiVerbindungAufHintergrund = { start, position -> zustand.öffneKnotenAuswahl(position, start) },
                    beiKnotenDoppelklick = { it.kartenVerweis?.let(zustand::öffne) },
                )
                KartenMarkierungen(zustand.editor)
                KartenWerkzeuge(
                    editor = zustand.editor,
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
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
    val titel = when (kontext) {
        is GraphKontext.Knoten -> "ID: ${kontext.id.wert}"
        is GraphKontext.Knotengruppe -> "${kontext.knotenIds.size} Knoten ausgewählt"
        is GraphKontext.Verbindung -> "ID: ${kontext.id.wert}"
        is GraphKontext.Anschluss -> "Anschluss: ${kontext.ref.anschlussId.wert}"
    }
    AlertDialog(
        onDismissRequest = schließen,
        title = { Text(titel, style = MaterialTheme.typography.titleMedium) },
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
                    is GraphKontext.Knotengruppe -> {
                        Text("Gruppenauswahl", style = MaterialTheme.typography.labelLarge)
                        Button(
                            onClick = {
                                zustand.editor.stelleAuswahlWiederHer(kontext.knotenIds, kontext.knotenIds.lastOrNull())
                                zustand.editor.gruppiereAuswahlVisuell()
                                schließen()
                            },
                            enabled = kontext.knotenIds.size >= 2,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Visuell gruppieren") }
                        if (kontext.knotenIds.size < 2) {
                            Text("Mindestens zwei Knoten werden benötigt.", style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(
                            onClick = {
                                zustand.editor.stelleAuswahlWiederHer(kontext.knotenIds, kontext.knotenIds.lastOrNull())
                                zustand.editor.hebeVisuelleGruppierungDerAuswahlAuf()
                                schließen()
                            },
                            enabled = zustand.editor.auswahlIstVisuellGruppiert(),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Gruppierung aufheben") }
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
                    is GraphKontext.Anschluss -> {
                        val knoten = zustand.editor.karte.knoten.firstOrNull { it.id == kontext.ref.knotenId }
                        val anschluss = knoten?.anschlüsse?.firstOrNull { it.id == kontext.ref.anschlussId }
                        if (knoten == null || anschluss == null) {
                            Text("Der Anschluss existiert nicht mehr.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text(knoten.name, style = MaterialTheme.typography.labelLarge)
                            Text("${anschluss.name} · ${anschluss.richtung.name} · ${anschluss.art.wert}")
                            val erweiterbar = zustand.editor.kannAnschlussRelativEinfügen(kontext.ref)
                            Button(
                                onClick = {
                                    zustand.editor.fügeAnschlussRelativEin(
                                        kontext.ref,
                                        AnschlussEinfügePosition.Davor,
                                    )
                                    schließen()
                                },
                                enabled = erweiterbar,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Anschluss oberhalb einfügen") }
                            OutlinedButton(
                                onClick = {
                                    zustand.editor.fügeAnschlussRelativEin(
                                        kontext.ref,
                                        AnschlussEinfügePosition.Danach,
                                    )
                                    schließen()
                                },
                                enabled = erweiterbar,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Anschluss unterhalb einfügen") }
                            if (!erweiterbar) {
                                Text(
                                    "Dieser Knoten besitzt eine feste Anschlusszahl.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
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
    var jsonGeöffnet by remember(zustand.editor.karte.id) { mutableStateOf(false) }
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
        TextButton(onClick = { jsonGeöffnet = true }) { Text("JSON anzeigen") }
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
    if (jsonGeöffnet) {
        KartenJsonDialog(zustand = zustand, schließen = { jsonGeöffnet = false })
    }
}

@Composable
private fun KartenMarkierungen(editor: KartenEditorZustand) {
    val gruppenFarbe = MaterialTheme.colorScheme.tertiary
    val auswahlFarbe = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxSize()) {
        val karte = editor.karte
        val zoom = karte.ansicht.zoom
        fun rahmen(ids: Set<KnotenId>, puffer: Float): Pair<Offset, Size>? {
            val knoten = karte.knoten.filter { it.id in ids }
            if (knoten.isEmpty()) return null
            val links = knoten.minOf { it.position.x } - puffer
            val oben = knoten.minOf { it.position.y } - puffer
            val rechts = knoten.maxOf { it.position.x + it.größe.breite } + puffer
            val unten = knoten.maxOf { it.position.y + it.größe.höhe } + puffer
            val start = Offset(
                karte.ansicht.verschiebung.x + links * density * zoom,
                karte.ansicht.verschiebung.y + oben * density * zoom,
            )
            return start to Size((rechts - links) * density * zoom, (unten - oben) * density * zoom)
        }
        karte.visuelleGruppen.forEach { gruppe ->
            rahmen(gruppe.knotenIds, 14f)?.let { (start, größe) ->
                drawRoundRect(
                    color = gruppenFarbe.copy(alpha = .8f),
                    topLeft = start,
                    size = größe,
                    cornerRadius = CornerRadius(14.dp.toPx() * zoom),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
        editor.ausgewählteKnoten.forEach { id ->
            rahmen(setOf(id), 4f)?.let { (start, größe) ->
                drawRoundRect(
                    color = auswahlFarbe,
                    topLeft = start,
                    size = größe,
                    cornerRadius = CornerRadius(10.dp.toPx() * zoom),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
    }
}

@Composable
private fun KartenWerkzeuge(editor: KartenEditorZustand, modifier: Modifier = Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, tonalElevation = 4.dp) {
        Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            KartenWerkzeugKnopf("↶", "Rückgängig", editor.kannRückgängig(), onClick = editor::rückgängig)
            KartenWerkzeugKnopf("↷", "Wiederholen", editor.kannWiederholen(), onClick = editor::wiederholen)
            HorizontalDivider()
            KartenWerkzeugKnopf(
                "1",
                "Einzelauswahl",
                aktiv = editor.auswahlModus == AuswahlModus.Einzeln,
                onClick = { editor.setzeAuswahlModus(AuswahlModus.Einzeln) },
            )
            KartenWerkzeugKnopf(
                "▦",
                "Gruppenauswahl",
                aktiv = editor.auswahlModus == AuswahlModus.Gruppe,
                onClick = { editor.setzeAuswahlModus(AuswahlModus.Gruppe) },
            )
        }
    }
}

@Composable
private fun KartenWerkzeugKnopf(
    symbol: String,
    beschreibung: String,
    aktiviert: Boolean = true,
    aktiv: Boolean = false,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = aktiviert,
        modifier = Modifier.size(42.dp).semantics { contentDescription = beschreibung },
        contentPadding = PaddingValues(0.dp),
        shape = MaterialTheme.shapes.small,
        colors = if (aktiv) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors(),
    ) {
        Text(symbol, style = MaterialTheme.typography.titleMedium)
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
    val knoten = editor.ausgewählteKnoten
    val aktiverKnoten = editor.ausgewählterKnoten
    val verbindung = editor.ausgewählteVerbindung
    editor.ersetzeKarte(neueKarte, historieLeeren = false)
    when {
        knoten.isNotEmpty() -> editor.stelleAuswahlWiederHer(knoten, aktiverKnoten)
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
