package de.TeutonStudio.MathematikKnoten

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.BaselineShift
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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
    fun `Reitertitel rendern Relationen Zahlbereiche Indizes und Klartext`() {
        assertEquals("x ≤ 0", vereinfacheLatexAnzeige("x \\leq 0"))
        assertEquals("ℝ und ℕ", vereinfacheLatexAnzeige("\\mathbb{R} und \\mathbb{N}"))
        assertEquals("Definition", vereinfacheLatexAnzeige("Definition"))

        val indexierterTitel = latexZuAnnotiertemText("a_{n}")
        assertEquals("an", indexierterTitel.text)
        assertEquals(BaselineShift.Subscript, indexierterTitel.spanStyles.single().item.baselineShift)
    }

    @Test
    fun `rendert alle verwendeten Doppelstrichbuchstaben`() {
        assertEquals(
            "ℕ ℤ ℚ ℝ ℂ ℍ ℙ 𝔽 𝕂",
            vereinfacheLatexAnzeige(
                "\\mathbb{N} \\mathbb{Z} \\mathbb{Q} \\mathbb{R} \\mathbb{C} " +
                    "\\mathbb{H} \\mathbb{P} \\mathbb{F} \\mathbb{K}",
            ),
        )
    }

    @Test
    fun `rendert vert und kombinierte mathematische Reitertitel`() {
        val titel = latexZuAnnotiertemText("x_{n} \\in \\mathbb{K} \\vert n \\in \\mathbb{N}")

        assertEquals("xn ∈ 𝕂 | n ∈ ℕ", titel.text)
        assertEquals(BaselineShift.Subscript, titel.spanStyles.single().item.baselineShift)
        assertFalse("vert" in titel.text)
    }

    @Test
    fun `mehrbuchstabiges mathbb Argument bleibt als sichtbarer Fallback erhalten`() {
        assertEquals("AB", vereinfacheLatexAnzeige("\\mathbb{AB}"))
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
    fun `Integral bleibt im Klartext als portables Standardsymbol lesbar`() {
        assertEquals("∫x ∈ I f(x)·dx", vereinfacheLatexAnzeige("\\int_{x \\in I} f(x)\\cdot dx"))
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
    @Test
    fun `mathbb funktioniert gruppiert und ungruppiert`() {
        assertEquals("ℝ ℕ", vereinfacheLatexAnzeige("\\mathbb R \\mathbb{N}"))
    }

    @Test
    fun `Formelbegrenzer werden in Reitertiteln entfernt`() {
        assertEquals("ℝ", vereinfacheLatexAnzeige("$\\mathbb{R}$"))
        assertEquals("ℕ", vereinfacheLatexAnzeige("\\(\\mathbb{N}\\)"))
    }

    @Test
    fun `historische Intervall Delimiter bleiben lesbar`() {
        assertEquals("]1,3[", vereinfacheLatexAnzeige("\\mathopen{]}1,3\\mathclose{[}"))
    }

    @Test
    fun `annotierte reelle Intervalle rendern Zahlbereich und Randrelationen`() {
        assertEquals("1≤ℝ<3", vereinfacheLatexAnzeige("{}^{1\\leq}\\mathbb{R}^{<3}"))
    }
}
