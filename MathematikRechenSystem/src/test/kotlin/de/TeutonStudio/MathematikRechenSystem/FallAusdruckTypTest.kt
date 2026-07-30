package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class FallAusdruckTypTest {
    private val bedingung = Gleichheit(Variable("x"), RationaleZahl.Null)

    @Test fun `Zahlzweige erzeugen einen Zahlfall`() {
        val fall = FallAusdruck(RationaleZahl.Eins, bedingung, RationaleZahl.von(2))

        assertIs<ZahlFallAusdruck>(fall)
        assertEquals(NatürlicheZahlen, inferiereZielmenge(fall))
    }

    @Test fun `Mengenzweige erzeugen einen Mengenfall`() {
        val fall = FallAusdruck(
            EndlicheMenge(setOf(RationaleZahl.Eins)),
            bedingung,
            ReellesIntervall(
                links = RationaleZahl.Null,
                linksOffen = false,
                rechts = RationaleZahl.Eins,
                rechtsOffen = false,
            ),
        )

        assertIs<MengenFallAusdruck>(fall)
        assertTrue(inferiereZielmenge(fall) is MengenAusdruck)
    }

    @Test fun `Aussagezweige erzeugen einen Aussagenfall`() {
        val fall = FallAusdruck(WahrheitsKonstante(true), bedingung, WahrheitsKonstante(false))

        assertIs<AussagenFallAusdruck>(fall)
        assertNull(fall.entscheide().wahrheitswert)
    }

    @Test fun `Verschiedene Zweigarten bleiben allgemeiner Fall`() {
        val fall = FallAusdruck(RationaleZahl.Eins, bedingung, EndlicheMenge(setOf(RationaleZahl.Eins)))

        assertIs<FallAusdruck>(fall)
        assertFalse(fall is ZahlAusdruck)
        assertFalse(fall is MengenAusdruck)
        assertFalse(fall is Aussage)
    }
}
