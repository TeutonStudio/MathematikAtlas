package de.TeutonStudio.KnotenKartenVerwalter

import androidx.compose.ui.geometry.Offset
import com.TeutonStudio.KnotenKartenVerwalter.daten.KarteZustand
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.KoordinatenUmrechnung
import org.junit.Assert.assertEquals
import org.junit.Test

class KoordinatenUmrechnungTest {
    @Test
    fun weltUndBildschirmSindUmkehrbar() {
        val zustand = KarteZustand(
            verschiebung = Offset(20f, -10f),
            zoom = 2f,
        )
        val welt = Offset(15f, 30f)

        val bildschirm = KoordinatenUmrechnung.weltZuBildschirm(welt, zustand)
        val zurueck = KoordinatenUmrechnung.bildschirmZuWelt(bildschirm, zustand)

        assertEquals(50f, bildschirm.x, 0.001f)
        assertEquals(50f, bildschirm.y, 0.001f)
        assertEquals(welt.x, zurueck.x, 0.001f)
        assertEquals(welt.y, zurueck.y, 0.001f)
    }

    @Test
    fun deltaWirdDurchZoomGeteilt() {
        val zustand = KarteZustand(zoom = 4f)
        val delta = KoordinatenUmrechnung.deltaZuWelt(Offset(20f, -8f), zustand)

        assertEquals(5f, delta.x, 0.001f)
        assertEquals(-2f, delta.y, 0.001f)
    }

    @Test
    fun ungueltigerZoomFaelltAufEinsZurueck() {
        val zustand = KarteZustand(
            verschiebung = Offset(10f, 10f),
            zoom = 0f,
        )
        val welt = KoordinatenUmrechnung.bildschirmZuWelt(Offset(15f, 5f), zustand)

        assertEquals(5f, welt.x, 0.001f)
        assertEquals(-5f, welt.y, 0.001f)
    }
}
