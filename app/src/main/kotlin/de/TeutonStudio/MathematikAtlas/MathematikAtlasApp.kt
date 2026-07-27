package de.TeutonStudio.MathematikAtlas

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenKartenEditor
import de.TeutonStudio.MathematikKnoten.LatexText
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
                    rendererFür = { zustand.renderer() },
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
        TextButton(onClick = zustand.editor::rückgängig, enabled = zustand.editor.kannRückgängig()) { Text("Rückgängig") }
        TextButton(onClick = zustand.editor::wiederholen, enabled = zustand.editor.kannWiederholen()) { Text("Wiederholen") }
        TextButton(onClick = onImport) { Text("Import") }
        TextButton(onClick = onExport) { Text("Export") }
        Button(onClick = zustand::speichereAktuell) { Text("Speichern") }
    }
}

@Composable
private fun VerwaltungsFenster(zustand: AtlasZustand, modifier: Modifier) {
    Surface(modifier, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column {
            Text("Mathematik Atlas", Modifier.padding(18.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            PrimaryScrollableTabRow(selectedTabIndex = zustand.linkerBereich.ordinal, edgePadding = 8.dp) {
                VerwaltungsBereich.entries.forEach { bereich ->
                    Tab(selected = zustand.linkerBereich == bereich, onClick = { zustand.linkerBereich = bereich }, text = { Text(bereich.name) })
                }
            }
            when (zustand.linkerBereich) {
                VerwaltungsBereich.Karten -> KartenListe(zustand)
                VerwaltungsBereich.Konzepte -> KonzeptListe()
                VerwaltungsBereich.Variablen -> VariablenListe(zustand)
                VerwaltungsBereich.Auswertung -> AuswertungsListe(zustand)
                VerwaltungsBereich.Fehler -> FehlerListe(zustand)
            }
        }
    }
}

@Composable
private fun KartenListe(zustand: AtlasZustand) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = zustand::neueKarte) { Text("Neue Karte") }
            OutlinedButton(onClick = zustand::archiviereAktuell) { Text("Archivieren") }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(zustand.karten, key = { it.id.wert }) { karte ->
                ListItem(
                    headlineContent = { Text(karte.name) },
                    supportingContent = { Text("Version ${karte.version}") },
                    modifier = Modifier.clip(MaterialTheme.shapes.medium).clickable { zustand.öffne(karte) }
                        .background(if (karte.id == zustand.editor.karte.id) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface),
                )
            }
        }
    }
}

@Composable private fun KonzeptListe() {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Mathematische Systeme", style = MaterialTheme.typography.titleMedium) }
        items(listOf("Standardanalysis", "Nichtstandardanalysis", "Aussagenlogik", "Mengenlehre", "Lineare Algebra")) { name ->
            ListItem(headlineContent = { Text(name) }, supportingContent = { Text("Definitionen, Kurzverfahren und Beispiele können als Karten hinterlegt werden.") })
        }
    }
}

@Composable private fun VariablenListe(zustand: AtlasZustand) {
    val variablen = zustand.auswertung.knoten.values.flatMap { it.ausgaben.values }.mapNotNull { it.objekt as? de.TeutonStudio.MathematikRechenSystem.kern.Variable }.distinctBy { it.name }
    LazyColumn(contentPadding = PaddingValues(12.dp)) {
        if (variablen.isEmpty()) item { Text("Keine freien Variablen in der aktuellen Karte.") }
        items(variablen) { ListItem(headlineContent = { Text(it.name) }) }
    }
}

@Composable private fun AuswertungsListe(zustand: AtlasZustand) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(zustand.editor.karte.knoten) { knoten ->
            val e = zustand.auswertung.knoten[knoten.id]
            ListItem(
                headlineContent = { Text(knoten.name) },
                supportingContent = {
                    val latex = e?.ausgaben?.values?.joinToString { it.objekt.zuLatex() }
                    if (latex != null) LatexText(latex, style = MaterialTheme.typography.bodyMedium)
                    else Text(e?.fehler ?: "Noch kein Ergebnis")
                },
            )
        }
    }
}

