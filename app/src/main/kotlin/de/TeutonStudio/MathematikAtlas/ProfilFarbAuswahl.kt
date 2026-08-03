package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.TeutonStudio.MathematikAtlas.speicher.*

@Composable
internal fun ProfilFarbAuswahl(
    startFarbe: ProfilFarbe,
    farbeGeaendert: (ProfilFarbe) -> Unit,
    modifier: Modifier = Modifier,
) {
    var entwurf by remember(startFarbe) { mutableStateOf(ProfilFarbEntwurf.von(startFarbe)) }

    fun setze(neu: ProfilFarbEntwurf) {
        entwurf = neu
        if (neu.istGueltig) farbeGeaendert(neu.letzteGueltigeFarbe)
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Exakter Farbwähler", style = MaterialTheme.typography.titleMedium)
        Text(
            "Farbton, Sättigung und Helligkeit sowie Hex und RGB bleiben synchron. Ungültige Texteingaben verändern die Vorschau nicht.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val farbtonFarbe = Color(entwurf.hsv.copy(saettigung = 1f, helligkeit = 1f).zuProfilFarbe().argbLong)
        Canvas(
            modifier = Modifier.fillMaxWidth().height(220.dp)
                .semantics { contentDescription = "Sättigung und Helligkeit der Profilfarbe" }
                .pointerInput(entwurf.hsv.farbton) {
                    detectTapGestures { position ->
                        setze(
                            entwurf.mitHsv(
                                saettigung = position.x / size.width,
                                helligkeit = 1f - position.y / size.height,
                            ),
                        )
                    }
                }
                .pointerInput(entwurf.hsv.farbton) {
                    detectDragGestures(
                        onDragStart = { position ->
                            setze(
                                entwurf.mitHsv(
                                    saettigung = position.x / size.width,
                                    helligkeit = 1f - position.y / size.height,
                                ),
                            )
                        },
                        onDrag = { aenderung, _ ->
                            aenderung.consume()
                            setze(
                                entwurf.mitHsv(
                                    saettigung = aenderung.position.x / size.width,
                                    helligkeit = 1f - aenderung.position.y / size.height,
                                ),
                            )
                        },
                    )
                },
        ) {
            drawRect(Brush.horizontalGradient(listOf(Color.White, farbtonFarbe)))
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
            val marker = Offset(
                x = entwurf.hsv.saettigung * size.width,
                y = (1f - entwurf.hsv.helligkeit) * size.height,
            )
            drawCircle(Color.White, radius = 10.dp.toPx(), center = marker)
            drawCircle(
                Color.Black,
                radius = 7.dp.toPx(),
                center = marker,
                style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()),
            )
        }

        Text("Farbton: ${entwurf.hsv.farbton.toInt()}°", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = entwurf.hsv.farbton,
            onValueChange = { setze(entwurf.mitHsv(farbton = it)) },
            valueRange = 0f..360f,
            modifier = Modifier.semantics { contentDescription = "Farbton der Profilfarbe" },
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = entwurf.hexText,
                onValueChange = { setze(entwurf.mitHex(it)) },
                label = { Text("Hex") },
                isError = entwurf.fehler != null,
                singleLine = true,
                modifier = Modifier.weight(1.4f),
            )
            RgbFeld("R", entwurf.rotText, Modifier.weight(1f)) { setze(entwurf.mitRgb(rot = it)) }
            RgbFeld("G", entwurf.gruenText, Modifier.weight(1f)) { setze(entwurf.mitRgb(gruen = it)) }
            RgbFeld("B", entwurf.blauText, Modifier.weight(1f)) { setze(entwurf.mitRgb(blau = it)) }
        }
        entwurf.fehler?.let { fehler ->
            Text(fehler, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        val dunkel = LocalDarstellungsSteuerung.current.modus.istDunkel(isSystemInDarkTheme())
        val schema = remember(entwurf.letzteGueltigeFarbe, dunkel) {
            ProfilFarbschemaGenerator.erzeuge(entwurf.letzteGueltigeFarbe, dunkel)
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = schema.surface,
            contentColor = schema.onSurface,
            shape = MaterialTheme.shapes.large,
            border = androidx.compose.foundation.BorderStroke(1.dp, schema.outline),
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Live-Vorschau ${entwurf.letzteGueltigeFarbe.rgbHex}", style = MaterialTheme.typography.titleMedium)
                Surface(
                    color = schema.surfaceVariant,
                    contentColor = schema.onSurfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text("Profilfarbige Fläche mit kontrastsicherem Text", Modifier.padding(12.dp))
                }
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(
                        containerColor = schema.primary,
                        contentColor = schema.onPrimary,
                    ),
                ) { Text("Primäre Aktion") }
            }
        }

        OutlinedButton(onClick = { setze(entwurf.zuruecksetzen()) }) {
            Text("Auf Standardfarbe zurücksetzen")
        }
    }
}

@Composable
private fun RgbFeld(
    label: String,
    wert: String,
    modifier: Modifier,
    aendern: (String) -> Unit,
) {
    OutlinedTextField(
        value = wert,
        onValueChange = { aendern(it.filter(Char::isDigit).take(3)) },
        label = { Text(label) },
        singleLine = true,
        modifier = modifier,
    )
}
