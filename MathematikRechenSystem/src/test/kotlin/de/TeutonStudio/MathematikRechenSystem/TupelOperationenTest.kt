package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals

class TupelOperationenTest {
    private fun z(wert: Int) = RationaleZahl.parse(wert.toString())

    @Test
    fun `Tupel werden geordnet und nur eine Ebene zusammengeführt`() {
        val verschachtelt = Tupel(listOf(z(7)))
        val links = Tupel(listOf(z(1), verschachtelt))
        val rechts = Tupel(listOf(z(2), z(3)))

        assertEquals(
            Tupel(listOf(z(1), verschachtelt, z(2), z(3))),
            ergänzeTupel(listOf(links, rechts)),
        )
    }

    @Test
    fun `Leere Tupel verschieben die nachfolgenden Komponenten nicht falsch`() {
        val x = Tupel(listOf(z(1), z(2)))
        val y = Tupel(emptyList())
        val z = Tupel(listOf(z(3)))

        assertEquals(
            Tupel(listOf(this.z(1), this.z(2), this.z(3))),
            ergänzeTupel(listOf(x, y, z)),
        )
    }

    @Test
    fun `Elementmodus lässt ein Tupel als einzelnes Element verschachtelt`() {
        val basis = Tupel(listOf(z(1)))
        val element = Tupel(listOf(z(2), z(3)))

        assertEquals(
            Tupel(listOf(z(1), element)),
            ergänzeTupelUmElemente(basis, listOf(element)),
        )
    }
}
