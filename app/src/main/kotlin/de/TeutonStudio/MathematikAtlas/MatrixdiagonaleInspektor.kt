package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.MATRIXDIAGONALE_ART_PARAMETER
import de.TeutonStudio.MathematikRechenSystem.kern.MatrixDiagonalArt

internal object MatrixdiagonaleInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        val gespeichert = knoten.parameter[MATRIXDIAGONALE_ART_PARAMETER]
        val aktuell = MatrixDiagonalArt.vonParameter(gespeichert)
        val unbekannt = gespeichert != null && MatrixDiagonalArt.ausParameterOderNull(gespeichert) == null

        Text("Diagonale", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = aktuell == MatrixDiagonalArt.HAUPTDIAGONALE,
                onClick = {
                    aktionen.parameter(
                        MATRIXDIAGONALE_ART_PARAMETER,
                        MatrixDiagonalArt.HAUPTDIAGONALE.parameterWert,
                    )
                },
                label = { Text("Hauptdiagonale") },
            )
            FilterChip(
                selected = aktuell == MatrixDiagonalArt.NEBENDIAGONALE,
                onClick = {
                    aktionen.parameter(
                        MATRIXDIAGONALE_ART_PARAMETER,
                        MatrixDiagonalArt.NEBENDIAGONALE.parameterWert,
                    )
                },
                label = { Text("Nebendiagonale") },
            )
        }
        Text(
            "Bei rechteckigen Matrizen beginnt die Nebendiagonale rechts oben und besitzt die Länge min(Zeilen, Spalten).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (unbekannt) {
            Text(
                "Unbekannte gespeicherte Diagonalart; Hauptdiagonale wird verwendet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        ergebnis?.fehler?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
