package de.TeutonStudio.MathematikAtlas.speicher

import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KartenOrdnungPapierkorbTest {
    @Test
    fun `Ordnerbaum liefert alle Karten und kann geschlossen entfernt werden`() {
        val a = KartenId("a")
        val b = KartenId("b")
        val c = KartenId("c")
        val ordnung = KartenOrdnung()
            .mitKarteInOrdner(a, listOf("Analysis"))
            .mitKarteInOrdner(b, listOf("Analysis", "Ableitung"))
            .mitKarteInOrdner(c, listOf("Algebra"))

        assertEquals(setOf(a, b), ordnung.kartenUnter(listOf("Analysis")))
        val ohneAnalysis = ordnung.ohneOrdnerBaum(listOf("Analysis"))
        assertFalse(listOf("Analysis") in ohneAnalysis.ordner)
        assertFalse(a in ohneAnalysis.kartenOrdner)
        assertFalse(b in ohneAnalysis.kartenOrdner)
        assertTrue(c in ohneAnalysis.kartenOrdner)
    }

    @Test
    fun `Wiederherstellung erzeugt Ordner und Kartenpfade erneut`() {
        val karte = KartenId("karte")
        val wiederhergestellt = KartenOrdnung()
            .mitOrdnern(setOf(listOf("Analysis"), listOf("Analysis", "Grenzwerte")))
            .mitKartenInOrdnern(mapOf(karte to listOf("Analysis", "Grenzwerte")))

        assertEquals(listOf("Analysis", "Grenzwerte"), wiederhergestellt.ordnerFür(karte))
        assertTrue(listOf("Analysis") in wiederhergestellt.ordner)
        assertTrue(listOf("Analysis", "Grenzwerte") in wiederhergestellt.ordner)
    }
}
