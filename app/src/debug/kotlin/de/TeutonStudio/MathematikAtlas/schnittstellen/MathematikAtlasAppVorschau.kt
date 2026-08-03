package de.TeutonStudio.MathematikAtlas.schnittstellen

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.TeutonStudio.MathematikAtlas.MathematikAtlasApp

@Preview(
    name = "Gesamtoberfläche · Desktop",
    widthDp = 1500,
    heightDp = 920,
    showBackground = true,
)
@Composable
private fun MathematikAtlasAppVorschau() {
    val zustand = erinnereVorschauAtlasZustand()
    MathematikAtlasVorschauRahmen(dunkel = true) {
        MathematikAtlasApp(zustand)
    }
}
