package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikAtlas.speicher.KartenJson
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.util.UUID

@Composable
internal fun KartenJsonDialog(zustand: AtlasZustand, schließen: () -> Unit) {
    val ausgangstext = remember(zustand.editor.karte.id, zustand.editor.karte.version) {
        zustand.speicher.exportiere(zustand.editor.karte)
    }
    var wert by remember(ausgangstext) { mutableStateOf(TextFieldValue(ausgangstext)) }
    var analyse by remember(ausgangstext) { mutableStateOf(analysiereJson(ausgangstext)) }
    var eingeklappt by remember(ausgangstext) { mutableStateOf(emptySet<Int>()) }
    var speicherFehler by remember(ausgangstext) { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(wert.text) {
        delay(100)
        val neu = analysiereJson(wert.text)
        analyse = neu
        eingeklappt = eingeklappt.intersect(neu.faltungen.mapTo(mutableSetOf()) { it.startOffset })
    }

    fun formatieren() {
        val formatiert = runCatching { JSONObject(wert.text).toString(2) }.getOrNull() ?: return
        wert = TextFieldValue(formatiert, TextRange(wert.selection.start.coerceAtMost(formatiert.length)))
        eingeklappt = emptySet()
        speicherFehler = null
    }

    fun übernehmen() {
        speicherFehler = zustand.übernehmeJson(wert.text)
        if (speicherFehler == null) schließen()
    }

    fun listenEintragHinzufügen(liste: JsonListe) {
        val eingefügt = fügeJsonListenEintragEin(wert.text, liste, zustand.editor.karte)
        wert = TextFieldValue(eingefügt.text, TextRange(eingefügt.cursor))
        eingeklappt = eingeklappt - liste.startOffset
        speicherFehler = null
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(.96f).fillMaxHeight(.92f).widthIn(max = 1320.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
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
                    TextButton(onClick = ::formatieren, enabled = analyse.fehler == null) { Text("Formatieren") }
                    TextButton(onClick = { clipboard.setText(AnnotatedString(wert.text)) }) { Text("Kopieren") }
                    TextButton(
                        onClick = { eingeklappt = analyse.faltungen.filter { it.tiefe <= 1 }.mapTo(mutableSetOf()) { it.startOffset } },
                        enabled = analyse.faltungen.isNotEmpty(),
                    ) { Text("Einklappen") }
                    TextButton(onClick = { eingeklappt = emptySet() }, enabled = eingeklappt.isNotEmpty()) { Text("Ausklappen") }
                }
                HorizontalDivider()
                JsonEditor(
                    wert = wert,
                    onWertÄnderung = { wert = it; speicherFehler = null },
                    analyse = analyse,
                    eingeklappt = eingeklappt,
                    onFaltung = { faltung ->
                        eingeklappt = if (faltung.startOffset in eingeklappt) eingeklappt - faltung.startOffset else eingeklappt + faltung.startOffset
                    },
                    onListePlus = ::listenEintragHinzufügen,
                    übernehmen = ::übernehmen,
                    formatieren = ::formatieren,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                JsonIdAssistent(
                    wert = wert,
                    karte = zustand.editor.karte,
                    onWertÄnderung = { wert = it; speicherFehler = null },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
                HorizontalDivider()
                JsonStatusLeiste(wert, analyse, speicherFehler)
                HorizontalDivider()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = schließen) { Text("Verwerfen") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = ::übernehmen, enabled = analyse.fehler == null) { Text("Übernehmen") }
                }
            }
        }
    }
}

