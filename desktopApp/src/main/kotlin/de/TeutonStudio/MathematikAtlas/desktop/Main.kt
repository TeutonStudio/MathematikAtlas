package de.TeutonStudio.MathematikAtlas.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.*
import de.TeutonStudio.MathematikKnoten.MatlasKartenContainer
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.nio.file.Paths
import java.util.prefs.Preferences
import javax.swing.JOptionPane

private const val DESKTOP_APP_VERSION = "2.32.2"

private sealed interface DesktopKontext {
    val position: GraphPunkt
    data class Hintergrund(override val position: GraphPunkt) : DesktopKontext
    data class Knoten(val daten: KnotenDaten) : DesktopKontext { override val position = daten.position }
    data class Verbindung(val daten: VerbindungDaten, override val position: GraphPunkt) : DesktopKontext
    data class Anschluss(val ref: AnschlussVerweis, override val position: GraphPunkt) : DesktopKontext
}

fun main(args: Array<String>) {
    if (args.firstOrNull() == "--smoke-test") {
        val basis = args.firstOrNull { it.startsWith("--data-dir=") }
            ?.substringAfter('=')
            ?.let(Paths::get)
            ?: DesktopKartenSpeicher.standardDatenVerzeichnis()
        val speicher = DesktopKartenSpeicher(basis)
        val vorher = speicher.ladeAktuell()
        val gespeichert = speicher.speichere(
            vorher?.copy(name = "${vorher.name} – Neustartprüfung")
                ?: KartenDaten(name = "Paketprüfung"),
        )
        check(DesktopKartenSpeicher(basis).ladeAktuell() == gespeichert) {
            "Persistenzprüfung nach simuliertem Neustart fehlgeschlagen."
        }
        println("Desktop-Paketprüfung erfolgreich: ${gespeichert.id.wert}, Version ${gespeichert.version}")
        return
    }
    desktopAnwendung()
}

