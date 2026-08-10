package de.TeutonStudio.MathematikKnoten

import kotlin.test.Test
import kotlin.test.assertEquals

class GeometrieAchsenIndexTest {
    @Test
    fun `ganzzahlige Schrittweite wird beim Herauszoomen ausgeduennt`() {
        assertEquals(1, geometrieGanzzahlSchritt(80.0))
        assertEquals(2, geometrieGanzzahlSchritt(30.0))
        assertEquals(10, geometrieGanzzahlSchritt(6.0))
    }
}
