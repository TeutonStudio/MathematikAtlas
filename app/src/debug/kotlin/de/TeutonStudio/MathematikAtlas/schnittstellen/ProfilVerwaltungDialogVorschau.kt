package de.TeutonStudio.MathematikAtlas.schnittstellen

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.TeutonStudio.MathematikAtlas.ProfilVerwaltungDialog

@Preview(
    name = "Profil und Verwaltung · befülltes Profil",
    widthDp = 1040,
    heightDp = 900,
    showBackground = true,
)
@Composable
private fun ProfilVerwaltungDialogVorschau() {
    val zustand = erinnereVorschauAtlasZustand()
    MathematikAtlasVorschauRahmen {
        ProfilVerwaltungDialog(
            zustand = zustand,
            schließen = {},
            einstellungenÖffnen = {},
            profilGeändert = {},
        )
    }
}