@Composable private fun FehlerListe(zustand: AtlasZustand) {
    LazyColumn(contentPadding = PaddingValues(12.dp)) {
        if (zustand.auswertung.fehler.isEmpty()) item { Text("Keine Auswertungsfehler.") }
        items(zustand.auswertung.fehler) { fehler -> ListItem(headlineContent = { Text(fehler, color = MaterialTheme.colorScheme.error) }) }
    }
}

@Composable
private fun Inspektor(zustand: AtlasZustand, modifier: Modifier) {
    Surface(modifier, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        val knoten = zustand.ausgewählterKnoten
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Inspektor", style = MaterialTheme.typography.headlineSmall)
            if (knoten == null) {
                Text("Wähle einen Knoten oder eine Verbindung aus.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                zustand.editor.ausgewählteVerbindung?.let { id ->
                    Text("Verbindung ${id.wert.take(8)}")
                    Button(onClick = zustand.editor::löscheAuswahl) { Text("Verbindung löschen") }
                }
                return@Column
            }
            Text(knoten.name, style = MaterialTheme.typography.titleLarge)
            Text(knoten.art, style = MaterialTheme.typography.labelMedium)
            if (knoten.art in setOf("mathematik.addition", "mathematik.vereinigung", "mathematik.schnitt", "mathematik.kartesischesProdukt", "mathematik.tupel", "mathematik.vektor", "mathematik.zeilenVektor", "mathematik.matrix")) {
                val wert = knoten.parameter["festeEingänge"] ?: "2"
                var text by remember(knoten.id, wert) { mutableStateOf(wert) }
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        it.toIntOrNull()?.let { anzahl -> zustand.editor.setzeFesteEingangAnzahl(knoten.id, anzahl) }
                    },
                    label = { Text("Feste Eingänge") },
                    supportingText = { Text("Mindestens 2; weitere Eingänge entstehen beim Verbinden.") },
                    modifier = Modifier.fillMaxWidth(),
                )
                val zeigeWerte = knoten.parameter["operatorAnzeige"] != "name"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Anzeige: Namen", modifier = Modifier.weight(1f))
                    Switch(
                        checked = zeigeWerte,
                        onCheckedChange = { werte ->
                            zustand.editor.führeAus(
                                KartenAktion.KnotenParameterÄndern(
                                    knoten.id,
                                    "operatorAnzeige",
                                    if (werte) "wert" else "name",
                                ),
                            )
                        },
                    )
                    Text("Werte")
                }
            }
            if (knoten.art == "mathematik.termZuMethode") {
                HorizontalDivider()
                Text("Argumentreihenfolge", style = MaterialTheme.typography.titleSmall)
                val argumente = knoten.anschlüsse.filter {
                    it.richtung == de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung.Eingang && it.name !in setOf("term", "zielmenge")
                }.sortedBy { it.reihenfolge }
                argumente.forEachIndexed { index, anschluss ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${index + 1}. ${anschluss.name}", modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = { zustand.editor.verschiebeEingang(knoten.id, anschluss.id, -1) }, enabled = index > 0) { Text("↑") }
                        OutlinedButton(onClick = { zustand.editor.verschiebeEingang(knoten.id, anschluss.id, 1) }, enabled = index < argumente.lastIndex) { Text("↓") }
                    }
                }
            }
            knoten.parameter.filterKeys { it !in setOf("festeEingänge", "operatorAnzeige") }.forEach { (schlüssel, wert) ->
                var text by remember(knoten.id, schlüssel, wert) { mutableStateOf(wert) }
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        zustand.editor.führeAus(KartenAktion.KnotenParameterÄndern(knoten.id, schlüssel, it))
                    },
                    label = { Text(schlüssel) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            knoten.kartenVerweis?.let { ref ->
                HorizontalDivider()
                Text("Kartenverweis: ${ref.kartenId.wert.take(8)}, Version ${ref.version}")
                Button(onClick = { zustand.öffne(ref) }) { Text("Unterkarte öffnen") }
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = zustand.editor::dupliziereAuswahl) { Text("Duplizieren") }
                Button(onClick = zustand.editor::löscheAuswahl, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Löschen") }
            }
        }
    }
}

