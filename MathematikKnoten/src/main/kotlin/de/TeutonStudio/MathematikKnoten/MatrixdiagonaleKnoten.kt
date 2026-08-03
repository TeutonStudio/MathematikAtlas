package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.MatrixDiagonalArt
import de.TeutonStudio.MathematikRechenSystem.kern.Tensorartig
import de.TeutonStudio.MathematikRechenSystem.kern.matrixDiagonale

const val MATRIXDIAGONALE_ART = "mathematik.matrixdiagonale"
const val MATRIXDIAGONALE_ART_PARAMETER = "diagonalArt"

object MatrixdiagonaleKnotenVorlagen {
    val Matrixdiagonale = KnotenVorlage(
        art = MATRIXDIAGONALE_ART,
        name = "Matrixdiagonale",
        kategorie = "Matrizen",
        beschreibung = "Liest die Haupt- oder rechts oben verankerte Nebendiagonale als kartesisches Tupel.",
        standardGröße = GraphGröße(270f, 115f),
        anschlüsse = listOf(
            AnschlussDaten(
                name = "matrix",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = MathematikAnschlussArten.Matrix.id,
            ),
            AnschlussDaten(
                name = "diagonale",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.Tupel.id,
            ),
        ),
        standardParameter = mapOf(
            MATRIXDIAGONALE_ART_PARAMETER to MatrixDiagonalArt.HAUPTDIAGONALE.parameterWert,
        ),
    )

    val alle = listOf(Matrixdiagonale) +
        SpurKnotenVorlagen.alle +
        StrukturRechnerKnotenVorlagen.alle.filterNot { it.art in historischeSkalarproduktArten } +
        EinheitsvektorKnotenVorlagen.alle +
        VektorRechnerKnotenVorlagen.standard
}

internal fun MathematikAuswerterRegister.registriereMatrixdiagonale() {
    registriere(MATRIXDIAGONALE_ART) { kontext ->
        val eingang = kontext.eingänge["matrix"] ?: error("Matrixeingang fehlt.")
        val matrix = eingang.objekt as? Tensorartig ?: error("Der Eingang ist keine Matrix.")
        require(matrix.tensorStufe == 2) {
            "Ein Tensor höherer Stufe benötigt eine explizite Matrixkonversion."
        }

        val gespeichert = kontext.knoten.parameter[MATRIXDIAGONALE_ART_PARAMETER]
        val art = MatrixDiagonalArt.vonParameter(gespeichert)
        val warnungen = if (
            gespeichert != null && MatrixDiagonalArt.ausParameterOderNull(gespeichert) == null
        ) {
            listOf("Die gespeicherte Diagonalart ist unbekannt; Hauptdiagonale wurde als Rückfall verwendet.")
        } else {
            emptyList()
        }
        val annahmen = kontext.eingänge.values.flatMap { it.annahmen }.toSet()

        KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "diagonale" to BedingterWert(matrixDiagonale(matrix, art), annahmen),
            ),
            eingänge = kontext.eingänge,
            warnungen = warnungen,
        )
    }
}
