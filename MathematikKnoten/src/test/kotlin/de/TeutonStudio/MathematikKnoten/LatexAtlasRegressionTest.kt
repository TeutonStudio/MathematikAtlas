package de.TeutonStudio.MathematikKnoten

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class LatexAtlasRegressionTest {
    @Test
    fun `Mengenklammern werden zentral skalierend kanonisiert`() {
        val latex = kanonisiereAtlasLatex("\\{1,2,3\\}")
        assertEquals("\\left\\{1,2,3\\right\\}", latex)

        val bereitsSkaliert = kanonisiereAtlasLatex("\\left\\{x\\mid P(x)\\right\\}")
        assertEquals("\\left\\{x\\mid P(x)\\right\\}", bereitsSkaliert)
    }

    @Test
    fun `mathbb Exponenten werden mit geklammertem Alphabet gerendert`() {
        val latex = kanonisiereAtlasLatex("M^{\\mathbb Z}+M^{\\mathbb N_0}")
        assertContains(latex, "M^{\\mathbb{Z}}")
        assertContains(latex, "M^{\\mathbb{N}_0}")
        assertFalse("\\mathbb Z" in latex)
        assertFalse("\\mathbb N_0" in latex)
    }

    @Test
    fun `Real und Imaginaerteil werden als mathcal kanonisiert`() {
        val latex = kanonisiereAtlasLatex("\\operatorname{Re}(z)+\\operatorname{Im}(z)")
        assertEquals("\\mathcal{Re}(z)+\\mathcal{Im}(z)", latex)
    }

    @Test
    fun `grosses kartesisches Produkt wird vor Renderer in Operator und Index zerlegt`() {
        val teile = assertNotNull(
            zerlegeGrossesKartesischesProdukt(
                "\\mathop{\\Large\\times}\\limits_{i\\in I} A(i)",
            ),
        )
        assertEquals("", teile.vorher)
        assertEquals("i\\in I", teile.untereAnnotation)
        assertEquals("A(i)", teile.nachher)
    }

    @Test
    fun `Potenzmenge bleibt mathcal und Mengenoperatorplatzhalter bleibt rendererfaehig`() {
        val latex = atlasLatexQuelltext("\\mathcal{P}(\\dots)", dunklesSchema = false)
        assertContains(latex, "\\mathcal{P}(\\dots)")
    }
}
