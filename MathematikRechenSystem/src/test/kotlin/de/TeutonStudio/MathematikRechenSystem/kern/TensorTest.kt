package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.*

class TensorTest {
    private fun zahlen(anzahl: Int) = List(anzahl) { RationaleZahl.von(it.toLong()) }

    @Test fun `Achsenpermutation ändert Dimensionen und Komponenten`() {
        val tensor = Tensor(listOf(2, 3, 4), zahlen(24))
        val permutiert = tensor.permutiereAchsen(listOf(1, 0, 2))

        assertEquals(listOf(3, 2, 4), permutiert.dimensionen)
        assertEquals(tensor.wertAn(listOf(1, 2, 3)), permutiert.wertAn(listOf(2, 1, 3)))
    }

    @Test fun `Zweifache Achsenvertauschung ergibt Ausgangstensor`() {
        val tensor = Tensor(listOf(2, 3, 4), zahlen(24))
        assertEquals(tensor, tensor.permutiereAchsen(listOf(1, 0, 2)).permutiereAchsen(listOf(1, 0, 2)))
    }

    @Test fun `Ungültige Permutationen werden abgelehnt`() {
        val tensor = Tensor(listOf(2, 3, 4), zahlen(24))
        assertFailsWith<IllegalArgumentException> { tensor.permutiereAchsen(listOf(0, 0, 2)) }
        assertFailsWith<IllegalArgumentException> { tensor.permutiereAchsen(listOf(0, 1)) }
    }

    @Test fun `Parser akzeptiert nur vollständige Permutationen`() {
        assertEquals(listOf(1, 0, 2), parseTensorPermutationOderNull("1, 0, 2", 3))
        assertNull(parseTensorPermutationOderNull("1,0", 3))
        assertNull(parseTensorPermutationOderNull("1,1,2", 3))
        assertNull(parseTensorPermutationOderNull("1,,2", 3))
        assertNull(parseTensorPermutationOderNull("a,0,2", 3))
    }

    @Test fun `Veraltete Konfiguration fällt auf Standard zurück`() {
        assertEquals(listOf(1, 0, 2), parseTensorPermutation("1,0", 3))
        assertEquals(listOf(1, 0, 2, 3), standardTensorPermutation(4))
    }
}
