package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class MatrixStrukturKonvertierungTest {
    @Test
    fun `zweiachsiger Tensor wird zeilenweise als Matrix gelesen`() {
        val werte = (1L..6L).map(RationaleZahl::von)
        val tensor = Tensor(listOf(2, 3), werte)
        val matrix = matrixAusZweiachsigerStruktur(tensor)
        assertEquals(2, matrix.zeilenAnzahl)
        assertEquals(3, matrix.spaltenAnzahl)
        assertEquals(listOf(werte.subList(0, 3), werte.subList(3, 6)), matrix.zeilen)
    }

    @Test
    fun `Matrix bleibt unveraendert und falscher Rang wird abgewiesen`() {
        val matrix = Matrix(listOf(listOf(RationaleZahl.Eins)))
        assertSame(matrix, matrixAusZweiachsigerStruktur(matrix))
        assertFailsWith<IllegalArgumentException> {
            matrixAusZweiachsigerStruktur(Tensor(listOf(2, 2, 2), (1L..8L).map(RationaleZahl::von)))
        }
        assertFailsWith<IllegalArgumentException> {
            matrixAusZweiachsigerStruktur(RationaleZahl.Eins)
        }
    }
}
