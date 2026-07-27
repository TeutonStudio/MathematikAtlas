package de.TeutonStudio.MathematikKnoten.visualisierung.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenInteraktionsModus
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenRenderer
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenRendererAktionen
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.LatexText
import de.TeutonStudio.MathematikKnoten.visualisierung.modell.*
import de.TeutonStudio.MathematikKnoten.visualisierung.sampling.*
import de.TeutonStudio.MathematikRechenSystem.kern.MengenAusdruck
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.*

/** Compose-Renderer mit abbrechbarem Hintergrundsampling und lokaler Kamerasteuerung. */
class VisualisierungsKnotenRenderer(
    private val ergebnisFür: (KnotenDaten) -> KnotenAuswertungsErgebnis?,
) : KnotenRenderer {
    override val interaktionsModus = KnotenInteraktionsModus.NurKopfzeileZiehbar

    @Composable override fun Inhalt(knoten: KnotenDaten, ausgewählt: Boolean, aktionen: KnotenRendererAktionen) {
        val menge = ergebnisFür(knoten)?.eingänge?.get("menge")?.objekt as? MengenAusdruck
        var konfiguration by remember(knoten.id, knoten.eigenschaften) { mutableStateOf(VisualisierungsKonfiguration.aus(knoten.eigenschaften)) }
        fun ändern(neu: VisualisierungsKonfiguration) {
            konfiguration = neu
            aktionen.eigenschaftenErsetzen(neu.zuEigenschaften())
        }
        val ergebnis by produceState<VisualisierungsErgebnis?>(null, menge, konfiguration.samplingSignatur()) {
            value = if (menge == null) VisualisierungsErgebnis.NichtDarstellbar("Verbinde eine Menge mit dem Eingang.") else {
                delay(140)
                withContext(Dispatchers.Default) { VisualisierungsSampler.sample(menge, konfiguration) }
            }
        }
        Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth().height(34.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Visualisierung", style = MaterialTheme.typography.titleMedium)
                Text(if (konfiguration.dimension == RaumDimension.R2) "R²" else "R³", style = MaterialTheme.typography.titleMedium)
            }
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Plot(
                    ergebnis = ergebnis,
                    konfiguration = konfiguration,
                    onKamera = { ändern(konfiguration.copy(kamera = it)) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                Steuerung(konfiguration, ::ändern, Modifier.width(128.dp).fillMaxHeight())
            }
            LatexText(legende(konfiguration), style = MaterialTheme.typography.bodySmall)
            when (val wert = ergebnis) {
                is VisualisierungsErgebnis.NichtDarstellbar -> Text(wert.grund, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                is VisualisierungsErgebnis.Teilweise -> Text(wert.hinweise.joinToString(" "), style = MaterialTheme.typography.labelSmall)
                is VisualisierungsErgebnis.Erfolgreich -> if (wert.istApproximation) Text("Numerische Approximation", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                null -> Text("Berechne Darstellung …", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable private fun Plot(ergebnis: VisualisierungsErgebnis?, konfiguration: VisualisierungsKonfiguration, onKamera: (KameraZustand) -> Unit, modifier: Modifier) {
    val kamera = konfiguration.kamera
    val hintergrund = MaterialTheme.colorScheme.surfaceVariant
    val raster = MaterialTheme.colorScheme.outlineVariant
    val rahmen = MaterialTheme.colorScheme.outline
    val beschriftung = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier.pointerInput(konfiguration.dimension, kamera) {
        detectTransformGestures { _, pan, zoom, _ ->
            val neu = if (konfiguration.dimension == RaumDimension.R3) kamera.copy(rotationY = kamera.rotationY + pan.x * 0.5, rotationX = kamera.rotationX + pan.y * 0.5, zoom = (kamera.zoom * zoom).coerceIn(0.1, 20.0))
            else kamera.copy(translationX = kamera.translationX - pan.x / 20.0, translationY = kamera.translationY + pan.y / 20.0, zoom = (kamera.zoom * zoom).coerceIn(0.1, 20.0))
            onKamera(neu)
        }
    }) {
        drawRect(hintergrund)
        val center = Offset(size.width / 2f, size.height / 2f)
        drawLine(raster, Offset(0f, center.y), Offset(size.width, center.y), 1f)
        drawLine(raster, Offset(center.x, 0f), Offset(center.x, size.height), 1f)
        val punkte = when (ergebnis) { is VisualisierungsErgebnis.Erfolgreich -> ergebnis.punkte; is VisualisierungsErgebnis.Teilweise -> ergebnis.punkte; else -> emptyList() }
        punkte.forEach { punkt ->
            val projektion = projekt(punkt, konfiguration, size.width, size.height)
            drawCircle(farbeFür(punkt.farbwert, konfiguration), if (konfiguration.dimension == RaumDimension.R3) 2.8f else 2.2f, projektion)
        }
        zeichneAchsenBeschriftungen(konfiguration, beschriftung)
        drawRect(rahmen, style = Stroke(1f))
    }
}

private fun DrawScope.zeichneAchsenBeschriftungen(c: VisualisierungsKonfiguration, farbe: Color) {
    val abstand = 7.dp.toPx()
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = farbe.toArgb()
        textSize = 12.dp.toPx()
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val canvas = drawContext.canvas.nativeCanvas
    if (c.dimension == RaumDimension.R2) {
        val x = c.achsen.x.ifBlank { "x" }
        val y = c.achsen.y.ifBlank { "y" }
        canvas.drawText(x, size.width - paint.measureText(x) - abstand, size.height / 2f - abstand, paint)
        canvas.drawText(y, size.width / 2f + abstand, paint.textSize + abstand, paint)
    } else {
        listOf(
            "X: ${c.achsen.x.ifBlank { "x" }}",
            "Y: ${c.achsen.y.ifBlank { "y" }}",
            "Z: ${c.achsen.z?.ifBlank { "z" } ?: "z"}",
        ).forEachIndexed { index, text ->
            canvas.drawText(text, abstand, abstand + paint.textSize * (index + 1), paint)
        }
    }
}

private fun projekt(p: VisualisierungsPunkt, c: VisualisierungsKonfiguration, breite: Float, höhe: Float): Offset {
    var x = p.x + c.kamera.translationX; var y = p.y + c.kamera.translationY; var z = (p.z ?: 0.0) + c.kamera.translationZ
    if (c.dimension == RaumDimension.R3) {
        val ry = c.kamera.rotationY * Math.PI / 180; val rx = c.kamera.rotationX * Math.PI / 180
        val nx = x * cos(ry) + z * sin(ry); z = -x * sin(ry) + z * cos(ry); val ny = y * cos(rx) - z * sin(rx); x = nx; y = ny
    }
    val bx = c.bereiche.x; val by = c.bereiche.y
    return Offset(((x - bx.minimum) / (bx.maximum - bx.minimum) * breite * c.kamera.zoom).toFloat(), (höhe - (y - by.minimum) / (by.maximum - by.minimum) * höhe * c.kamera.zoom).toFloat())
}
private fun farbeFür(wert: Double?, c: VisualisierungsKonfiguration): Color {
    if (c.farbe.modus == FarbModus.Keine) return Color.Transparent
    if (c.farbe.modus != FarbModus.Spektrum || wert == null) return Color(c.farbe.festeFarbe ?: 0xFF2563EB)
    val b = c.farbe.bereich ?: ZahlenBereich(-1.0, 1.0); val t = ((wert - b.minimum) / (b.maximum - b.minimum)).coerceIn(0.0, 1.0).toFloat()
    return when (c.farbe.palette) {
        "Sonnenuntergang" -> Color(0.95f, 0.18f + .6f * t, 0.12f + .55f * t)
        "Wald" -> Color(0.08f + .35f * t, 0.25f + .6f * t, 0.16f + .2f * t)
        else -> Color(0.1f + .8f * t, 0.25f, 0.9f - .7f * t)
    }
}

@Composable private fun Steuerung(c: VisualisierungsKonfiguration, ändern: (VisualisierungsKonfiguration) -> Unit, modifier: Modifier) {
    val standard = KameraZustand(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("Rotation", style = MaterialTheme.typography.labelMedium)
        listOf("X" to { d: Double -> c.kamera.copy(rotationX = c.kamera.rotationX + d) }, "Y" to { d: Double -> c.kamera.copy(rotationY = c.kamera.rotationY + d) }, "Z" to { d: Double -> c.kamera.copy(rotationZ = c.kamera.rotationZ + d) }).forEach { (name, funktion) -> AchsenTaste(name, c.dimension == RaumDimension.R3) { ändern(c.copy(kamera = funktion(it))) } }
        OutlinedButton(onClick = { ändern(c.copy(kamera = c.kamera.copy(rotationX = 0.0, rotationY = 0.0, rotationZ = 0.0))) }, enabled = abs(c.kamera.rotationX) + abs(c.kamera.rotationY) + abs(c.kamera.rotationZ) > 1e-6, modifier = Modifier.fillMaxWidth()) { Text("Reset") }
        Spacer(Modifier.height(5.dp)); Text("Verschiebung", style = MaterialTheme.typography.labelMedium)
        val schritt = (c.bereiche.x.maximum - c.bereiche.x.minimum) * .1
        listOf("X" to { d: Double -> c.kamera.copy(translationX = c.kamera.translationX + d) }, "Y" to { d: Double -> c.kamera.copy(translationY = c.kamera.translationY + d) }, "Z" to { d: Double -> c.kamera.copy(translationZ = c.kamera.translationZ + d) }).forEach { (name, funktion) -> AchsenTaste(name, c.dimension == RaumDimension.R3 || name != "Z") { ändern(c.copy(kamera = funktion(it * schritt))) } }
        OutlinedButton(onClick = { ändern(c.copy(kamera = c.kamera.copy(translationX = 0.0, translationY = 0.0, translationZ = 0.0))) }, enabled = abs(c.kamera.translationX) + abs(c.kamera.translationY) + abs(c.kamera.translationZ) > 1e-6, modifier = Modifier.fillMaxWidth()) { Text("Reset") }
        OutlinedButton(onClick = { ändern(c.copy(kamera = standard)) }, enabled = !c.kamera.istStandard(c.dimension), modifier = Modifier.fillMaxWidth()) { Text("Ansicht zurück") }
    }
}
@Composable private fun AchsenTaste(name: String, enabled: Boolean, ändern: (Double) -> Unit) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { TextButton(onClick = { ändern(-15.0) }, enabled = enabled) { Text("−") }; Text(name, modifier = Modifier.padding(top = 12.dp)); TextButton(onClick = { ändern(15.0) }, enabled = enabled) { Text("+") } }
private fun legende(c: VisualisierungsKonfiguration): String {
    val farbBereich = c.farbe.bereich ?: ZahlenBereich(-1.0, 1.0)
    return "${c.achsen.x}\\text{-Achse}: ${c.achsen.x},\\quad ${c.achsen.y}\\text{-Achse}: ${c.achsen.y}" +
        if (c.dimension == RaumDimension.R3) ",\\quad ${c.achsen.z}\\text{-Achse}: ${c.achsen.z}" else "" +
        if (c.farbe.modus == FarbModus.Spektrum) ",\\quad \\operatorname{Farbe}: ${c.farbe.variable ?: "t"},\\quad ${farbBereich.minimum}\\le ${c.farbe.variable ?: "t"}\\le ${farbBereich.maximum}" else ""
}
