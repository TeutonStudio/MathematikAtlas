package de.TeutonStudio.MathematikKnoten

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class LatexTextTest {
    @Test
    fun `vereinfacht Brüche und Zahlbereiche ohne ungültige Regex`() {
        assertEquals("(a)⁄(b) + ℝ", vereinfacheLatexAnzeige("\\frac{a}{b} + \\mathbb{R}"))
    }

    @Test
    fun `vereinfacht verschachtelte Brüche von innen nach außen`() {
        assertEquals("((a)⁄(b))⁄(c)", vereinfacheLatexAnzeige("\\frac{\\frac{a}{b}}{c}"))
    }

    @Test
    fun `formelbefehle werden nicht als Latex Quellcode angezeigt`() {
        assertEquals("input2 ∪ (π)⁄(x2)", vereinfacheLatexAnzeige("input_{2} \\cup \\frac{\\pi}{x^{2}}"))
    }

    @Test
    fun `ungleichheitsbefehle werden als Symbol dargestellt`() {
        assertEquals("2 ≠ -2", vereinfacheLatexAnzeige("2 \\neq -2"))
        assertEquals("2 ≠ -2", vereinfacheLatexAnzeige("2 \\ne -2"))
    }

    @Test
    fun `rendert große und gepunktete Aussagenoperatoren`() {
        assertEquals("a ∨̇ b", vereinfacheLatexAnzeige("a \\stackrel{\\bullet}{\\lor} b"))
        assertEquals(
            "⋁̇idx ∈ {A} methode(idx)",
            vereinfacheLatexAnzeige("\\stackrel{\\bullet}{\\bigvee}_{idx \\in \\Set{A}} methode(idx)"),
        )
        assertEquals("⋀idx ∈ {A} P(idx)", vereinfacheLatexAnzeige("\\bigwedge_{idx \\in \\Set{A}} P(idx)"))
    }

    @Test
    fun `limits erscheint nie als Klartext und die Indexbedingung bleibt erhalten`() {
        val text = vereinfacheLatexAnzeige("\\sum\\limits_{i \\in I} f(i)")

        assertEquals("∑i ∈ I f(i)", text)
        assertFalse("limits" in text)
    }

    @Test
    fun `Wahrheitswerte verwenden universelle Symbole und Farben`() {
        val wahr = latexZuAnnotiertemText("\\mathcal{Wahr}", Color.Green, Color.Red)
        val lüge = latexZuAnnotiertemText("\\mathcal{Lüge}", Color.Green, Color.Red)
        val historisch = latexZuAnnotiertemText("\\top \\land \\bot", Color.Green, Color.Red)

        assertEquals("Wahr", wahr.text)
        assertEquals(Color.Green, wahr.spanStyles.single().item.color)
        assertEquals("Lüge", lüge.text)
        assertEquals(Color.Red, lüge.spanStyles.single().item.color)
        assertEquals("Wahr ∧ Lüge", historisch.text)
        assertEquals(listOf(Color.Green, Color.Red), historisch.spanStyles.map { it.item.color })
    }

    @Test
    fun `unterstützt Fallunterscheidungen und Methodenpfeile`() {
        assertEquals("f:{\nℝ → ℂ\nx ↦ x}", vereinfacheLatexAnzeige("f:\\begin{cases}\\mathbb{R} \\longrightarrow \\mathbb{C}\\\\x \\mapsto x\\end{cases}"))
    }
}
