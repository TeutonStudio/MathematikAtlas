package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.TeutonStudio.MathematikAtlas.speicher.*
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Wiederverwendbare Farbauswahl ohne Profil- oder Persistenzwissen.
 * Die einzige Ausgabe ist deckendes sRGB; Änderungen bleiben bis [onBestaetigen] lokal.
 */
@Composable
internal fun FarbAuswahlDialog(
    offen: Boolean,
    ausgangsFarbe: RgbFarbe,
    standardFarbe: RgbFarbe,
    titel: String,
    onAbbrechen: () -> Unit,
    onBestaetigen: (RgbFarbe) -> Unit,
    startModus: FarbEingabeModus = FarbEingabeModus.RGB,
    initialerEntwurf: FarbEntwurf? = null,
) {
    if (!offen) return
    var entwurf by remember(ausgangsFarbe, offen, startModus, initialerEntwurf) {
        mutableStateOf(initialerEntwurf ?: FarbEntwurf.von(ausgangsFarbe, startModus))
    }

    Dialog(
        onDismissRequest = onAbbrechen,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(.92f)
                .fillMaxHeight(.9f)
                .widthIn(max = 760.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 8.dp,
        ) {
            Column(Modifier.fillMaxSize()) {
                DialogKopf(titel, entwurf.kanonisch)
                HorizontalDivider()
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    FarbVorschau(entwurf.kanonisch)
                    FarbModusAuswahl(entwurf.modus) { entwurf = entwurf.mitModus(it) }
                    Text(
                        "Allgemeine Farbauswahl (HSB)",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    SättigungHelligkeitFeld(entwurf) { entwurf = it }
                    FarbtonRegler(entwurf) { entwurf = it }
                    FarbModusEingabe(entwurf) { entwurf = it }
                    OutlinedTextField(
                        value = entwurf.hexText,
                        onValueChange = { entwurf = entwurf.mitHex(it) },
                        label = { Text("Hex #RRGGBB") },
                        singleLine = true,
                        isError = entwurf.fehler == FarbEingabeFehler.HexUngueltig,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    entwurf.fehlerText?.let { fehler ->
                        Text(
                            fehler,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.semantics { contentDescription = "Fehler: $fehler" },
                        )
                    }
                }
                DialogFuss(
                    gültig = entwurf.istGueltig,
                    zurücksetzen = { entwurf = entwurf.zuruecksetzen(standardFarbe) },
                    abbrechen = onAbbrechen,
                    übernehmen = { onBestaetigen(entwurf.kanonisch) },
                )
            }
        }
    }
}

@Composable
private fun DialogKopf(titel: String, farbe: RgbFarbe) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(titel, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        Text(farbe.rgbHex, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun DialogFuss(
    gültig: Boolean,
    zurücksetzen: () -> Unit,
    abbrechen: () -> Unit,
    übernehmen: () -> Unit,
) {
    HorizontalDivider()
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(onClick = zurücksetzen) { Text("Zurücksetzen") }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = abbrechen) { Text("Abbrechen") }
        Button(onClick = übernehmen, enabled = gültig) { Text("Übernehmen") }
    }
}

@Composable
private fun FarbVorschau(farbe: RgbFarbe) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .semantics { contentDescription = "Farbvorschau ${farbe.rgbHex}" },
        color = Color(farbe.argbLong),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {}
}

@Composable
private fun FarbModusAuswahl(
    modus: FarbEingabeModus,
    ändern: (FarbEingabeModus) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FarbEingabeModus.entries.forEach { eintrag ->
            val titel = when (eintrag) {
                FarbEingabeModus.RGB -> "RGB"
                FarbEingabeModus.HSB -> "HSB"
                FarbEingabeModus.HSL -> "HSL"
                FarbEingabeModus.LAB -> "Lab"
                FarbEingabeModus.CMYK -> "CMYK"
            }
            FarbModusKnopf(titel, modus == eintrag) { ändern(eintrag) }
        }
    }
}

