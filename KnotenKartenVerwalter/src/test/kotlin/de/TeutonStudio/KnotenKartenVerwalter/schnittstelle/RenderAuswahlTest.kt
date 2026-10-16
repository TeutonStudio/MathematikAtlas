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
    @Test fun `Sichtbare lange Kopfzeile behaelt einen ausserhalb liegenden Koerper in der Renderauswahl`() {
        val knoten = KnotenDaten(art = "test", name = "K", position = GraphPunkt(20f, 1600f))
        val karte = KartenDaten(name = "Test", knoten = listOf(knoten))
        val fenster = Rect(0f, 0f, 300f, 200f)
        assertTrue(renderAuswahlFür(karte, fenster).knoten.isEmpty())
        val anhänge = mapOf(knoten.id to KnotenAnhangMaße(oben = 1550f))
        assertTrue(renderAuswahlFür(karte, fenster, anhänge).knoten.contains(knoten))
        assertFalse(knoten.enthältRelativenPunkt(GraphPunkt(40f, -20f)))
        assertFalse(knoten.enthältRelativenPunkt(GraphPunkt(40f, knoten.größe.höhe + 10f)))
    }

    @Test fun `Hintergrund erkennt echte Anschluss und Resize Griffe aber keine Anhaenge`() {
        val anschluss = AnschlussDaten(name = "aus", richtung = AnschlussRichtung.Ausgang, art = AnschlussArtId("test"), kante = AnschlussKante.Rechts)
        val knoten = KnotenDaten(art = "test", name = "K", position = GraphPunkt(100f, 100f), größe = GraphGröße(200f, 120f), anschlüsse = listOf(anschluss))
        val karte = KartenDaten(name = "Test", knoten = listOf(knoten))
        for (zoom in listOf(.5f, 1f, 2f)) {
            val ansicht = AnsichtsFenster(zoom = zoom)
            fun hit(x: Float, y: Float, ausgewählt: Boolean = false) = trifftKnoten(
                androidx.compose.ui.geometry.Offset(x * zoom, y * zoom), karte, ansicht, 1f,
                if (ausgewählt) setOf(knoten.id) else emptySet(),
            )
            assertTrue(hit(300f + 5f / zoom, 160f))
            assertTrue(hit(303f, 223f, ausgewählt = true))
            assertFalse(hit(303f, 223f))
            assertFalse(hit(200f, 230f, ausgewählt = true))
            assertFalse(hit(200f, 90f, ausgewählt = true))
        }
    }

}
