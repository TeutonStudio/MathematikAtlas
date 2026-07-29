package de.TeutonStudio.MathematikAtlas

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DarstellungsModusTest {
    @Test
    fun `System folgt der Systemdarstellung`() {
        assertFalse(DarstellungsModus.System.istDunkel(systemIstDunkel = false))
        assertTrue(DarstellungsModus.System.istDunkel(systemIstDunkel = true))
    }

    @Test
    fun `Manuelle Auswahl überschreibt die Systemdarstellung`() {
        assertFalse(DarstellungsModus.Hell.istDunkel(systemIstDunkel = true))
        assertTrue(DarstellungsModus.Dunkel.istDunkel(systemIstDunkel = false))
    }

    @Test
    fun `Unbekannter Speicherwert fällt auf System zurück`() {
        assertEquals(DarstellungsModus.System, DarstellungsModus.ausGespeichert(null))
        assertEquals(DarstellungsModus.System, DarstellungsModus.ausGespeichert("Unbekannt"))
        assertEquals(DarstellungsModus.Dunkel, DarstellungsModus.ausGespeichert("Dunkel"))
    }
}