@Composable
private fun FarbModusKnopf(
    titel: String,
    ausgewählt: Boolean,
    onClick: () -> Unit,
) {
    val modifier = Modifier
        .widthIn(min = 96.dp)
        .semantics { contentDescription = "$titel-Farbmodus${if (ausgewählt) ", ausgewählt" else ""}" }
    if (ausgewählt) Button(onClick, modifier) { Text(titel) }
    else OutlinedButton(onClick, modifier) { Text(titel) }
}

@Composable
private fun SättigungHelligkeitFeld(
    entwurf: FarbEntwurf,
    ändern: (FarbEntwurf) -> Unit,
) {
    val farbtonFarbe = Color(entwurf.hsb.copy(saettigung = 1f, helligkeit = 1f).zuRgb().argbLong)
    fun aktualisiere(position: Offset, breite: Float, höhe: Float) {
        ändern(
            entwurf.mitHsb(
                saettigung = (position.x / breite.coerceAtLeast(1f)).coerceIn(0f, 1f),
                helligkeit = (1f - position.y / höhe.coerceAtLeast(1f)).coerceIn(0f, 1f),
            ),
        )
    }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(220.dp)
            .semantics { contentDescription = "Allgemeine Sättigungs- und Helligkeitsauswahl auf HSB-Basis" }
            .pointerInput(entwurf.hsb.farbton) {
                detectTapGestures { punkt -> aktualisiere(punkt, size.width.toFloat(), size.height.toFloat()) }
            }
            .pointerInput(entwurf.hsb.farbton) {
                detectDragGestures(
                    onDragStart = { punkt -> aktualisiere(punkt, size.width.toFloat(), size.height.toFloat()) },
                    onDrag = { änderung, _ ->
                        änderung.consume()
                        aktualisiere(änderung.position, size.width.toFloat(), size.height.toFloat())
                    },
                )
            },
    ) {
        drawRect(Brush.horizontalGradient(listOf(Color.White, farbtonFarbe)))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        val marker = Offset(
            entwurf.hsb.saettigung * size.width,
            (1f - entwurf.hsb.helligkeit) * size.height,
        )
        drawCircle(Color.White, 11.dp.toPx(), marker)
        drawCircle(Color.Black, 8.dp.toPx(), marker, style = Stroke(2.dp.toPx()))
    }
}

@Composable
private fun FarbtonRegler(
    entwurf: FarbEntwurf,
    ändern: (FarbEntwurf) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Farbton: ${entwurf.hsb.farbton.roundToInt()}°", style = MaterialTheme.typography.labelLarge)
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red),
                    ),
                    CircleShape,
                ),
        )
        Slider(
            value = entwurf.hsb.farbton,
            onValueChange = { ändern(entwurf.mitHsb(farbton = it)) },
            valueRange = 0f..360f,
            modifier = Modifier.semantics { contentDescription = "Allgemeiner Farbton in Grad" },
        )
    }
}

