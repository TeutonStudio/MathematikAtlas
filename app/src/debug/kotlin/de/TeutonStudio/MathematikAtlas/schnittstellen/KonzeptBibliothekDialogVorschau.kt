package de.TeutonStudio.MathematikAtlas.schnittstellen

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikAtlas.KonzeptBibliothekDialog

@Preview(
    name = "Konzeptbibliothek · Fachgebiete",
    widthDp = 1280,
    heightDp = 900,
    showBackground = true,
)
@Composable
private fun KonzeptBibliothekDialogVorschau() {
    val zustand = erinnereVorschauAtlasZustand()
    MathematikAtlasVorschauRahmen {
        KonzeptBibliothekDialog(
            zustand = zustand,
            position = GraphPunkt(420f, 260f),
            vorlagen = zustand.sichtbareVorlagen(),
            onStandardWähler = {},
        )
    }
}
