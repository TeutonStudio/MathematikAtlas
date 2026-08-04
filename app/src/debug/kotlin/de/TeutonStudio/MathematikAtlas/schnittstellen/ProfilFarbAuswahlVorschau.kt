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
import de.TeutonStudio.MathematikAtlas.speicher.*

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

@Preview(name = "Farbdialog · RGB · Hell", widthDp = 780, heightDp = 900, showBackground = true)
@Composable
private fun FarbAuswahlDialogRgbVorschau() = VorschauDialog(
    FarbEntwurf.von(RgbFarbe.aus(VorschauProfilFarbe), FarbEingabeModus.RGB),
)

@Preview(name = "Farbdialog · HSL", widthDp = 780, heightDp = 900, showBackground = true)
@Composable
private fun FarbAuswahlDialogHslVorschau() = VorschauDialog(
    FarbEntwurf.von(RgbFarbe.aus(VorschauProfilFarbe), FarbEingabeModus.HSL),
)

@Preview(name = "Farbdialog · Lab negativ", widthDp = 780, heightDp = 900, showBackground = true)
@Composable
private fun FarbAuswahlDialogLabVorschau() = VorschauDialog(
    FarbEntwurf.von(RgbFarbe.aus(requireNotNull(ProfilFarbe.parse("#336699"))), FarbEingabeModus.LAB),
)

@Preview(name = "Farbdialog · Lab außerhalb sRGB", widthDp = 780, heightDp = 900, showBackground = true)
@Composable
private fun FarbAuswahlDialogLabGamutVorschau() = VorschauDialog(
    FarbEntwurf.von(RgbFarbe.aus(VorschauProfilFarbe), FarbEingabeModus.LAB)
        .mitLabText(helligkeit = "50", a = "127", b = "127"),
)

@Preview(name = "Farbdialog · CMYK generisch", widthDp = 780, heightDp = 900, showBackground = true)
@Composable
private fun FarbAuswahlDialogCmykVorschau() = VorschauDialog(
    FarbEntwurf.von(RgbFarbe.aus(VorschauProfilFarbe), FarbEingabeModus.CMYK)
        .mitCmykText(cyan = "20", magenta = "65", gelb = "0", schwarz = "10"),
)

@Preview(name = "Farbdialog · CMYK Schwarz", widthDp = 780, heightDp = 900, showBackground = true)
@Composable
private fun FarbAuswahlDialogCmykSchwarzVorschau() = VorschauDialog(
    FarbEntwurf.von(RgbFarbe(0.0, 0.0, 0.0), FarbEingabeModus.CMYK),
)

@Preview(name = "Farbdialog · schmale Breite", widthDp = 360, heightDp = 820, showBackground = true)
@Composable
private fun FarbAuswahlDialogSchmalVorschau() = VorschauDialog(
    FarbEntwurf.von(RgbFarbe.aus(VorschauProfilFarbe), FarbEingabeModus.HSL),
)

@Preview(name = "Farbdialog · ungültige Dezimaleingabe", widthDp = 780, heightDp = 900, showBackground = true)
@Composable
private fun FarbAuswahlDialogUngueltigVorschau() = VorschauDialog(
    FarbEntwurf.von(RgbFarbe.aus(VorschauProfilFarbe), FarbEingabeModus.LAB)
        .mitLabText(a = "-"),
)

@Preview(name = "Farbdialog · Lab · Dunkel", widthDp = 780, heightDp = 900, showBackground = true)
@Composable
private fun FarbAuswahlDialogDunkelVorschau() {
    val violett = requireNotNull(ProfilFarbe.parse("#7C3AED"))
    VorschauDialog(
        entwurf = FarbEntwurf.von(RgbFarbe.aus(violett), FarbEingabeModus.LAB),
        dunkel = true,
        profilFarbe = violett,
    )
}

@Composable
private fun VorschauDialog(
    entwurf: FarbEntwurf,
    dunkel: Boolean = false,
    profilFarbe: ProfilFarbe = VorschauProfilFarbe,
) {
    MathematikAtlasVorschauRahmen(dunkel = dunkel, profilFarbe = profilFarbe) {
        FarbAuswahlDialog(
            offen = true,
            ausgangsFarbe = entwurf.kanonisch,
            standardFarbe = RgbFarbe.aus(ProfilFarbe.Standard),
            titel = "Profilfarbe auswählen",
            onAbbrechen = {},
            onBestaetigen = {},
            initialerEntwurf = entwurf,
        )
    }
}
