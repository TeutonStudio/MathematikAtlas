package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MultinomTest {
    @Test
    fun `Multinomfolge beginnt bei null und endet bei dim`() {
        val x = Variable("x")
        assertEquals(
            listOf(
                RationaleZahl.Eins,
                x,
                Potenz(x, RationaleZahl.von(2)),
                Potenz(x, RationaleZahl.von(3)),
            ),
            multinomFolge(x, 3),
        )
    }

    @Test
    fun `Konkretes Argument wird durch bestehende Vereinfachung ausgewertet`() {
        assertEquals(
            listOf(
                RationaleZahl.Eins,
                RationaleZahl.von(2),
                RationaleZahl.von(4),
                RationaleZahl.von(8),
            ),
            multinomFolge(RationaleZahl.von(2), 3),
        )
    }

    @Test
    fun `Dimension null erzeugt genau das konstante Monom`() {
        assertEquals(listOf(RationaleZahl.Eins), multinomFolge(Variable("x"), 0))
    }

    @Test
    fun `Negative Dimension wird abgelehnt`() {
        assertFailsWith<IllegalArgumentException> { multinomFolge(Variable("x"), -1) }
    }
}
