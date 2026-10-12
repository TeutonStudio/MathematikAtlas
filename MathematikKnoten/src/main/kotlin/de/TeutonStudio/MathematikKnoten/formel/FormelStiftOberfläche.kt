package de.TeutonStudio.MathematikKnoten

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

internal data class FormelEditorAbstände(val standard: androidx.compose.ui.unit.Dp = 12.dp,
    val bereich: androidx.compose.ui.unit.Dp = 16.dp, val dialog: androidx.compose.ui.unit.Dp = 16.dp,
    val inhalt: androidx.compose.ui.unit.Dp = 12.dp, val winzig: androidx.compose.ui.unit.Dp = 4.dp,
    val eng: androidx.compose.ui.unit.Dp = 8.dp)
internal val LocalFormelEditorAbstände = staticCompositionLocalOf { FormelEditorAbstände() }

enum class FormelEingabeModus { Tastatur, Stift, Foto }

/** Plattformunabhängige Modi; Zeichnung und mathematische Interpretation werden nie vermischt. */
@Composable
internal fun FormelEingabeModi(entwurf: FormelZeichnungsEntwurf) {
    var modus by remember { mutableStateOf(FormelEingabeModus.Tastatur) }
    var revision by remember { mutableIntStateOf(0) }
    var radieren by remember { mutableStateOf(false) }
    var diagnose by remember { mutableStateOf<String?>(null) }
    var punkte by remember { mutableStateOf(emptyList<FormelStiftPunkt>()) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FormelEingabeModus.entries.forEach { m ->
            FilterChip(selected = modus == m, onClick = { modus = m }, label = { Text(m.name) })
        }
    }
    when (modus) {
        FormelEingabeModus.Tastatur -> Text("Die strukturierte Formel unten ist die Interpretation; Originalstriche bleiben getrennt erhalten.", style = MaterialTheme.typography.bodySmall)
        FormelEingabeModus.Foto -> Text("Fotoimport und automatische Handschrift-/Fotoerkennung sind erst in Beta verfügbar. Alpha speichert keine Fotos.", style = MaterialTheme.typography.bodyMedium)
        FormelEingabeModus.Stift -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = radieren, onClick = { radieren = !radieren }, label = { Text("Radierer") })
                TextButton(onClick = { entwurf.rückgängig(); revision++ }, enabled = entwurf.kannRückgängig) { Text("Strich zurück") }
                TextButton(onClick = { entwurf.wiederholen(); revision++ }, enabled = entwurf.kannWiederholen) { Text("Strich wiederholen") }
            }
            val zeichnung = remember(revision) { entwurf.zeichnung }
            val operationen = zeichnung.striche.filter { it.radierung }.sumOf { it.punkte.size.toLong() } * zeichnung.striche.sumOf { it.punkte.size.toLong() }
            val darstellung = remember(zeichnung) { runCatching { check(operationen <= 2_000_000); zeichnung.sichtbareFragmente() } }
            val fragmente = darstellung.getOrDefault(emptyList())
            if (darstellung.isFailure) Text("Zeichnung bleibt gespeichert; das Darstellungsbudget ist erschöpft.", color = MaterialTheme.colorScheme.error)
            else Canvas(Modifier.fillMaxWidth().height(180.dp).border(1.dp, MaterialTheme.colorScheme.outline)
                .semantics { contentDescription = "Formelzeichenfläche; logische Koordinaten 1024 mal 512" }
                .pointerInput(radieren, entwurf) {
                    fun punkt(p: Offset) = FormelStiftPunkt((p.x / size.width * 1024).toDouble().coerceIn(0.0, 1024.0),
                        (p.y / size.height * 512).toDouble().coerceIn(0.0, 512.0))
                    awaitEachGesture {
                        val anfang = awaitFirstDown(requireUnconsumed = false)
                        punkte = listOf(punkt(anfang.position)); anfang.consume()
                        var abgebrochen = false
                        try {
                            do {
                                val ereignis = awaitPointerEvent()
                                val change = ereignis.changes.firstOrNull { it.id == anfang.id } ?: break
                                if (punkte.size < 16384) punkte = punkte + punkt(change.position)
                                else diagnose = "Punktgrenze erreicht; Geste wird begrenzt."
                                change.consume()
                            } while (change.pressed)
                        } catch (e: java.util.concurrent.CancellationException) {
                            abgebrochen = true
                            throw e
                        } finally {
                            if (!abgebrochen && punkte.isNotEmpty()) {
                                runCatching { entwurf.geste(FormelVektorStrich(punkte = verdichteFormelStiftPunkte(punkte), breite = if (radieren) 24.0 else 3.0, radierung = radieren)) }
                                    .onFailure { diagnose = it.message }
                                revision++
                            }
                            punkte = emptyList()
                        }
                    }
                }) {
                fun position(p: FormelStiftPunkt) = Offset((p.x / 1024 * size.width).toFloat(), (p.y / 512 * size.height).toFloat())
                fragmente.forEach { s ->
                    val farbe = Color(s.farbe)
                    val breite = (s.breite / 1024 * size.width).toFloat().coerceAtLeast(1f)
                    if (s.punkte.size == 1) drawCircle(farbe, breite / 2, position(s.punkte.single()))
                    s.punkte.zipWithNext().forEach { (a,b) -> drawLine(farbe, position(a), position(b), breite, StrokeCap.Round) }
                }
                punkte.zipWithNext().forEach { (a,b) -> drawLine(if (radieren) Color.Gray else Color(0xff1565c0), position(a), position(b), if (radieren) 12f else 2f, StrokeCap.Round) }
            }
            Text("Zeichnung ohne automatische Erkennung. Die Formel darunter wird separat bearbeitet.", style = MaterialTheme.typography.bodySmall)
        }
    }
    (entwurf.diagnose ?: diagnose)?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
}
