package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.*

internal object OrientierungsKnotenInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        val orientierung = orientierungFürOrientierungsKnoten(knoten)
        Text("Orientierung", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = orientierung == VEKTOR_ORIENTIERUNG_SPALTE,
                onClick = { aktionen.knoten(konfiguriereOrientierungsKnoten(knoten, VEKTOR_ORIENTIERUNG_SPALTE)) },
                label = { Text("Spalte") },
            )
            FilterChip(
                selected = orientierung == VEKTOR_ORIENTIERUNG_ZEILE,
                onClick = { aktionen.knoten(konfiguriereOrientierungsKnoten(knoten, VEKTOR_ORIENTIERUNG_ZEILE)) },
                label = { Text("Zeile") },
            )
        }
        Text(
            "Bei einem Orientierungswechsel werden inkompatible Anschlüsse als echte Knotenersetzung behandelt und bleiben damit rückgängig machbar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ergebnis?.fehler?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}
