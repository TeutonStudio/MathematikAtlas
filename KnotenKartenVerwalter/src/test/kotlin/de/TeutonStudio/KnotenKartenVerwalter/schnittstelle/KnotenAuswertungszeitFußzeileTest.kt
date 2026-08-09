package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import kotlin.test.Test
import kotlin.test.assertEquals

class KnotenAuswertungszeitFußzeileTest {
    @Test fun `Auswertungsdauer wird kompakt formatiert`() {
        assertEquals("<1 µs", formatiereAuswertungsDauerNanos(999L))
        assertEquals("12,4 ms", formatiereAuswertungsDauerNanos(12_400_000L))
        assertEquals("1,24 s", formatiereAuswertungsDauerNanos(1_240_000_000L))
    }
}
