package de.TeutonStudio.MathematikKnoten

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.anzeigeLatex
import de.TeutonStudio.MathematikRechenSystem.kern.WahrheitsKonstante

@Composable
internal fun DreieckRechnerInhalt(ergebnis: KnotenAuswertungsErgebnis?) {
    val status = ergebnis?.ausgaben?.get("status")?.objekt?.zuLatex()?.removePrefix("\\mathrm{")?.removeSuffix("}")
        ?: "unzureichend"
    val bestimmt = (ergebnis?.ausgaben?.get("bestimmt")?.objekt as? WahrheitsKonstante)?.wert == true
    val fläche = MaterialTheme.colorScheme.primary.copy(alpha = .14f)
    val kontur = MaterialTheme.colorScheme.primary
    val ecken = MaterialTheme.colorScheme.secondary
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Dreieckrechner", style = MaterialTheme.typography.titleMedium)
            Text(status.replaceFirstChar(Char::uppercase), style = MaterialTheme.typography.labelLarge)
        }
        Canvas(Modifier.fillMaxWidth().weight(1f).heightIn(min = 150.dp)) {
            val pA = Offset(size.width * .14f, size.height * .82f)
            val pB = Offset(size.width * .86f, size.height * .82f)
            val pC = Offset(size.width * .55f, size.height * .13f)
            val pfad = Path().apply { moveTo(pA.x, pA.y); lineTo(pB.x, pB.y); lineTo(pC.x, pC.y); close() }
            drawPath(pfad, fläche)
            drawPath(pfad, kontur, style = Stroke(3f))
            drawCircle(ecken, 5f, pA)
            drawCircle(ecken, 5f, pB)
            drawCircle(ecken, 5f, pC)
        }
        val namen = listOf("aWert", "bWert", "cWert", "alphaWert", "betaWert", "gammaWert")
        val werte = namen.mapNotNull { name -> ergebnis?.ausgaben?.get(name)?.anzeigeLatex() }
        if (werte.isNotEmpty()) {
            werte.chunked(3).forEach { zeile ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    zeile.forEach { LatexText(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        } else {
            Text(
                if (status == "mehrdeutig") "Die Eingaben erlauben mehrere Dreiecke; ergänze einen weiteren Wert."
                else "Verbinde mindestens drei unabhängige Werte, darunter eine Seitenlänge.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            when {
                bestimmt -> "Alle sechs Werte sind eindeutig bestimmt."
                status == "mehrdeutig" -> "Legitime, aber mehrdeutige SSW-Kombination."
                status == "ungültig" -> ergebnis?.fehler ?: "Die Werte sind widersprüchlich."
                else -> "Noch nicht genügend unabhängige Werte."
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (status == "ungültig") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
