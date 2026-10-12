package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.*

/** Dimension ergibt sich aus der Anzahl der Winkel; Trennzeichen ist ein Semikolon. */
object RichtungsWinkelInspektor : KnotenInspektor {
    @Composable override fun Inhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen) {
        Text("Richtung in Rⁿ", style = MaterialTheme.typography.titleSmall)
        val sign = knoten.parameter[RICHTUNG_WINKEL_VORZEICHEN] ?: "1"
        Row(horizontalArrangement = Arrangement.spacedBy(androidx.compose.ui.unit.Dp(12f))) {
            listOf(1, -1).forEach { wert ->
                FilterChip(selected = sign == wert.toString(),
                    onClick = { aktionen.parameter(RICHTUNG_WINKEL_VORZEICHEN, wert.toString()) },
                    label = { Text(if (wert == 1) "+1" else "−1") })
            }
        }
        OutlinedTextField(
            value = knoten.parameter[RICHTUNG_WINKEL_LISTE].orEmpty(),
            onValueChange = { aktionen.parameter(RICHTUNG_WINKEL_LISTE, it) },
            label = { Text("Halbkreiswinkel in Grad, getrennt durch ;") },
            supportingText = { Text("Keine Winkel: R¹. Ein Winkel: R². Zwei Winkel: R³. Jeder Winkel liegt in [0°, 180°).") },
            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
        )
        ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
