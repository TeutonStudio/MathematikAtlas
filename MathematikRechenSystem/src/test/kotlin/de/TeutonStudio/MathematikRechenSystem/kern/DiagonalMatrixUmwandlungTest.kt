package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.*

class DiagonalMatrixUmwandlungTest {
    @Test fun `alle Strukturformen erzeugen identische Diagonalmatrix`() {
        val zahlen = listOf(RationaleZahl.von(2), RationaleZahl.von(3), RationaleZahl.von(5))
        val matrix = diagonalmatrixErzeugen(Tupel(zahlen))
        assertEquals(matrix, diagonalmatrixErzeugen(ZeilenVektor(zahlen)))
        assertEquals(matrix, diagonalmatrixErzeugen(SpaltenVektor(zahlen)))
        assertEquals(RationaleZahl.Null, matrix.zeilen[0][1])
        assertEquals(zahlen[2], matrix.zeilen[2][2])
        assertTrue(istDiagonalmatrix(matrix))
    }

    @Test fun `Rundreise auf Diagonalmatrizen ist umkehrbar`() {
        val zahlen = listOf(RationaleZahl.von(2), RationaleZahl.von(7))
        val matrix = diagonalmatrixErzeugen(SpaltenVektor(zahlen))
        for (ausgabe in listOf("tupel", "zeile", "spalte")) {
            assertEquals(matrix, diagonalmatrixErzeugen(diagonalmatrixAuslesen(matrix, ausgabe)))
        }
    }

    @Test fun `Nichtdiagonale und nichtquadratische Matrizen werden unterschieden`() {
        val a = Matrix(listOf(listOf(RationaleZahl.Eins, RationaleZahl.Eins),
            listOf(RationaleZahl.Null, RationaleZahl.Eins)))
        assertFalse(istDiagonalmatrix(a))
        assertEquals(listOf(RationaleZahl.Eins, RationaleZahl.Eins),
            (diagonalmatrixAuslesen(a) as Tupel).elemente)
        assertFailsWith<IllegalArgumentException> {
            diagonalmatrixAuslesen(Matrix(listOf(listOf(RationaleZahl.Eins, RationaleZahl.Eins))))
        }
    }
}
