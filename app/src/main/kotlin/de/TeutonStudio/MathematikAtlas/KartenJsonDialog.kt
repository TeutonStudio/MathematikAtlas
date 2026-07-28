package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.TeutonStudio.MathematikAtlas.speicher.KartenJson
import kotlinx.coroutines.delay
import org.json.JSONObject

@Composable
internal fun KartenJsonDialog(zustand: AtlasZustand, schließen: () -> Unit) {
    val ausgangstext = remember(zustand.editor.karte.id, zustand.editor.karte.version) {
        zustand.speicher.exportiere(zustand.editor.karte)
    }
    var textWert by remember(ausgangstext) { mutableStateOf(TextFieldValue(ausgangstext)) }
    var analyse by remember(ausgangstext) { mutableStateOf(analysiereJson(ausgangstext)) }
    var eingeklappt by remember(ausgangstext) { mutableStateOf(emptySet<Int>()) }
    var speicherFehler by remember(ausgangstext) { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(textWert.text) {
        delay(120)
        val neueAnalyse = analysiereJson(textWert.text)
        analyse = neueAnalyse
        eingeklappt = eingeklappt.intersect(neueAnalyse.faltungen.mapTo(mutableSetOf()) { it.startOffset })
    }

    fun formatieren() {
        val formatiert = runCatching { JSONObject(textWert.text).toString(2) }.getOrNull() ?: return
        val cursor = textWert.selection.start.coerceAtMost(formatiert.length)
        textWert = TextFieldValue(formatiert, TextRange(cursor))
        eingeklappt = emptySet()
        speicherFehler = null
    }

    fun übernehmen() {
        speicherFehler = zustand.übernehmeJson(textWert.text)
        if (speicherFehler == null) schließen()
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(.94f)
                .fillMaxHeight(.9f)
                .widthIn(max = 1240.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
        ) {
            Column(Modifier.fillMaxSize()) {
                JsonDialogKopf(
                    kartenName = zustand.editor.karte.name,
                    version = zustand.editor.karte.version,
                    geändert = textWert.text != ausgangstext,
                    kannFormatieren = analyse.fehler == null,
                    hatFaltungen = analyse.faltungen.isNotEmpty(),
                    hatEingeklappteBereiche = eingeklappt.isNotEmpty(),
                    formatieren = ::formatieren,
                    kopieren = { clipboard.setText(AnnotatedString(textWert.text)) },
                    allesEinklappen = {
                        val ersteInhaltsebene = analyse.faltungen.filter { it.tiefe == 1 }
                        val kandidaten = ersteInhaltsebene.ifEmpty { analyse.faltungen.filter { it.tiefe == 0 } }
                        eingeklappt = kandidaten.mapTo(mutableSetOf()) { it.startOffset }
                    },
                    allesAusklappen = { eingeklappt = emptySet() },
                )
                HorizontalDivider()
                JsonEditor(
                    wert = textWert,
                    onWertÄnderung = {
                        textWert = it
                        speicherFehler = null
                    },
                    analyse = analyse,
                    eingeklappt = eingeklappt,
                    onFaltungUmschalten = { faltung ->
                        val wirdEingeklappt = faltung.startOffset !in eingeklappt
                        if (wirdEingeklappt && textWert.selection.start in (faltung.startOffset + 1) until faltung.endeOffset) {
                            textWert = textWert.copy(selection = TextRange(faltung.startOffset + 1))
                        }
                        eingeklappt = if (wirdEingeklappt) {
                            eingeklappt + faltung.startOffset
                        } else {
                            eingeklappt - faltung.startOffset
                        }
                    },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    übernehmen = ::übernehmen,
                    formatieren = ::formatieren,
                )
                HorizontalDivider()
                JsonStatusLeiste(
                    wert = textWert,
                    analyse = analyse,
                    speicherFehler = speicherFehler,
                )
                HorizontalDivider()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = schließen) { Text("Verwerfen") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = ::übernehmen,
                        enabled = analyse.fehler == null,
                    ) { Text("Übernehmen") }
                }
            }
        }
    }
}

