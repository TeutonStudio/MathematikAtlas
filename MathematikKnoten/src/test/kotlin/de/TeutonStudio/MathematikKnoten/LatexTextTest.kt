package de.TeutonStudio.MathematikKnoten

import kotlin.test.*

class LatexTextTest {
    @Test fun `Display Latex wird ohne doppelte Begrenzung normalisiert`() {
        assertEquals("\\[x+1\\]", alsDisplayLatex("\$x+1\$"))
        assertEquals("\\[x+1\\]", alsDisplayLatex("\\[x+1\\]"))
    }

    @Test fun `Brüche werden ohne störende Klammerpaare dargestellt`() {
        assertEquals("a⁄b + ℝ", vereinfacheLatexAnzeige("\\frac{a}{b} + \\mathbb{R}"))
    }

    @Test fun `Verschachtelte Brüche bleiben lesbar`() {
        assertEquals("(a⁄b)⁄c", vereinfacheLatexAnzeige("\\frac{\\frac{a}{b}}{c}"))
    }

    @Test fun `Formelbefehle werden nicht als Latex Quellcode angezeigt`() {
        assertEquals("input2 ∪ π⁄x2", vereinfacheLatexAnzeige("input_{2} \\cup \\frac{\\pi}{x^{2}}"))
    }

    @Test fun `Display Begrenzer erscheinen nicht im gerenderten Text`() {
        assertEquals("x + 1", vereinfacheLatexAnzeige("\\[x + 1\\]"))
    }

    @Test fun `unterstützt Fallunterscheidungen und Methodenpfeile`() {
        assertEquals("f:{\nℝ → ℂ\nx ↦ x}", vereinfacheLatexAnzeige("f:\\begin{cases}\\mathbb{R} \\longrightarrow \\mathbb{C}\\\\x \\mapsto x\\end{cases}"))
    }
}
