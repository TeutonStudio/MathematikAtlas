package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MengenMassNormTest {
    @Test
    fun `zaehlmass normiert endliche menge auf ihre maechtigkeit`() {
        val menge = EndlicheMenge(
            setOf(
                RationaleZahl.von(1),
                RationaleZahl.von(2),
                RationaleZahl.von(3),
            ),
        )

        val ergebnis = normEinerMenge(menge, IntegralMass.Zaehlmass)

        assertEquals(RationaleZahl.von(3), ergebnis.wert)
        assertEquals(IntegralUnterstuetzungsStatus.EXAKT, ergebnis.status)
        assertTrue(ergebnis.voraussetzungen.isEmpty())
    }

    @Test
    fun `allgemeines mass behaelt integral und messbarkeitsvoraussetzung`() {
        val menge = BenannteMenge("A")
        val ergebnis = normEinerMenge(menge, IntegralMass.Allgemein("\\mu"))

        assertEquals("\\int_{A} 1\\,\\mathrm d\\mu", ergebnis.wert.zuLatex())
        assertEquals("\\int_{A}1\\cdot\\mathrm d\\mu", ergebnis.integral.zuLatex())
        assertTrue(ergebnis.voraussetzungen.isNotEmpty())
    }

    @Test
    fun `iteriertes kartesisches produkt verwendet grossen produktoperator`() {
        val i = Variable("i")
        val methode = Methode(
            name = "A",
            parameter = listOf(i),
            vorschrift = BenannteMenge("A_i", "A_i"),
            zielMenge = Potenzmenge(BenannteMenge("U")),
            werteVorräte = mapOf(i.name to NatürlicheZahlen),
        )
        val ausdruck = IteriertesKartesischesProdukt(methode, NatürlicheZahlen)

        assertTrue(ausdruck.zuLatex().startsWith("\\mathop{\\Large\\times}\\limits_"))
    }
}
