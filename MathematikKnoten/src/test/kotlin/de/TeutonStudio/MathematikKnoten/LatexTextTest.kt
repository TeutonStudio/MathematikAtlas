package de.TeutonStudio.MathematikKnoten

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LatexTextTest {
    @Test
    fun `Formelbegrenzer werden entfernt ohne den Formelinhalt umzuschreiben`() {
        assertEquals("\\frac{a}{b}", normalisiereLatexQuelltext("$\\frac{a}{b}$"))
        assertEquals("\\begin{cases}x&x>0\\end{cases}", normalisiereLatexQuelltext("\\[\\begin{cases}x&x>0\\end{cases}\\]"))
    }

    @Test
    fun `Rendererquelltext erhält echte Latex Strukturen`() {
        val latex = "f:\\begin{cases}\\mathbb{R} \\longrightarrow \\mathbb{C}\\\\x \\mapsto \\frac{x}{2}\\end{cases}"
        val quelltext = atlasLatexQuelltext(latex, dunklesSchema = false)

        assertContains(quelltext, "\\begin{cases}")
        assertContains(quelltext, "\\end{cases}")
        assertContains(quelltext, "\\frac{x}{2}")
        assertFalse("f:{" in quelltext)
    }

    @Test
    fun `Rendererquelltext erhält dots Differential Matrizen und skalierende Delimiter`() {
        val latex = "\\dots+\\frac{\\mathrm{d}}{\\mathrm{d}x}(\\dots)+\\left(\\begin{pmatrix}1&2\\\\3&4\\end{pmatrix}\\right)"
        val quelltext = atlasLatexQuelltext(latex, dunklesSchema = false)

        assertContains(quelltext, "\\dots")
        assertContains(quelltext, "\\frac{\\mathrm{d}}{\\mathrm{d}x}")
        assertContains(quelltext, "\\begin{pmatrix}")
        assertContains(quelltext, "\\left(")
        assertContains(quelltext, "\\right)")
    }

    @Test
    fun `Atlas Kompatibilitätsmakros werden dem Renderer bereitgestellt`() {
        val quelltext = atlasLatexQuelltext("\\Set{x} \\implies P \\iff Q", dunklesSchema = false)

        assertContains(quelltext, "\\newcommand{\\Set}")
        assertContains(quelltext, "\\newcommand{\\implies}")
        assertContains(quelltext, "\\newcommand{\\iff}")
        assertContains(quelltext, "\\Set{x} \\implies P \\iff Q")
    }

    @Test
    fun `Wahrheitsfarben werden genau einmal ergänzt`() {
        val quelltext = atlasLatexQuelltext("\\top \\land \\bot \\land \\mathcal{Wahr}", dunklesSchema = false)

        assertContains(quelltext, "\\textcolor{#1B5E20}{\\mathcal{Wahr}}")
        assertContains(quelltext, "\\textcolor{#B71C1C}{\\mathcal{Lüge}}")
        assertFalse("\\textcolor{#1B5E20}{\\textcolor" in quelltext)
    }

    @Test
    fun `Integralparser trennt Unter und Oberannotation in beiden Reihenfolgen`() {
        val untenDannOben = assertNotNull(zerlegeIntegralOperator("\\int_{x \\in I}^{b} f(x)"))
        assertEquals("", untenDannOben.vorher)
        assertEquals("x \\in I", untenDannOben.untereAnnotation)
        assertEquals("b", untenDannOben.obereAnnotation)
        assertEquals("f(x)", untenDannOben.nachher)

        val obenDannUnten = assertNotNull(zerlegeIntegralOperator("a+\\int^{b}_{a}g"))
        assertEquals("a+", obenDannUnten.vorher)
        assertEquals("a", obenDannUnten.untereAnnotation)
        assertEquals("b", obenDannUnten.obereAnnotation)
        assertEquals("g", obenDannUnten.nachher)
    }

    @Test
    fun `Integralparser unterstützt limits und verwechselt längere Befehle nicht mit int`() {
        val mitLimits = assertNotNull(zerlegeIntegralOperator("\\int\\limits_{I} f"))
        assertEquals("I", mitLimits.untereAnnotation)
        assertEquals("f", mitLimits.nachher)
        assertNull(zerlegeIntegralOperator("\\integer"))
    }

    @Test
    fun `Klartextfallback bleibt nur eine Diagnosehilfe`() {
        val text = vereinfacheLatexAnzeige("\\dots \\longrightarrow \\mathbb{R}")
        assertContains(text, "…")
        assertContains(text, "→")
        assertContains(text, "ℝ")
    }
}
