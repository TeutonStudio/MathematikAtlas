package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import de.TeutonStudio.MathematikRechenSystem.kern.ReelleZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.Variable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @Test
    fun `Methodenformel verwendet Display Cases mit Signatur und Abbildungsvorschrift`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )

        val formel = methodenFormel(methode)

        assertTrue(formel.startsWith("\\[f:\\begin{cases}"))
        assertTrue("\\longrightarrow \\mathbb{R}" in formel)
        assertTrue("x \\mapsto x" in formel)
        assertTrue(formel.endsWith("\\end{cases}\\]"))
    }

    @Test
    fun `Display Begrenzer werden vor dem Zeilenrendering entfernt`() {
        assertEquals("x + 1", entferneLatexDisplayBegrenzer("\\[x + 1\\]"))
        assertEquals("x + 1", entferneLatexDisplayBegrenzer("\$\$x + 1\$\$"))
    }

    @Test
    fun `Cases werden in echte Formelzeilen zerlegt`() {
        val teile = zerlegeLatexFallFormel(
            "\\[f:\\begin{cases}\\mathbb{R} \\longrightarrow \\mathbb{C}\\\\x \\mapsto f(x)\\end{cases}\\]",
        )

        assertEquals("f:", teile?.vorher)
        assertEquals(
            listOf("\\mathbb{R} \\longrightarrow \\mathbb{C}", "x \\mapsto f(x)"),
            teile?.zeilen,
        )
        assertEquals("", teile?.nachher)
    }
}
