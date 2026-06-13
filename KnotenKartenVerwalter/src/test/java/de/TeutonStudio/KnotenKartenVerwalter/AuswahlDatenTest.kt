package de.TeutonStudio.KnotenKartenVerwalter

import com.TeutonStudio.KnotenKartenVerwalter.daten.auswahl.AuswahlDaten
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuswahlDatenTest {
    @Test
    fun neueAuswahlIstLeer() {
        assertTrue(AuswahlDaten().istLeer)
    }

    @Test
    fun knotenKannHinzugefuegtUndEntferntWerden() {
        val auswahl = AuswahlDaten()
            .mitKnoten("a")
            .mitKnoten("b")
            .ohneKnoten("a")

        assertEquals(setOf("b"), auswahl.knotenIds)
        assertFalse(auswahl.istLeer)
    }

    @Test
    fun umschaltenAendertAuswahlzustand() {
        val auswahl = AuswahlDaten()
            .umgeschalteterKnoten("a")
            .umgeschalteterKnoten("a")
            .umgeschalteteVerbindung("v")

        assertTrue(auswahl.knotenIds.isEmpty())
        assertEquals(setOf("v"), auswahl.verbindungIds)
    }
}
