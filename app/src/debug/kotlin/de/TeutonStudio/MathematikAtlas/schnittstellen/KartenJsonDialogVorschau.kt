package de.TeutonStudio.MathematikAtlas.schnittstellen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
import de.TeutonStudio.MathematikAtlas.KartenJsonDialogV2311

@Preview(
    name = "Karten-JSON · Lineares Gleichungssystem",
    widthDp = 1380,
    heightDp = 900,
    showBackground = true,
)
@Composable
private fun KartenJsonDialogVorschau() {
    val zustand = erinnereVorschauAtlasZustand()
    LaunchedEffect(zustand) {
        zustand.editor.ersetzeKarte(VorschauDaten.BeispielKarte)
    }
    MathematikAtlasVorschauRahmen(dunkel = true) {
        KartenJsonDialogV2311(
            zustand = zustand,
            schließen = {},
        )
    }
}
