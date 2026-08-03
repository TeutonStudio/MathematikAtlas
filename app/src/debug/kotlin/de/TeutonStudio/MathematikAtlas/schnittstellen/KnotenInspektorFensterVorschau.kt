package de.TeutonStudio.MathematikAtlas.schnittstellen

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.MathematikAtlas.Inspektor

@Preview(
    name = "Inspektor · Matrixdiagonale",
    widthDp = 380,
    heightDp = 900,
    showBackground = true,
)
@Composable
private fun KnotenInspektorFensterVorschau() {
    val zustand = erinnereVorschauAtlasZustand()
    LaunchedEffect(zustand) {
        zustand.editor.ersetzeKarte(
            KartenDaten(
                name = "Matrixanalyse",
                knoten = listOf(VorschauDaten.Matrixdiagonale),
            ),
        )
        zustand.editor.wähleKnoten(VorschauDaten.Matrixdiagonale.id)
        zustand.aktualisiereAuswertung()
    }
    MathematikAtlasVorschauRahmen {
        Inspektor(
            zustand = zustand,
            modifier = Modifier.width(330.dp).fillMaxHeight(),
        )
    }
}
