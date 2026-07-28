package de.TeutonStudio.MathematikAtlas.speicher

import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KartenOrdnungTest {
    @Test
    fun `verschachtelte Ordner erzeugen alle Elternpfade`() {
        val ordnung = KartenOrdnung().mitOrdner(listOf("Algebra", "Lineare Algebra", "Matrizen"))

        assertEquals(
            setOf(
                listOf("Algebra"),
                listOf("Algebra", "Lineare Algebra"),
                listOf("Algebra", "Lineare Algebra", "Matrizen"),
            ),
            ordnung.ordner,
        )
    }

    @Test
    fun `Ordner verschieben aktualisiert Unterordner und Karten`() {
        val karte = KartenId("karte-1")
        val ordnung = KartenOrdnung()
            .mitOrdner(listOf("Algebra", "Linear"))
            .mitKarteInOrdner(karte, listOf("Algebra", "Linear"))
            .verschiebeOrdner(listOf("Algebra"), listOf("Mathematik", "Algebra"))

        assertEquals(listOf("Mathematik", "Algebra", "Linear"), ordnung.ordnerFür(karte))
        assertTrue(listOf("Mathematik", "Algebra", "Linear") in ordnung.ordner)
        assertFalse(listOf("Algebra") in ordnung.ordner)
    }

    @Test
    fun `nur leere Blattordner können gelöscht werden`() {
        val karte = KartenId("karte-1")
        var ordnung = KartenOrdnung()
            .mitOrdner(listOf("Analysis", "Grenzwerte"))
            .mitKarteInOrdner(karte, listOf("Analysis", "Grenzwerte"))

        assertFalse(ordnung.kannOrdnerLöschen(listOf("Analysis")))
        assertFalse(ordnung.kannOrdnerLöschen(listOf("Analysis", "Grenzwerte")))

        ordnung = ordnung.mitKarteInOrdner(karte, emptyList())
        assertTrue(ordnung.kannOrdnerLöschen(listOf("Analysis", "Grenzwerte")))
        assertFalse(ordnung.ohneOrdner(listOf("Analysis", "Grenzwerte")).ordner.contains(listOf("Analysis", "Grenzwerte")))
    }
}
