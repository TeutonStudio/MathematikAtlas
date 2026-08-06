package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.MathematikKnoten.MathematischeEigenschaftRegister
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnalysisEigenschaftInspektorTest {
    @Test
    fun `Inspector bietet genau die sieben Analysis Eigenschaften an`() {
        assertEquals(
            listOf(
                MathematischeEigenschaftRegister.Minimum.id,
                MathematischeEigenschaftRegister.Maximum.id,
                MathematischeEigenschaftRegister.Extremum.id,
                MathematischeEigenschaftRegister.Sattelpunkt.id,
                MathematischeEigenschaftRegister.Konvexitaetsbereich.id,
                MathematischeEigenschaftRegister.Konkavitaetsbereich.id,
                MathematischeEigenschaftRegister.Wendestelle.id,
            ),
            AnalysisEigenschaftInspektorModell.eigenschaften.map(AnalysisEigenschaftAuswahl::wert),
        )
    }

    @Test
    fun `Geltung und Strenge erscheinen nur bei fachlich passenden Eigenschaften`() {
        val minimum = AnalysisEigenschaftInspektorModell.eigenschaft("minimum")
        assertTrue(minimum.zeigtGeltung)
        assertTrue(minimum.zeigtStrenge)

        val konvex = AnalysisEigenschaftInspektorModell.eigenschaft("konvex")
        assertFalse(konvex.zeigtGeltung)
        assertTrue(konvex.zeigtStrenge)

        val wende = AnalysisEigenschaftInspektorModell.eigenschaft("wendestelle")
        assertFalse(wende.zeigtGeltung)
        assertFalse(wende.zeigtStrenge)
    }
}
