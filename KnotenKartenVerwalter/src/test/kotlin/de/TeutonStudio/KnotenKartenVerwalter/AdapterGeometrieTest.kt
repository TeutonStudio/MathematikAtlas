package de.TeutonStudio.KnotenKartenVerwalter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdapterGeometrieTest {
    private val eingänge = (0..2).map { AnschlussDaten(name = "in$it", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = AnschlussArtId("wert"), reihenfolge = it) }
    private val adapter = KnotenDaten(art = "test.adapter", name = "Adapter", größe = GraphGröße(200f, 100f), anschlüsse = eingänge, eigenschaften = mapOf(KNOTEN_FORM_EIGENSCHAFT to KnotenEigenschaft.Text("adapter")))

    @Test fun `Griffe liegen auf der gerundeten Kapselkontur`() {
        assertEquals(14.64466f, adapter.anschlussPositionRelativ(eingänge[0]).x, .0001f)
        assertEquals(14.64466f, adapter.anschlussPositionRelativ(eingänge[0]).y, .0001f)
        assertEquals(GraphPunkt(0f, 50f), adapter.anschlussPositionRelativ(eingänge[1]))
        assertEquals(14.64466f, adapter.anschlussPositionRelativ(eingänge[2]).x, .0001f)
        assertEquals(85.35534f, adapter.anschlussPositionRelativ(eingänge[2]).y, .0001f)
        eingänge.forEach { assertTrue(adapter.enthältRelativenPunkt(adapter.anschlussPositionRelativ(it))) }
        assertFalse(adapter.enthältRelativenPunkt(GraphPunkt(0f, 0f)))
        assertTrue(adapter.enthältRelativenPunkt(GraphPunkt(100f, 0f)))
        assertTrue(adapter.enthältRelativenPunkt(GraphPunkt(100f, 100f)))
    }
    @Test fun `Klassifikation ist persistierbar und nicht aus Typnamen abgeleitet`() {
        val karte = KartenDaten(name = "Test", knoten = listOf(adapter))
        val gelesen = KartenDatenJson.lese(KartenDatenJson.schreibe(karte)).knoten.single()
        assertEquals(KnotenDarstellungsForm.Adapter, gelesen.darstellungsForm)
        assertFalse(gelesen.enthältRelativenPunkt(GraphPunkt(0f, 0f)))
        assertEquals(KnotenDarstellungsForm.Rechteck, adapter.copy(eigenschaften = emptyMap()).darstellungsForm)
    }
}
