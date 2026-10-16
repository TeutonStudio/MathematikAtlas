package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenInspektorBereich
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
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.MatrixDiagonalArt

internal object MatrixdiagonaleInspektor : KnotenInspektor {
    override fun hatBereich(knoten: KnotenDaten, bereich: KnotenInspektorBereich): Boolean = when (bereich) {
        KnotenInspektorBereich.Knotendaten -> true
        KnotenInspektorBereich.Eingangsdaten -> false
        KnotenInspektorBereich.Ausgangsdaten -> matrixdiagonaleModus(knoten) == MATRIXDIAGONALE_AUSLESEN
    }
    @Composable override fun Inhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen) = FachInhalt(knoten, ergebnis, aktionen, true)
    @Composable override fun BereichInhalt(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen, bereich: KnotenInspektorBereich) {
        when (bereich) {
            KnotenInspektorBereich.Knotendaten -> FachInhalt(knoten, ergebnis, aktionen, false)
            KnotenInspektorBereich.Eingangsdaten -> {  }
            KnotenInspektorBereich.Ausgangsdaten -> { MatrixdiagonaleAusgang(knoten, ergebnis, aktionen) }
        }
    }

    @Composable
    private fun FachInhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
        mitAnschlüssen: Boolean,
    ) {
        val gespeichert = knoten.parameter[MATRIXDIAGONALE_ART_PARAMETER]
        val aktuell = MatrixDiagonalArt.vonParameter(gespeichert)
        val unbekannt = gespeichert != null && MatrixDiagonalArt.ausParameterOderNull(gespeichert) == null

        Text("Operation", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(LocalAtlasAbstände.current.standard)) {
            listOf(MATRIXDIAGONALE_ERZEUGEN to "Erzeugen", MATRIXDIAGONALE_AUSLESEN to "Auslesen").forEach { (modus, titel) ->
                FilterChip(selected = matrixdiagonaleModus(knoten) == modus,
                    onClick = { aktionen.knoten(konfiguriereMatrixdiagonale(knoten, modus = modus)) },
                    label = { Text(titel) })
            }
        }
        if (mitAnschlüssen) MatrixdiagonaleAusgang(knoten, ergebnis, aktionen)

        Text("Diagonale", style = MaterialTheme.typography.titleSmall)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LocalAtlasAbstände.current.standard),
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
            if (matrixdiagonaleModus(knoten) == MATRIXDIAGONALE_ERZEUGEN)
                "Aus Tupel oder Vektor entsteht eine quadratische Nullmatrix mit den Werten auf der gewählten Diagonale."
            else "Bei rechteckigen Matrizen beginnt die Nebendiagonale rechts oben und besitzt die Länge min(Zeilen, Spalten).",
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

@Composable
private fun MatrixdiagonaleAusgang(knoten: KnotenDaten, ergebnis: KnotenAuswertungsErgebnis?, aktionen: KnotenInspektorAktionen) {
    de.TeutonStudio.MathematikKnoten.InspektorAusgangsEinstellungen(knoten, ergebnis, aktionen::knoten)
}