@Composable
private fun KnotenAuswahlDialog(zustand: AtlasZustand, position: GraphPunkt) {
    AlertDialog(
        onDismissRequest = zustand::schließeKnotenAuswahl,
        title = { Text("Knoten einfügen") },
        text = {
            Column(Modifier.heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(zustand.suchText, zustand::setzeSuchText, label = { Text("Suchen") }, modifier = Modifier.fillMaxWidth())
                val sichtbareVorlagen = zustand.sichtbareVorlagen()
                var hauptTab by remember { mutableIntStateOf(0) }
                val rechnen = sichtbareVorlagen.filterNot { it.kategorie.startsWith("Aussagen:") }
                val aussagen = sichtbareVorlagen.filter { it.kategorie.startsWith("Aussagen:") }
                PrimaryScrollableTabRow(selectedTabIndex = hauptTab, edgePadding = 0.dp) {
                    Tab(
                        selected = hauptTab == 0,
                        onClick = { hauptTab = 0 },
                        text = { Text("Rechnen (${rechnen.size})") },
                    )
                    Tab(selected = hauptTab == 1, onClick = { hauptTab = 1 }, text = { Text("Aussagen (${aussagen.size})") })
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val vorlagen = if (hauptTab == 0) rechnen else aussagen
                    val gruppen = vorlagen.groupBy { kategorieAnzeige(it.kategorie) }.toMutableMap()
                    if (hauptTab == 0) gruppen.putIfAbsent("Tensoren", emptyList())
                    val reihenfolge = if (hauptTab == 0) listOf("Zahlen", "Vektoren", "Matrizen", "Tensoren", "Abbildungen", "Mengen", "Steuerung", "Karten") else listOf("Aussagenlogik", "Mengenprädikate", "Zahlenprädikate", "Aussagenprädikate")
                    reihenfolge.filter { it in gruppen }.forEach { gruppe ->
                        val einträge = gruppen.getValue(gruppe)
                        item { Text(gruppe, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp, start = 4.dp)) }
                        if (einträge.isEmpty()) item { Text("Noch keine Knoten", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp)) }
                        items(einträge) { vorlage ->
                            ListItem(
                                headlineContent = { Text(vorlage.name) }, supportingContent = { Text(vorlage.beschreibung) },
                                modifier = Modifier.clip(MaterialTheme.shapes.small).clickable { zustand.fügeKnotenEin(vorlage, position) },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = zustand::schließeKnotenAuswahl) { Text("Schließen") } },
    )
}

private fun kategorieAnzeige(kategorie: String): String = when {
    kategorie.startsWith("Aussagen:") -> kategorie.substringAfter(": ")
    kategorie in setOf("Rechnen", "Analysis", "Algebra", "Operatoren") -> "Zahlen"
    kategorie in setOf("Methoden") -> "Abbildungen"
    kategorie in setOf("Gruppen", "Gespeicherte Karten") -> "Karten"
    else -> kategorie
}

private fun anschlussFarbe(id: String) = when (id) {
    "mathematik.zahl" -> androidx.compose.ui.graphics.Color(0xFF2563EB)
    "mathematik.aussage" -> androidx.compose.ui.graphics.Color(0xFF7C3AED)
    "mathematik.menge" -> androidx.compose.ui.graphics.Color(0xFF059669)
    "mathematik.vektor", "mathematik.matrix" -> androidx.compose.ui.graphics.Color(0xFFEA580C)
    "mathematik.funktion" -> androidx.compose.ui.graphics.Color(0xFFDB2777)
    else -> androidx.compose.ui.graphics.Color(0xFF475569)
}
