package de.TeutonStudio.MathematikKnoten

import kotlin.test.Test
import kotlin.test.assertTrue

class AnschlussArtBeschreibungenTest {
    @Test
    fun `alle registrierten mathematischen Anschlussarten besitzen Beschreibungen`() {
        val ohneBeschreibung = MathematikAnschlussArten.alle.filter { it.beschreibung.isBlank() }

        assertTrue(
            ohneBeschreibung.isEmpty(),
            "Anschlussarten ohne Beschreibung: ${ohneBeschreibung.joinToString { it.id.wert }}",
        )
    }
}
