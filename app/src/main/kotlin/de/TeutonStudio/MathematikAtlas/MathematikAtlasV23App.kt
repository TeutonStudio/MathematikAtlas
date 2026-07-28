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

private sealed interface V23GraphKontext {
    data class Knoten(val id: KnotenId) : V23GraphKontext
    data class Knotengruppe(val knotenIds: Set<KnotenId>) : V23GraphKontext
    data class Verbindung(val id: VerbindungsId) : V23GraphKontext
    data class Anschluss(val ref: AnschlussVerweis) : V23GraphKontext
}

@Composable
fun MathematikAtlasV23App(zustand: AtlasZustand) {
    val context = LocalContext.current
    val konzeptSitzung = remember { KonzeptSitzung() }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use { w -> w.write(zustand.speicher.exportiere(zustand.editor.karte)) } }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> zustand.importiere(r.readText()) } }
    }
    var graphKontext by remember { mutableStateOf<V23GraphKontext?>(null) }

    LaunchedEffect(zustand.editor.karte) {
        zustand.aktualisiereAuswertung()
        delay(650)
        zustand.speichereAktuell()
        if (konzeptSitzung.istAktiv) konzeptSitzung.schließe()
    }
    LaunchedEffect(zustand.linkerBereich) {
        if (zustand.linkerBereich != VerwaltungsBereich.Konzepte && konzeptSitzung.istAktiv) {
            konzeptSitzung.schließe()
        }
    }

    Row(
        Modifier.fillMaxSize()
            .navigationBarsPadding()
            .background(MaterialTheme.colorScheme.background),
    ) {
        V23VerwaltungsFenster(
            zustand = zustand,
            konzeptSitzung = konzeptSitzung,
            modifier = Modifier.width(280.dp).fillMaxHeight(),
        )
        VerticalDivider()
        Column(Modifier.weight(1f).fillMaxHeight()) {
            if (konzeptSitzung.istAktiv) {
                KonzeptKopfLeiste(konzeptSitzung)
                HorizontalDivider()
                KonzeptArbeitsbereich(konzeptSitzung, Modifier.weight(1f).fillMaxWidth())
            } else {
                V23WerkzeugLeiste(
                    zustand,
                    onImport = { import.launch(arrayOf("application/json", "text/plain")) },
                    onExport = { export.launch("${zustand.editor.karte.name}.json") },
                )
                HorizontalDivider()
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    KnotenKartenEditor(
                        zustand = zustand.editor,
                        modifier = Modifier.fillMaxSize(),
                        rendererFür = zustand::rendererFür,
                        farbeFürAnschluss = { anschluss -> anschlussFarbe(anschluss.art.wert) },
                        beiHintergrundKontext = { zustand.öffneKnotenAuswahl(it) },
                        beiKnotenKontext = { knoten ->
                            graphKontext = if (zustand.editor.auswahlModus == AuswahlModus.Gruppe) {
                                V23GraphKontext.Knotengruppe(zustand.editor.ausgewählteKnoten + knoten.id)
                            } else V23GraphKontext.Knoten(knoten.id)
                        },
                        beiVerbindungKontext = { graphKontext = V23GraphKontext.Verbindung(it.id) },
                        beiAnschlussKontext = { graphKontext = V23GraphKontext.Anschluss(it) },
                        beiVerbindungAufHintergrund = { start, position -> zustand.öffneKnotenAuswahl(position, start) },
                        beiKnotenDoppelklick = { it.kartenVerweis?.let(zustand::öffne) },
                    )
                    V23KartenMarkierungen(zustand.editor)
                    V23KartenWerkzeuge(
                        editor = zustand.editor,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            // Die Minimap ist 180 × 120 dp groß und besitzt 16 dp Rand.
                            // 180 + 16 klebt die Werkzeugleiste exakt an ihre linke Kante.
                            .offset(x = (-196).dp, y = (-16).dp)
                            .width(104.dp)
                            .height(120.dp),
                    )
                    zustand.knotenAuswahlPosition?.let { KnotenAuswahlDialog(zustand, it) }
                    graphKontext?.let { V23KontextDialog(zustand, it) { graphKontext = null } }
                }
            }
        }
        VerticalDivider()
        if (konzeptSitzung.istAktiv) {
            KonzeptInspektor(konzeptSitzung, Modifier.width(310.dp).fillMaxHeight())
        } else {
            Inspektor(zustand, Modifier.width(310.dp).fillMaxHeight())
        }
    }
}

