package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.MathematikKnoten.historischeMengenKnotenArten
import de.TeutonStudio.MathematikKnoten.historischeSkalarproduktArten
import de.TeutonStudio.MathematikKnoten.historischeZahlenRechnerArten
import kotlin.test.Test
import kotlin.test.assertTrue

class StandardKartenIntegrationTest {
    @Test
    fun `Standardkarten akzeptieren lade-kompatible historische Knotenarten`() {
        val bekannteArten = bekannteStandardKartenKnotenArten()

        assertTrue(bekannteArten.containsAll(historischeZahlenRechnerArten))
        assertTrue(bekannteArten.containsAll(historischeMengenKnotenArten))
        assertTrue(bekannteArten.containsAll(historischeSkalarproduktArten))
        assertTrue("mathematik.einheitsSpalte" in bekannteArten)
        assertTrue("mathematik.einheitsZeile" in bekannteArten)
    }

    @Test
    fun `aktuelle Standardkarten-Historientypen werden nicht mehr verworfen`() {
        val bekannteArten = bekannteStandardKartenKnotenArten()

        listOf(
            "mathematik.addition",
            "mathematik.multiplikation",
            "mathematik.potenz",
            "mathematik.reelleZahlen",
        ).forEach { art ->
            assertTrue(art in bekannteArten, "Standardkarten-Knotentyp $art muss lade-kompatibel sein.")
        }
    }
}
