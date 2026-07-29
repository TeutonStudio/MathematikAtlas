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

/** Compose-Renderer für eindimensionale, ebene und räumliche Mengen. */
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
                Text(dimensionText(konfiguration.dimension), style = MaterialTheme.typography.titleMedium)
            }
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Plot(ergebnis, konfiguration, { ändern(konfiguration.copy(kamera = it)) }, Modifier.weight(1f).fillMaxHeight())
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

private fun dimensionText(dimension: RaumDimension) = when (dimension) { RaumDimension.R1 -> "R¹"; RaumDimension.R2 -> "R²"; RaumDimension.R3 -> "R³" }

@Composable
private fun Plot(
    ergebnis: VisualisierungsErgebnis?,
    konfiguration: VisualisierungsKonfiguration,
    onKamera: (KameraZustand) -> Unit,
    modifier: Modifier,
) {
    val kamera = konfiguration.kamera
    val hintergrund = MaterialTheme.colorScheme.surfaceVariant
    val raster = MaterialTheme.colorScheme.outlineVariant
    val rahmen = MaterialTheme.colorScheme.outline
    val beschriftung = MaterialTheme.colorScheme.onSurfaceVariant
    val xAchse = MaterialTheme.colorScheme.primary
    val yAchse = MaterialTheme.colorScheme.secondary
    val zAchse = MaterialTheme.colorScheme.tertiary
    Canvas(modifier.pointerInput(konfiguration.dimension, kamera) {
        detectTransformGestures { _, pan, zoom, _ ->
            val neu = when (konfiguration.dimension) {
                RaumDimension.R1 -> kamera.copy(translationX = kamera.translationX - pan.x / 20.0, zoom = (kamera.zoom * zoom).coerceIn(0.1, 20.0))
                RaumDimension.R2 -> kamera.copy(translationX = kamera.translationX - pan.x / 20.0, translationY = kamera.translationY + pan.y / 20.0, zoom = (kamera.zoom * zoom).coerceIn(0.1, 20.0))
                RaumDimension.R3 -> kamera.copy(rotationY = kamera.rotationY + pan.x * 0.5, rotationX = kamera.rotationX + pan.y * 0.5, zoom = (kamera.zoom * zoom).coerceIn(0.1, 20.0))
            }
            onKamera(neu)
        }
    }) {
        drawRect(hintergrund)
        when (konfiguration.dimension) {
            RaumDimension.R1 -> zeichne1DAchse(konfiguration, raster, beschriftung)
            RaumDimension.R2 -> {
                val center = Offset(size.width / 2f, size.height / 2f)
                drawLine(raster, Offset(0f, center.y), Offset(size.width, center.y), 1f)
                drawLine(raster, Offset(center.x, 0f), Offset(center.x, size.height), 1f)
                zeichne2DAchsenBeschriftungen(konfiguration, beschriftung)
            }
            RaumDimension.R3 -> zeichne3DAchsen(konfiguration, xAchse, yAchse, zAchse)
        }
        val punkte = when (ergebnis) {
            is VisualisierungsErgebnis.Erfolgreich -> ergebnis.punkte
            is VisualisierungsErgebnis.Teilweise -> ergebnis.punkte
            else -> emptyList()
        }
        punkte.forEach { punkt ->
            val projektion = projekt(punkt, konfiguration, size.width, size.height)
            val farbe = farbeFür(punkt.farbwert, konfiguration)
            if (konfiguration.dimension == RaumDimension.R1) {
                drawLine(farbe, Offset(projektion.x, projektion.y - 8f), Offset(projektion.x, projektion.y + 8f), 2.2f)
                drawCircle(farbe, 3.2f, projektion)
            } else drawCircle(farbe, if (konfiguration.dimension == RaumDimension.R3) 2.8f else 2.2f, projektion)
        }
        drawRect(rahmen, style = Stroke(1f))
    }
}

private fun DrawScope.achsenPaint(farbe: Color) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = farbe.toArgb()
    textSize = 12.dp.toPx()
    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
}

private fun DrawScope.zeichne1DAchse(c: VisualisierungsKonfiguration, farbe: Color, textFarbe: Color) {
    val y = size.height / 2f
    drawLine(farbe, Offset(0f, y), Offset(size.width, y), 2f)
    val paint = achsenPaint(textFarbe)
    val canvas = drawContext.canvas.nativeCanvas
    val minimum = c.bereiche.x.minimum.toString()
    val maximum = c.bereiche.x.maximum.toString()
    canvas.drawText(minimum, 5.dp.toPx(), y - 8.dp.toPx(), paint)
    canvas.drawText(maximum, size.width - paint.measureText(maximum) - 5.dp.toPx(), y - 8.dp.toPx(), paint)
    val name = c.achsen.x.ifBlank { "x" }
    canvas.drawText(name, size.width - paint.measureText(name) - 5.dp.toPx(), y + paint.textSize + 8.dp.toPx(), paint)
}

