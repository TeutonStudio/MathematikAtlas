package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import kotlin.test.Test
import kotlin.test.assertEquals

class MathematikKnotenRendererTest {
    @Test
    fun `Variable zeigt Namen und reellen Wertevorrat als Elementbeziehung`() {
        val variable = MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero)

        assertEquals("x \\in \\mathbb{R}", variablenFormel(variable, de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis(emptyMap(), eingänge = testGrundraum(variable))))
    }

    @Test
    fun `Variable mit Tupelstruktur zeigt kartesischen Wertebereich`() {
        val variable = konfiguriereVariablenStruktur(
            MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero),
            VARIABLE_STRUKTUR_TUPEL,
        ).copy(parameter = mapOf(
            "name" to "x", "werteVorrat" to "R",
            VARIABLE_STRUKTUR to VARIABLE_STRUKTUR_TUPEL,
            VARIABLE_DIMENSION to "3",
        ))
        assertEquals("x \\in \\mathbb{R} \\times \\mathbb{R} \\times \\mathbb{R}", variablenFormel(variable, de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis(emptyMap(), eingänge = testGrundraum(variable))))
    }

    @Test
    fun `Variable übernimmt Namen und anderen Grundmengenvorrat`() {
        val variable = MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero).copy(
            parameter = mapOf("name" to "k", "werteVorrat" to "Z"),
        )

        assertEquals("k \\in \\mathbb{Z}", variablenFormel(variable, de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis(emptyMap(), eingänge = testGrundraum(variable))))
    }
}