@Composable
private fun V23VerwaltungsFenster(
    zustand: AtlasZustand,
    konzeptSitzung: KonzeptSitzung,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        VerwaltungsFenster(zustand, Modifier.fillMaxSize())
        if (zustand.linkerBereich == VerwaltungsBereich.Konzepte) {
            Box(Modifier.fillMaxSize().padding(top = 112.dp)) {
                Surface(
                    Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    KonzeptBrowser(konzeptSitzung)
                }
            }
        }
    }
}

@Composable
private fun V23KontextDialog(zustand: AtlasZustand, kontext: V23GraphKontext, schließen: () -> Unit) {
    if (kontext is V23GraphKontext.Knoten) {
        val knoten = zustand.editor.karte.knoten.firstOrNull { it.id == kontext.id }
        if (knoten != null) {
            KnotenKonzeptDialog(zustand, knoten, schließen)
            return
        }
    }

    val titel = when (kontext) {
        is V23GraphKontext.Knoten -> "Knoten existiert nicht mehr"
        is V23GraphKontext.Knotengruppe -> "${kontext.knotenIds.size} Knoten ausgewählt"
        is V23GraphKontext.Verbindung -> "ID: ${kontext.id.wert}"
        is V23GraphKontext.Anschluss -> "Anschluss: ${kontext.ref.anschlussId.wert}"
    }
    AlertDialog(
        onDismissRequest = schließen,
        title = { Text(titel, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (kontext) {
                    is V23GraphKontext.Knoten -> Text("Der Knoten existiert nicht mehr.")
                    is V23GraphKontext.Knotengruppe -> {
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
                    is V23GraphKontext.Verbindung -> {
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
                    is V23GraphKontext.Anschluss -> {
                        val knoten = zustand.editor.karte.knoten.firstOrNull { it.id == kontext.ref.knotenId }
                        val anschluss = knoten?.anschlüsse?.firstOrNull { it.id == kontext.ref.anschlussId }
                        if (knoten == null || anschluss == null) {
                            Text("Der Anschluss existiert nicht mehr.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text(knoten.name, style = MaterialTheme.typography.labelLarge)
                            Text("${anschluss.name} · ${anschluss.richtung.name} · ${anschluss.art.wert}")
                            val erweiterbar = zustand.editor.kannAnschlussRelativEinfügen(kontext.ref)
                            val vernichtbar = zustand.editor.kannAnschlussVernichten(kontext.ref)
                            Button(
                                onClick = {
                                    zustand.editor.fügeAnschlussRelativEin(kontext.ref, AnschlussEinfügePosition.Davor)
                                    schließen()
                                },
                                enabled = erweiterbar,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Anschluss oberhalb einfügen") }
                            Button(
                                onClick = {
                                    zustand.editor.vernichteAnschluss(kontext.ref)
                                    schließen()
                                },
                                enabled = vernichtbar,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            ) { Text("Anschluss vernichten") }
                            OutlinedButton(
                                onClick = {
                                    zustand.editor.fügeAnschlussRelativEin(kontext.ref, AnschlussEinfügePosition.Danach)
                                    schließen()
                                },
                                enabled = erweiterbar,
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Anschluss unterhalb einfügen") }
                            when {
                                !erweiterbar -> Text(
                                    "Dieser Knoten besitzt eine feste Anschlusszahl.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                !vernichtbar -> Text(
                                    "Der Knoten benötigt mindestens zwei feste Eingänge.",
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
private fun V23WerkzeugLeiste(zustand: AtlasZustand, onImport: () -> Unit, onExport: () -> Unit) {
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
private fun V23KartenMarkierungen(editor: KartenEditorZustand) {
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
private fun V23KartenWerkzeuge(editor: KartenEditorZustand, modifier: Modifier = Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, tonalElevation = 3.dp) {
        Column(
            Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                V23KartenWerkzeugKnopf("↶", "Rückgängig", editor.kannRückgängig(), onClick = editor::rückgängig)
                V23KartenWerkzeugKnopf("↷", "Wiederholen", editor.kannWiederholen(), onClick = editor::wiederholen)
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                V23KartenWerkzeugKnopf(
                    "1",
                    "Einzelauswahl",
                    aktiv = editor.auswahlModus == AuswahlModus.Einzeln,
                    onClick = { editor.setzeAuswahlModus(AuswahlModus.Einzeln) },
                )
                V23KartenWerkzeugKnopf(
                    "▦",
                    "Gruppenauswahl",
                    aktiv = editor.auswahlModus == AuswahlModus.Gruppe,
                    onClick = { editor.setzeAuswahlModus(AuswahlModus.Gruppe) },
                )
            }
        }
    }
}

@Composable
private fun V23KartenWerkzeugKnopf(
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
