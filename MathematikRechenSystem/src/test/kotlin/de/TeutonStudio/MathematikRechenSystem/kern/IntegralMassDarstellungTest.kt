package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntegralMassDarstellungTest {
    private val x = Variable("x")
    private val methode = Methode(
        name = "f",
        parameter = listOf(x),
        vorschrift = x,
        zielMenge = ReelleZahlen,
        werteVorräte = mapOf("x" to ReelleZahlen),
    )
    private val bereich = IntegralBereich(
        listOf(
            ReellesIntervall(
                links = RationaleZahl.Null,
                linksOffen = false,
                rechts = RationaleZahl.Eins,
                rechtsOffen = false,
            ),
        ),
    )

    @Test
    fun `allgemeines Mass bleibt trotz angeforderter Kurzform sichtbar`() {
        val integral = methodenIntegral(
            methode = methode,
            bereich = bereich,
            kurz = true,
            mass = IntegralMass.Allgemein("\\nu"),
            vertrag = null,
        )

        assertEquals(IntegralMethodenDarstellung.VOLLSTAENDIG, integral.methodenDarstellung)
        assertTrue(integral.zuLatex().contains("d\\nu"))
    }

    @Test
    fun `gewichtetes Mass rendert Gewicht mal Massdifferential`() {
        val gewicht = Variable("w")
        val integral = termIntegral(
            term = x,
            bereiche = bereich.komponenten,
            bindungen = listOf(IntegralBindung(x, "quelle.x")),
            mass = IntegralMass.Gewichtet(IntegralMass.Allgemein("\\mu"), gewicht),
            vertrag = null,
        )

        assertTrue(integral.zuLatex().contains("w\\cdot d\\mu"))
        assertTrue(integral.zuLatex().contains("\\cdotw\\cdot d\\mu"))
    }
}
