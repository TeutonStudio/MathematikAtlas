package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InspektorSichtbarkeitTest {
    @AfterTest
    fun standardWiederherstellen() {
        InspektorSichtbarkeit.öffnen()
    }

    @Test
    fun `Inspector kann geschlossen und wieder geöffnet werden`() {
        InspektorSichtbarkeit.öffnen()
        assertTrue(InspektorSichtbarkeit.offen)

        InspektorSichtbarkeit.schließen()
        assertFalse(InspektorSichtbarkeit.offen)

        InspektorSichtbarkeit.öffnen()
        assertTrue(InspektorSichtbarkeit.offen)
    }
}
