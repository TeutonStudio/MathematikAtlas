package de.TeutonStudio.MathematikAtlas.schnittstellen

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.TeutonStudio.MathematikAtlas.AussagenOperatorDialog

@Preview(
    name = "Aussagenoperator · Adjunktionstabelle",
    widthDp = 760,
    heightDp = 720,
    showBackground = true,
)
@Composable
private fun AussagenOperatorDialogVorschau() {
    val zustand = erinnereVorschauAtlasZustand()
    val knoten = VorschauDaten.knoten(
        art = "mathematik.adjunktion",
        name = "Adjunktion mit drei Aussagen",
        parameter = mapOf("festeEingänge" to "3"),
    )
    MathematikAtlasVorschauRahmen {
        AussagenOperatorDialog(
            zustand = zustand,
            knoten = knoten,
            definitionÖffnen = {},
            schließen = {},
        )
    }
}
