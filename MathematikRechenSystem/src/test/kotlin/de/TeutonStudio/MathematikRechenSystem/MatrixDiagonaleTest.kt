package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class MatrixDiagonaleTest {
    @Test
    fun `Hauptdiagonale funktioniert fuer quadratische und rechteckige Matrizen`() {
        val quadratisch = matrix(3, 3)
        val breit = matrix(2, 4)
        val hoch = matrix(4, 2)

        assertEquals(listOf(zahl(1), zahl(5), zahl(9)), matrixDiagonale(quadratisch, MatrixDiagonalArt.HAUPTDIAGONALE).elemente)
        assertEquals(listOf(zahl(1), zahl(6)), matrixDiagonale(breit, MatrixDiagonalArt.HAUPTDIAGONALE).elemente)
        assertEquals(listOf(zahl(1), zahl(4)), matrixDiagonale(hoch, MatrixDiagonalArt.HAUPTDIAGONALE).elemente)
    }

    @Test
    fun `Nebendiagonale ist bei rechteckigen Matrizen rechts oben verankert`() {
        val quadratisch = matrix(3, 3)
        val breit = matrix(2, 4)
        val hoch = matrix(4, 2)

        assertEquals(listOf(zahl(3), zahl(5), zahl(7)), matrixDiagonale(quadratisch, MatrixDiagonalArt.NEBENDIAGONALE).elemente)
        assertEquals(listOf(zahl(4), zahl(7)), matrixDiagonale(breit, MatrixDiagonalArt.NEBENDIAGONALE).elemente)
        assertEquals(listOf(zahl(2), zahl(3)), matrixDiagonale(hoch, MatrixDiagonalArt.NEBENDIAGONALE).elemente)
    }

    @Test
    fun `Einmal-eins ist fuer beide Diagonalarten identisch`() {
        val matrix = Matrix(listOf(listOf(zahl(7))))

        assertEquals(
            matrixDiagonale(matrix, MatrixDiagonalArt.HAUPTDIAGONALE),
            matrixDiagonale(matrix, MatrixDiagonalArt.NEBENDIAGONALE),
        )
    }

    @Test
    fun `Ergebnis bleibt Tupel und erhaelt kartesischen Vertrag`() {
        val diagonale = matrixDiagonale(matrix(2, 3), MatrixDiagonalArt.HAUPTDIAGONALE)
        val vertrag = assertIs<StrukturPruefung.Gueltig<KartesischerTupelVertrag>>(
            diagonale.kartesischerTupelVertrag(),
        ).wert

        assertIs<Tupel>(diagonale)
        assertEquals(2, vertrag.laenge)
        assertEquals(NatürlicheZahlen, vertrag.zahlBereich)
    }

    @Test
    fun `Tensor hoeherer Stufe wird abgewiesen`() {
        val tensor = Tensor(List(3) { 2 }, List(8) { zahl((it + 1).toLong()) })

        val fehler = assertFailsWith<IllegalArgumentException> {
            matrixDiagonale(tensor, MatrixDiagonalArt.HAUPTDIAGONALE)
        }
        assertEquals("Der Eingang ist keine Matrix; erwartet wird ein Tensor zweiter Stufe.", fehler.message)
    }

    private fun matrix(zeilen: Int, spalten: Int): Matrix = Matrix(
        List(zeilen) { zeile ->
            List(spalten) { spalte -> zahl((zeile * spalten + spalte + 1).toLong()) }
        },
    )

    private fun zahl(wert: Long): RationaleZahl = RationaleZahl.von(wert)
}
