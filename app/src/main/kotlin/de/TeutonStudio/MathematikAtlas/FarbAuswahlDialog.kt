package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import kotlin.math.roundToInt

/**
 * Wiederverwendbare Farbauswahl ohne Profil- oder Persistenzwissen.
 * Änderungen bleiben bis [onBestaetigen] ausschließlich im lokalen Dialogentwurf.
 */
@Composable
internal fun FarbAuswahlDialog(
    offen: Boolean,
    ausgangsFarbe: RgbFarbe,
    standardFarbe: RgbFarbe,
    titel: String,
    onAbbrechen: () -> Unit,
    onBestaetigen: (RgbFarbe) -> Unit,
) {
    if (!offen) return
    var entwurf by remember(ausgangsFarbe, offen) {
        mutableStateOf(FarbEntwurf.von(ausgangsFarbe))
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
                    SättigungHelligkeitFeld(entwurf) { entwurf = it }
                    FarbtonRegler(entwurf) { entwurf = it }
                    when (entwurf.modus) {
                        FarbEingabeModus.RGB -> RgbEingabe(entwurf) { entwurf = it }
                        FarbEingabeModus.HSB -> HsbEingabe(entwurf) { entwurf = it }
                    }
                    OutlinedTextField(
                        value = entwurf.hexText,
                        onValueChange = { entwurf = entwurf.mitHex(it) },
                        label = { Text("Hex #RRGGBB") },
                        singleLine = true,
                        isError = entwurf.fehler != null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    entwurf.fehler?.let { fehler ->
                        Text(
                            fehler,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
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
        Column(Modifier.weight(1f)) {
            Text(titel, style = MaterialTheme.typography.headlineSmall)
            Text(
                "HSB verwendet dieselbe Semantik wie HSV: Helligkeit entspricht dem Value-Kanal.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FarbModusKnopf("RGB", modus == FarbEingabeModus.RGB, Modifier.weight(1f)) {
            ändern(FarbEingabeModus.RGB)
        }
        FarbModusKnopf("HSB", modus == FarbEingabeModus.HSB, Modifier.weight(1f)) {
            ändern(FarbEingabeModus.HSB)
        }
    }
}

@Composable
private fun FarbModusKnopf(
    titel: String,
    ausgewählt: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
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
            .semantics { contentDescription = "Sättigung und Helligkeit" }
            .pointerInput(entwurf.hsb.farbton) {
                detectTapGestures { punkt ->
                    aktualisiere(punkt, size.width.toFloat(), size.height.toFloat())
                }
            }
            .pointerInput(entwurf.hsb.farbton) {
                detectDragGestures(
                    onDragStart = { punkt ->
                        aktualisiere(punkt, size.width.toFloat(), size.height.toFloat())
                    },
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
            modifier = Modifier.semantics { contentDescription = "Farbton in Grad" },
        )
    }
}

@Composable
private fun RgbEingabe(entwurf: FarbEntwurf, ändern: (FarbEntwurf) -> Unit) {
    RgbKanal("Rot", entwurf.rotText, { entwurf.mitRgb(rot = it) }, ändern)
    RgbKanal("Grün", entwurf.gruenText, { entwurf.mitRgb(gruen = it) }, ändern)
    RgbKanal("Blau", entwurf.blauText, { entwurf.mitRgb(blau = it) }, ändern)
}

@Composable
private fun RgbKanal(
    titel: String,
    text: String,
    aktualisiere: (String) -> FarbEntwurf,
    ändern: (FarbEntwurf) -> Unit,
) {
    val wert = text.toIntOrNull()?.coerceIn(0, 255) ?: 0
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text,
            onValueChange = { ändern(aktualisiere(it.filter(Char::isDigit).take(3))) },
            label = { Text(titel) },
            singleLine = true,
            modifier = Modifier.width(110.dp),
        )
        Slider(
            value = wert.toFloat(),
            onValueChange = { ändern(aktualisiere(it.roundToInt().toString())) },
            valueRange = 0f..255f,
            steps = 254,
            modifier = Modifier.weight(1f).semantics { contentDescription = "$titel 0 bis 255" },
        )
    }
}

@Composable
private fun HsbEingabe(entwurf: FarbEntwurf, ändern: (FarbEntwurf) -> Unit) {
    HsbKanal(
        "Farbton", "°", entwurf.farbtonText, entwurf.hsb.farbton, 0f..360f,
        { ändern(entwurf.mitHsbText(farbton = it)) },
        { ändern(entwurf.mitHsb(farbton = it)) },
    )
    HsbKanal(
        "Sättigung", "%", entwurf.saettigungText, entwurf.hsb.saettigung * 100f, 0f..100f,
        { ändern(entwurf.mitHsbText(saettigung = it)) },
        { ändern(entwurf.mitHsb(saettigung = it / 100f)) },
    )
    HsbKanal(
        "Helligkeit", "%", entwurf.helligkeitText, entwurf.hsb.helligkeit * 100f, 0f..100f,
        { ändern(entwurf.mitHsbText(helligkeit = it)) },
        { ändern(entwurf.mitHsb(helligkeit = it / 100f)) },
    )
}

@Composable
private fun HsbKanal(
    titel: String,
    einheit: String,
    text: String,
    sliderWert: Float,
    bereich: ClosedFloatingPointRange<Float>,
    textÄndern: (String) -> Unit,
    sliderÄndern: (Float) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text,
            onValueChange = { eingabe -> textÄndern(eingabe.filter { it.isDigit() || it == '.' }.take(6)) },
            label = { Text("$titel $einheit") },
            singleLine = true,
            modifier = Modifier.width(130.dp),
        )
        Slider(
            value = sliderWert.coerceIn(bereich.start, bereich.endInclusive),
            onValueChange = sliderÄndern,
            valueRange = bereich,
            modifier = Modifier.weight(1f).semantics { contentDescription = "$titel $einheit" },
        )
    }
}
