package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IntegralModellTest {
    private val x = Variable("x")
    private val f = Methode(
        name = "f",
        parameter = listOf(x),
        vorschrift = x,
        zielMenge = ReelleZahlen,
        werteVorräte = mapOf("x" to ReelleZahlen),
    )

    @Test
    fun `Methodenform rendert Differential der eingeschraenkten Identitaet`() {
        val integral = methodenIntegral(f, IntegralBereich(listOf(ReelleZahlen)))

        assertEquals(
            "\\int_{\\mathbb R}f\\cdotd\\left(\\operatorname{id}\\vert_{\\mathbb R}\\right)",
            integral.zuLatex(),
        )
    }

    @Test
    fun `Methodenkurzform laesst das Differential kontrolliert weg`() {
        val integral = methodenIntegral(f, IntegralBereich(listOf(ReelleZahlen)), kurz = true)

        assertEquals("\\int_{\\mathbb R}f", integral.zuLatex())
        assertEquals(IntegralAusgabeform.METHODE_KURZ, integral.ausgabeform)
    }

    @Test
    fun `Termform bindet Variable und Differential ueber dieselbe Quellen ID`() {
        val integral = termIntegral(
            term = Potenz(x, RationaleZahl.von(2)),
            bereiche = listOf(ReelleZahlen),
            bindungen = listOf(IntegralBindung(x, "integral-x")),
        )

        assertEquals("\\int_{x\\in\\mathbb R}{x}^{2}\\cdotdx", integral.zuLatex())
        assertEquals(listOf("integral-x"), integral.volumenElement.quellenIds)
    }

    @Test
    fun `mehrdimensionales Integral nutzt kartesischen Bereich und geordnetes Zellvolumen`() {
        val y = Variable("y")
        val integral = termIntegral(
            term = addition(x, y),
            bereiche = listOf(ReelleZahlen, ReelleZahlen),
            bindungen = listOf(IntegralBindung(x, "x"), IntegralBindung(y, "y")),
        )

        assertEquals(
            "\\int_{\\left(x,y\\right)\\in\\left(\\mathbb R\\times\\mathbb R\\right)}" +
                "\\left(x + y\\right)\\cdotdx\\cdotdy",
            integral.zuLatex(),
        )
    }

    @Test
    fun `jede Bereichskomponente benoetigt genau eine Bindung`() {
        assertFailsWith<IllegalArgumentException> {
            termIntegral(
                term = x,
                bereiche = listOf(ReelleZahlen, ReelleZahlen),
                bindungen = listOf(IntegralBindung(x)),
            )
        }
    }

    @Test
    fun `erste Riemann Umsetzung unterscheidet Nachweis und symbolische Gueltigkeit`() {
        val bereich = IntegralBereich(listOf(ReelleZahlen))
        val nachgewiesen = RiemannIntegralVertrag(bereich, true, true)
        val offen = RiemannIntegralVertrag(bereich, null, null)

        assertTrue(nachgewiesen.ersteUmsetzungUnterstuetzt)
        assertTrue(nachgewiesen.voraussetzungen.isEmpty())
        assertFalse(offen.ersteUmsetzungUnterstuetzt)
        assertEquals(2, offen.voraussetzungen.size)
        assertTrue(offen.symbolischZulaessig)
    }
}
