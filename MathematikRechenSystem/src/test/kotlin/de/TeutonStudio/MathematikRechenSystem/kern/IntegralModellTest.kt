package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
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

    private fun intervall(links: Long = 0, rechts: Long = 1): ReellesIntervall = ReellesIntervall(
        links = RationaleZahl.von(links),
        linksOffen = false,
        rechts = RationaleZahl.von(rechts),
        rechtsOffen = false,
    )

    @Test
    fun `Methodenform rendert kurz oder vollstaendig ohne eigenes Argument`() {
        val bereich = IntegralBereich(listOf(intervall()))
        val vollstaendig = methodenIntegral(f, bereich, kurz = false)
        val kurz = methodenIntegral(f, bereich, kurz = true)

        assertEquals(
            "\\int_{${intervall().zuLatex()}}f\\cdotd\\left(\\operatorname{id}\\vert_{${intervall().zuLatex()}}\\right)",
            vollstaendig.zuLatex(),
        )
        assertEquals("\\int_{${intervall().zuLatex()}}f", kurz.zuLatex())
        assertEquals(IntegralAusgabeform.METHODE, vollstaendig.ausgabeform)
        assertEquals(IntegralAusgabeform.METHODE, kurz.ausgabeform)
        assertFalse(vollstaendig.zuLatex().contains("f(x)"))
    }

    @Test
    fun `Termform bindet Variable und Differential ueber dieselbe Quellen ID`() {
        val bereich = intervall()
        val integral = termIntegral(
            term = Potenz(x, RationaleZahl.von(2)),
            bereiche = listOf(bereich),
            bindungen = listOf(IntegralBindung(x, "integral-x")),
        )

        assertEquals("\\int_{x\\in${bereich.zuLatex()}}{x}^{2}\\cdotdx", integral.zuLatex())
        assertEquals(listOf("integral-x"), integral.volumenElement.quellenIds)
        assertFalse(integral.zuLatex().contains("\\int_{0}"))
    }

    @Test
    fun `mehrdimensionales Integral nutzt kartesischen Bereich und geordnetes Volumenelement`() {
        val y = Variable("y")
        val xBereich = intervall()
        val yBereich = intervall(-1, 1)
        val integral = termIntegral(
            term = addition(x, y),
            bereiche = listOf(xBereich, yBereich),
            bindungen = listOf(IntegralBindung(x, "x"), IntegralBindung(y, "y")),
        )

        assertEquals(
            "\\int_{\\left(x,y\\right)\\in\\left(${xBereich.zuLatex()}\\times${yBereich.zuLatex()}\\right)}" +
                "\\left(x + y\\right)\\cdotdx\\cdotdy",
            integral.zuLatex(),
        )
        assertEquals(listOf("x", "y"), integral.volumenElement.quellenIds)
    }

    @Test
    fun `jede Bereichskomponente benoetigt genau eine Bindung`() {
        assertFailsWith<IllegalArgumentException> {
            termIntegral(
                term = x,
                bereiche = listOf(intervall(), intervall()),
                bindungen = listOf(IntegralBindung(x)),
            )
        }
    }

    @Test
    fun `Mass wird nur bei eindeutigem Bereich abgeleitet`() {
        assertSame(IntegralMass.StandardReell, leiteIntegralMassOderNull(IntegralBereich(listOf(intervall()))))
        assertSame(
            IntegralMass.Zaehlmass,
            leiteIntegralMassOderNull(
                IntegralBereich(listOf(EndlicheMenge(setOf(RationaleZahl.Eins)))),
            ),
        )
        assertNull(leiteIntegralMassOderNull(IntegralBereich(listOf(ReelleZahlen))))
    }

    @Test
    fun `Zaehlen stimmt auf endlicher Menge mit Summe ueberein`() {
        val menge = EndlicheMenge(
            setOf(RationaleZahl.Eins, RationaleZahl.von(2), RationaleZahl.von(3)),
        )
        val integral = termIntegral(
            term = x,
            bereiche = listOf(menge),
            bindungen = listOf(IntegralBindung(x, "x")),
            mass = IntegralMass.Zaehlmass,
        )

        val ergebnis = werteIntegralAus(integral)

        assertEquals(IntegralUnterstuetzungsStatus.EXAKT, ergebnis.status)
        assertEquals(RationaleZahl.von(6), ergebnis.wert)
        assertTrue(ergebnis.regel.contains("Zählmaß"))
    }

    @Test
    fun `Polynom wird auf beschraenktem reellen Intervall exakt integriert`() {
        val bereich = intervall(0, 2)
        val integral = termIntegral(
            term = Potenz(x, RationaleZahl.von(2)),
            bereiche = listOf(bereich),
            bindungen = listOf(IntegralBindung(x, "x")),
        )

        val ergebnis = werteIntegralAus(integral)

        assertEquals(IntegralUnterstuetzungsStatus.EXAKT, ergebnis.status)
        assertEquals(RationaleZahl.von(8, 3), ergebnis.wert)
        assertTrue(ergebnis.schritte.any { it.regelId == "analysis.integral.hauptsatz" })
    }

    @Test
    fun `Unbekannte geschlossene Form bleibt gueltig symbolisch`() {
        val integral = termIntegral(
            term = Betrag(x),
            bereiche = listOf(intervall(-1, 1)),
            bindungen = listOf(IntegralBindung(x, "x")),
        )

        val ergebnis = werteIntegralAus(integral)

        assertEquals(IntegralUnterstuetzungsStatus.SYMBOLISCH, ergebnis.status)
        assertEquals(integral, ergebnis.wert)
        assertTrue(ergebnis.regel.contains("symbolisch"))
    }

    @Test
    fun `Nichtstandarddarstellung bleibt an Voraussetzungen gebunden`() {
        val integral = termIntegral(
            term = x,
            bereiche = listOf(intervall()),
            bindungen = listOf(IntegralBindung(x, "x")),
            mass = IntegralMass.NichtstandardZellgewicht(),
            vertrag = null,
        )

        val ergebnis = werteIntegralAus(integral)

        assertEquals(IntegralUnterstuetzungsStatus.BEDINGT, ergebnis.status)
        assertIs<NichtstandardIntegralDarstellung>(ergebnis.wert)
        assertEquals(3, ergebnis.voraussetzungen.size)
        assertTrue(ergebnis.wert.zuLatex().contains("\\operatorname{st}"))
    }

    @Test
    fun `Freie Zusatzparameter bleiben im Termmodus frei`() {
        val y = Variable("y")
        val integral = termIntegral(
            term = addition(x, y),
            bereiche = listOf(intervall()),
            bindungen = listOf(IntegralBindung(x, "quelle.x")),
        )

        assertEquals(setOf(y), integral.freieVariablen)
    }

    @Test
    fun `Riemann Vertrag unterscheidet Nachweis und symbolische Gueltigkeit`() {
        val bereich = IntegralBereich(listOf(intervall()))
        val nachgewiesen = RiemannIntegralVertrag(bereich, true, true)
        val offen = RiemannIntegralVertrag(bereich, null, null)

        assertTrue(nachgewiesen.ersteUmsetzungUnterstuetzt)
        assertTrue(nachgewiesen.voraussetzungen.isEmpty())
        assertFalse(offen.ersteUmsetzungUnterstuetzt)
        assertEquals(2, offen.voraussetzungen.size)
        assertTrue(offen.symbolischZulaessig)
    }
}
