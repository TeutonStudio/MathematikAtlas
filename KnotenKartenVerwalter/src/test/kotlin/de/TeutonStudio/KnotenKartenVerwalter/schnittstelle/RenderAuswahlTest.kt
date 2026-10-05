package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.ui.geometry.Rect
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RenderAuswahlTest {
    @Test fun `Sicherheitszone verhindert erneutes Culling bei kleinem Pan`() {
        val auswahl = renderAuswahlFür(KartenDaten(name = "Test"), Rect(0f, 0f, 100f, 100f))

        assertTrue(auswahl.deckt(Rect(120f, 0f, 220f, 100f)))
        assertFalse(auswahl.deckt(Rect(300f, 0f, 400f, 100f)))
    }
}
