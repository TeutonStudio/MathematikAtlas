package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.METHODEN_ARGUMENTE_ART
import de.TeutonStudio.MathematikKnoten.katalog.StandardMathematikAuswerterPakete
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MathematikAuswerterPaketeTest {
    @Test
    fun `Registrierungspakete besitzen eindeutige Namen`() {
        val namen = StandardMathematikAuswerterPakete.alle.map { it.name }
        assertEquals(namen.size, namen.distinct().size)
    }

    @Test
    fun `Verfeinerungen bleiben nach den Basisdomänen`() {
        val alle = StandardMathematikAuswerterPakete.alle.map { it.name }
        val letzteBasis = StandardMathematikAuswerterPakete.basis.last().name
        val ersteVerfeinerung = StandardMathematikAuswerterPakete.verfeinerungen.first().name

        assertTrue(alle.indexOf(letzteBasis) < alle.indexOf(ersteVerfeinerung))
        assertEquals("konsolidierte-knoten", ersteVerfeinerung)
        assertEquals("polynom-multinom-vertrag", StandardMathematikAuswerterPakete.verfeinerungen.last().name)
    }

    @Test
    fun `Gesamtregister enthält zentrale konsolidierte Auswerter`() {
        val register = GesamterMathematikAuswerter.erzeugeRegister()

        assertNotNull(register.finde(ZAHLENRECHNER_ART))
        assertNotNull(register.finde(VEKTOR_KONSTRUKTOR_ART))
        assertNotNull(register.finde(MULTINOMVEKTOR_ART))
        assertNotNull(register.finde(METHODEN_ARGUMENTE_ART))
    }
}
