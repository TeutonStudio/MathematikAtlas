package de.TeutonStudio.MathematikAtlas.schnittstellen

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.TeutonStudio.MathematikAtlas.EinstellungenDialogV2291

@Preview(
    name = "Einstellungen · Breit",
    widthDp = 1100,
    heightDp = 760,
    showBackground = true,
)
@Composable
private fun EinstellungenDialogBreitVorschau() {
    val zustand = erinnereVorschauAtlasZustand()
    MathematikAtlasVorschauRahmen {
        EinstellungenDialogV2291(zustand = zustand, schließen = {})
    }
}

@Preview(
    name = "Einstellungen · Kompakt · Dunkel",
    widthDp = 390,
    heightDp = 800,
    showBackground = true,
)
@Composable
private fun EinstellungenDialogKompaktVorschau() {
    val zustand = erinnereVorschauAtlasZustand()
    MathematikAtlasVorschauRahmen(dunkel = true) {
        EinstellungenDialogV2291(zustand = zustand, schließen = {})
    }
}
