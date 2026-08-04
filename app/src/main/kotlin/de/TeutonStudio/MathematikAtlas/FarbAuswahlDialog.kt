package de.TeutonStudio.MathematikAtlas

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
                    Text(
                        entwurf.kanonisch.rgbHex,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
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
                    FarbModusAuswahl(
                        modus = entwurf.modus,
                        ändern = { entwurf = entwurf.mitModus(it) },
                    )
                    SättigungHelligkeitFeld(
                        entwurf = entwurf,
                        ändern = { entwurf = it },
                    )
                    FarbtonRegler(
                        entwurf = entwurf,
                        ändern = { entwurf = it },
                    )
                    when (entwurf.modus) {
                        FarbEingabeModus.RGB -> RgbEingabe(
                            entwurf = entwurf,
                            ändern = { entwurf = it },
                        )
                        FarbEingabeModus.HSB -> HsbEingabe(
                            entwurf = entwurf,
                            ändern = { entwurf = it },
                        )
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

                HorizontalDivider()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { entwurf = entwurf.zuruecksetzen(standardFarbe) },
                    ) { Text("Zurücksetzen") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onAbbrechen) { Text("Abbrechen") }
                    Button(
                        onClick = { onBestaetigen(entwurf.kanonisch) },
                        enabled = entwurf.istGueltig,
                    ) { Text("Übernehmen") }
                }
            }
        }
    }
}

@Composable
private fun FarbVorschau(farbe: RgbFarbe) {
    val composeFarbe = Color(farbe.argbLong)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .semantics { contentDescription = "Farbvorschau ${farbe.rgbHex}" },
        color = composeFarbe,
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {}
}

@Composable
private fun FarbModusAuswahl(
    modus: FarbEingabeModus,
    ändern: (FarbEingabeModus) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FarbModusKnopf(
            titel = "RGB",
            ausgewählt = modus == FarbEingabeModus.RGB,
            modifier = Modifier.weight(1f),
        ) { ändern(FarbEingabeModus.RGB) }
        FarbModusKnopf(
            titel = "HSB",
            ausgewählt = modus == FarbEingabeModus.HSB,
            modifier = Modifier.weight(1f),
        ) { ändern(FarbEingabeModus.HSB) }
    }
}

@Composable
private fun FarbModusKnopf(
    titel: String,
    ausgewählt: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    if (ausgewählt) {
        Button(onClick = onClick, modifier = modifier) { Text(titel) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(titel) }
    }
}

@Composable
private fun SättigungHelligkeitFeld(
    entwurf: FarbEntwurf,
    ändern: (FarbEntwurf) -> Unit,
) {
    val farbtonFarbe = Color(
        entwurf.hsb.copy(saettigung = 1f, helligkeit = 1f).zuRgb().argbLong,
    )
    fun aktualisiere(position: Offset, breite: Float, höhe: Float) {
        ändern(
            entwurf.mitHsb(
                saettigung = (position.x / breite).coerceIn(0f, 1f),
                helligkeit = (1f - position.y / höhe).coerceIn(0f, 1f),
            ),
        )
    }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .semantics { contentDescription = "Sättigung und Helligkeit" }
            .pointerInput(entwurf.hsb.farbton) {
                detectTapGestures { aktualisiere(it, size.width, size.height) }
            }
            .pointerInput(entwurf.hsb.farbton) {
                detectDragGestures(
                    onDragStart = { aktualisiere(it, size.width, size.height) },
                    onDrag = { änderung, _ ->
                        änderung.consume()
                        aktualisiere(änderung.position, size.width, size.height)
                    },
                )
            },
    ) {
        drawRect(Brush.horizontalGradient(listOf(Color.White, farbtonFarbe)))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        val marker = Offset(
            x = entwurf.hsb.saettigung * size.width,
            y = (1f - entwurf.hsb.helligkeit) * size.height,
        )
        drawCircle(Color.White, radius = 11.dp.toPx(), center = marker)
        drawCircle(
            Color.Black,
            radius = 8.dp.toPx(),
            center = marker,
            style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()),
        )
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
                        listOf(
                            Color.Red,
                            Color.Yellow,
                            Color.Green,
                            Color.Cyan,
                            Color.Blue,
                            Color.Magenta,
                            Color.Red,
                        ),
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
private fun RgbEingabe(
    entwurf: FarbEntwurf,
    ändern: (FarbEntwurf) -> Unit,
) {
    val kanäle = listOf(
        Triple("Rot", entwurf.rotText, { wert: String -> entwurf.mitRgb(rot = wert) }),
        Triple("Grün", entwurf.gruenText, { wert: String -> entwurf.mitRgb(gruen = wert) }),
        Triple("Blau", entwurf.blauText, { wert: String -> entwurf.mitRgb(blau = wert) }),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        kanäle.forEach { (titel, text, setzeText) ->
            val wert = text.toIntOrNull()?.coerceIn(0, 255) ?: 0
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { ändern(setzeText(it.filter(Char::isDigit).take(3))) },
                    label = { Text(titel) },
                    singleLine = true,
                    modifier = Modifier.width(110.dp),
                )
                Slider(
                    value = wert.toFloat(),
                    onValueChange = { ändern(setzeText(it.roundToInt().toString())) },
                    valueRange = 0f..255f,
                    steps = 254,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "$titel 0 bis 255" },
                )
            }
        }
    }
}

@Composable
private fun HsbEingabe(
    entwurf: FarbEntwurf,
    ändern: (FarbEntwurf) -> Unit,
) {
    HsbKanal(
        titel = "Farbton",
        einheit = "°",
        text = entwurf.farbtonText,
        sliderWert = entwurf.hsb.farbton,
        bereich = 0f..360f,
        onText = { ändern(entwurf.mitHsbText(farbton = it)) },
        onSlider = { ändern(entwurf.mitHsb(farbton = it)) },
    )
    HsbKanal(
        titel = "Sättigung",
        einheit = "%",
        text = entwurf.saettigungText,
        sliderWert = entwurf.hsb.saettigung * 100f,
        bereich = 0f..100f,
        onText = { ändern(entwurf.mitHsbText(saettigung = it)) },
        onSlider = { ändern(entwurf.mitHsb(saettigung = it / 100f)) },
    )
    HsbKanal(
        titel = "Helligkeit",
        einheit = "%",
        text = entwurf.helligkeitText,
        sliderWert = entwurf.hsb.helligkeit * 100f,
        bereich = 0f..100f,
        onText = { ändern(entwurf.mitHsbText(helligkeit = it)) },
        onSlider = { ändern(entwurf.mitHsb(helligkeit = it / 100f)) },
    )
}

@Composable
private fun HsbKanal(
    titel: String,
    einheit: String,
    text: String,
    sliderWert: Float,
    bereich: ClosedFloatingPointRange<Float>,
    onText: (String) -> Unit,
    onSlider: (Float) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text,
            onValueChange = { eingabe ->
                onText(eingabe.filter { it.isDigit() || it == '.' }.take(6))
            },
            label = { Text("$titel $einheit") },
            singleLine = true,
            modifier = Modifier.width(130.dp),
        )
        Slider(
            value = sliderWert.coerceIn(bereich.start, bereich.endInclusive),
            onValueChange = onSlider,
            valueRange = bereich,
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = "$titel $einheit" },
        )
    }
}
