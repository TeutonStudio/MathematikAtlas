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
        assertEquals("\\operatorname{Re}(\\dots)", UniversellerZahlenOperator.REALTEIL.vorschauLatex)
        assertEquals("\\operatorname{Im}(\\dots)", UniversellerZahlenOperator.IMAGINAERTEIL.vorschauLatex)
        assertEquals("\\sin(\\dots)", UniversellerZahlenOperator.SINUS.vorschauLatex)
        assertEquals("\\sinh(\\dots)", ErweiterterZahlenOperator.SINUS_HYPERBOLICUS.vorschauLatex)
        assertEquals(
            "(c_k)_k\\cdot(x^k)_k\\text{ für }k\\in\\mathbb N_0^{\\leq n}",
            ErweiterterZahlenOperator.POLYNOM.vorschauLatex,
        )
    }

    @Test
    fun `norm ist kein eigener Zahlenoperator mehr`() {
        assertEquals(false, UniversellerZahlenOperator.entries.any { it.stabileId == "zahl.norm" })
        assertEquals(UniversellerZahlenOperator.BETRAG, UniversellerZahlenOperator.vonId("zahl.norm"))
    }

    @Test
    fun `grosse Operatoren tragen Index und Anwendung`() {
        assertContains(UniversellerZahlenOperator.ITERIERTE_SUMME.vorschauLatex, "\\limits_{idx\\in\\dots}")
        assertContains(UniversellerZahlenOperator.ITERIERTE_SUMME.vorschauLatex, "(\\dots)(idx)")
        assertContains(UniversellerZahlenOperator.ITERIERTES_PRODUKT.vorschauLatex, "\\limits_{idx\\in\\dots}")
    }
}