package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class ZahlenOperatorVorschauTest {
    @Test
    fun `kritische Vorschauen enthalten vollständige Anwendungen`() {
        assertEquals("(\\dots)\\div(\\dots)", UniversellerZahlenOperator.DIVISION.vorschauLatex)
        assertEquals("(\\dots)^{p}", UniversellerZahlenOperator.POTENZ.vorschauLatex)
        assertEquals("\\sqrt[p]{(\\dots)}", UniversellerZahlenOperator.WURZEL.vorschauLatex)
        assertEquals("\\log_{b}(\\dots)", UniversellerZahlenOperator.LOGARITHMUS.vorschauLatex)
        assertEquals(
            "\\frac{\\mathrm{d}}{\\mathrm{d}x}(\\dots)",
            UniversellerZahlenOperator.DIFFERENTIAL.vorschauLatex,
        )
        assertEquals("\\min(\\dots,\\dots)", UniversellerZahlenOperator.MINIMUM.vorschauLatex)
        assertEquals("\\max(\\dots,\\dots)", UniversellerZahlenOperator.MAXIMUM.vorschauLatex)
        assertEquals("\\lVert(\\dots)\\rVert", UniversellerZahlenOperator.NORM.vorschauLatex)
        assertEquals("\\operatorname{Re}(\\dots)", UniversellerZahlenOperator.REALTEIL.vorschauLatex)
        assertEquals("\\operatorname{Im}(\\dots)", UniversellerZahlenOperator.IMAGINAERTEIL.vorschauLatex)
        assertEquals("\\sin(\\dots)", UniversellerZahlenOperator.SINUS.vorschauLatex)
        assertEquals("\\sinh(\\dots)", ErweiterterZahlenOperator.SINUS_HYPERBOLICUS.vorschauLatex)
        assertEquals("(c_i)_i\\cdot\\vec{x}", ErweiterterZahlenOperator.POLYNOM.vorschauLatex)
    }

    @Test
    fun `grosse Operatoren tragen Index und Anwendung`() {
        assertContains(UniversellerZahlenOperator.ITERIERTE_SUMME.vorschauLatex, "\\limits_{idx\\in\\dots}")
        assertContains(UniversellerZahlenOperator.ITERIERTE_SUMME.vorschauLatex, "(\\dots)(idx)")
        assertContains(UniversellerZahlenOperator.ITERIERTES_PRODUKT.vorschauLatex, "\\limits_{idx\\in\\dots}")
    }
}