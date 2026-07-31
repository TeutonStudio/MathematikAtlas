package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import kotlin.test.Test
import kotlin.test.assertEquals

class MathematikKnotenRendererTest {
    @Test
    fun `Variable zeigt Namen und reellen Wertevorrat als Elementbeziehung`() {
        val variable = MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero)

        assertEquals("x \\in \\mathbb{R}", variablenFormel(variable))
    }

    @Test
    fun `Variable übernimmt Namen und anderen Grundmengenvorrat`() {
        val variable = MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero).copy(
            parameter = mapOf("name" to "k", "werteVorrat" to "Z"),
        )

        assertEquals("k \\in \\mathbb{Z}", variablenFormel(variable))
    }
}