private fun DrawScope.zeichne2DAchsenBeschriftungen(c: VisualisierungsKonfiguration, farbe: Color) {
    val abstand = 7.dp.toPx()
    val paint = achsenPaint(farbe)
    val canvas = drawContext.canvas.nativeCanvas
    val x = c.achsen.x.ifBlank { "x" }
    val y = c.achsen.y.ifBlank { "y" }
    canvas.drawText(x, size.width - paint.measureText(x) - abstand, size.height / 2f - abstand, paint)
    canvas.drawText(y, size.width / 2f + abstand, paint.textSize + abstand, paint)
}

private fun DrawScope.zeichne3DAchsen(c: VisualisierungsKonfiguration, xFarbe: Color, yFarbe: Color, zFarbe: Color) {
    val zBereich = c.bereiche.z ?: return
    val xStart = projekt(VisualisierungsPunkt(c.bereiche.x.minimum, 0.0, 0.0), c, size.width, size.height)
    val xEnde = projekt(VisualisierungsPunkt(c.bereiche.x.maximum, 0.0, 0.0), c, size.width, size.height)
    val yStart = projekt(VisualisierungsPunkt(0.0, c.bereiche.y.minimum, 0.0), c, size.width, size.height)
    val yEnde = projekt(VisualisierungsPunkt(0.0, c.bereiche.y.maximum, 0.0), c, size.width, size.height)
    val zStart = projekt(VisualisierungsPunkt(0.0, 0.0, zBereich.minimum), c, size.width, size.height)
    val zEnde = projekt(VisualisierungsPunkt(0.0, 0.0, zBereich.maximum), c, size.width, size.height)
    drawLine(xFarbe.copy(alpha = .78f), xStart, xEnde, 2f)
    drawLine(yFarbe.copy(alpha = .78f), yStart, yEnde, 2f)
    drawLine(zFarbe.copy(alpha = .78f), zStart, zEnde, 2f)
    zeichneAchsenText(c.achsen.x.ifBlank { "x" }, xEnde, achsenPaint(xFarbe))
    zeichneAchsenText(c.achsen.y.ifBlank { "y" }, yEnde, achsenPaint(yFarbe))
    zeichneAchsenText(c.achsen.z?.ifBlank { "z" } ?: "z", zEnde, achsenPaint(zFarbe))
}

private fun DrawScope.zeichneAchsenText(text: String, position: Offset, paint: Paint) {
    val rand = 5.dp.toPx()
    val x = (position.x + rand).coerceIn(rand, (size.width - paint.measureText(text) - rand).coerceAtLeast(rand))
    val y = (position.y - rand).coerceIn(paint.textSize + rand, size.height - rand)
    drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
}

private fun projekt(p: VisualisierungsPunkt, c: VisualisierungsKonfiguration, breite: Float, höhe: Float): Offset {
    var x = p.x + c.kamera.translationX
    var y = p.y + c.kamera.translationY
    var z = (p.z ?: 0.0) + c.kamera.translationZ
    if (c.dimension == RaumDimension.R3) {
        val ry = c.kamera.rotationY * Math.PI / 180
        val rx = c.kamera.rotationX * Math.PI / 180
        val rz = c.kamera.rotationZ * Math.PI / 180
        val xNachY = x * cos(ry) + z * sin(ry)
        val zNachY = -x * sin(ry) + z * cos(ry)
        x = xNachY; z = zNachY
        val yNachX = y * cos(rx) - z * sin(rx)
        val zNachX = y * sin(rx) + z * cos(rx)
        y = yNachX; z = zNachX
        val xNachZ = x * cos(rz) - y * sin(rz)
        val yNachZ = x * sin(rz) + y * cos(rz)
        x = xNachZ; y = yNachZ
    }
    val bx = c.bereiche.x
    val px = ((x - bx.minimum) / (bx.maximum - bx.minimum) * breite * c.kamera.zoom).toFloat()
    if (c.dimension == RaumDimension.R1) return Offset(px, höhe / 2f)
    val by = c.bereiche.y
    return Offset(px, (höhe - (y - by.minimum) / (by.maximum - by.minimum) * höhe * c.kamera.zoom).toFloat())
}

