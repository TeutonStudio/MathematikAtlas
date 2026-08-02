package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MatrixdiagonaleKnotenTest {
    private val auswerter = GesamterMathematikAuswerter.erzeugeRegister().finde(MATRIXDIAGONALE_ART)!!

    @Test
    fun `Vorlage ist im Erstellen-Dialog mit Matrixeingang und Tupelausgang verfuegbar`() {
        val vorlage = alleMathematikKnotenVorlagen().single { it.art == MATRIXDIAGONALE_ART }
        val knoten = vorlage.erzeuge(GraphPunkt.Zero)

        assertEquals(MatrixDiagonalArt.HAUPTDIAGONALE.parameterWert, knoten.parameter[MATRIXDIAGONALE_ART_PARAMETER])
        assertEquals(
            MathematikAnschlussArten.Matrix.id,
            knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Eingang }.art,
        )
        assertEquals(
            MathematikAnschlussArten.Tupel.id,
            knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }.art,
        )
    }

    @Test
    fun `Standard ist Hauptdiagonale und Ausgang traegt kartesischen Tupelvertrag`() {
        val knoten = MatrixdiagonaleKnotenVorlagen.Matrixdiagonale.erzeuge(GraphPunkt.Zero)
        val matrix = Matrix(
            listOf(
                listOf(zahl(1), zahl(2), zahl(3)),
                listOf(zahl(4), zahl(5), zahl(6)),
            ),
        )

        val ergebnis = auswerter.auswerten(
            KnotenAuswertungsKontext(knoten, mapOf("matrix" to BedingterWert(matrix)), RechenKontext()),
        )
        val tupel = assertIs<Tupel>(ergebnis.ausgaben.getValue("diagonale").objekt)
        val vertrag = assertIs<StrukturPruefung.Gueltig<KartesischerTupelVertrag>>(
            tupel.kartesischerTupelVertrag(),
        ).wert

        assertEquals(listOf(zahl(1), zahl(5)), tupel.elemente)
        assertEquals(2, vertrag.laenge)
        assertTrue(ergebnis.warnungen.isEmpty())
    }

    @Test
    fun `Persistierter Wechsel liest rechteckige Nebendiagonale`() {
        val basis = MatrixdiagonaleKnotenVorlagen.Matrixdiagonale.erzeuge(GraphPunkt.Zero)
        val knoten = basis.copy(
            parameter = basis.parameter + (
                MATRIXDIAGONALE_ART_PARAMETER to MatrixDiagonalArt.NEBENDIAGONALE.parameterWert
            ),
        )
        val matrix = Matrix(
            listOf(
                listOf(zahl(1), zahl(2), zahl(3), zahl(4)),
                listOf(zahl(5), zahl(6), zahl(7), zahl(8)),
            ),
        )

        val ergebnis = auswerter.auswerten(
            KnotenAuswertungsKontext(knoten, mapOf("matrix" to BedingterWert(matrix)), RechenKontext()),
        )

        assertEquals(
            listOf(zahl(4), zahl(7)),
            assertIs<Tupel>(ergebnis.ausgaben.getValue("diagonale").objekt).elemente,
        )
    }

    @Test
    fun `Unbekannte gespeicherte Diagonalart faellt mit Diagnose zurueck`() {
        val basis = MatrixdiagonaleKnotenVorlagen.Matrixdiagonale.erzeuge(GraphPunkt.Zero)
        val knoten = basis.copy(parameter = basis.parameter + (MATRIXDIAGONALE_ART_PARAMETER to "historisch"))
        val matrix = Matrix(listOf(listOf(zahl(1), zahl(2)), listOf(zahl(3), zahl(4))))

        val ergebnis = auswerter.auswerten(
            KnotenAuswertungsKontext(knoten, mapOf("matrix" to BedingterWert(matrix)), RechenKontext()),
        )

        assertEquals(listOf(zahl(1), zahl(4)), assertIs<Tupel>(ergebnis.ausgaben.getValue("diagonale").objekt).elemente)
        assertNotNull(ergebnis.warnungen.singleOrNull())
    }

    private fun zahl(wert: Long): RationaleZahl = RationaleZahl.von(wert)
}
