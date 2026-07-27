package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class DefinierteMengeTest {
    @Test fun `latex einer eindimensionalen Menge`() {
        val x = Variable("x")
        assertEquals("\\left\\{x\\in\\mathbb{R}\\mid {x}^{2} = 1\\right\\}", DefinierteMenge(listOf(GebundeneMengenVariable(x, ReelleZahlen)), Gleichheit(Potenz(x, RationaleZahl.von(2)), RationaleZahl.Eins)).zuLatex())
    }

    @Test fun `latex einer R2 Menge und unterschiedlicher Grundmengen`() {
        val x = Variable("x"); val y = Variable("y")
        val gleich = Gleichheit(addition(Potenz(x, RationaleZahl.von(2)), Potenz(y, RationaleZahl.von(2))), RationaleZahl.Eins)
        assertTrue(DefinierteMenge(listOf(GebundeneMengenVariable(x, ReelleZahlen), GebundeneMengenVariable(y, ReelleZahlen)), gleich).zuLatex().contains("\\mathbb{R}^2"))
        assertTrue(DefinierteMenge(listOf(GebundeneMengenVariable(x, ReelleZahlen), GebundeneMengenVariable(y, NatürlicheZahlen)), gleich).zuLatex().contains("\\mathbb{R} \\times \\mathbb{N}"))
    }

    @Test fun `freie Variablen respektieren Mengenbindung`() {
        val x = Variable("x"); val y = Variable("y")
        val gleich = Gleichheit(x, y)
        assertEquals(setOf(x, y), gleich.freieVariablen())
        assertEquals(setOf(y), DefinierteMenge(listOf(GebundeneMengenVariable(x, ReelleZahlen)), gleich).freieVariablen())
    }

    @Test fun `Aussagen werden vollständig substituiert`() {
        val x = Variable("x"); val y = Variable("y")
        val gleich = assertIs<Gleichheit>(ersetze(Gleichheit(x, RationaleZahl.Eins), mapOf("x" to RationaleZahl.von(2))))
        assertEquals(RationaleZahl.von(2), gleich.links)
        val logisch = assertIs<Konjunktion>(ersetze(Konjunktion(listOf(Vergleich(x, VergleichsArt.Größer, RationaleZahl.Null), Vergleich(y, VergleichsArt.Kleiner, RationaleZahl.von(4)))), mapOf("x" to RationaleZahl.von(3))))
        assertEquals(RationaleZahl.von(3), (logisch.aussagen.first() as Vergleich).links)
    }
}
