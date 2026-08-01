package de.TeutonStudio.MathematikAtlas.speicher

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import kotlin.test.Test
import kotlin.test.assertEquals

class EingangsKartenVerweiseJsonTest {
    @Test
    fun `eingangsbezogene Kartenreferenzen bleiben versionsfest erhalten`() {
        val verweis = KartenVerweis(KartenId("methoden-karte"), 7)
        val karte = KartenDaten(
            id = KartenId("test-karte"),
            name = "Test",
            knoten = listOf(
                KnotenDaten(
                    id = KnotenId("iteration"),
                    art = "mathematik.iterierteSumme",
                    name = "Iterierte Summe",
                    eingangsKartenVerweise = mapOf("methode" to verweis),
                ),
            ),
        )

        val gelesen = KartenJson.lese(KartenJson.schreibe(karte))

        assertEquals(mapOf("methode" to verweis), gelesen.knoten.single().eingangsKartenVerweise)
    }
}
