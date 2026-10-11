package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VisualisierungsKonzeptReiterTest {
    @Test
    fun `nur Visualisierungsknoten erhalten den Hochaufloesungsreiter im Haltedialog`() {
        val visualisierung = MathematikKnotenVorlagen.Visualisierung.erzeuge(GraphPunkt.Zero)
        val zahl = MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero)

        assertTrue(besitztHochauflösendenVisualisierungsReiter(visualisierung))
        assertFalse(besitztHochauflösendenVisualisierungsReiter(zahl))
        assertFalse(besitztHochauflösendenVisualisierungsReiter(null))
    }
}
