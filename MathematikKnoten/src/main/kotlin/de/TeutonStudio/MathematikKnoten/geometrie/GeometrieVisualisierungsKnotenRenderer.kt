package de.TeutonStudio.MathematikKnoten

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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.*
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.math.*

/** Direkter Geometrie-Renderer. Die Dimension stammt aus dem Raum und ist keine frei widersprechbare UI-Einstellung. */
class GeometrieVisualisierungsKnotenRenderer(
    private val ergebnisFür: (KnotenDaten) -> KnotenAuswertungsErgebnis?,
) : KnotenRenderer {
    override val interaktionsModus = KnotenInteraktionsModus.NurKopfzeileZiehbar

    @Composable
    override fun Inhalt(knoten: KnotenDaten, ausgewählt: Boolean, aktionen: KnotenRendererAktionen) {
        val objekt = ergebnisFür(knoten)?.eingänge?.get("objekt")?.objekt as? GeometrischerAusdruck
        val dimension = objekt?.raum?.dimension
        var kamera by remember(knoten.id) { mutableStateOf(GeometrieKamera()) }
        val linie = MaterialTheme.colorScheme.primary
        val punkt = MaterialTheme.colorScheme.tertiary
        val achse = MaterialTheme.colorScheme.outlineVariant
        val hintergrund = MaterialTheme.colorScheme.surfaceVariant

        Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Geometrievisualisierung", style = MaterialTheme.typography.titleMedium)
                Text(dimension?.let { "R$it" } ?: "–", style = MaterialTheme.typography.titleMedium)
            }
            when {
                objekt == null -> Hinweis("Verbinde ein geometrisches Objekt.")
                dimension !in 1..3 -> Hinweis("R$dimension ist erst nach einer expliziten Projektion in R1, R2 oder R3 darstellbar.")
                else -> {
                    GeometrieCanvas(
                        objekt,
                        kamera,
                        { kamera = it },
                        linie,
                        punkt,
                        achse,
                        hintergrund,
                        Modifier.fillMaxWidth().weight(1f),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${objekt::class.simpleName} · ${objekt.raum.id}", style = MaterialTheme.typography.labelSmall)
                        TextButton(onClick = { kamera = GeometrieKamera() }, enabled = kamera != GeometrieKamera()) { Text("Standardansicht") }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hinweis(text: String) {
    Box(Modifier.fillMaxWidth().padding(12.dp)) { Text(text, style = MaterialTheme.typography.bodyMedium) }
}

private data class GeometrieKamera(
    val rotationX: Double = 0.0,
    val rotationY: Double = 0.0,
    val verschiebungX: Double = 0.0,
    val verschiebungY: Double = 0.0,
    val zoom: Double = 1.0,
)

private data class Linie3(val a: List<Double>, val b: List<Double>)
private data class RenderGeometrie(val punkte: List<List<Double>>, val linien: List<Linie3>, val kreise: List<Pair<List<Double>, Double>>)

@Composable
private fun GeometrieCanvas(
    objekt: GeometrischerAusdruck,
    kamera: GeometrieKamera,
    onKamera: (GeometrieKamera) -> Unit,
    linienFarbe: Color,
    punktFarbe: Color,
    achsenFarbe: Color,
    hintergrund: Color,
    modifier: Modifier,
) {
    val render = remember(objekt) { renderDaten(objekt) }
    Canvas(modifier.pointerInput(objekt.raum.dimension, kamera) {
        detectTransformGestures { _, pan, zoom, _ ->
            onKamera(
                if (objekt.raum.dimension == 3) kamera.copy(
                    rotationY = kamera.rotationY + pan.x * 0.45,
                    rotationX = kamera.rotationX + pan.y * 0.45,
                    zoom = (kamera.zoom * zoom).coerceIn(0.1, 20.0),
                ) else kamera.copy(
                    verschiebungX = kamera.verschiebungX + pan.x,
                    verschiebungY = kamera.verschiebungY + pan.y,
                    zoom = (kamera.zoom * zoom).coerceIn(0.1, 20.0),
                ),
            )
        }
    }) {
        drawRect(hintergrund)
        zeichneAchsen(objekt.raum.dimension, kamera, achsenFarbe)
        render.linien.forEach { drawLine(linienFarbe, projekt(it.a, size.width, size.height, kamera), projekt(it.b, size.width, size.height, kamera), 3f) }
        render.kreise.forEach { (mitte, radius) ->
            val m = projekt(mitte, size.width, size.height, kamera)
            drawCircle(linienFarbe, (radius * skala(size.width, size.height, kamera)).toFloat(), m, style = Stroke(3f))
        }
        render.punkte.forEach { drawCircle(punktFarbe, 5f, projekt(it, size.width, size.height, kamera)) }
        drawRect(achsenFarbe, style = Stroke(1f))
    }
}

private fun DrawScope.zeichneAchsen(dimension: Int, kamera: GeometrieKamera, farbe: Color) {
    val nullpunkt = projekt(List(dimension) { 0.0 }, size.width, size.height, kamera)
    drawLine(farbe, Offset(0f, nullpunkt.y), Offset(size.width, nullpunkt.y), 1f)
    if (dimension >= 2) drawLine(farbe, Offset(nullpunkt.x, 0f), Offset(nullpunkt.x, size.height), 1f)
    if (dimension == 3) {
        val z0 = projekt(listOf(0.0, 0.0, -10.0), size.width, size.height, kamera)
        val z1 = projekt(listOf(0.0, 0.0, 10.0), size.width, size.height, kamera)
        drawLine(farbe, z0, z1, 1f)
    }
}

private fun renderDaten(objekt: GeometrischerAusdruck): RenderGeometrie = when (objekt) {
    is GeometriePunkt -> RenderGeometrie(listOfNotNull(objekt.dezimalKoordinaten()), emptyList(), emptyList())
    is GeometrieStrecke -> ausPunktPaar(objekt.anfang, objekt.ende)
    is GeometrieGerade -> ausPunktPaar(objekt.a, objekt.b, verlängern = true)
    is GeometrieStrahl -> ausPunktPaar(objekt.ursprung, objekt.richtungsPunkt, strahl = true)
    is GeometrieWinkel -> kombiniere(renderDaten(GeometrieStrahl(objekt.scheitel, objekt.a)), renderDaten(GeometrieStrahl(objekt.scheitel, objekt.c)))
    is GeometrieKreislinie -> {
        val mitte = objekt.mittelpunkt.dezimalKoordinaten()
        val rand = objekt.randpunkt.dezimalKoordinaten()
        val radius = if (mitte != null && rand != null) sqrt(mitte.zip(rand) { a, b -> (a - b).pow(2) }.sum()) else null
        RenderGeometrie(listOfNotNull(mitte, rand), emptyList(), if (mitte != null && radius != null && objekt.raum.dimension == 2) listOf(mitte to radius) else emptyList())
    }
    is GeometriePolygon -> {
        val p = objekt.ecken.mapNotNull { it.dezimalKoordinaten() }
        val l = if (p.size == objekt.ecken.size) p.indices.map { Linie3(p[it], p[(it + 1) % p.size]) } else emptyList()
        RenderGeometrie(p, l, emptyList())
    }
    is GeometrieGruppe -> objekt.objekte.map(::renderDaten).reduce(::kombiniere)
    is GeometrieEbene -> kombiniere(ausPunktPaar(objekt.a, objekt.b), ausPunktPaar(objekt.b, objekt.c), ausPunktPaar(objekt.c, objekt.a))
    is TransformiertesGeometrieObjekt -> {
        val p = objekt.struktur.stufen.flatMap { it.zellen }.mapNotNull { (it.geometrie as? GeometriePunkt)?.dezimalKoordinaten() }
        RenderGeometrie(p, emptyList(), emptyList())
    }
    else -> RenderGeometrie(emptyList(), emptyList(), emptyList())
}

private fun ausPunktPaar(a: GeometriePunkt, b: GeometriePunkt, verlängern: Boolean = false, strahl: Boolean = false): RenderGeometrie {
    val pa = a.dezimalKoordinaten()
    val pb = b.dezimalKoordinaten()
    if (pa == null || pb == null) return RenderGeometrie(emptyList(), emptyList(), emptyList())
    val d = pa.zip(pb) { x, y -> y - x }
    val start = if (verlängern) pa.zip(d) { x, delta -> x - delta * 20 } else pa
    val ende = if (verlängern || strahl) pb.zip(d) { x, delta -> x + delta * 20 } else pb
    return RenderGeometrie(listOf(pa, pb), listOf(Linie3(start, ende)), emptyList())
}

private fun kombiniere(vararg daten: RenderGeometrie) = RenderGeometrie(
    daten.flatMap { it.punkte }, daten.flatMap { it.linien }, daten.flatMap { it.kreise },
)

private fun projekt(punkt: List<Double>, breite: Float, höhe: Float, kamera: GeometrieKamera): Offset {
    var x = punkt.getOrElse(0) { 0.0 }
    var y = punkt.getOrElse(1) { 0.0 }
    var z = punkt.getOrElse(2) { 0.0 }
    if (punkt.size >= 3) {
        val ry = Math.toRadians(kamera.rotationY)
        val rx = Math.toRadians(kamera.rotationX)
        val nx = x * cos(ry) + z * sin(ry)
        val nz = -x * sin(ry) + z * cos(ry)
        val ny = y * cos(rx) - nz * sin(rx)
        x = nx
        y = ny
        z = y * sin(rx) + nz * cos(rx)
    }
    val s = skala(breite, höhe, kamera)
    return Offset(
        (breite / 2 + x * s + kamera.verschiebungX).toFloat(),
        (höhe / 2 - y * s + z * s * 0.18 + kamera.verschiebungY).toFloat(),
    )
}

private fun skala(breite: Float, höhe: Float, kamera: GeometrieKamera) = min(breite, höhe) / 20.0 * kamera.zoom
