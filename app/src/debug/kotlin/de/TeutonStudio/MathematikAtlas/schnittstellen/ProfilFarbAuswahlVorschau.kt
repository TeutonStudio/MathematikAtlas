package de.TeutonStudio.MathematikAtlas.schnittstellen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.TeutonStudio.MathematikAtlas.FarbAuswahlDialog
import de.TeutonStudio.MathematikAtlas.ProfilFarbAuswahl
import de.TeutonStudio.MathematikAtlas.speicher.ProfilFarbe
import de.TeutonStudio.MathematikAtlas.speicher.RgbFarbe

@Preview(
    name = "Profilfarbe · kompakter Aufrufer",
    widthDp = 760,
    heightDp = 420,
    showBackground = true,
)
@Composable
private fun ProfilFarbAuswahlHellVorschau() {
    MathematikAtlasVorschauRahmen {
        ProfilFarbAuswahl(
            startFarbe = VorschauProfilFarbe,
            farbeGeaendert = {},
            modifier = Modifier
                .widthIn(max = 700.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        )
    }
}

@Preview(
    name = "Farbdialog · RGB · Hell",
    widthDp = 780,
    heightDp = 900,
    showBackground = true,
)
@Composable
private fun FarbAuswahlDialogRgbVorschau() {
    MathematikAtlasVorschauRahmen {
        FarbAuswahlDialog(
            offen = true,
            ausgangsFarbe = RgbFarbe.aus(VorschauProfilFarbe),
            standardFarbe = RgbFarbe.aus(ProfilFarbe.Standard),
            titel = "Profilfarbe auswählen",
            onAbbrechen = {},
            onBestaetigen = {},
        )
    }
}

@Preview(
    name = "Farbdialog · Violett · Dunkel",
    widthDp = 780,
    heightDp = 900,
    showBackground = true,
)
@Composable
private fun FarbAuswahlDialogDunkelVorschau() {
    val violett = requireNotNull(ProfilFarbe.parse("#7C3AED"))
    MathematikAtlasVorschauRahmen(dunkel = true, profilFarbe = violett) {
        FarbAuswahlDialog(
            offen = true,
            ausgangsFarbe = RgbFarbe.aus(violett),
            standardFarbe = RgbFarbe.aus(ProfilFarbe.Standard),
            titel = "Profilfarbe auswählen",
            onAbbrechen = {},
            onBestaetigen = {},
        )
    }
}
