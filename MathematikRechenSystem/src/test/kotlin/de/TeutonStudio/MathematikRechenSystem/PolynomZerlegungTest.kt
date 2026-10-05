package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PolynomZerlegungTest {
    private val x = Variable("x")

    @Test
    fun `quadratisches polynom wird aufsteigend zerlegt`() {
        val ausdruck = addition(
            multiplikation(RationaleZahl.von(3), Potenz(x, RationaleZahl.von(2))),
            multiplikation(RationaleZahl.von(2), x),
            RationaleZahl.Eins,
        )

        assertEquals(
            listOf(RationaleZahl.Eins, RationaleZahl.von(2), RationaleZahl.von(3)),
            zerlegeAlsPolynom(ausdruck, x)?.koeffizienten,
        )
    }

    @Test
    fun `fehlende koeffizienten werden mit null aufgefuellt`() {
        val ausdruck = addition(Potenz(x, RationaleZahl.von(3)), RationaleZahl.von(4))
        assertEquals(
            listOf(RationaleZahl.von(4), RationaleZahl.Null, RationaleZahl.Null, RationaleZahl.Eins),
            zerlegeAlsPolynom(ausdruck, x)?.koeffizienten,
        )
    }

    @Test
    fun `produkt zweier linearer terme wird erkannt`() {
        val ausdruck = multiplikation(
            addition(x, RationaleZahl.Eins),
            addition(x, RationaleZahl.von(-1)),
        )
        assertEquals(
            listOf(RationaleZahl.von(-1), RationaleZahl.Null, RationaleZahl.Eins),
            zerlegeAlsPolynom(ausdruck, x)?.koeffizienten,
        )
    }

    @Test
    fun `symbolische koeffizienten unabhaengig von x bleiben erhalten`() {
        val a = Variable("a")
        val ausdruck = addition(multiplikation(a, x), RationaleZahl.Eins)
        assertEquals(listOf(RationaleZahl.Eins, a), zerlegeAlsPolynom(ausdruck, x)?.koeffizienten)
    }

    @Test
    fun `nichtpolynomiale und negative potenzen werden abgelehnt`() {
        assertNull(zerlegeAlsPolynom(Sinus(x), x))
        assertNull(zerlegeAlsPolynom(Potenz(x, RationaleZahl.von(-1)), x))
    }
}
