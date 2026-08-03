package de.TeutonStudio.MathematikAtlas.schnittstellen

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.TeutonStudio.MathematikAtlas.FormelBauerDialog

@Preview(
    name = "Formelbauer · strukturierte Formel",
    widthDp = 1180,
    heightDp = 820,
    showBackground = true,
)
@Composable
private fun FormelBauerDialogHellVorschau() {
    MathematikAtlasVorschauRahmen {
        FormelBauerDialog(
            startLatex = VorschauDaten.BeispielFormel,
            schließen = {},
            übernehmen = {},
        )
    }
}

@Preview(
    name = "Formelbauer · Dunkelmodus",
    widthDp = 1180,
    heightDp = 820,
    showBackground = true,
)
@Composable
private fun FormelBauerDialogDunkelVorschau() {
    MathematikAtlasVorschauRahmen(dunkel = true) {
        FormelBauerDialog(
            startLatex = "\\sum_{i=1}^{n} i^2",
            schließen = {},
            übernehmen = {},
        )
    }
}
