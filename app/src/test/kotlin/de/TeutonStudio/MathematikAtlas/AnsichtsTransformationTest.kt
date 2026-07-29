package de.TeutonStudio.MathematikAtlas

import androidx.compose.ui.geometry.Offset
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnsichtsFenster
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import kotlin.test.Test
import kotlin.test.assertEquals

class AnsichtsTransformationTest {
    @Test
    fun `Zoom hält Gestenmittelpunkt am selben Bildschirmpunkt`() {
        val ausgang = AnsichtsFenster(GraphPunkt(10f, 20f), 1f)

        val ergebnis = ausgang.transformiereAnsicht(
            zentrum = Offset(100f, 50f),
            pan = Offset.Zero,
            zoomFaktor = 2f,
        )

        assertEquals(2f, ergebnis.zoom)
        assertEquals(-80f, ergebnis.verschiebung.x)
        assertEquals(-10f, ergebnis.verschiebung.y)
    }

    @Test
    fun `Pan bleibt im Verschieben-Modus parallel zum Zoom erhalten`() {
        val ausgang = AnsichtsFenster(GraphPunkt.Zero, 1f)

        val ergebnis = ausgang.transformiereAnsicht(
            zentrum = Offset(100f, 100f),
            pan = Offset(12f, -8f),
            zoomFaktor = 1f,
        )

        assertEquals(1f, ergebnis.zoom)
        assertEquals(12f, ergebnis.verschiebung.x)
        assertEquals(-8f, ergebnis.verschiebung.y)
    }

    @Test
    fun `Zoom wird auf erlaubten Bereich begrenzt`() {
        val ausgang = AnsichtsFenster(GraphPunkt.Zero, 3f)

        val ergebnis = ausgang.transformiereAnsicht(
            zentrum = Offset.Zero,
            pan = Offset.Zero,
            zoomFaktor = 10f,
        )

        assertEquals(3.5f, ergebnis.zoom)
    }
}
