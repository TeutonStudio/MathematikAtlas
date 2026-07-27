package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals

class WahrheitswertBenennungTest {
    @Test
    fun wahrheitswerteHeißenAusschließlichWahrUndLüge() {
        assertEquals(listOf("Wahr", "Lüge"), Wahrheitswert.entries.map { it.name })
        assertEquals(Wahrheitswert.Wahr, WahrheitsKonstante(true).entscheide().wahrheitswert)
        assertEquals(Wahrheitswert.Lüge, WahrheitsKonstante(false).entscheide().wahrheitswert)
    }

    @Test
    fun wahrheitsmengeEnthältWahrUndLüge() {
        val menge = inferiereZielmenge(WahrheitsKonstante(true))
        assertEquals(
            EndlicheMenge(setOf(WahrheitsKonstante(true), WahrheitsKonstante(false))),
            menge,
        )
    }
}
