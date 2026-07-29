package de.TeutonStudio.MathematikAtlas

import kotlin.test.*

class ReelleMethodenSummenKonzeptTest {
    @Test fun `Konzept besitzt Unter- und Obersummenkarte`() {
        val konzept = ReelleMethodenSummenKonzept.definition

        assertEquals(setOf("definition", "obersumme"), konzept.reiter.map { it.id }.toSet())
        assertTrue(konzept.reiter("definition").karte.knoten.any { it.art == "mathematik.reelleMethodenSumme" && it.parameter["summenArt"] == "untersumme" })
        assertTrue(konzept.reiter("obersumme").karte.knoten.any { it.art == "mathematik.reelleMethodenSumme" && it.parameter["summenArt"] == "obersumme" })
    }
}