@Composable
private fun JsonDialogKopf(
    kartenName: String,
    version: Int,
    geändert: Boolean,
    kannFormatieren: Boolean,
    hatFaltungen: Boolean,
    hatEingeklappteBereiche: Boolean,
    formatieren: () -> Unit,
    kopieren: () -> Unit,
    allesEinklappen: () -> Unit,
    allesAusklappen: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("JSON der aktuellen Karte", style = MaterialTheme.typography.titleLarge)
                Text(
                    "$kartenName · Version $version · ${if (geändert) "Nicht übernommen" else "Unverändert"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (geändert) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = formatieren, enabled = kannFormatieren) { Text("Formatieren") }
            TextButton(onClick = kopieren) { Text("Kopieren") }
            TextButton(onClick = allesEinklappen, enabled = hatFaltungen) { Text("Einklappen") }
            TextButton(onClick = allesAusklappen, enabled = hatEingeklappteBereiche) { Text("Ausklappen") }
        }
    }
}

@Composable
private fun JsonEditor(
    wert: TextFieldValue,
    onWertÄnderung: (TextFieldValue) -> Unit,
    analyse: JsonAnalyse,
    eingeklappt: Set<Int>,
    onFaltungUmschalten: (JsonFaltung) -> Unit,
    modifier: Modifier = Modifier,
    übernehmen: () -> Unit,
    formatieren: () -> Unit,
) {
    val vertikal = rememberScrollState()
    val horizontal = rememberScrollState()
    val farben = jsonEditorFarben()
    val zeilenHöhe = 20.sp
    val density = LocalDensity.current
    val zeilenHöheDp = with(density) { zeilenHöhe.toDp() }
    val sichtbareZeilen = remember(analyse, eingeklappt) {
        sichtbareJsonZeilen(analyse.zeilenAnzahl, analyse.faltungen, eingeklappt)
    }
    val cursorZeile = offsetZuZeileSpalte(wert.text, wert.selection.start).zeile
    val aktuelleSichtbareZeile = sichtbareZeilen.indexOfFirst { sichtbare ->
        cursorZeile == sichtbare.originalZeile || sichtbare.faltung?.let { cursorZeile in it.startZeile..it.endeZeile } == true
    }.coerceAtLeast(0)
    val fehlerSichtbareZeile = analyse.fehler?.zeile?.let { fehlerZeile ->
        sichtbareZeilen.indexOfFirst { sichtbare ->
            fehlerZeile == sichtbare.originalZeile || sichtbare.faltung?.let { fehlerZeile in it.startZeile..it.endeZeile } == true
        }.takeIf { it >= 0 }
    }
    val längsteZeile = wert.text.lineSequence().maxOfOrNull { it.length } ?: 0
    val editorBreite = (längsteZeile * 8 + 64).coerceIn(900, 5000).dp
    val nummernBreite = ((analyse.zeilenAnzahl.toString().length * 9) + 34).dp

    Surface(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(8.dp),
        color = farben.hintergrund,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .verticalScroll(vertikal),
        ) {
            JsonZeilenRand(
                sichtbareZeilen = sichtbareZeilen,
                aktuelleSichtbareZeile = aktuelleSichtbareZeile,
                breite = nummernBreite,
                zeilenHöhe = zeilenHöheDp,
                farben = farben,
                onFaltungUmschalten = onFaltungUmschalten,
            )
            Box(
                Modifier
                    .weight(1f)
                    .horizontalScroll(horizontal),
            ) {
                val zeilenHöhePx = with(density) { zeilenHöhe.toPx() }
                val vertikalerInnenabstandPx = with(density) { 8.dp.toPx() }
                Box(
                    Modifier
                        .width(editorBreite)
                        .drawBehind {
                            drawRect(
                                color = farben.aktuelleZeile,
                                topLeft = Offset(0f, vertikalerInnenabstandPx + aktuelleSichtbareZeile * zeilenHöhePx),
                                size = Size(size.width, zeilenHöhePx),
                            )
                            fehlerSichtbareZeile?.let { index ->
                                drawRect(
                                    color = farben.fehlerZeile,
                                    topLeft = Offset(0f, vertikalerInnenabstandPx + index * zeilenHöhePx),
                                    size = Size(size.width, zeilenHöhePx),
                                )
                            }
                        },
                ) {
                    BasicTextField(
                        value = wert,
                        onValueChange = onWertÄnderung,
                        modifier = Modifier
                            .fillMaxWidth()
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
                            lineHeight = zeilenHöhe,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        visualTransformation = JsonVisualTransformation(
                            faltungen = analyse.faltungen.filter { it.startOffset in eingeklappt },
                            farben = farben,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun JsonZeilenRand(
    sichtbareZeilen: List<JsonSichtbareZeile>,
    aktuelleSichtbareZeile: Int,
    breite: Dp,
    zeilenHöhe: Dp,
    farben: JsonEditorFarben,
    onFaltungUmschalten: (JsonFaltung) -> Unit,
) {
    Column(
        Modifier
            .width(breite)
            .background(farben.zeilenRand)
            .padding(vertical = 8.dp),
    ) {
        sichtbareZeilen.forEachIndexed { index, zeile ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(zeilenHöhe)
                    .background(if (index == aktuelleSichtbareZeile) farben.aktuelleZeile else Color.Transparent)
                    .clickable(enabled = zeile.faltung != null) { zeile.faltung?.let(onFaltungUmschalten) }
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = when {
                        zeile.faltung == null -> " "
                        zeile.eingeklappt -> "▶"
                        else -> "▼"
                    },
                    modifier = Modifier.width(14.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = farben.faltSymbol,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                    ),
                )
                Text(
                    text = zeile.originalZeile.toString(),
                    modifier = Modifier.weight(1f),
                    color = if (index == aktuelleSichtbareZeile) farben.aktuelleZeilennummer else farben.zeilennummer,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun JsonStatusLeiste(
    wert: TextFieldValue,
    analyse: JsonAnalyse,
    speicherFehler: String?,
) {
    val position = offsetZuZeileSpalte(wert.text, wert.selection.start)
    val fehler = speicherFehler ?: analyse.fehler?.meldung
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Zeile ${position.zeile}, Spalte ${position.spalte}", style = MaterialTheme.typography.labelSmall)
        Text("${analyse.zeilenAnzahl} Zeilen", style = MaterialTheme.typography.labelSmall)
        Text(
            text = fehler ?: "JSON-Struktur lesbar",
            modifier = Modifier.weight(1f),
            color = if (fehler == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
        )
        Text("Strg+S Übernehmen · Strg+Umschalt+F Formatieren", style = MaterialTheme.typography.labelSmall)
    }
}

@Immutable
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
    val aktuelleZeilennummer: Color,
    val faltSymbol: Color,
    val aktuelleZeile: Color,
    val fehlerZeile: Color,
)

@Composable
private fun jsonEditorFarben() = JsonEditorFarben(
    hintergrund = MaterialTheme.colorScheme.surface,
    zeilenRand = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f),
    standard = MaterialTheme.colorScheme.onSurface,
    schlüssel = MaterialTheme.colorScheme.primary,
    zeichenkette = MaterialTheme.colorScheme.tertiary,
    zahl = MaterialTheme.colorScheme.secondary,
    literal = MaterialTheme.colorScheme.primary.copy(alpha = .78f),
    struktur = MaterialTheme.colorScheme.onSurfaceVariant,
    zeilennummer = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .72f),
    aktuelleZeilennummer = MaterialTheme.colorScheme.primary,
    faltSymbol = MaterialTheme.colorScheme.onSurfaceVariant,
    aktuelleZeile = MaterialTheme.colorScheme.primary.copy(alpha = .07f),
    fehlerZeile = MaterialTheme.colorScheme.error.copy(alpha = .11f),
)

internal data class JsonAnalyse(
    val faltungen: List<JsonFaltung>,
    val fehler: JsonFehler?,
    val zeilenAnzahl: Int,
)

internal data class JsonFaltung(
    val startOffset: Int,
    val endeOffset: Int,
    val startZeile: Int,
    val endeZeile: Int,
    val tiefe: Int,
)

internal data class JsonFehler(
    val meldung: String,
    val offset: Int?,
    val zeile: Int?,
    val spalte: Int?,
)

internal data class JsonPosition(val zeile: Int, val spalte: Int)

internal data class JsonSichtbareZeile(
    val originalZeile: Int,
    val faltung: JsonFaltung?,
    val eingeklappt: Boolean,
)

internal fun analysiereJson(text: String): JsonAnalyse {
    val faltungen = analysiereJsonFaltungen(text)
    val fehler = runCatching { KartenJson.lese(text) }.exceptionOrNull()?.let { throwable ->
        jsonFehler(text, throwable.message ?: throwable::class.simpleName.orEmpty())
    }
    return JsonAnalyse(
        faltungen = faltungen,
        fehler = fehler,
        zeilenAnzahl = text.count { it == '\n' } + 1,
    )
}

internal fun analysiereJsonFaltungen(text: String): List<JsonFaltung> {
    data class Offen(val zeichen: Char, val offset: Int, val zeile: Int, val tiefe: Int)

    val stapel = mutableListOf<Offen>()
    val ergebnis = mutableListOf<JsonFaltung>()
    var inZeichenkette = false
    var maskiert = false
    var zeile = 1

    text.forEachIndexed { index, zeichen ->
        if (inZeichenkette) {
            when {
                maskiert -> maskiert = false
                zeichen == '\\' -> maskiert = true
                zeichen == '"' -> inZeichenkette = false
            }
        } else {
            when (zeichen) {
                '"' -> inZeichenkette = true
                '{', '[' -> stapel += Offen(zeichen, index, zeile, stapel.size)
                '}', ']' -> {
                    val erwartet = if (zeichen == '}') '{' else '['
                    val offen = stapel.lastOrNull()
                    if (offen?.zeichen == erwartet) {
                        stapel.removeAt(stapel.lastIndex)
                        if (offen.zeile < zeile) {
                            ergebnis += JsonFaltung(
                                startOffset = offen.offset,
                                endeOffset = index,
                                startZeile = offen.zeile,
                                endeZeile = zeile,
                                tiefe = offen.tiefe,
                            )
                        }
                    }
                }
            }
        }
        if (zeichen == '\n') zeile++
    }
    return ergebnis.sortedBy { it.startOffset }
}

internal fun sichtbareJsonZeilen(
    zeilenAnzahl: Int,
    faltungen: List<JsonFaltung>,
    eingeklappt: Set<Int>,
): List<JsonSichtbareZeile> {
    val faltungNachStartzeile = faltungen
        .groupBy(JsonFaltung::startZeile)
        .mapValues { (_, werte) -> werte.maxByOrNull(JsonFaltung::endeZeile)!! }
    val ergebnis = mutableListOf<JsonSichtbareZeile>()
    var zeile = 1
    while (zeile <= zeilenAnzahl) {
        val faltung = faltungNachStartzeile[zeile]
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
    for (index in 0 until begrenzt) {
        if (text[index] == '\n') {
            zeile++
            letzterUmbruch = index
        }
    }
    return JsonPosition(zeile, begrenzt - letzterUmbruch)
}

private fun jsonFehler(text: String, meldung: String): JsonFehler {
    val vollständig = Regex("""at\s+(\d+)\s+\[character\s+(\d+)\s+line\s+(\d+)]""").find(meldung)
    if (vollständig != null) {
        val offset = vollständig.groupValues[1].toIntOrNull()?.coerceIn(0, text.length)
        return JsonFehler(
            meldung = "Ungültiges JSON: $meldung",
            offset = offset,
            spalte = vollständig.groupValues[2].toIntOrNull(),
            zeile = vollständig.groupValues[3].toIntOrNull(),
        )
    }
    val offset = Regex("""at\s+(\d+)""").find(meldung)?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(0, text.length)
    val position = offset?.let { offsetZuZeileSpalte(text, it) }
    return JsonFehler(
        meldung = "Ungültiges JSON: $meldung",
        offset = offset,
        zeile = position?.zeile,
        spalte = position?.spalte,
    )
}

private class JsonVisualTransformation(
    private val faltungen: List<JsonFaltung>,
    private val farben: JsonEditorFarben,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val eingeklappt = transformiereFaltungen(text.text, faltungen)
        return TransformedText(
            text = jsonHervorheben(eingeklappt.text, farben),
            offsetMapping = eingeklappt.offsetMapping,
        )
    }
}

private data class FaltungsTransformation(
    val text: String,
    val offsetMapping: OffsetMapping,
)

private fun transformiereFaltungen(text: String, faltungen: List<JsonFaltung>): FaltungsTransformation {
    if (faltungen.isEmpty()) return FaltungsTransformation(text, OffsetMapping.Identity)
    val gültigeFaltungen = buildList {
        var verdecktBis = -1
        faltungen.sortedBy(JsonFaltung::startOffset).forEach { faltung ->
            if (faltung.startOffset > verdecktBis) {
                add(faltung)
                verdecktBis = faltung.endeOffset
            }
        }
    }
    val originalZuTransformiert = IntArray(text.length + 1)
    val transformiertZuOriginal = mutableListOf(0)
    val ausgabe = StringBuilder()
    var original = 0

    fun originalZeichenAnhängen(index: Int) {
        originalZuTransformiert[index] = ausgabe.length
        ausgabe.append(text[index])
        originalZuTransformiert[index + 1] = ausgabe.length
        transformiertZuOriginal += index + 1
    }

    gültigeFaltungen.forEach { faltung ->
        val ersetzungsStart = (faltung.startOffset + 1).coerceAtMost(text.length)
        val ersetzungsEnde = faltung.endeOffset.coerceIn(ersetzungsStart, text.length)
        while (original < ersetzungsStart) originalZeichenAnhängen(original++)

        val transformierterStart = ausgabe.length
        val verborgeneZeilen = faltung.endeZeile - faltung.startZeile
        val platzhalter = " … $verborgeneZeilen Zeilen "
        repeat(platzhalter.length) {
            ausgabe.append(platzhalter[it])
            transformiertZuOriginal += ersetzungsStart
        }
        for (offset in ersetzungsStart until ersetzungsEnde) originalZuTransformiert[offset] = transformierterStart
        original = ersetzungsEnde
        originalZuTransformiert[original] = ausgabe.length
        transformiertZuOriginal[transformiertZuOriginal.lastIndex] = original
    }
    while (original < text.length) originalZeichenAnhängen(original++)
    originalZuTransformiert[text.length] = ausgabe.length

    val transformiertZuOriginalArray = transformiertZuOriginal.toIntArray()
    return FaltungsTransformation(
        text = ausgabe.toString(),
        offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                originalZuTransformiert[offset.coerceIn(0, originalZuTransformiert.lastIndex)]

            override fun transformedToOriginal(offset: Int): Int =
                transformiertZuOriginalArray[offset.coerceIn(0, transformiertZuOriginalArray.lastIndex)]
        },
    )
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
        builder.addStyle(
            SpanStyle(
                color = farbe,
                fontWeight = if (token.art == JsonTokenArt.Schlüssel) FontWeight.SemiBold else FontWeight.Normal,
            ),
            token.start,
            token.ende,
        )
    }
    return builder.toAnnotatedString()
}

private fun jsonTokens(text: String): List<JsonToken> {
    val tokens = mutableListOf<JsonToken>()
    var index = 0
    while (index < text.length) {
        when (val zeichen = text[index]) {
            '"' -> {
                val start = index++
                var maskiert = false
                while (index < text.length) {
                    val aktuell = text[index++]
                    if (maskiert) {
                        maskiert = false
                    } else if (aktuell == '\\') {
                        maskiert = true
                    } else if (aktuell == '"') {
                        break
                    }
                }
                val danach = text.indexOfFirstAb(index) { !it.isWhitespace() }
                val art = if (danach < text.length && text[danach] == ':') JsonTokenArt.Schlüssel else JsonTokenArt.Zeichenkette
                tokens += JsonToken(start, index, art)
            }
            '-', in '0'..'9' -> {
                val start = index++
                while (index < text.length && (text[index].isDigit() || text[index] in charArrayOf('.', 'e', 'E', '+', '-'))) index++
                tokens += JsonToken(start, index, JsonTokenArt.Zahl)
            }
            '{', '}', '[', ']', ':', ',' -> {
                tokens += JsonToken(index, index + 1, JsonTokenArt.Struktur)
                index++
            }
            else -> {
                val literal = listOf("true", "false", "null").firstOrNull { text.startsWith(it, index) }
                if (literal != null) {
                    tokens += JsonToken(index, index + literal.length, JsonTokenArt.Literal)
                    index += literal.length
                } else {
                    index++
                }
            }
        }
    }
    return tokens
}

private inline fun String.indexOfFirstAb(start: Int, prädikat: (Char) -> Boolean): Int {
    for (index in start until length) if (prädikat(this[index])) return index
    return length
}
