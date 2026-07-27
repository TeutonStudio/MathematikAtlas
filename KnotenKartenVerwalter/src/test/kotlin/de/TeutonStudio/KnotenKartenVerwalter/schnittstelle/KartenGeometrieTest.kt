package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnsichtsFenster
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KartenGeometrieTest {
    @Test fun sichtbereichWirdAusPixelkameraInWeltKoordinatenZurückgerechnet() {
        val bereich = sichtbarerWeltBereich(
            ansicht = AnsichtsFenster(verschiebung = GraphPunkt(-100f, 50f), zoom = 2f),
            anzeigeGröße = IntSize(400, 200),
            dichte = 2f,
        )!!

        assertEquals(25f, bereich.left)
        assertEquals(-12.5f, bereich.top)
        assertEquals(125f, bereich.right)
        assertEquals(37.5f, bereich.bottom)
    }

    @Test fun knotenBleibtBisZumViewportPufferSichtbar() {
        val viewport = Rect(0f, 0f, 100f, 100f)
        val amPuffer = KnotenDaten(
            art = "test", name = "sichtbar", position = GraphPunkt(300f, 0f), größe = GraphGröße(20f, 20f),
        )
        val außerhalb = amPuffer.copy(position = GraphPunkt(301f, 0f))

        assertTrue(amPuffer.istImBereich(viewport, puffer = 200f))
        assertFalse(außerhalb.istImBereich(viewport, puffer = 200f))
    }

    @Test fun minimapProjektionIstUmkehrbar() {
        val projektion = MiniMapProjektion(
            grenzen = Rect(-100f, -50f, 300f, 250f),
            größe = Size(180f, 120f),
        )
        val welt = Offset(120f, 80f)

        val zurück = projektion.zuWelt(projektion.zuMiniMap(welt))

        assertEquals(welt.x, zurück.x, absoluteTolerance = 0.001f)
        assertEquals(welt.y, zurück.y, absoluteTolerance = 0.001f)
    }
}
