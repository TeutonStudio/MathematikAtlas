package de.TeutonStudio.MathematikAtlas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnsichtsFenster
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import kotlin.test.Test
import kotlin.test.assertEquals

class KartenDragTest {
    @Test
    fun `Ablage berücksichtigt Verschiebung Zoom Dichte und Griffpunkt`() {
        val position = berechneKnotenAblagePosition(
            positionImEditor = Offset(500f, 350f),
            ansicht = AnsichtsFenster(
                verschiebung = GraphPunkt(100f, 50f),
                zoom = 2f,
            ),
            dichte = 2f,
            griffPosition = Offset(45f, 27f),
            quellGröße = Size(180f, 108f),
            knotenGröße = GraphGröße(240f, 120f),
        )

        assertEquals(GraphPunkt(40f, 45f), position)
    }

    @Test
    fun `Griffpunkt wird bei fehlerhafter Quellgroesse sicher begrenzt`() {
        val position = berechneKnotenAblagePosition(
            positionImEditor = Offset(20f, 30f),
            ansicht = AnsichtsFenster(),
            dichte = 1f,
            griffPosition = Offset(10f, 10f),
            quellGröße = Size.Zero,
            knotenGröße = GraphGröße(100f, 80f),
        )

        assertEquals(GraphPunkt(-80f, -50f), position)
    }
}