private fun desktopAnwendung() = application {
    val einstellungen = remember { FensterEinstellungen.lade() }
    val fensterZustand = rememberWindowState(
        size = DpSize(einstellungen.breite.dp, einstellungen.höhe.dp),
        position = WindowPosition(einstellungen.x.dp, einstellungen.y.dp),
    )
    var awtFenster by remember { mutableStateOf<java.awt.Window?>(null) }
    val atlas = remember { DesktopAtlasZustand() }
    Window(
        title = "Mathematik Atlas",
        state = fensterZustand,
        onCloseRequest = {
            awtFenster?.bounds?.let(FensterEinstellungen::speichere)
            atlas.speichere()
            exitApplication()
        },
    ) {
        SideEffect { awtFenster = window }
        DesktopAtlasApp(atlas, window)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FrameWindowScope.DesktopAtlasApp(atlas: DesktopAtlasZustand, awtFenster: java.awt.Window) {
    var graphFokussiert by remember { mutableStateOf(true) }
    var letzterZeiger by remember { mutableStateOf(GraphPunkt(240f, 180f)) }
    var editorGröße by remember { mutableStateOf(IntSize.Zero) }
    var suchtext by remember { mutableStateOf("") }
    var kontext by remember { mutableStateOf<DesktopKontext?>(null) }
    var umbenennenOffen by remember { mutableStateOf(false) }
    var fehler by remember { mutableStateOf<String?>(null) }
    val dichte = LocalDensity.current
    val graphFokus = remember { FocusRequester() }
    val katalogFokus = remember { FocusRequester() }
    val inspectorFokus = remember { FocusRequester() }
    var fokusIndex by remember { mutableIntStateOf(1) }

    fun befehlsKontext(fokus: AtlasFokusBereich = if (graphFokussiert) AtlasFokusBereich.Karte else AtlasFokusBereich.Anwendung) = BefehlsKontext(
        fokus = fokus,
        zeigerPosition = letzterZeiger,
        sichtbareMitte = GraphPunkt(
            (editorGröße.width / dichte.density / 2f - atlas.editor.karte.ansicht.verschiebung.x) / atlas.editor.karte.ansicht.zoom,
            (editorGröße.height / dichte.density / 2f - atlas.editor.karte.ansicht.verschiebung.y) / atlas.editor.karte.ansicht.zoom,
        ),
        anzeigeBreiteDp = editorGröße.width / dichte.density,
        anzeigeHöheDp = editorGröße.height / dichte.density,
    )

    val befehle = remember(atlas.editor) {
        AtlasBefehlsAusführer(
            editor = atlas.editor,
            speichern = atlas::speichere,
            knotenAuswahlÖffnen = { katalogFokus.requestFocus() },
            umbenennen = { umbenennenOffen = true },
            sucheÖffnen = { katalogFokus.requestFocus() },
        )
    }
    val tastatur = remember(befehle) { AtlasTastaturAusführer(befehle) { befehlsKontext() } }

    fun ausführen(befehl: AtlasBefehl) {
        befehle.führeAus(befehl, befehlsKontext(AtlasFokusBereich.Karte))
        atlas.aktualisiereAuswertung()
    }

    fun importieren() {
        dateiWählen(awtFenster, FileDialog.LOAD, "Atlas-Karte importieren")?.let { datei ->
            runCatching { datei.readText() }.onSuccess(atlas::importiere).onFailure { fehler = it.message }
        }
    }

    fun exportieren() {
        val format = JOptionPane.showInputDialog(
            awtFenster,
            "Exportformat auswählen:",
            "Atlas-Karte exportieren",
            JOptionPane.QUESTION_MESSAGE,
            null,
            arrayOf("JSON", ".matlas"),
            "JSON",
        ) as? String ?: return
        val endung = if (format == ".matlas") MatlasKartenContainer.DATEI_ENDUNG else ".json"
        val basisName = atlas.editor.karte.name
            .removeSuffix(".json")
            .removeSuffix(MatlasKartenContainer.DATEI_ENDUNG)
            .ifBlank { "Karte" }
        dateiWählen(awtFenster, FileDialog.SAVE, "Atlas-Karte exportieren", "$basisName$endung")?.let { datei ->
            runCatching {
                if (format == ".matlas") {
                    MatlasKartenContainer.schreibeAtomar(datei.toPath(), atlas.editor.karte, DESKTOP_APP_VERSION)
                } else {
                    datei.writeText(atlas.speicher.exportiere(atlas.editor.karte))
                }
            }.onFailure { fehler = it.message }
        }
    }

    MenuBar {
        Menu("Datei") {
            Item("Neue Karte", onClick = atlas::neueKarte)
            Item("Importieren …", onClick = ::importieren)
            Item("Exportieren …", onClick = ::exportieren)
            Separator()
            Item("Speichern    Ctrl+S", onClick = { ausführen(AtlasBefehl.Speichern) })
        }
        Menu("Bearbeiten") {
            Item("Rückgängig    Ctrl+Z", enabled = befehle.istVerfügbar(AtlasBefehl.Rückgängig, befehlsKontext(AtlasFokusBereich.Karte)), onClick = { ausführen(AtlasBefehl.Rückgängig) })
            Item("Wiederholen    Ctrl+Shift+Z", enabled = befehle.istVerfügbar(AtlasBefehl.Wiederholen, befehlsKontext(AtlasFokusBereich.Karte)), onClick = { ausführen(AtlasBefehl.Wiederholen) })
            Separator()
            Item("Kopieren    Ctrl+C", onClick = { ausführen(AtlasBefehl.AuswahlKopieren) })
            Item("Ausschneiden    Ctrl+X", onClick = { ausführen(AtlasBefehl.AuswahlAusschneiden) })
            Item("Einfügen    Ctrl+V", onClick = { ausführen(AtlasBefehl.AuswahlEinfügen()) })
            Item("Löschen    Entf", onClick = { ausführen(AtlasBefehl.AuswahlLöschen) })
        }
        Menu("Ansicht") {
            Item("Inhalt einpassen    Home", onClick = { ausführen(AtlasBefehl.InhaltEinpassen) })
            Item("Auswahl zentrieren    F", onClick = { ausführen(AtlasBefehl.AuswahlZentrieren) })
            Item("Zoom 100 %    0", onClick = { ausführen(AtlasBefehl.ZoomSetzen(1f)) })
        }
    }

    MaterialTheme {
        Row(
            Modifier.fillMaxSize()
                .onPreviewKeyEvent { event ->
                    when {
                        event.type == KeyEventType.KeyDown && event.key == Key.F6 -> {
                            fokusIndex = (fokusIndex + if (event.isShiftPressed) 2 else 1) % 3
                            listOf(katalogFokus, graphFokus, inspectorFokus)[fokusIndex].requestFocus()
                            true
                        }
                        event.type == KeyEventType.KeyDown && (event.isCtrlPressed || event.isMetaPressed) && event.key == Key.S -> {
                            ausführen(AtlasBefehl.Speichern); true
                        }
                        else -> false
                    }
                }
                .onKeyEvent { tastatur.verarbeite(it).also { verarbeitet -> if (verarbeitet) atlas.aktualisiereAuswertung() } },
        ) {
            Katalog(
                atlas = atlas,
                suchtext = suchtext,
                onSuchtext = { suchtext = it },
                position = letzterZeiger,
                fokusRequester = katalogFokus,
                modifier = Modifier.width(270.dp).fillMaxHeight(),
            )
            VerticalDivider()
            Column(Modifier.weight(1f).fillMaxHeight()) {
                DesktopWerkzeugleiste(::ausführen, befehle, befehlsKontext(AtlasFokusBereich.Karte))
                HorizontalDivider()
                Box(
                    Modifier.weight(1f).fillMaxWidth()
                        .onSizeChanged { editorGröße = it }
                        .focusRequester(graphFokus)
                        .onFocusChanged { graphFokussiert = it.hasFocus }
                        .focusable()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    if (awaitPointerEvent().type == PointerEventType.Press) graphFokus.requestFocus()
                                }
                            }
                        },
                ) {
                    KnotenKartenEditor(
                        zustand = atlas.editor,
                        modifier = Modifier.fillMaxSize(),
                        rendererFür = atlas::rendererFür,
                        farbeFürAnschluss = { anschlussFarbe(it.art.wert) },
                        beiZeigerPosition = { letzterZeiger = it },
                        beiHintergrundKontext = { kontext = DesktopKontext.Hintergrund(it) },
                        beiHintergrundDoppelklick = { katalogFokus.requestFocus() },
                        beiKnotenKontext = { kontext = DesktopKontext.Knoten(it) },
                        beiVerbindungKontext = { kontext = DesktopKontext.Verbindung(it, letzterZeiger) },
                        beiAnschlussKontext = { kontext = DesktopKontext.Anschluss(it, letzterZeiger) },
                        beiVerbindungAufHintergrund = { _, position -> kontext = DesktopKontext.Hintergrund(position) },
                        zeigeKnotenInspektor = false,
                    )
                    kontext?.let { ziel ->
                        DesktopKontextMenü(
                            ziel = ziel,
                            atlas = atlas,
                            befehle = befehle,
                            befehlsKontext = befehlsKontext(AtlasFokusBereich.Karte),
                            schließen = { kontext = null },
                            knotenFokus = { katalogFokus.requestFocus() },
                        )
                    }
                }
            }
            VerticalDivider()
            DesktopInspektor(atlas, inspectorFokus, Modifier.width(300.dp).fillMaxHeight())
        }

        if (umbenennenOffen) {
            UmbenennenDialog(atlas, schließen = { umbenennenOffen = false })
        }
        (fehler ?: atlas.meldung)?.let { text ->
            AlertDialog(
                onDismissRequest = { fehler = null; atlas.schließeMeldung() },
                confirmButton = { TextButton(onClick = { fehler = null; atlas.schließeMeldung() }) { Text("OK") } },
                title = { Text("Mathematik Atlas") },
                text = { Text(text) },
            )
        }
    }
}

