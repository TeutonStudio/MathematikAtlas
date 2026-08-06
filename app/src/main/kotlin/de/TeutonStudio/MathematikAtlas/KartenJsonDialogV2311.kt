package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikAtlas.speicher.KartenJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

@Composable
internal fun KartenJsonDialogV2311(zustand: AtlasZustand, schließen: () -> Unit) {
    val ausgangstext = remember(zustand.editor.karte.id, zustand.editor.karte.version) {
        zustand.speicher.exportiere(zustand.editor.karte)
    }
    val analyseCache = remember(ausgangstext) { JsonEditorAnalyseCache(::prüfeJsonV2311) }
    var wert by remember(ausgangstext) { mutableStateOf(TextFieldValue(ausgangstext)) }
    var prüfung by remember(ausgangstext) { mutableStateOf(analyseCache.sofort(ausgangstext)) }
    var übernahmeFehler by remember(ausgangstext) { mutableStateOf<String?>(null) }
    var verwerfenBestätigen by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(wert.text) {
        val auftrag = analyseCache.beauftrage(wert.text)
        delay(150)
        val ergebnis = withContext(Dispatchers.Default) { analyseCache.analysiere(auftrag) }
        analyseCache.übernehme(ergebnis)?.let { aktuellePrüfung ->
            prüfung = aktuellePrüfung
        }
    }

    fun dialogSchließen() {
        if (wert.text == ausgangstext) schließen() else verwerfenBestätigen = true
    }

    fun formatieren() {
        val formatiert = runCatching { JSONObject(wert.text).toString(2) }.getOrNull() ?: return
        val altePosition = offsetZuZeileSpalte(wert.text, wert.selection.start)
        val neuerOffset = offsetFürZeileSpalte(formatiert, altePosition.zeile, altePosition.spalte)
        wert = TextFieldValue(formatiert, TextRange(neuerOffset))
        übernahmeFehler = null
    }

    fun übernehmen() {
        übernahmeFehler = zustand.übernehmeJson(wert.text)
        if (übernahmeFehler == null) schließen()
    }

    Dialog(
        onDismissRequest = ::dialogSchließen,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            Modifier.fillMaxWidth(.96f).fillMaxHeight(.92f).widthIn(max = 1360.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp,
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("JSON der aktuellen Karte", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${zustand.editor.karte.name} · Version ${zustand.editor.karte.version} · ${if (wert.text == ausgangstext) "Unverändert" else "Nicht übernommen"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = ::formatieren, enabled = prüfung.syntaxFehler == null) { Text("Formatieren") }
                    TextButton(onClick = { clipboard.setText(AnnotatedString(wert.text)) }) { Text("Kopieren") }
                }
                HorizontalDivider()

                JsonStrukturWerkzeugeV2311(
                    wert = wert,
                    karte = zustand.editor.karte,
                    prüfung = prüfung,
                    onWert = { wert = it; übernahmeFehler = null },
                    modifier = Modifier.fillMaxWidth(),
                )
                HorizontalDivider()

                JsonEditorV2311(
                    wert = wert,
                    onWert = { wert = it; übernahmeFehler = null },
                    prüfung = prüfung,
                    übernehmen = ::übernehmen,
                    formatieren = ::formatieren,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                HorizontalDivider()

                JsonStatusV2311(
                    wert = wert,
                    prüfung = prüfung,
                    übernahmeFehler = übernahmeFehler,
                )
                HorizontalDivider()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = ::dialogSchließen) { Text("Verwerfen") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = ::übernehmen,
                        enabled = prüfung.syntaxFehler == null && prüfung.schemaFehler == null,
                    ) { Text("Übernehmen") }
                }
            }
        }
    }

    if (verwerfenBestätigen) {
        AlertDialog(
            onDismissRequest = { verwerfenBestätigen = false },
            title = { Text("Änderungen verwerfen?") },
            text = { Text("Die nicht übernommenen JSON-Änderungen gehen verloren.") },
            confirmButton = {
                Button(
                    onClick = schließen,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Verwerfen") }
            },
            dismissButton = {
                TextButton(onClick = { verwerfenBestätigen = false }) { Text("Weiter bearbeiten") }
            },
        )
    }
}

