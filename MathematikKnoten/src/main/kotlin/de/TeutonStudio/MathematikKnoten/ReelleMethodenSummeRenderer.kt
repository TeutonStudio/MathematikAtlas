package de.TeutonStudio.MathematikKnoten

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.anzeigeLatex
import de.TeutonStudio.MathematikRechenSystem.kern.MathematischesObjekt
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.Tupel
import kotlin.math.max
import kotlin.math.min

@Composable
internal fun ReelleMethodenSummeInhalt(ergebnis: KnotenAuswertungsErgebnis?) {
    val balken = ergebnis?.ausgaben?.get("visualisierung")?.objekt.zuSummenBalken()
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Reelle Methodensumme", style = MaterialTheme.typography.titleMedium)
        if (balken.isNotEmpty()) {
            val fläche = MaterialTheme.colorScheme.primary.copy(alpha = .38f)
            val kontur = MaterialTheme.colorScheme.primary
            val achse = MaterialTheme.colorScheme.onSurfaceVariant
            Canvas(Modifier.fillMaxWidth().weight(1f).heightIn(min = 150.dp)) {
                val links = balken.minOf { it.links }
                val rechts = balken.maxOf { it.rechts }
                val minimum = min(0.0, balken.minOf { it.höhe })
                val maximum = max(0.0, balken.maxOf { it.höhe })
                val xSpanne = (rechts - links).takeIf { it > 0 } ?: 1.0
                val ySpanne = (maximum - minimum).takeIf { it > 0 } ?: 1.0
                fun x(wert: Double) = ((wert - links) / xSpanne * size.width).toFloat()
                fun y(wert: Double) = (size.height - (wert - minimum) / ySpanne * size.height).toFloat()
                val nullY = y(0.0)
                drawLine(achse, Offset(0f, nullY), Offset(size.width, nullY), 1.5f)
                val pfad = Path()
                balken.forEachIndexed { index, balkenWert ->
                    val x0 = x(balkenWert.links)
                    val x1 = x(balkenWert.rechts)
                    val oben = y(balkenWert.höhe)
                    val top = min(oben, nullY)
                    val höhe = kotlin.math.abs(nullY - oben).coerceAtLeast(1f)
                    drawRect(fläche, Offset(x0, top), Size((x1 - x0).coerceAtLeast(1f), höhe))
                    drawRect(kontur, Offset(x0, top), Size((x1 - x0).coerceAtLeast(1f), höhe), style = Stroke(1.2f))
                    val mitte = (x0 + x1) / 2f
                    if (index == 0) pfad.moveTo(mitte, oben) else pfad.lineTo(mitte, oben)
                }
                drawPath(pfad, kontur, style = Stroke(2f))
            }
        } else {
            Spacer(Modifier.weight(1f))
            Text("Verbinde Methode, Partitionsanzahl und Bereich.", style = MaterialTheme.typography.bodySmall)
        }
        ergebnis?.ausgaben?.get("wert")?.let { LatexText(it.anzeigeLatex(), style = MaterialTheme.typography.bodyMedium) }
        ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
    }
}

private data class SummenBalkenAnzeige(val links: Double, val rechts: Double, val höhe: Double)

private fun MathematischesObjekt?.zuSummenBalken(): List<SummenBalkenAnzeige> =
    (this as? Tupel)?.elemente.orEmpty().mapNotNull { element ->
        val tupel = element as? Tupel ?: return@mapNotNull null
        val werte = tupel.elemente.map { (it as? RationaleZahl)?.zuDezimal(16)?.toDouble() }
        if (werte.size != 3 || werte.any { it == null }) null else SummenBalkenAnzeige(werte[0]!!, werte[1]!!, werte[2]!!)
    }
