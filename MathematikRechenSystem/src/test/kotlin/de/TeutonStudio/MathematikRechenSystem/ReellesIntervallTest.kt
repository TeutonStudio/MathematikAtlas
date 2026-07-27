package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ReellesIntervallTest {
    @Test
    fun `rationale Grenzen werden kanonisch normalisiert`() {
        val intervall = assertIs<ReellesIntervall>(reellesIntervall(RationaleZahl.von(1), RationaleZahl.von(3)))

        assertEquals("\\left[1,3\\right]", intervall.zuLatex())
        assertEquals(LeereMenge, reellesIntervall(RationaleZahl.von(3), RationaleZahl.von(1)))
        assertEquals(EndlicheMenge(setOf(RationaleZahl.von(2))), reellesIntervall(RationaleZahl.von(2), RationaleZahl.von(2)))
    }

    @Test
    fun `rationale Mitgliedschaft im Intervall wird exakt entschieden`() {
        val intervall = ReellesIntervall(RationaleZahl.von(1), RationaleZahl.von(3))

        listOf(1L, 2L, 3L).forEach { wert ->
            assertEquals(Wahrheitswert.Wahr, ElementBeziehung(RationaleZahl.von(wert), intervall).entscheide().wahrheitswert)
        }
        listOf(0L, 4L).forEach { wert ->
            assertEquals(Wahrheitswert.Falsch, ElementBeziehung(RationaleZahl.von(wert), intervall).entscheide().wahrheitswert)
        }
    }

    @Test
    fun `symbolische Grenzen bleiben substituierbar und variablenhaltig`() {
        val unten = Variable("a")
        val symbolisch = ReellesIntervall(unten, RationaleZahl.von(2))

        assertEquals(setOf(unten), symbolisch.freieVariablen())
        assertEquals(EndlicheMenge(setOf(RationaleZahl.von(2))), ersetze(symbolisch, mapOf("a" to RationaleZahl.von(2))))
    }
}