@Composable
private fun JsonEditorV2311(
    wert: TextFieldValue,
    onWert: (TextFieldValue) -> Unit,
    prüfung: JsonPrüfungV2311,
    übernehmen: () -> Unit,
    formatieren: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vertikal = rememberScrollState()
    val horizontal = rememberScrollState()
    val farben = jsonFarbenV2311()
    val cursorZeile = offsetZuZeileSpalte(wert.text, wert.selection.start).zeile
    val längsteZeile = wert.text.lineSequence().maxOfOrNull(String::length) ?: 0
    val zeilen = prüfung.zeilenAnzahl

    Surface(
        modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(8.dp),
        color = farben.hintergrund,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.fillMaxSize().verticalScroll(vertikal)) {
            Column(
                Modifier.width(((zeilen.toString().length * 9) + 30).dp)
                    .background(farben.zeilenRand)
                    .padding(vertical = 8.dp),
            ) {
                repeat(zeilen) { index ->
                    val zeile = index + 1
                    Text(
                        zeile.toString(),
                        modifier = Modifier.fillMaxWidth().height(20.dp).padding(end = 8.dp),
                        color = if (zeile == cursorZeile) MaterialTheme.colorScheme.primary else farben.zeilennummer,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    )
                }
            }
            Box(Modifier.weight(1f).horizontalScroll(horizontal)) {
                BasicTextField(
                    value = wert,
                    onValueChange = onWert,
                    modifier = Modifier
                        .width((längsteZeile * 8 + 120).coerceIn(900, 6000).dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .onPreviewKeyEvent { ereignis ->
                            if (ereignis.type != KeyEventType.KeyDown || !ereignis.isCtrlPressed) return@onPreviewKeyEvent false
                            when {
                                ereignis.key == Key.S -> { übernehmen(); true }
                                ereignis.key == Key.F && ereignis.isShiftPressed -> { formatieren(); true }
                                else -> false
                            }
                        },
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = farben.standard,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    visualTransformation = JsonFarbTransformationV2311(farben),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JsonStrukturWerkzeugeV2311(
    wert: TextFieldValue,
    karte: KartenDaten,
    prüfung: JsonPrüfungV2311,
    onWert: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    val idKontext = remember(prüfung.idBereiche, wert.selection) {
        jsonIdKontextV2311(prüfung.idBereiche, wert.selection.start)
    }
    val listen = prüfung.listen.filter { it.schlüssel in unterstützteListenV2311 }
    Column(modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (idKontext != null) {
            val optionen = remember(idKontext, karte) { idOptionenV2311(idKontext, karte) }
            var offen by remember(idKontext) { mutableStateOf(false) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${idKontext.schlüssel}:", style = MaterialTheme.typography.labelMedium)
                ExposedDropdownMenuBox(
                    expanded = offen,
                    onExpandedChange = { offen = it },
                    modifier = Modifier.weight(1f),
                ) {
                    OutlinedTextField(
                        value = idKontext.aktuellerWert,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(offen) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = offen, onDismissRequest = { offen = false }) {
                        optionen.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(option.titel)
                                        Text(option.id, style = MaterialTheme.typography.labelSmall)
                                    }
                                },
                                onClick = {
                                    offen = false
                                    val neu = wert.text.replaceRange(idKontext.wertStart, idKontext.wertEnde, option.id)
                                    onWert(TextFieldValue(neu, TextRange(idKontext.wertStart + option.id.length)))
                                },
                            )
                        }
                    }
                }
            }
        } else {
            Text(
                "Cursor in knotenId oder anschlussId setzen, um gültige Referenzen auszuwählen.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (listen.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listen.forEach { liste ->
                    AssistChip(
                        onClick = {
                            val eingefügt = fügeJsonListenEintragV2311(wert.text, liste, karte)
                            onWert(TextFieldValue(eingefügt.text, TextRange(eingefügt.cursor)))
                        },
                        label = { Text("+ ${liste.schlüssel}") },
                    )
                }
            }
        }
    }
}

