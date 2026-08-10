package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.Abbildungsmenge
import de.TeutonStudio.MathematikRechenSystem.kern.GaußscheGanzeZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.GaußschePrimzahlen
import de.TeutonStudio.MathematikRechenSystem.kern.ModuloZahlenraum
import de.TeutonStudio.MathematikRechenSystem.kern.Potenzmenge
import de.TeutonStudio.MathematikRechenSystem.kern.Primzahlen
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.ReelleZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.Tensorraum
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MengenraeumeTest {
    private fun dimensionen(vararg werte: Long) = werte.map(RationaleZahl::von)

    @Test
    fun `benannte Zahlenmengen besitzen eindeutige LaTeX Darstellungen`() {
        assertEquals("\\mathbb{P}", Primzahlen.zuLatex())
        assertEquals("\\mathbb{Z}[i]", GaußscheGanzeZahlen.zuLatex())
        assertEquals("\\mathbb{P}_{\\mathbb{Z}[i]}", GaußschePrimzahlen.zuLatex())
    }

    @Test
    fun `Potenzmenge und Abbildungsmenge bewahren ihre Richtung`() {
        assertEquals("\\mathcal{P}\\left(\\mathbb{R}\\right)", Potenzmenge(ReelleZahlen).zuLatex())
        assertEquals(
            "\\mathbb{P}^{\\mathbb{R}}",
            Abbildungsmenge(zielMenge = Primzahlen, definitionsMenge = ReelleZahlen).zuLatex(),
        )
    }

    @Test
    fun `Tensorraum unterstützt beliebigen positiven Rang`() {
        assertEquals("\\mathbb{R}^{3}", Tensorraum(ReelleZahlen, dimensionen(3)).zuLatex())
        assertEquals("\\mathbb{R}^{2\\times3}", Tensorraum(ReelleZahlen, dimensionen(2, 3)).zuLatex())
        assertEquals("\\mathbb{R}^{2\\times3\\times4}", Tensorraum(ReelleZahlen, dimensionen(2, 3, 4)).zuLatex())
    }

    @Test
    fun `Tensor und Modulo Räume lehnen ungültige Dimensionen ab`() {
        assertFailsWith<IllegalArgumentException> { Tensorraum(ReelleZahlen, emptyList()) }
        assertFailsWith<IllegalArgumentException> { Tensorraum(ReelleZahlen, dimensionen(2, 0)) }
        assertFailsWith<IllegalArgumentException> { ModuloZahlenraum(1) }
        assertEquals("\\mathbb{Z}/5\\mathbb{Z}", ModuloZahlenraum(5).zuLatex())
    }
}
