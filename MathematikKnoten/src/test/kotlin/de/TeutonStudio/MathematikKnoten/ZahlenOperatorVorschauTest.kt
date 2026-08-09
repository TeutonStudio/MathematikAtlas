package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ZahlenOperatorVorschauTest {
    @Test
    fun `alle auswählbaren Operatoren besitzen vollständige Vorschauen`() {
        assertTrue(UniversellerZahlenOperator.entries.all { it.vorschauLatex().isNotBlank() })
        assertTrue(ErweiterterZahlenOperator.entries.all { it.vorschauLatex().isNotBlank() })
    }

    @Test
    fun `kritische Standardoperatoren enthalten Argumente und Parameter`() {
        assertEquals("(\\dots)\\div(\\dots)", UniversellerZahlenOperator.DIVISION.vorschauLatex())
        assertEquals("(\\dots)^{p}", UniversellerZahlenOperator.POTENZ.vorschauLatex())
        assertEquals("\\sqrt[p]{(\\dots)}", UniversellerZahlenOperator.WURZEL.vorschauLatex())
        assertEquals("\\log_{b}(\\dots)", UniversellerZahlenOperator.LOGARITHMUS.vorschauLatex())
        assertEquals(
            "\\frac{\\mathrm{d}}{\\mathrm{d}x}(\\dots)",
            UniversellerZahlenOperator.DIFFERENTIAL.vorschauLatex(),
        )
        assertEquals("\\min(\\dots,\\dots)", UniversellerZahlenOperator.MINIMUM.vorschauLatex())
        assertEquals("\\lVert(\\dots)\\rVert", UniversellerZahlenOperator.NORM.vorschauLatex())
        assertEquals("\\operatorname{Re}(\\dots)", UniversellerZahlenOperator.REALTEIL.vorschauLatex())
    }

    @Test
    fun `große Operatoren und Polynom besitzen vollständige Anwendungsvorschau`() {
        assertEquals(
            "\\sum\\limits_{idx\\in\\dots}(\\dots)(idx)",
            UniversellerZahlenOperator.ITERIERTE_SUMME.vorschauLatex(),
        )
        assertEquals(
            "\\prod\\limits_{idx\\in\\dots}(\\dots)(idx)",
            UniversellerZahlenOperator.ITERIERTES_PRODUKT.vorschauLatex(),
        )
        assertEquals("(c_i)_i\\cdot\\vec{x}", ErweiterterZahlenOperator.POLYNOM.vorschauLatex())
    }
}