@Composable
private fun JsonStatusV2311(
    wert: TextFieldValue,
    prüfung: JsonPrüfungV2311,
    übernahmeFehler: String?,
) {
    val position = offsetZuZeileSpalte(wert.text, wert.selection.start)
    val fehler = übernahmeFehler ?: prüfung.syntaxFehler ?: prüfung.schemaFehler
    val status = when {
        übernahmeFehler != null -> "Übernahme: $übernahmeFehler"
        prüfung.syntaxFehler != null -> "Syntax: ${prüfung.syntaxFehler}"
        prüfung.schemaFehler != null -> "Kartenschema: ${prüfung.schemaFehler}"
        else -> "Syntax und Kartenschema gültig"
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Zeile ${position.zeile}, Spalte ${position.spalte}", style = MaterialTheme.typography.labelSmall)
        Text("${prüfung.zeilenAnzahl} Zeilen", style = MaterialTheme.typography.labelSmall)
        Text(
            status,
            modifier = Modifier.weight(1f),
            color = if (fehler == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
        )
        Text("Strg+S · Strg+Umschalt+F", style = MaterialTheme.typography.labelSmall)
    }
}

internal data class JsonPrüfungV2311(
    val syntaxFehler: String?,
    val schemaFehler: String?,
    val zeilenAnzahl: Int,
    val listen: List<JsonListeV2311>,
    val idBereiche: List<JsonIdBereichV2311>,
)

internal data class JsonListeV2311(
    val startOffset: Int,
    val endeOffset: Int,
    val schlüssel: String?,
)

internal data class JsonEinfügungV2311(val text: String, val cursor: Int)

internal data class JsonIdBereichV2311(
    val schlüssel: String,
    val aktuellerWert: String,
    val wertStart: Int,
    val wertEnde: Int,
    val knotenId: String?,
)

internal data class JsonIdKontextV2311(
    val schlüssel: String,
    val aktuellerWert: String,
    val wertStart: Int,
    val wertEnde: Int,
    val knotenId: String?,
)

private data class JsonIdOptionV2311(val id: String, val titel: String)

internal fun prüfeJsonV2311(text: String): JsonPrüfungV2311 {
    val syntaxFehler = runCatching { JSONObject(text) }.exceptionOrNull()?.message
    val schemaFehler = if (syntaxFehler == null) {
        runCatching { KartenJson.lese(text) }.exceptionOrNull()?.message
    } else null
    return JsonPrüfungV2311(
        syntaxFehler = syntaxFehler,
        schemaFehler = schemaFehler,
        zeilenAnzahl = text.count { it == '\n' } + 1,
        listen = analysiereJsonListenV2311(text),
        idBereiche = analysiereJsonIdBereicheV2311(text),
    )
}

internal fun analysiereJsonListenV2311(text: String): List<JsonListeV2311> {
    data class Offen(val offset: Int, val schlüssel: String?)
    val stapel = mutableListOf<Offen>()
    val listen = mutableListOf<JsonListeV2311>()
    var inText = false
    var maskiert = false
    var textStart = -1
    var letzterSchlüssel: String? = null

    text.forEachIndexed { index, zeichen ->
        if (inText) {
            when {
                maskiert -> maskiert = false
                zeichen == '\\' -> maskiert = true
                zeichen == '"' -> {
                    inText = false
                    val danach = text.indexOfFirstAbV2311(index + 1) { !it.isWhitespace() }
                    if (danach < text.length && text[danach] == ':') {
                        letzterSchlüssel = text.substring(textStart + 1, index)
                    }
                }
            }
        } else {
            when (zeichen) {
                '"' -> { inText = true; textStart = index }
                '[' -> { stapel += Offen(index, letzterSchlüssel); letzterSchlüssel = null }
                ']' -> stapel.removeLastOrNull()?.let { offen ->
                    listen += JsonListeV2311(offen.offset, index, offen.schlüssel)
                    letzterSchlüssel = null
                }
                ',' -> letzterSchlüssel = null
                else -> if (!zeichen.isWhitespace() && zeichen != ':') letzterSchlüssel = null
            }
        }
    }
    return listen.sortedBy(JsonListeV2311::startOffset)
}

/** Analysiert alle ID-Felder genau einmal pro Textrevision. */
internal fun analysiereJsonIdBereicheV2311(text: String): List<JsonIdBereichV2311> {
    val idRegex = Regex("\"(knotenId|anschlussId)\"\\s*:\\s*\"([^\"]*)\"")
    val knotenRegex = Regex("\"knotenId\"\\s*:\\s*\"([^\"]+)\"")
    return idRegex.findAll(text).map { treffer ->
        val wertGruppe = treffer.groups[2]!!
        val objektStart = text.lastIndexOf('{', treffer.range.first).coerceAtLeast(0)
        val objektText = text.substring(objektStart, treffer.range.first)
        JsonIdBereichV2311(
            schlüssel = treffer.groupValues[1],
            aktuellerWert = treffer.groupValues[2],
            wertStart = wertGruppe.range.first,
            wertEnde = wertGruppe.range.last + 1,
            knotenId = knotenRegex.findAll(objektText).lastOrNull()?.groupValues?.get(1),
        )
    }.toList()
}

internal fun jsonIdKontextV2311(
    bereiche: List<JsonIdBereichV2311>,
    cursor: Int,
): JsonIdKontextV2311? = bereiche.firstOrNull { cursor in it.wertStart..it.wertEnde }?.let { bereich ->
    JsonIdKontextV2311(
        schlüssel = bereich.schlüssel,
        aktuellerWert = bereich.aktuellerWert,
        wertStart = bereich.wertStart,
        wertEnde = bereich.wertEnde,
        knotenId = bereich.knotenId,
    )
}

/** Quellkompatibler Einstieg für Tests und Werkzeuge außerhalb der Compose-Ansicht. */
internal fun jsonIdKontextV2311(text: String, cursor: Int): JsonIdKontextV2311? =
    jsonIdKontextV2311(analysiereJsonIdBereicheV2311(text), cursor)

private fun idOptionenV2311(kontext: JsonIdKontextV2311, karte: KartenDaten): List<JsonIdOptionV2311> = when (kontext.schlüssel) {
    "knotenId" -> karte.knoten.map { JsonIdOptionV2311(it.id.wert, it.name) }
    "anschlussId" -> {
        val knoten = karte.knoten.firstOrNull { it.id.wert == kontext.knotenId }
        val besitzerUndAnschlüsse = if (knoten != null) {
            knoten.anschlüsse.map { knoten to it }
        } else {
            karte.knoten.flatMap { besitzer -> besitzer.anschlüsse.map { besitzer to it } }
        }
        besitzerUndAnschlüsse.map { (besitzer, anschluss) ->
            JsonIdOptionV2311(
                anschluss.id.wert,
                "${besitzer.name} · ${anschluss.name} · ${anschluss.richtung.name}",
            )
        }
    }
    else -> emptyList()
}

internal fun fügeJsonListenEintragV2311(
    text: String,
    liste: JsonListeV2311,
    karte: KartenDaten,
): JsonEinfügungV2311 {
    val vorlage = jsonListenVorlageV2311(liste.schlüssel, karte)
    val zeilenStart = text.lastIndexOf('\n', liste.startOffset).let { if (it < 0) 0 else it + 1 }
    val einzug = text.substring(zeilenStart, liste.startOffset).takeWhile(Char::isWhitespace)
    val kinderEinzug = "$einzug  "
    val innenStart = liste.startOffset + 1
    val innenEnde = liste.endeOffset.coerceAtMost(text.length)
    val innen = text.substring(innenStart, innenEnde)
    val ersetzt = if (innen.isBlank()) {
        "\n$kinderEinzug$vorlage\n$einzug"
    } else {
        val ohneRechts = innen.trimEnd()
        val rest = innen.substring(ohneRechts.length)
        "$ohneRechts,\n$kinderEinzug$vorlage$rest"
    }
    val neu = text.replaceRange(innenStart, innenEnde, ersetzt)
    return JsonEinfügungV2311(neu, innenStart + ersetzt.indexOf(vorlage) + vorlage.length)
}

private val unterstützteListenV2311 = setOf("knoten", "anschlüsse", "verbindungen", "visuelleGruppen", "knotenIds")

private fun jsonListenVorlageV2311(schlüssel: String?, karte: KartenDaten): String = when (schlüssel) {
    "knotenIds" -> "\"${karte.knoten.firstOrNull()?.id?.wert.orEmpty()}\""
    "knoten" -> """{
      "id": "${UUID.randomUUID()}",
      "art": "mathematik.zahl",
      "name": "Neuer Knoten",
      "position": { "x": 0, "y": 0 },
      "größe": { "breite": 210, "höhe": 100 },
      "parameter": { "wert": "0" },
      "eigenschaften": {},
      "anschlüsse": []
    }"""
    "anschlüsse" -> """{
      "id": "${UUID.randomUUID()}",
      "name": "wert",
      "richtung": "Eingang",
      "kante": "Links",
      "art": "mathematik.objekt",
      "reihenfolge": 0,
      "kannSichErweitern": false,
      "dynamischErzeugt": false
    }"""
    "verbindungen" -> {
        val vonKnoten = karte.knoten.firstOrNull { k -> k.anschlüsse.any { it.richtung == AnschlussRichtung.Ausgang } }
        val zuKnoten = karte.knoten.firstOrNull { k -> k.anschlüsse.any { it.richtung == AnschlussRichtung.Eingang } }
        val von = vonKnoten?.anschlüsse?.firstOrNull { it.richtung == AnschlussRichtung.Ausgang }
        val zu = zuKnoten?.anschlüsse?.firstOrNull { it.richtung == AnschlussRichtung.Eingang }
        """{
      "id": "${UUID.randomUUID()}",
      "von": { "knotenId": "${vonKnoten?.id?.wert.orEmpty()}", "anschlussId": "${von?.id?.wert.orEmpty()}" },
      "zu": { "knotenId": "${zuKnoten?.id?.wert.orEmpty()}", "anschlussId": "${zu?.id?.wert.orEmpty()}" }
    }"""
    }
    "visuelleGruppen" -> """{
      "id": "${UUID.randomUUID()}",
      "knotenIds": []
    }"""
    else -> "null"
}

private fun offsetFürZeileSpalte(text: String, zeile: Int, spalte: Int): Int {
    var aktuelleZeile = 1
    var index = 0
    while (index < text.length && aktuelleZeile < zeile) {
        if (text[index++] == '\n') aktuelleZeile++
    }
    val zeilenEnde = text.indexOf('\n', index).let { if (it < 0) text.length else it }
    return (index + spalte - 1).coerceIn(index, zeilenEnde)
}

private data class JsonFarbenV2311(
    val hintergrund: Color,
    val zeilenRand: Color,
    val standard: Color,
    val schlüssel: Color,
    val zeichenkette: Color,
    val zahl: Color,
    val literal: Color,
    val struktur: Color,
    val zeilennummer: Color,
)

@Composable
private fun jsonFarbenV2311() = JsonFarbenV2311(
    hintergrund = MaterialTheme.colorScheme.surface,
    zeilenRand = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .48f),
    standard = MaterialTheme.colorScheme.onSurface,
    schlüssel = MaterialTheme.colorScheme.primary,
    zeichenkette = MaterialTheme.colorScheme.tertiary,
    zahl = MaterialTheme.colorScheme.secondary,
    literal = MaterialTheme.colorScheme.primary.copy(alpha = .8f),
    struktur = MaterialTheme.colorScheme.onSurfaceVariant,
    zeilennummer = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .72f),
)

