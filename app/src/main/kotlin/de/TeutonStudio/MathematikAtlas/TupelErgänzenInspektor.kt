package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.TupelErgänzenModus
import de.TeutonStudio.MathematikKnoten.konfiguriereTupelErgänzen

internal object TupelErgänzenInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        val aktuell = TupelErgänzenModus.von(knoten)
        var ausstehend by remember(knoten.id, aktuell) { mutableStateOf<KnotenDaten?>(null) }

        Text("Ergänzungsart", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = aktuell == TupelErgänzenModus.Tupel,
                onClick = {
                    if (aktuell != TupelErgänzenModus.Tupel) {
                        val neu = konfiguriereTupelErgänzen(knoten, TupelErgänzenModus.Tupel)
                        val vorschau = aktionen.vorschauKnotenErsetzen(neu)
                        if (vorschau.entfallendeVerbindungen.isEmpty()) aktionen.knoten(neu) else ausstehend = neu
                    }
                },
                label = { Text("Tupel ergänzen") },
            )
            FilterChip(
                selected = aktuell == TupelErgänzenModus.Elemente,
                onClick = {
                    if (aktuell != TupelErgänzenModus.Elemente) {
                        val neu = konfiguriereTupelErgänzen(knoten, TupelErgänzenModus.Elemente)
                        val vorschau = aktionen.vorschauKnotenErsetzen(neu)
                        if (vorschau.entfallendeVerbindungen.isEmpty()) aktionen.knoten(neu) else ausstehend = neu
                    }
                },
                label = { Text("Elemente ergänzen") },
            )
        }
        Text(
            if (aktuell == TupelErgänzenModus.Tupel) {
                "Alle Eingangstupel werden in Anschlussreihenfolge direkt verkettet."
            } else {
                "Das Basistupel wird um einzelne Objekte ergänzt; ein Tupel bleibt dabei ein verschachteltes Element."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ergebnis?.fehler?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        ausstehend?.let { neu ->
            val vorschau = aktionen.vorschauKnotenErsetzen(neu)
            AlertDialog(
                onDismissRequest = { ausstehend = null },
                title = { Text("Ergänzungsart wechseln?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Der Moduswechsel entfernt ${vorschau.entfallendeVerbindungen.size} Verbindung(en).")
                        if (vorschau.entfallendeAnschlüsse.isNotEmpty()) {
                            Text(
                                "Entfallende Anschlüsse: " +
                                    vorschau.entfallendeAnschlüsse.joinToString { it.name },
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(
                            "Nur der erste Tupelanschluss bleibt erhalten; weitere Anschlüsse werden nicht stillschweigend umgedeutet.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        aktionen.knoten(neu)
                        ausstehend = null
                    }) { Text("Wechseln") }
                },
                dismissButton = {
                    TextButton(onClick = { ausstehend = null }) { Text("Abbrechen") }
                },
            )
        }
    }
}
