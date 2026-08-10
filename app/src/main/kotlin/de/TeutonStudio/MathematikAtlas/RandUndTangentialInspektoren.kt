package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.TANGENTIAL_DIFFERENTIALBEGRIFF_PARAMETER
import de.TeutonStudio.MathematikKnoten.aktuelleTangentialAusgabeform
import de.TeutonStudio.MathematikKnoten.konfiguriereTangentialKnoten
import de.TeutonStudio.MathematikRechenSystem.kern.DifferentialBegriff
import de.TeutonStudio.MathematikRechenSystem.kern.TangentialAusgabeForm

internal object TangentialKnotenInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        val ausgabeform = aktuelleTangentialAusgabeform(knoten)
        Text("Ausgabe", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = ausgabeform == TangentialAusgabeForm.METHODE,
                onClick = {
                    if (ausgabeform != TangentialAusgabeForm.METHODE) {
                        aktionen.knoten(konfiguriereTangentialKnoten(knoten, TangentialAusgabeForm.METHODE))
                    }
                },
                label = { Text("Methode") },
            )
            FilterChip(
                selected = ausgabeform == TangentialAusgabeForm.MENGE,
                onClick = {
                    if (ausgabeform != TangentialAusgabeForm.MENGE) {
                        aktionen.knoten(konfiguriereTangentialKnoten(knoten, TangentialAusgabeForm.MENGE))
                    }
                },
                label = { Text("Menge") },
            )
        }

        val begriff = DifferentialBegriff.entries.firstOrNull {
            it.name == knoten.parameter[TANGENTIAL_DIFFERENTIALBEGRIFF_PARAMETER]
        } ?: DifferentialBegriff.REELL_FRECHET
        Text("Differentialbegriff", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = begriff == DifferentialBegriff.REELL_FRECHET,
                onClick = { aktionen.parameter(TANGENTIAL_DIFFERENTIALBEGRIFF_PARAMETER, DifferentialBegriff.REELL_FRECHET.name) },
                label = { Text("reell / Fréchet") },
            )
            FilterChip(
                selected = begriff == DifferentialBegriff.KOMPLEX,
                onClick = { aktionen.parameter(TANGENTIAL_DIFFERENTIALBEGRIFF_PARAMETER, DifferentialBegriff.KOMPLEX.name) },
                label = { Text("komplex") },
            )
        }
        Text(
            "Beim Wechsel der Ausgabeform wird derselbe Knoten atomar ersetzt; die stabilen Eingänge bleiben erhalten und der Ausgang wechselt zwischen Methode und Menge.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

internal object RandKnotenInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        val topologie = knoten.parameter["topologie"] ?: "kanonisch"
        Text("Topologie", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(
                "kanonisch" to "kanonisch",
                "diskret" to "diskret",
                "indiskret" to "indiskret",
            ).forEach { (wert, label) ->
                FilterChip(
                    selected = topologie == wert,
                    onClick = { aktionen.parameter("topologie", wert) },
                    label = { Text(label) },
                )
            }
        }
        FilterChip(
            selected = topologie == "symbolisch",
            onClick = { aktionen.parameter("topologie", "symbolisch") },
            label = { Text("symbolisch") },
        )

        val umgebungsraum = knoten.parameter["umgebungsraum"] ?: "R"
        var raumText by remember(knoten.id, umgebungsraum) { mutableStateOf(umgebungsraum) }
        OutlinedTextField(
            value = raumText,
            onValueChange = {
                raumText = it
                aktionen.parameter("umgebungsraum", it.trim())
            },
            label = { Text("Umgebungsraum X") },
            modifier = Modifier.fillMaxWidth(),
        )

        val relativ = knoten.parameter["relativ"]?.toBooleanStrictOrNull() ?: false
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Relativer Rand ∂ₓA")
            Switch(
                checked = relativ,
                onCheckedChange = { aktionen.parameter("relativ", it.toString()) },
            )
        }
        Text(
            "Kanonische, diskrete und indiskrete Spezialfälle werden exakt reduziert; nicht entscheidbare Fälle bleiben als strukturiertes ∂ₓA erhalten.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}