private class JsonFarbTransformationV2311(private val farben: JsonFarbenV2311) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText = TransformedText(
        jsonHervorhebenV2311(text.text, farben),
        OffsetMapping.Identity,
    )
}

private enum class JsonTokenArtV2311 { Schlüssel, Zeichenkette, Zahl, Literal, Struktur }
private data class JsonTokenV2311(val start: Int, val ende: Int, val art: JsonTokenArtV2311)

private fun jsonHervorhebenV2311(text: String, farben: JsonFarbenV2311): AnnotatedString {
    val builder = AnnotatedString.Builder(text)
    jsonTokensV2311(text).forEach { token ->
        val farbe = when (token.art) {
            JsonTokenArtV2311.Schlüssel -> farben.schlüssel
            JsonTokenArtV2311.Zeichenkette -> farben.zeichenkette
            JsonTokenArtV2311.Zahl -> farben.zahl
            JsonTokenArtV2311.Literal -> farben.literal
            JsonTokenArtV2311.Struktur -> farben.struktur
        }
        builder.addStyle(
            SpanStyle(
                color = farbe,
                fontWeight = if (token.art == JsonTokenArtV2311.Schlüssel) FontWeight.SemiBold else FontWeight.Normal,
            ),
            token.start,
            token.ende,
        )
    }
    return builder.toAnnotatedString()
}

