package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KnotenInteraktionsModus
import kotlin.test.Test
import kotlin.test.assertEquals

class MathematikKnotenRendererInteraktionTest {
    @Test
    fun `mathematikknoten lassen sich ueber den gesamten inhalt verschieben`() {
        assertEquals(
            KnotenInteraktionsModus.GanzeFlächeZiehbar,
            MathematikKnotenRenderer().interaktionsModus,
        )
    }
}
