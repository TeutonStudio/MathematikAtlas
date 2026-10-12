package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.math.abs
import kotlin.test.*

class RichtungsWinkelTest {
    @Test fun `in R1 entstehen beide Orientierungen`() {
        assertEquals(listOf(1.0), richtungAusHalbkreiswinkeln(1, emptyList()))
        assertEquals(listOf(-1.0), richtungAusHalbkreiswinkeln(-1, emptyList()))
    }
    @Test fun `in R2 entsteht voller Kreis mit Vorzeichen`() {
        assertEquals(listOf(0.0, 1.0), richtungAusHalbkreiswinkeln(1, listOf(90.0)))
        assertEquals(listOf(0.0, -1.0), richtungAusHalbkreiswinkeln(-1, listOf(90.0)))
        val links = richtungAusHalbkreiswinkeln(1, listOf(135.0))
        assertTrue(links[0] < 0.0 && links[1] > 0.0)
    }
    @Test fun `in R3 sind beide Pole erreichbar`() {
        assertEquals(listOf(0.0, 0.0, 1.0), richtungAusHalbkreiswinkeln(1, listOf(45.0, 90.0)))
        assertEquals(listOf(0.0, 0.0, -1.0), richtungAusHalbkreiswinkeln(-1, listOf(45.0, 90.0)))
    }
    @Test fun `Dimensionen und Norm bleiben korrekt`() {
        val v = richtungAusHalbkreiswinkeln(1, listOf(20.0, 55.0, 120.0))
        assertEquals(4, v.size)
        assertTrue(abs(v.sumOf { it * it } - 1.0) < 1e-12)
    }
    @Test fun `ungueltige Winkel werden abgewiesen`() {
        assertFailsWith<IllegalArgumentException> { richtungAusHalbkreiswinkeln(0, emptyList()) }
        assertFailsWith<IllegalArgumentException> { richtungAusHalbkreiswinkeln(1, listOf(180.0)) }
        assertFailsWith<IllegalArgumentException> { richtungAusHalbkreiswinkeln(1, listOf(-1.0)) }
        assertFailsWith<IllegalArgumentException> { richtungAusHalbkreiswinkeln(1, listOf(Double.NaN)) }
    }
}