private fun jsonTokensV2311(text: String): List<JsonTokenV2311> {
    val tokens = mutableListOf<JsonTokenV2311>()
    var index = 0
    while (index < text.length) {
        when (text[index]) {
            '"' -> {
                val start = index++
                var maskiert = false
                while (index < text.length) {
                    val aktuell = text[index++]
                    if (maskiert) maskiert = false
                    else if (aktuell == '\\') maskiert = true
                    else if (aktuell == '"') break
                }
                val danach = text.indexOfFirstAbV2311(index) { !it.isWhitespace() }
                tokens += JsonTokenV2311(
                    start,
                    index,
                    if (danach < text.length && text[danach] == ':') JsonTokenArtV2311.Schlüssel else JsonTokenArtV2311.Zeichenkette,
                )
            }
            '-', in '0'..'9' -> {
                val start = index++
                while (index < text.length && (text[index].isDigit() || text[index] in charArrayOf('.', 'e', 'E', '+', '-'))) index++
                tokens += JsonTokenV2311(start, index, JsonTokenArtV2311.Zahl)
            }
            '{', '}', '[', ']', ':', ',' -> {
                tokens += JsonTokenV2311(index, index + 1, JsonTokenArtV2311.Struktur)
                index++
            }
            else -> {
                val literal = listOf("true", "false", "null").firstOrNull { text.startsWith(it, index) }
                if (literal != null) {
                    tokens += JsonTokenV2311(index, index + literal.length, JsonTokenArtV2311.Literal)
                    index += literal.length
                } else index++
            }
        }
    }
    return tokens
}

private inline fun String.indexOfFirstAbV2311(start: Int, prädikat: (Char) -> Boolean): Int {
    for (index in start until length) if (prädikat(this[index])) return index
    return length
}