@Composable
private fun FarbModusEingabe(entwurf: FarbEntwurf, ändern: (FarbEntwurf) -> Unit) {
    val istFehler = entwurf.fehler?.modus == entwurf.modus
    when (entwurf.modus) {
        FarbEingabeModus.RGB -> {
            FarbKanalEingabe("Rot", "", entwurf.texte.rgb.rot, entwurf.kanonisch.rot.toFloat() * 255f, 0f..255f, true, false, istFehler,
                { ändern(entwurf.mitRgb(rot = it)) }, { ändern(entwurf.mitRgb(rot = it.roundToInt().toString())) })
            FarbKanalEingabe("Grün", "", entwurf.texte.rgb.gruen, entwurf.kanonisch.gruen.toFloat() * 255f, 0f..255f, true, false, istFehler,
                { ändern(entwurf.mitRgb(gruen = it)) }, { ändern(entwurf.mitRgb(gruen = it.roundToInt().toString())) })
            FarbKanalEingabe("Blau", "", entwurf.texte.rgb.blau, entwurf.kanonisch.blau.toFloat() * 255f, 0f..255f, true, false, istFehler,
                { ändern(entwurf.mitRgb(blau = it)) }, { ändern(entwurf.mitRgb(blau = it.roundToInt().toString())) })
        }
        FarbEingabeModus.HSB -> {
            ModusHinweis("HSB entspricht HSV; Helligkeit ist der Value-Kanal.")
            FarbKanalEingabe("Farbton", "°", entwurf.texte.hsb.farbton, entwurf.hsb.farbton, 0f..360f, false, false, istFehler,
                { ändern(entwurf.mitHsbText(farbton = it)) }, { ändern(entwurf.mitHsb(farbton = it)) })
            FarbKanalEingabe("Sättigung", "%", entwurf.texte.hsb.saettigung, entwurf.hsb.saettigung * 100f, 0f..100f, false, false, istFehler,
                { ändern(entwurf.mitHsbText(saettigung = it)) }, { ändern(entwurf.mitHsb(saettigung = it / 100f)) })
            FarbKanalEingabe("Helligkeit", "%", entwurf.texte.hsb.helligkeit, entwurf.hsb.helligkeit * 100f, 0f..100f, false, false, istFehler,
                { ändern(entwurf.mitHsbText(helligkeit = it)) }, { ändern(entwurf.mitHsb(helligkeit = it / 100f)) })
        }
        FarbEingabeModus.HSL -> {
            ModusHinweis("HSL-Helligkeit ist der Mittelwert aus größtem und kleinstem RGB-Kanal und unterscheidet sich von HSB.")
            FarbKanalEingabe("Farbton", "°", entwurf.texte.hsl.farbton, entwurf.hsl.farbton.toFloat(), 0f..360f, false, false, istFehler,
                { ändern(entwurf.mitHslText(farbton = it)) }, { ändern(entwurf.mitHslText(farbton = formatiereSlider(it))) })
            FarbKanalEingabe("Sättigung", "%", entwurf.texte.hsl.saettigung, (entwurf.hsl.saettigung * 100.0).toFloat(), 0f..100f, false, false, istFehler,
                { ändern(entwurf.mitHslText(saettigung = it)) }, { ändern(entwurf.mitHslText(saettigung = formatiereSlider(it))) })
            FarbKanalEingabe("Helligkeit", "%", entwurf.texte.hsl.helligkeit, (entwurf.hsl.helligkeit * 100.0).toFloat(), 0f..100f, false, false, istFehler,
                { ändern(entwurf.mitHslText(helligkeit = it)) }, { ändern(entwurf.mitHslText(helligkeit = formatiereSlider(it))) })
        }
        FarbEingabeModus.LAB -> {
            ModusHinweis("CIE Lab, Weißpunkt D50; Ausgabe auf sRGB begrenzt.")
            FarbKanalEingabe("L*", "", entwurf.texte.lab.helligkeit, entwurf.lab.helligkeit.toFloat(), 0f..100f, false, false, istFehler,
                { ändern(entwurf.mitLabText(helligkeit = it)) }, { ändern(entwurf.mitLabText(helligkeit = formatiereSlider(it))) })
            FarbKanalEingabe("a*", "", entwurf.texte.lab.a, entwurf.lab.a.toFloat(), -128f..127f, false, true, istFehler,
                { ändern(entwurf.mitLabText(a = it)) }, { ändern(entwurf.mitLabText(a = formatiereSlider(it))) })
            FarbKanalEingabe("b*", "", entwurf.texte.lab.b, entwurf.lab.b.toFloat(), -128f..127f, false, true, istFehler,
                { ändern(entwurf.mitLabText(b = it)) }, { ändern(entwurf.mitLabText(b = formatiereSlider(it))) })
        }
        FarbEingabeModus.CMYK -> {
            ModusHinweis("Generisches CMYK dient der rechnerischen Eingabe. Druckergebnisse hängen von Papier, Tinte, Druckverfahren und ICC-Profil ab.")
            FarbKanalEingabe("Cyan", "%", entwurf.texte.cmyk.cyan, (entwurf.cmyk.cyan * 100.0).toFloat(), 0f..100f, false, false, istFehler,
                { ändern(entwurf.mitCmykText(cyan = it)) }, { ändern(entwurf.mitCmykText(cyan = formatiereSlider(it))) })
            FarbKanalEingabe("Magenta", "%", entwurf.texte.cmyk.magenta, (entwurf.cmyk.magenta * 100.0).toFloat(), 0f..100f, false, false, istFehler,
                { ändern(entwurf.mitCmykText(magenta = it)) }, { ändern(entwurf.mitCmykText(magenta = formatiereSlider(it))) })
            FarbKanalEingabe("Gelb", "%", entwurf.texte.cmyk.gelb, (entwurf.cmyk.gelb * 100.0).toFloat(), 0f..100f, false, false, istFehler,
                { ändern(entwurf.mitCmykText(gelb = it)) }, { ändern(entwurf.mitCmykText(gelb = formatiereSlider(it))) })
            FarbKanalEingabe("Schwarz", "%", entwurf.texte.cmyk.schwarz, (entwurf.cmyk.schwarz * 100.0).toFloat(), 0f..100f, false, false, istFehler,
                { ändern(entwurf.mitCmykText(schwarz = it)) }, { ändern(entwurf.mitCmykText(schwarz = formatiereSlider(it))) })
        }
    }
}

