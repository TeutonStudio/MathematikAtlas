package de.TeutonStudio.MathematikKnoten

import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun `unterstützt Fallunterscheidungen und Methodenpfeile`() {
        assertEquals("f:{\nℝ → ℂ\nx ↦ x}", vereinfacheLatexAnzeige("f:\\begin{cases}\\mathbb{R} \\longrightarrow \\mathbb{C}\\\\x \\mapsto x\\end{cases}"))
    }
}
