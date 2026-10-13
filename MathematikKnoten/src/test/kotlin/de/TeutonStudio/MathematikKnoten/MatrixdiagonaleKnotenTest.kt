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
import kotlin.test.assertFalse

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
        assertTrue(ergebnis.warnungen.any { it.contains("Nebendiagonaleinträge") })
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
        assertTrue(ergebnis.warnungen.any { it.contains("Unbekannte Diagonalart") })
    }

    @Test
    fun `Erzeugen und Auslesen behalten Vektorwerte und Anschlusssignaturen`() {
        val basis = MatrixdiagonaleKnotenVorlagen.Matrixdiagonale.erzeuge(GraphPunkt.Zero)
        val erzeugen = konfiguriereMatrixdiagonale(basis, MATRIXDIAGONALE_ERZEUGEN)
        assertEquals(listOf("werte", "matrix"), erzeugen.anschlüsse.map { it.name })
        assertEquals(basis.anschlüsse.map { it.id }, erzeugen.anschlüsse.map { it.id })
        assertEquals(MathematikAnschlussArten.Matrix.id, erzeugen.anschlüsse.last().art)
        val werte = SpaltenVektor(listOf(zahl(3), zahl(2)))
        val matrix = auswerter.auswerten(
            KnotenAuswertungsKontext(erzeugen, mapOf("werte" to BedingterWert(werte)), RechenKontext())
        ).ausgaben.getValue("matrix").objekt as Matrix
        assertEquals(Matrix(listOf(listOf(zahl(3), zahl(0)), listOf(zahl(0), zahl(2)))), matrix)
        val auslesen = konfiguriereMatrixdiagonale(erzeugen, MATRIXDIAGONALE_AUSLESEN, "spalte")
        assertEquals(basis.anschlüsse.map { it.id }, auslesen.anschlüsse.map { it.id })
        assertEquals(werte, auswerter.auswerten(
            KnotenAuswertungsKontext(auslesen, mapOf("matrix" to BedingterWert(matrix)), RechenKontext())
        ).ausgaben.getValue("diagonale").objekt)
    }

    @Test
    fun `Nichtdiagonale Matrix meldet Informationsverlust`() {
        val k = MatrixdiagonaleKnotenVorlagen.Matrixdiagonale.erzeuge(GraphPunkt.Zero)
        val matrix = Matrix(listOf(listOf(zahl(1), zahl(7)), listOf(zahl(8), zahl(2))))
        val result = auswerter.auswerten(KnotenAuswertungsKontext(k,
            mapOf("matrix" to BedingterWert(matrix)), RechenKontext()))
        assertTrue(result.warnungen.any { it.contains("umkehrbar") })
    }

    private fun zahl(wert: Long): RationaleZahl = RationaleZahl.von(wert)
}
