package de.TeutonStudio.MathematikAtlas.schnittstellen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikAtlas.KnotenAuswahlDialog
import de.TeutonStudio.MathematikAtlas.KnotenWählerModus
import de.TeutonStudio.MathematikAtlas.KnotenWählerModusSpeicher

@Preview(
    name = "Knoten einfügen · Standardliste · Matrixsuche",
    widthDp = 900,
    heightDp = 820,
    showBackground = true,
)
@Composable
private fun KnotenAuswahlStandardVorschau() {
    val context = LocalContext.current
    remember(context) {
        KnotenWählerModusSpeicher(context).apply { speichere(KnotenWählerModus.Standard) }
    }
    val zustand = erinnereVorschauAtlasZustand()
    LaunchedEffect(zustand) { zustand.setzeSuchText("Matrix") }
    MathematikAtlasVorschauRahmen {
        KnotenAuswahlDialog(zustand, GraphPunkt(320f, 220f))
    }
}

@Preview(
    name = "Knoten einfügen · Konzeptbibliothek",
    widthDp = 1220,
    heightDp = 860,
    showBackground = true,
)
@Composable
private fun KnotenAuswahlKonzeptbibliothekVorschau() {
    val context = LocalContext.current
    remember(context) {
        KnotenWählerModusSpeicher(context).apply { speichere(KnotenWählerModus.Konzeptbibliothek) }
    }
    val zustand = erinnereVorschauAtlasZustand()
    LaunchedEffect(zustand) { zustand.setzeSuchText("") }
    MathematikAtlasVorschauRahmen {
        KnotenAuswahlDialog(zustand, GraphPunkt(480f, 300f))
    }
}