private fun farbeFür(wert: Double?, c: VisualisierungsKonfiguration): Color {
    if (c.farbe.modus == FarbModus.Keine) return Color(c.farbe.festeFarbe ?: 0xFF2563EB)
    if (c.farbe.modus != FarbModus.Spektrum || wert == null) return Color(c.farbe.festeFarbe ?: 0xFF2563EB)
    val b = c.farbe.bereich ?: ZahlenBereich(-1.0, 1.0)
    val t = ((wert - b.minimum) / (b.maximum - b.minimum)).coerceIn(0.0, 1.0).toFloat()
    return when (c.farbe.palette) {
        "Sonnenuntergang" -> Color(.95f, .18f + .6f * t, .12f + .55f * t)
        "Wald" -> Color(.08f + .35f * t, .25f + .6f * t, .16f + .2f * t)
        else -> Color(.1f + .8f * t, .25f, .9f - .7f * t)
    }
}

@Composable
private fun Steuerung(c: VisualisierungsKonfiguration, ändern: (VisualisierungsKonfiguration) -> Unit, modifier: Modifier) {
    val standard = KameraZustand(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("Rotation", style = MaterialTheme.typography.labelMedium)
        listOf(
            "X" to { d: Double -> c.kamera.copy(rotationX = c.kamera.rotationX + d) },
            "Y" to { d: Double -> c.kamera.copy(rotationY = c.kamera.rotationY + d) },
            "Z" to { d: Double -> c.kamera.copy(rotationZ = c.kamera.rotationZ + d) },
        ).forEach { (name, funktion) -> AchsenTaste(name, c.dimension == RaumDimension.R3) { ändern(c.copy(kamera = funktion(it))) } }
        OutlinedButton(
            onClick = { ändern(c.copy(kamera = c.kamera.copy(rotationX = 0.0, rotationY = 0.0, rotationZ = 0.0))) },
            enabled = c.dimension == RaumDimension.R3 && abs(c.kamera.rotationX) + abs(c.kamera.rotationY) + abs(c.kamera.rotationZ) > 1e-6,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Reset") }
        Spacer(Modifier.height(5.dp))
        Text("Verschiebung", style = MaterialTheme.typography.labelMedium)
        val schritt = (c.bereiche.x.maximum - c.bereiche.x.minimum) * .1
        listOf(
            "X" to { d: Double -> c.kamera.copy(translationX = c.kamera.translationX + d) },
            "Y" to { d: Double -> c.kamera.copy(translationY = c.kamera.translationY + d) },
            "Z" to { d: Double -> c.kamera.copy(translationZ = c.kamera.translationZ + d) },
        ).forEach { (name, funktion) ->
            val aktiv = when (name) { "X" -> true; "Y" -> c.dimension != RaumDimension.R1; else -> c.dimension == RaumDimension.R3 }
            AchsenTaste(name, aktiv) { ändern(c.copy(kamera = funktion(it * schritt))) }
        }
        OutlinedButton(
            onClick = { ändern(c.copy(kamera = c.kamera.copy(translationX = 0.0, translationY = 0.0, translationZ = 0.0))) },
            enabled = abs(c.kamera.translationX) + abs(c.kamera.translationY) + abs(c.kamera.translationZ) > 1e-6,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Reset") }
        OutlinedButton(onClick = { ändern(c.copy(kamera = standard)) }, enabled = !c.kamera.istStandard(c.dimension), modifier = Modifier.fillMaxWidth()) { Text("Ansicht zurück") }
    }
}

@Composable
private fun AchsenTaste(name: String, enabled: Boolean, ändern: (Double) -> Unit) = Row(
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
) {
    TextButton(onClick = { ändern(-15.0) }, enabled = enabled) { Text("−") }
    Text(name, modifier = Modifier.padding(top = 12.dp))
    TextButton(onClick = { ändern(15.0) }, enabled = enabled) { Text("+") }
}

private fun legende(c: VisualisierungsKonfiguration): String {
    val farbBereich = c.farbe.bereich ?: ZahlenBereich(-1.0, 1.0)
    val achsen = when (c.dimension) {
        RaumDimension.R1 -> "${c.achsen.x}\\text{-Achse}: ${c.achsen.x}"
        RaumDimension.R2 -> "${c.achsen.x}\\text{-Achse}: ${c.achsen.x},\\quad ${c.achsen.y}\\text{-Achse}: ${c.achsen.y}"
        RaumDimension.R3 -> "${c.achsen.x}\\text{-Achse}: ${c.achsen.x},\\quad ${c.achsen.y}\\text{-Achse}: ${c.achsen.y},\\quad ${c.achsen.z}\\text{-Achse}: ${c.achsen.z}"
    }
    return achsen + if (c.farbe.modus == FarbModus.Spektrum) ",\\quad \\operatorname{Farbe}: ${c.farbe.variable ?: "t"},\\quad ${farbBereich.minimum}\\le ${c.farbe.variable ?: "t"}\\le ${farbBereich.maximum}" else ""
}
