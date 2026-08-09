package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals

class TupelOperationenTest {
    @Test
    fun `Tupel werden geordnet und nur eine Ebene zusammengeführt`() {
        val verschachtelt = Tupel(listOf(RationaleZahl.of(7)))
        val links = Tupel(listOf(RationaleZahl.of(1), verschachtelt))
        val rechts = Tupel(listOf(RationaleZahl.of(2), RationaleZahl.of(3)))

        assertEquals(
            Tupel(listOf(RationaleZahl.of(1), verschachtelt, RationaleZahl.of(2), RationaleZahl.of(3))),
            ergänzeTupel(listOf(links, rechts)),
        )
    }

    @Test
    fun `Leere Tupel verschieben die nachfolgenden Komponenten nicht falsch`() {
        val x = Tupel(listOf(RationaleZahl.of(1), RationaleZahl.of(2)))
        val y = Tupel(emptyList())
        val z = Tupel(listOf(RationaleZahl.of(3)))

        assertEquals(
            Tupel(listOf(RationaleZahl.of(1), RationaleZahl.of(2), RationaleZahl.of(3))),
            ergänzeTupel(listOf(x, y, z)),
        )
    }

    @Test
    fun `Elementmodus lässt ein Tupel als einzelnes Element verschachtelt`() {
        val basis = Tupel(listOf(RationaleZahl.of(1)))
        val element = Tupel(listOf(RationaleZahl.of(2), RationaleZahl.of(3)))

        assertEquals(
            Tupel(listOf(RationaleZahl.of(1), element)),
            ergänzeTupelUmElemente(basis, listOf(element)),
        )
    }
}
