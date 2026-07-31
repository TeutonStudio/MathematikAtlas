package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SymmetrischeDifferenzTest {
    @Test
    fun `wertet endliche Mengen exakt aus`() {
        val links = EndlicheMenge(setOf(zahl(1), zahl(2)))
        val rechts = EndlicheMenge(setOf(zahl(2), zahl(3)))

        assertEquals(
            EndlicheMenge(setOf(zahl(1), zahl(3))),
            symmetrischeDifferenz(links, rechts),
        )
    }

    @Test
    fun `normalisiert leere und identische Mengen`() {
        val menge = BenannteMenge("A")

        assertEquals(menge, symmetrischeDifferenz(menge, LeereMenge))
        assertEquals(menge, symmetrischeDifferenz(LeereMenge, menge))
        assertEquals(LeereMenge, symmetrischeDifferenz(menge, menge))
    }

    @Test
    fun `ordnet symbolische Operanden kanonisch`() {
        val a = BenannteMenge("A")
        val b = BenannteMenge("B")
        val ergebnis = assertIs<SymmetrischeDifferenz>(symmetrischeDifferenz(b, a))

        assertEquals(a, ergebnis.links)
        assertEquals(b, ergebnis.rechts)
        assertEquals("A \\triangle B", ergebnis.zuLatex())
    }

    private fun zahl(wert: Int): RationaleZahl = RationaleZahl.parse(wert.toString())
}
