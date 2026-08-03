package de.TeutonStudio.MathematikAtlas.schnittstellen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.VisuelleGruppenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.VisuelleKnotenGruppeDaten
import de.TeutonStudio.MathematikAtlas.VisuelleGruppenEbene

@Preview(
    name = "Visuelle Gruppe · Lineare Algebra",
    widthDp = 920,
    heightDp = 640,
    showBackground = true,
)
@Composable
private fun VisuelleGruppenEbeneVorschau() {
    val zustand = erinnereVorschauAtlasZustand()
    val erster = VorschauDaten.Matrixdiagonale.copy(position = GraphPunkt(120f, 150f))
    val zweiter = VorschauDaten.ZahlenRechner.copy(position = GraphPunkt(420f, 240f))
    LaunchedEffect(zustand) {
        zustand.editor.ersetzeKarte(
            KartenDaten(
                name = "Lineare Algebra Gruppe",
                knoten = listOf(erster, zweiter),
                visuelleGruppen = listOf(
                    VisuelleKnotenGruppeDaten(
                        id = VisuelleGruppenId("vorschau.gruppe.lineare-algebra"),
                        knotenIds = setOf(erster.id, zweiter.id),
                        titel = "Matrixoperationen und Auswertung",
                        position = GraphPunkt(80f, 90f),
                        größe = GraphGröße(650f, 360f),
                    ),
                ),
            ),
        )
    }
    MathematikAtlasVorschauRahmen {
        VisuelleGruppenEbene(zustand.editor)
    }
}
