package de.TeutonStudio.MathematikAtlas.schnittstellen

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.TeutonStudio.MathematikAtlas.KnotenKonzeptDialog

@Preview(
    name = "Knotendefinition · Matrixdiagonale",
    widthDp = 1240,
    heightDp = 900,
    showBackground = true,
)
@Composable
private fun KnotenKonzeptDialogVorschau() {
    val zustand = erinnereVorschauAtlasZustand()
    MathematikAtlasVorschauRahmen {
        KnotenKonzeptDialog(
            zustand = zustand,
            knoten = VorschauDaten.Matrixdiagonale,
            schließen = {},
        )
    }
}
