package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ReellesIntervallTest {
    private fun z(wert: Long) = RationaleZahl.von(wert)

    @Test
    fun `alle vier Randkombinationen verwenden deutsche Intervallschreibweise`() {
        assertEquals("\\mathopen{[}1,3\\mathclose{]}", assertIs<ReellesIntervall>(reellesIntervall(z(1), false, z(3), false)).zuLatex())
        assertEquals("\\mathopen{]}1,3\\mathclose{]}", assertIs<ReellesIntervall>(reellesIntervall(z(1), true, z(3), false)).zuLatex())
        assertEquals("\\mathopen{[}1,3\\mathclose{[}", assertIs<ReellesIntervall>(reellesIntervall(z(1), false, z(3), true)).zuLatex())
        assertEquals("\\mathopen{]}1,3\\mathclose{[}", assertIs<ReellesIntervall>(reellesIntervall(z(1), true, z(3), true)).zuLatex())
    }

    @Test
    fun `rationale Grenzfälle werden nach Offenheit normalisiert`() {
        assertEquals(LeereMenge, reellesIntervall(z(3), false, z(1), false))
        assertEquals(EndlicheMenge(setOf(z(2))), reellesIntervall(z(2), false, z(2), false))
        assertEquals(LeereMenge, reellesIntervall(z(2), true, z(2), false))
        assertEquals(LeereMenge, reellesIntervall(z(2), false, z(2), true))
        assertEquals(LeereMenge, reellesIntervall(z(2), true, z(2), true))
    }

    @Test
    fun `rationale Mitgliedschaft berücksichtigt beide Randarten`() {
        val offen = ReellesIntervall(z(1), true, z(3), true)
        val geschlossen = ReellesIntervall(z(1), false, z(3), false)

        assertEquals(Wahrheitswert.Lüge, ElementBeziehung(z(1), offen).entscheide().wahrheitswert)
        assertEquals(Wahrheitswert.Wahr, ElementBeziehung(z(2), offen).entscheide().wahrheitswert)
        assertEquals(Wahrheitswert.Lüge, ElementBeziehung(z(3), offen).entscheide().wahrheitswert)
        assertEquals(Wahrheitswert.Wahr, ElementBeziehung(z(1), geschlossen).entscheide().wahrheitswert)
        assertEquals(Wahrheitswert.Wahr, ElementBeziehung(z(3), geschlossen).entscheide().wahrheitswert)
    }

    @Test
    fun `symbolische Grenzen bleiben substituierbar und variablenhaltig`() {
        val links = Variable("a")
        val symbolisch = ReellesIntervall(links, true, z(2), false)

        assertEquals(setOf(links), symbolisch.freieVariablen())
        assertEquals(LeereMenge, ersetze(symbolisch, mapOf("a" to z(2))))
    }
}
