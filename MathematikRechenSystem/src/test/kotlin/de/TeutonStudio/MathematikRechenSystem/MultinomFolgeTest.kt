package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.Potenz
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.Variable
import de.TeutonStudio.MathematikRechenSystem.kern.multinomFolge
import de.TeutonStudio.MathematikRechenSystem.kern.polynomAusKoeffizienten
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class MultinomFolgeTest {
    @Test
    fun `Multinomfolge beginnt bei Exponent null und erhält Reihenfolge`() {
        val x = Variable("x")
        val folge = multinomFolge(x, 3)

        assertEquals(4, folge.size)
        assertEquals(RationaleZahl.Eins, folge[0])
        assertEquals(x, folge[1])
        assertIs<Potenz>(folge[2])
        assertIs<Potenz>(folge[3])
    }

    @Test
    fun `Dimension null ergibt nur das konstante Monom eins`() {
        assertEquals(listOf(RationaleZahl.Eins), multinomFolge(RationaleZahl.von(7), 0))
    }

    @Test
    fun `negative Dimension wird abgelehnt`() {
        assertFailsWith<IllegalArgumentException> { multinomFolge(RationaleZahl.von(2), -1) }
    }

    @Test
    fun `Polynom verwendet dieselbe Monomfolge auch numerisch`() {
        val koeffizienten = listOf(RationaleZahl.von(2), RationaleZahl.von(-3), RationaleZahl.von(5))
        assertEquals(RationaleZahl.von(16), polynomAusKoeffizienten(koeffizienten, RationaleZahl.von(2)))
    }
}
