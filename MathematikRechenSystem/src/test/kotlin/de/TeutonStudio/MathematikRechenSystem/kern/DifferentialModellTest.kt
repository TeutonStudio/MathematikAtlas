package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DifferentialModellTest {
    @Test
    fun `Argumentindizes beginnen bei eins`() {
        assertFailsWith<IllegalArgumentException> { DifferentialOperator.Partiell(0) }

        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = RationaleZahlen,
            werteVorräte = mapOf("x" to RationaleZahlen),
        )

        assertFailsWith<IllegalArgumentException> { partielleAbleitung(methode, 2) }
        assertEquals("\\partial_{1}f", partielleAbleitung(methode, 1).zuLatex())
    }

    @Test
    fun `Methodendifferential verwendet eingeschraenkte Identitaet`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = Potenz(x, RationaleZahl.von(2)),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen),
        )

        val differential = MethodenDifferentialGleichung(methode, ReelleZahlen)

        assertEquals(
            "df=f^{\\mathrm I}\\cdotd\\left(\\operatorname{id}\\vert_{\\mathbb R}\\right)",
            differential.zuLatex(),
        )
    }

    @Test
    fun `Termdifferential behaelt Differentialvariable und Quellen ID`() {
        val x = Variable("x")
        val differential = bildeDifferentialTerm(
            term = Potenz(x, RationaleZahl.von(2)),
            variable = x,
            quellenId = "argument-f-x",
        )

        assertEquals("argument-f-x", differential.quellenId)
        assertEquals("dx", differential.differentialVariable.zuLatex())
        assertTrue(differential.zuLatex().contains("\\cdotdx"))
    }

    @Test
    fun `totale und partielle Ableitung stimmen nur eindimensional automatisch ueberein`() {
        val x = Variable("x")
        val y = Variable("y")
        val eindimensional = Methode("f", listOf(x), x, ReelleZahlen, mapOf("x" to ReelleZahlen))
        val mehrdimensional = Methode("g", listOf(x, y), x, ReelleZahlen, mapOf("x" to ReelleZahlen, "y" to ReelleZahlen))

        assertTrue(eindimensionaleAbleitungenStimmenUeberein(eindimensional))
        assertFalse(eindimensionaleAbleitungenStimmenUeberein(mehrdimensional))
    }
}