@Composable
private fun Katalog(
    atlas: DesktopAtlasZustand,
    suchtext: String,
    onSuchtext: (String) -> Unit,
    position: GraphPunkt,
    fokusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val sichtbar = remember(atlas.vorlagen, suchtext) {
        atlas.vorlagen.filter { suchtext.isBlank() || it.name.contains(suchtext, true) || it.kategorie.contains(suchtext, true) }
    }
    Column(modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Knotenkatalog", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = suchtext,
            onValueChange = onSuchtext,
            label = { Text("Suchen (Ctrl+F)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().focusRequester(fokusRequester),
        )
        LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(sichtbar, key = { "${it.art}:${it.name}:${it.standardParameter}" }) { vorlage ->
                OutlinedButton(
                    onClick = { atlas.fügeEin(vorlage, position) },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(8.dp),
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(vorlage.name, maxLines = 1)
                        Text(vorlage.kategorie, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DesktopWerkzeugleiste(
    ausführen: (AtlasBefehl) -> Unit,
    befehle: AtlasBefehlsAusführer,
    kontext: BefehlsKontext,
) {
    Row(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            "↶" to AtlasBefehl.Rückgängig,
            "↷" to AtlasBefehl.Wiederholen,
            "⧉" to AtlasBefehl.AuswahlDuplizieren,
            "⌫" to AtlasBefehl.AuswahlLöschen,
            "⊞" to AtlasBefehl.InhaltEinpassen,
        ).forEach { (symbol, befehl) ->
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { PlainTooltip { Text("${befehle.metadaten(befehl).name} · ${befehle.metadaten(befehl).tastenkürzel.orEmpty()}") } },
                state = rememberTooltipState(),
            ) {
                FilledTonalButton(
                    onClick = { ausführen(befehl) },
                    enabled = befehle.istVerfügbar(befehl, kontext),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.size(40.dp),
                ) { Text(symbol) }
            }
        }
        Spacer(Modifier.weight(1f))
        Text("F6: Bereich wechseln", Modifier.align(Alignment.CenterVertically), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun DesktopInspektor(atlas: DesktopAtlasZustand, fokus: FocusRequester, modifier: Modifier = Modifier) {
    val knoten = atlas.editor.karte.knoten.firstOrNull { it.id == atlas.editor.ausgewählterKnoten }
    var name by remember(knoten?.id, knoten?.name) { mutableStateOf(knoten?.name.orEmpty()) }
    Column(modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Inspector", style = MaterialTheme.typography.titleLarge)
        if (knoten == null) {
            Text("Wähle einen Knoten aus.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().focusRequester(fokus),
            )
            Button(onClick = { atlas.benenneAuswahlUm(name) }, enabled = name.isNotBlank() && name != knoten.name) { Text("Übernehmen") }
            HorizontalDivider()
            Text("Art", style = MaterialTheme.typography.labelLarge)
            Text(knoten.art)
            Text("Position: ${knoten.position.x.toInt()}, ${knoten.position.y.toInt()}")
            Text("Größe: ${knoten.größe.breite.toInt()} × ${knoten.größe.höhe.toInt()}")
            Text("Anschlüsse: ${knoten.anschlüsse.size}")
            HorizontalDivider()
            Text("Auswertung", style = MaterialTheme.typography.labelLarge)
            val dauer = atlas.auswertung.knoten[knoten.id]?.auswertungsDauerNanos
            Text(
                dauer?.let { "Letzte Auswertung: ${formatiereAuswertungsDauerNanos(it)}" }
                    ?: "Noch keine Auswertungsdauer gemessen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = { atlas.berechneKnotenCacheNeu(knoten.id) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Cache neu errechnen") }
        }
        Spacer(Modifier.weight(1f))
        if (atlas.auswertung.fehler.isNotEmpty()) {
            Text("Diagnosen", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            atlas.auswertung.fehler.take(5).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun DesktopKontextMenü(
    ziel: DesktopKontext,
    atlas: DesktopAtlasZustand,
    befehle: AtlasBefehlsAusführer,
    befehlsKontext: BefehlsKontext,
    schließen: () -> Unit,
    knotenFokus: () -> Unit,
) {
    val ansicht = atlas.editor.karte.ansicht
    val offset = IntOffset(
        (ziel.position.x * ansicht.zoom + ansicht.verschiebung.x).toInt(),
        (ziel.position.y * ansicht.zoom + ansicht.verschiebung.y).toInt(),
    )
    Popup(alignment = Alignment.TopStart, offset = offset, onDismissRequest = schließen, properties = PopupProperties(focusable = true)) {
        Surface(shadowElevation = 8.dp, shape = MaterialTheme.shapes.medium) {
            Column(Modifier.width(220.dp).padding(6.dp)) {
                fun ausführen(befehl: AtlasBefehl) {
                    befehle.führeAus(befehl, befehlsKontext)
                    atlas.aktualisiereAuswertung()
                    schließen()
                }
                when (ziel) {
                    is DesktopKontext.Hintergrund -> {
                        TextButton(onClick = { knotenFokus(); schließen() }, modifier = Modifier.fillMaxWidth()) { Text("Knoten einfügen (N)") }
                        TextButton(onClick = { ausführen(AtlasBefehl.AuswahlEinfügen(ziel.position)) }, enabled = !befehle.zwischenablage.istLeer(), modifier = Modifier.fillMaxWidth()) { Text("Einfügen (Ctrl+V)") }
                        TextButton(onClick = { atlas.editor.wähleKnoten(null); schließen() }, modifier = Modifier.fillMaxWidth()) { Text("Auswahl aufheben") }
                    }
                    is DesktopKontext.Knoten -> {
                        TextButton(onClick = { ausführen(AtlasBefehl.AuswahlKopieren) }, modifier = Modifier.fillMaxWidth()) { Text("Kopieren") }
                        TextButton(onClick = { ausführen(AtlasBefehl.AuswahlDuplizieren) }, modifier = Modifier.fillMaxWidth()) { Text("Duplizieren") }
                        TextButton(onClick = { ausführen(AtlasBefehl.AuswahlGruppieren) }, enabled = atlas.editor.ausgewählteKnoten.size >= 2, modifier = Modifier.fillMaxWidth()) { Text("Visuell gruppieren") }
                        TextButton(onClick = { ausführen(AtlasBefehl.AuswahlLöschen) }, modifier = Modifier.fillMaxWidth()) { Text("Löschen") }
                    }
                    is DesktopKontext.Verbindung -> TextButton(
                        onClick = { atlas.editor.wähleVerbindung(ziel.daten.id); ausführen(AtlasBefehl.AuswahlLöschen) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Verbindung löschen") }
                    is DesktopKontext.Anschluss -> Text("Anschluss ${ziel.ref.anschlussId.wert}", Modifier.padding(10.dp))
                }
            }
        }
    }
}

@Composable
private fun UmbenennenDialog(atlas: DesktopAtlasZustand, schließen: () -> Unit) {
    val knoten = atlas.editor.karte.knoten.firstOrNull { it.id == atlas.editor.ausgewählterKnoten } ?: return
    var name by remember(knoten.id) { mutableStateOf(knoten.name) }
    AlertDialog(
        onDismissRequest = schließen,
        confirmButton = { Button(onClick = { atlas.benenneAuswahlUm(name); schließen() }, enabled = name.isNotBlank()) { Text("Übernehmen") } },
        dismissButton = { TextButton(onClick = schließen) { Text("Abbrechen") } },
        title = { Text("Knoten umbenennen") },
        text = { OutlinedTextField(name, { name = it }, singleLine = true) },
    )
}

@Composable
private fun anschlussFarbe(id: String): Color = when {
    "zahl" in id -> Color(0xFF1E88E5)
    "menge" in id -> Color(0xFF43A047)
    "aussage" in id -> Color(0xFF8E24AA)
    "methode" in id -> Color(0xFFFB8C00)
    else -> MaterialTheme.colorScheme.primary
}

private fun dateiWählen(fenster: java.awt.Window, modus: Int, titel: String, dateiname: String? = null): File? {
    val dialog = FileDialog(fenster as? Frame, titel, modus)
    dialog.file = dateiname
    dialog.isVisible = true
    val datei = dialog.file ?: return null
    return File(dialog.directory, datei)
}

private data class FensterEinstellungen(val x: Int, val y: Int, val breite: Int, val höhe: Int) {
    companion object {
        private val prefs = Preferences.userRoot().node("de/TeutonStudio/MathematikAtlas/desktop")
        fun lade() = FensterEinstellungen(
            prefs.getInt("x", 120), prefs.getInt("y", 80),
            prefs.getInt("breite", 1440).coerceAtLeast(900), prefs.getInt("höhe", 900).coerceAtLeast(650),
        )
        fun speichere(bounds: java.awt.Rectangle) {
            prefs.putInt("x", bounds.x); prefs.putInt("y", bounds.y)
            prefs.putInt("breite", bounds.width); prefs.putInt("höhe", bounds.height)
        }
    }
}