@Composable
private fun ModusHinweis(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { contentDescription = text },
    )
}

@Composable
private fun FarbKanalEingabe(
    titel: String,
    einheit: String,
    text: String,
    sliderWert: Float,
    bereich: ClosedFloatingPointRange<Float>,
    ganzzahlig: Boolean,
    vorzeichen: Boolean,
    istFehler: Boolean,
    textÄndern: (String) -> Unit,
    sliderÄndern: (Float) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { textÄndern(bereinigeKanalEingabe(it, ganzzahlig, vorzeichen)) },
            label = { Text(listOf(titel, einheit).filter(String::isNotBlank).joinToString(" ")) },
            singleLine = true,
            isError = istFehler,
            modifier = Modifier
                .width(142.dp)
                .semantics { contentDescription = "$titel $einheit, Bereich ${bereich.start} bis ${bereich.endInclusive}, aktueller Wert $text" },
        )
        Slider(
            value = sliderWert.coerceIn(bereich.start, bereich.endInclusive),
            onValueChange = sliderÄndern,
            valueRange = bereich,
            steps = if (ganzzahlig) (bereich.endInclusive - bereich.start).roundToInt() - 1 else 0,
            modifier = Modifier.weight(1f).semantics { contentDescription = "$titel $einheit" },
        )
    }
}

private fun bereinigeKanalEingabe(text: String, ganzzahlig: Boolean, vorzeichen: Boolean): String {
    if (ganzzahlig) return text.filter(Char::isDigit).take(3)
    val ergebnis = StringBuilder()
    var trennzeichenVorhanden = false
    text.forEachIndexed { index, zeichen ->
        when {
            zeichen.isDigit() -> ergebnis.append(zeichen)
            vorzeichen && zeichen == '-' && index == 0 && ergebnis.isEmpty() -> ergebnis.append(zeichen)
            (zeichen == '.' || zeichen == ',') && !trennzeichenVorhanden -> {
                ergebnis.append(zeichen)
                trennzeichenVorhanden = true
            }
        }
    }
    return ergebnis.toString().take(10)
}

private fun formatiereSlider(wert: Float): String {
    val gerundet = wert.roundToInt()
    if (abs(wert - gerundet) < .005f) return gerundet.toString()
    return String.format(Locale.ROOT, "%.1f", wert)
}
