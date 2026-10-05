package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VisuelleGruppenAuswertungszeitTest {
    @Test
    fun `kollektive Auswertungsdauer summiert ausschließlich Gruppenmitglieder`() {
        val a = KnotenId("a")
        val b = KnotenId("b")
        val außerhalb = KnotenId("außerhalb")
        val dauern = mapOf(a to 2_000L, b to 3_500L, außerhalb to 100_000L)

        assertEquals(
            5_500L,
            kollektiveAuswertungsDauerNanos(setOf(a, b)) { dauern[it] },
        )
    }

    @Test
    fun `fehlende Laufzeiten werden ignoriert und vollständig unbekannte Gruppen bleiben ohne Anzeige`() {
        val bekannt = KnotenId("bekannt")
        val unbekannt = KnotenId("unbekannt")

        assertEquals(
            7_000L,
            kollektiveAuswertungsDauerNanos(setOf(bekannt, unbekannt)) {
                if (it == bekannt) 7_000L else null
            },
        )
        assertNull(kollektiveAuswertungsDauerNanos(setOf(unbekannt)) { null })
    }
}
