package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import kotlin.test.Test
import kotlin.test.assertEquals

class KartenSchnittstellenTest {
    @Test
    fun `Karten-Schnittstellen haben genau einen Wert-Anschluss`() {
        val eingang = MathematikKnotenVorlagen.KartenEingang.erzeuge(GraphPunkt.Zero)
        val ausgang = MathematikKnotenVorlagen.KartenAusgang.erzeuge(GraphPunkt.Zero)

        assertEquals(listOf("wert"), eingang.anschlüsse.map { it.name })
        assertEquals(listOf("wert"), ausgang.anschlüsse.map { it.name })
    }
}
