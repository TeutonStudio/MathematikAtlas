package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KnotenInspektorFarbeTest {
    @Test
    fun `profilfarbe bleibt ohne zusaetzlichen hintergrund kontrastreich`() {
        val hintergruende = listOf(Color(0xFFF7F2FA), Color(0xFF211F26))
        val profilFarben = listOf(
            Color(0xFF6750A4),
            Color(0xFFFFF59D),
            Color(0xFF101010),
            Color(0xFFFF0000),
            Color(0xFF00A000),
            Color(0xFF0047FF),
        )

        hintergruende.forEach { hintergrund ->
            profilFarben.forEach { profilFarbe ->
                val ergebnis = kontrastAdaptiveProfilFarbe(profilFarbe, hintergrund)
                assertTrue(farbKontrastVerhältnis(ergebnis, hintergrund) >= 4.49f)
                assertEquals(1f, ergebnis.alpha)
            }
        }
    }

    @Test
    fun `bereits kontrastreiche profilfarbe bleibt unveraendert`() {
        val profilFarbe = Color(0xFF002060)
        val hintergrund = Color(0xFFF7F2FA)

        assertEquals(profilFarbe, kontrastAdaptiveProfilFarbe(profilFarbe, hintergrund))
    }
}
