package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.INTERVALL_STANDARDMENGEN
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis

internal object IntervallKnotenInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        if (knoten.art == "mathematik.halbesIntervall") {
            Text("Richtung", style = MaterialTheme.typography.titleSmall)
            val richtung = knoten.parameter["richtung"] ?: "aufwaerts"
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = richtung == "aufwaerts",
                    onClick = { aktionen.parameter("richtung", "aufwaerts") },
                    label = { Text("Untergrenze (≥)") },
                )
                FilterChip(
                    selected = richtung == "abwaerts",
                    onClick = { aktionen.parameter("richtung", "abwaerts") },
                    label = { Text("Obergrenze (≤)") },
                )
            }
        }
        Text("Standardmenge", style = MaterialTheme.typography.titleSmall)
        val aktuelleMenge = knoten.parameter["standardMenge"] ?: "R"
        val namen = mapOf("R" to "ℝ", "Q" to "ℚ", "Z" to "ℤ", "N" to "ℕ", "N0" to "ℕ₀", "P" to "ℙ")
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            INTERVALL_STANDARDMENGEN.chunked(3).forEach { zeile ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    zeile.forEach { auswahl ->
                        FilterChip(
                            selected = aktuelleMenge == auswahl.stabileId,
                            onClick = { aktionen.parameter("standardMenge", auswahl.stabileId) },
                            label = { Text(namen[auswahl.stabileId] ?: auswahl.titel) },
                        )
                    }
                }
            }
        }
        Text(
            "Der verbundene Eingang „grundmenge“ hat Vorrang. " +
                "Das Ergebnis ist die Schnittmenge mit dem reellen Intervall.",
            style = MaterialTheme.typography.bodySmall,
        )
        ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