@Composable
private fun JsonEditor(
    wert: TextFieldValue,
    onWertÄnderung: (TextFieldValue) -> Unit,
    analyse: JsonAnalyse,
    eingeklappt: Set<Int>,
    onFaltung: (JsonFaltung) -> Unit,
    onListePlus: (JsonListe) -> Unit,
    übernehmen: () -> Unit,
    formatieren: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vertikal = rememberScrollState()
    val horizontal = rememberScrollState()
    val farben = jsonEditorFarben()
    val sichtbareZeilen = remember(analyse, eingeklappt) {
        sichtbareJsonZeilen(analyse.zeilenAnzahl, analyse.faltungen, eingeklappt)
    }
    val cursorZeile = offsetZuZeileSpalte(wert.text, wert.selection.start).zeile
    val listenNachZeile = remember(analyse.listen) { analyse.listen.associateBy(JsonListe::startZeile) }

    Surface(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(8.dp),
        color = farben.hintergrund,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.fillMaxSize().verticalScroll(vertikal)) {
            Column(
                Modifier.width(86.dp).background(farben.zeilenRand).padding(vertical = 8.dp),
            ) {
                sichtbareZeilen.forEach { zeile ->
                    val liste = listenNachZeile[zeile.originalZeile]
                    Row(
                        Modifier.fillMaxWidth().height(20.dp)
                            .background(if (cursorZeile == zeile.originalZeile) farben.aktuelleZeile else Color.Transparent),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            when {
                                zeile.faltung == null -> " "
                                zeile.eingeklappt -> "▶"
                                else -> "▼"
                            },
                            Modifier.width(18.dp).clickable(enabled = zeile.faltung != null) { zeile.faltung?.let(onFaltung) },
                            color = farben.struktur,
                            fontSize = 10.sp,
                        )
                        Text(
                            zeile.originalZeile.toString(),
                            Modifier.weight(1f),
                            color = if (cursorZeile == zeile.originalZeile) MaterialTheme.colorScheme.primary else farben.zeilennummer,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                        )
                        Text(
                            if (liste != null) "+" else " ",
                            Modifier.width(22.dp).clickable(enabled = liste != null) { liste?.let(onListePlus) },
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                    }
                }
            }
            Box(Modifier.weight(1f).horizontalScroll(horizontal)) {
                val längsteZeile = wert.text.lineSequence().maxOfOrNull(String::length) ?: 0
                BasicTextField(
                    value = wert,
                    onValueChange = onWertÄnderung,
                    modifier = Modifier
                        .width((längsteZeile * 8 + 96).coerceIn(900, 6000).dp)
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
                    visualTransformation = JsonVisualTransformation(
                        analyse.faltungen.filter { it.startOffset in eingeklappt },
                        farben,
                    ),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JsonIdAssistent(
    wert: TextFieldValue,
    karte: KartenDaten,
    onWertÄnderung: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    val kontext = remember(wert.text, wert.selection) { jsonIdKontext(wert.text, wert.selection.start) }
    if (kontext == null) {
        Text(
            "Setze den Cursor in einen Wert von knotenId oder anschlussId, um eine referenzierte ID auszuwählen. Das + im Zeilenrand ergänzt bekannte Listen.",
            modifier = modifier.padding(vertical = 7.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val optionen = remember(kontext, karte) { idOptionen(kontext, karte) }
    var offen by remember(kontext) { mutableStateOf(false) }
    Row(modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("${kontext.schlüssel}:", style = MaterialTheme.typography.labelMedium)
        ExposedDropdownMenuBox(expanded = offen, onExpandedChange = { offen = it }, modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = kontext.aktuellerWert,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                label = { Text(if (kontext.schlüssel == "knotenId") "Knoten auswählen" else "Anschluss auswählen") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(offen) },
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = offen, onDismissRequest = { offen = false }) {
                optionen.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(option.titel)
                                Text(option.id, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        onClick = {
                            offen = false
                            val neu = wert.text.replaceRange(kontext.wertStart, kontext.wertEnde, option.id)
                            val cursor = kontext.wertStart + option.id.length
                            onWertÄnderung(TextFieldValue(neu, TextRange(cursor)))
                        },
                    )
                }
            }
        }
        Text("${optionen.size} Optionen", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun JsonStatusLeiste(wert: TextFieldValue, analyse: JsonAnalyse, speicherFehler: String?) {
    val position = offsetZuZeileSpalte(wert.text, wert.selection.start)
    val fehler = speicherFehler ?: analyse.fehler?.meldung
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Zeile ${position.zeile}, Spalte ${position.spalte}", style = MaterialTheme.typography.labelSmall)
        Text("${analyse.zeilenAnzahl} Zeilen · ${analyse.listen.size} Listen", style = MaterialTheme.typography.labelSmall)
        Text(
            fehler ?: "JSON-Struktur lesbar",
            Modifier.weight(1f),
            color = if (fehler == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
        )
        Text("Strg+S Übernehmen · Strg+Umschalt+F Formatieren", style = MaterialTheme.typography.labelSmall)
    }
}

private data class JsonEditorFarben(
    val hintergrund: Color,
    val zeilenRand: Color,
    val standard: Color,
    val schlüssel: Color,
    val zeichenkette: Color,
    val zahl: Color,
    val literal: Color,
    val struktur: Color,
    val zeilennummer: Color,
    val aktuelleZeile: Color,
)

@Composable
private fun jsonEditorFarben() = JsonEditorFarben(
    hintergrund = MaterialTheme.colorScheme.surface,
    zeilenRand = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .48f),
    standard = MaterialTheme.colorScheme.onSurface,
    schlüssel = MaterialTheme.colorScheme.primary,
    zeichenkette = MaterialTheme.colorScheme.tertiary,
    zahl = MaterialTheme.colorScheme.secondary,
    literal = MaterialTheme.colorScheme.primary.copy(alpha = .8f),
    struktur = MaterialTheme.colorScheme.onSurfaceVariant,
    zeilennummer = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .72f),
    aktuelleZeile = MaterialTheme.colorScheme.primary.copy(alpha = .08f),
)

internal data class JsonAnalyse(
    val faltungen: List<JsonFaltung>,
    val listen: List<JsonListe>,
    val fehler: JsonFehler?,
    val zeilenAnzahl: Int,
)
internal data class JsonFaltung(val startOffset: Int, val endeOffset: Int, val startZeile: Int, val endeZeile: Int, val tiefe: Int)
internal data class JsonListe(val startOffset: Int, val endeOffset: Int, val startZeile: Int, val endeZeile: Int, val tiefe: Int, val schlüssel: String?)
internal data class JsonFehler(val meldung: String, val offset: Int?, val zeile: Int?, val spalte: Int?)
internal data class JsonPosition(val zeile: Int, val spalte: Int)
internal data class JsonSichtbareZeile(val originalZeile: Int, val faltung: JsonFaltung?, val eingeklappt: Boolean)
internal data class JsonEinfügung(val text: String, val cursor: Int)
internal data class JsonIdKontext(
    val schlüssel: String,
    val aktuellerWert: String,
    val wertStart: Int,
    val wertEnde: Int,
    val knotenId: String?,
)
private data class JsonIdOption(val id: String, val titel: String)

internal fun analysiereJson(text: String): JsonAnalyse {
    val struktur = analysiereJsonStruktur(text)
    val fehler = runCatching { KartenJson.lese(text) }.exceptionOrNull()?.let { jsonFehler(text, it.message ?: it::class.simpleName.orEmpty()) }
    return JsonAnalyse(struktur.first, struktur.second, fehler, text.count { it == '\n' } + 1)
}

internal fun analysiereJsonFaltungen(text: String): List<JsonFaltung> = analysiereJsonStruktur(text).first
internal fun analysiereJsonListen(text: String): List<JsonListe> = analysiereJsonStruktur(text).second

private fun analysiereJsonStruktur(text: String): Pair<List<JsonFaltung>, List<JsonListe>> {
    data class Offen(val zeichen: Char, val offset: Int, val zeile: Int, val tiefe: Int, val schlüssel: String?)
    val stapel = mutableListOf<Offen>()
    val faltungen = mutableListOf<JsonFaltung>()
    val listen = mutableListOf<JsonListe>()
    var inZeichenkette = false
    var maskiert = false
    var zeile = 1
    var letzterSchlüssel: String? = null
    var zeichenkettenStart = -1

    text.forEachIndexed { index, zeichen ->
        if (inZeichenkette) {
            when {
                maskiert -> maskiert = false
                zeichen == '\\' -> maskiert = true
                zeichen == '"' -> {
                    inZeichenkette = false
                    val danach = text.indexOfFirstAb(index + 1) { !it.isWhitespace() }
                    if (danach < text.length && text[danach] == ':') {
                        letzterSchlüssel = text.substring(zeichenkettenStart + 1, index)
                    }
                }
            }
        } else {
            when (zeichen) {
                '"' -> { inZeichenkette = true; zeichenkettenStart = index }
                '{', '[' -> {
                    stapel += Offen(zeichen, index, zeile, stapel.size, if (zeichen == '[') letzterSchlüssel else null)
                    letzterSchlüssel = null
                }
                '}', ']' -> {
                    val erwartet = if (zeichen == '}') '{' else '['
                    val offen = stapel.lastOrNull()
                    if (offen?.zeichen == erwartet) {
                        stapel.removeAt(stapel.lastIndex)
                        if (offen.zeile < zeile) {
                            faltungen += JsonFaltung(offen.offset, index, offen.zeile, zeile, offen.tiefe)
                        }
                        if (zeichen == ']') listen += JsonListe(offen.offset, index, offen.zeile, zeile, offen.tiefe, offen.schlüssel)
                    }
                    letzterSchlüssel = null
                }
                ':' -> Unit
                ',' -> letzterSchlüssel = null
                else -> if (!zeichen.isWhitespace()) letzterSchlüssel = null
            }
        }
        if (zeichen == '\n') zeile++
    }
    return faltungen.sortedBy { it.startOffset } to listen.sortedBy { it.startOffset }
}

internal fun sichtbareJsonZeilen(zeilenAnzahl: Int, faltungen: List<JsonFaltung>, eingeklappt: Set<Int>): List<JsonSichtbareZeile> {
    val nachStart = faltungen.groupBy(JsonFaltung::startZeile).mapValues { (_, werte) -> werte.maxBy(JsonFaltung::endeZeile) }
    val ergebnis = mutableListOf<JsonSichtbareZeile>()
    var zeile = 1
    while (zeile <= zeilenAnzahl) {
        val faltung = nachStart[zeile]
        val istEingeklappt = faltung?.startOffset in eingeklappt
        ergebnis += JsonSichtbareZeile(zeile, faltung, istEingeklappt)
        zeile = if (faltung != null && istEingeklappt) faltung.endeZeile + 1 else zeile + 1
    }
    return ergebnis
}

internal fun offsetZuZeileSpalte(text: String, offset: Int): JsonPosition {
    val begrenzt = offset.coerceIn(0, text.length)
    var zeile = 1
    var letzterUmbruch = -1
    for (index in 0 until begrenzt) if (text[index] == '\n') { zeile++; letzterUmbruch = index }
    return JsonPosition(zeile, begrenzt - letzterUmbruch)
}

internal fun jsonIdKontext(text: String, cursor: Int): JsonIdKontext? {
    val regex = Regex("\\\"(knotenId|anschlussId)\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"")
    val treffer = regex.findAll(text).firstOrNull { cursor in it.groups[2]!!.range.first..(it.groups[2]!!.range.last + 1) } ?: return null
    val wertGruppe = treffer.groups[2]!!
    val objektStart = text.lastIndexOf('{', treffer.range.first).coerceAtLeast(0)
    val objektText = text.substring(objektStart, treffer.range.first)
    val knotenId = Regex("\\\"knotenId\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").findAll(objektText).lastOrNull()?.groupValues?.get(1)
    return JsonIdKontext(
        schlüssel = treffer.groupValues[1],
        aktuellerWert = treffer.groupValues[2],
        wertStart = wertGruppe.range.first,
        wertEnde = wertGruppe.range.last + 1,
        knotenId = knotenId,
    )
}

private fun idOptionen(kontext: JsonIdKontext, karte: KartenDaten): List<JsonIdOption> = when (kontext.schlüssel) {
    "knotenId" -> karte.knoten.map { JsonIdOption(it.id.wert, it.name) }
    "anschlussId" -> {
        val knoten = karte.knoten.firstOrNull { it.id.wert == kontext.knotenId }
        (knoten?.anschlüsse ?: karte.knoten.flatMap { it.anschlüsse }).map { anschluss ->
            val besitzer = knoten ?: karte.knoten.first { k -> k.anschlüsse.any { it.id == anschluss.id } }
            JsonIdOption(anschluss.id.wert, "${besitzer.name} · ${anschluss.name} · ${anschluss.richtung.name}")
        }
    }
    else -> emptyList()
}

internal fun fügeJsonListenEintragEin(text: String, liste: JsonListe, karte: KartenDaten): JsonEinfügung {
    val vorlage = jsonListenVorlage(liste.schlüssel, karte)
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
    val cursor = innenStart + ersetzt.indexOf(vorlage) + vorlage.length
    return JsonEinfügung(neu, cursor)
}

private fun jsonListenVorlage(schlüssel: String?, karte: KartenDaten): String = when (schlüssel) {
    "knotenIds" -> "\"${karte.knoten.firstOrNull()?.id?.wert.orEmpty()}\""
    "artVereinigtEingänge" -> "\"wahr\""
    "knoten" -> """{
      \"id\": \"${UUID.randomUUID()}\",
      \"art\": \"mathematik.zahl\",
      \"name\": \"Neuer Knoten\",
      \"position\": { \"x\": 0, \"y\": 0 },
      \"größe\": { \"breite\": 210, \"höhe\": 100 },
      \"parameter\": { \"wert\": \"0\" },
      \"eigenschaften\": {},
      \"anschlüsse\": []
    }"""
    "anschlüsse" -> """{
      \"id\": \"${UUID.randomUUID()}\",
      \"name\": \"wert\",
      \"richtung\": \"Eingang\",
      \"kante\": \"Links\",
      \"art\": \"mathematik.objekt\",
      \"reihenfolge\": 0,
      \"kannSichErweitern\": false,
      \"dynamischErzeugt\": false
    }"""
    "verbindungen" -> {
        val vonKnoten = karte.knoten.firstOrNull { k -> k.anschlüsse.any { it.richtung == AnschlussRichtung.Ausgang } }
        val zuKnoten = karte.knoten.firstOrNull { k -> k.anschlüsse.any { it.richtung == AnschlussRichtung.Eingang } }
        val von = vonKnoten?.anschlüsse?.firstOrNull { it.richtung == AnschlussRichtung.Ausgang }
        val zu = zuKnoten?.anschlüsse?.firstOrNull { it.richtung == AnschlussRichtung.Eingang }
        """{
      \"id\": \"${UUID.randomUUID()}\",
      \"von\": { \"knotenId\": \"${vonKnoten?.id?.wert.orEmpty()}\", \"anschlussId\": \"${von?.id?.wert.orEmpty()}\" },
      \"zu\": { \"knotenId\": \"${zuKnoten?.id?.wert.orEmpty()}\", \"anschlussId\": \"${zu?.id?.wert.orEmpty()}\" }
    }"""
    }
    "visuelleGruppen" -> """{
      \"id\": \"${UUID.randomUUID()}\",
      \"name\": \"Neue Gruppe\",
      \"knotenIds\": []
    }"""
    else -> "null"
}

private fun jsonFehler(text: String, meldung: String): JsonFehler {
    val vollständig = Regex("""at\s+(\d+)\s+\[character\s+(\d+)\s+line\s+(\d+)]""").find(meldung)
    if (vollständig != null) return JsonFehler(
        "Ungültiges JSON: $meldung",
        vollständig.groupValues[1].toIntOrNull()?.coerceIn(0, text.length),
        vollständig.groupValues[3].toIntOrNull(),
        vollständig.groupValues[2].toIntOrNull(),
    )
    val offset = Regex("""at\s+(\d+)""").find(meldung)?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(0, text.length)
    val position = offset?.let { offsetZuZeileSpalte(text, it) }
    return JsonFehler("Ungültiges JSON: $meldung", offset, position?.zeile, position?.spalte)
}

private class JsonVisualTransformation(
    private val faltungen: List<JsonFaltung>,
    private val farben: JsonEditorFarben,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val gefaltet = transformiereFaltungen(text.text, faltungen)
        return TransformedText(jsonHervorheben(gefaltet.text, farben), gefaltet.offsetMapping)
    }
}

private data class FaltungsTransformation(val text: String, val offsetMapping: OffsetMapping)
private fun transformiereFaltungen(text: String, faltungen: List<JsonFaltung>): FaltungsTransformation {
    if (faltungen.isEmpty()) return FaltungsTransformation(text, OffsetMapping.Identity)
    val gültige = buildList {
        var verdecktBis = -1
        faltungen.sortedBy(JsonFaltung::startOffset).forEach { if (it.startOffset > verdecktBis) { add(it); verdecktBis = it.endeOffset } }
    }
    val originalZuTransformiert = IntArray(text.length + 1)
    val transformiertZuOriginal = mutableListOf(0)
    val ausgabe = StringBuilder()
    var original = 0
    fun anhängen(index: Int) {
        originalZuTransformiert[index] = ausgabe.length
        ausgabe.append(text[index])
        originalZuTransformiert[index + 1] = ausgabe.length
        transformiertZuOriginal += index + 1
    }
    gültige.forEach { faltung ->
        val start = (faltung.startOffset + 1).coerceAtMost(text.length)
        val ende = faltung.endeOffset.coerceIn(start, text.length)
        while (original < start) anhängen(original++)
        val transformierterStart = ausgabe.length
        val platzhalter = " … ${faltung.endeZeile - faltung.startZeile} Zeilen "
        platzhalter.forEach { ausgabe.append(it); transformiertZuOriginal += start }
        for (offset in start until ende) originalZuTransformiert[offset] = transformierterStart
        original = ende
        originalZuTransformiert[original] = ausgabe.length
        transformiertZuOriginal[transformiertZuOriginal.lastIndex] = original
    }
    while (original < text.length) anhängen(original++)
    originalZuTransformiert[text.length] = ausgabe.length
    val rückweg = transformiertZuOriginal.toIntArray()
    return FaltungsTransformation(ausgabe.toString(), object : OffsetMapping {
        override fun originalToTransformed(offset: Int) = originalZuTransformiert[offset.coerceIn(0, originalZuTransformiert.lastIndex)]
        override fun transformedToOriginal(offset: Int) = rückweg[offset.coerceIn(0, rückweg.lastIndex)]
    })
}

private enum class JsonTokenArt { Schlüssel, Zeichenkette, Zahl, Literal, Struktur }
private data class JsonToken(val start: Int, val ende: Int, val art: JsonTokenArt)
private fun jsonHervorheben(text: String, farben: JsonEditorFarben): AnnotatedString {
    val builder = AnnotatedString.Builder(text)
    jsonTokens(text).forEach { token ->
        val farbe = when (token.art) {
            JsonTokenArt.Schlüssel -> farben.schlüssel
            JsonTokenArt.Zeichenkette -> farben.zeichenkette
            JsonTokenArt.Zahl -> farben.zahl
            JsonTokenArt.Literal -> farben.literal
            JsonTokenArt.Struktur -> farben.struktur
        }
        builder.addStyle(SpanStyle(color = farbe, fontWeight = if (token.art == JsonTokenArt.Schlüssel) FontWeight.SemiBold else FontWeight.Normal), token.start, token.ende)
    }
    return builder.toAnnotatedString()
}

private fun jsonTokens(text: String): List<JsonToken> {
    val tokens = mutableListOf<JsonToken>()
    var index = 0
    while (index < text.length) when (text[index]) {
        '"' -> {
            val start = index++
            var maskiert = false
            while (index < text.length) {
                val aktuell = text[index++]
                if (maskiert) maskiert = false else if (aktuell == '\\') maskiert = true else if (aktuell == '"') break
            }
            val danach = text.indexOfFirstAb(index) { !it.isWhitespace() }
            tokens += JsonToken(start, index, if (danach < text.length && text[danach] == ':') JsonTokenArt.Schlüssel else JsonTokenArt.Zeichenkette)
        }
        '-', in '0'..'9' -> {
            val start = index++
            while (index < text.length && (text[index].isDigit() || text[index] in charArrayOf('.', 'e', 'E', '+', '-'))) index++
            tokens += JsonToken(start, index, JsonTokenArt.Zahl)
        }
        '{', '}', '[', ']', ':', ',' -> { tokens += JsonToken(index, index + 1, JsonTokenArt.Struktur); index++ }
        else -> {
            val literal = listOf("true", "false", "null").firstOrNull { text.startsWith(it, index) }
            if (literal != null) { tokens += JsonToken(index, index + literal.length, JsonTokenArt.Literal); index += literal.length } else index++
        }
    }
    return tokens
}

private inline fun String.indexOfFirstAb(start: Int, prädikat: (Char) -> Boolean): Int {
    for (index in start until length) if (prädikat(this[index])) return index
    return length
}
