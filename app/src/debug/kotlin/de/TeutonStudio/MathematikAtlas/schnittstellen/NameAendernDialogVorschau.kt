package de.TeutonStudio.MathematikAtlas.schnittstellen

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.TeutonStudio.MathematikAtlas.NameÄndernDialog

@Preview(
    name = "Name ändern · lange Kartenbezeichnung",
    widthDp = 520,
    heightDp = 360,
    showBackground = true,
)
@Composable
private fun NameÄndernDialogVorschau() {
    MathematikAtlasVorschauRahmen {
        NameÄndernDialog(
            titel = "Karte umbenennen",
            aktuellerName = VorschauDaten.LangeBezeichnung,
            schließen = {},
            bestätigen = {},
        )
    }
}
