package de.TeutonStudio.MathematikAtlas.schnittstellen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.TeutonStudio.MathematikAtlas.ProfilFarbAuswahl
import de.TeutonStudio.MathematikAtlas.speicher.ProfilFarbe

@Preview(
    name = "Profilfarbe · Petrol",
    widthDp = 760,
    heightDp = 940,
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
    name = "Profilfarbe · Violett · Dunkel",
    widthDp = 760,
    heightDp = 940,
    showBackground = true,
)
@Composable
private fun ProfilFarbAuswahlDunkelVorschau() {
    val violett = requireNotNull(ProfilFarbe.parse("#7C3AED"))
    MathematikAtlasVorschauRahmen(dunkel = true, profilFarbe = violett) {
        ProfilFarbAuswahl(
            startFarbe = violett,
            farbeGeaendert = {},
            modifier = Modifier
                .widthIn(max = 700.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        )
    }
}
