package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals

class UniversellerZahlenRechnerTest {
    @Test
    fun `kanonische fundamentale Zahlmengen werden strukturell inferiert`() {
        val fälle = listOf(
            NatürlicheZahlen to ZahlenRechnerBereich.NATUERLICH,
            NichtnegativeGanzeZahlenSemantik.menge to ZahlenRechnerBereich.NATUERLICH_MIT_NULL,
            GanzeZahlen to ZahlenRechnerBereich.GANZ,
            RationaleZahlen to ZahlenRechnerBereich.RATIONAL,
            ReelleZahlen to ZahlenRechnerBereich.REELL,
            KomplexeZahlen to ZahlenRechnerBereich.KOMPLEX,
            FundamentalerZahlbereich.QUATERNION.alsMenge() to ZahlenRechnerBereich.QUATERNION,
        )

        fälle.forEach { (menge, erwartet) ->
            assertEquals(erwartet, inferiereZahlenRechnerBereich(RationaleZahl.Eins, menge))
        }
    }
}
