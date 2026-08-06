package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class StrukturierteDivisionTest {
    @Test
    fun `rechte Division multipliziert das Inverse rechts`() {
        val a = Variable("a")
        val b = Variable("b")
        val division = StrukturierteDivision(a, b, DivisionsSeite.RECHTS, KommutativitaetsStatus.NICHT_KOMMUTATIV)

        assertEquals("a\\div_Rb", division.zuLatex())
        assertEquals(listOf(a, InversesElement(b)), division.alsGeordnetesProdukt().faktoren)
    }

    @Test
    fun `linke Division multipliziert das Inverse links`() {
        val a = Variable("a")
        val b = Variable("b")
        val division = StrukturierteDivision(a, b, DivisionsSeite.LINKS, KommutativitaetsStatus.NICHT_KOMMUTATIV)

        assertEquals("a\\div_Lb", division.zuLatex())
        assertEquals(listOf(InversesElement(b), a), division.alsGeordnetesProdukt().faktoren)
    }

    @Test
    fun `kommutative Division normalisiert zum bestehenden Bruch`() {
        val a = Variable("a")
        val b = Variable("b")
        val ergebnis = strukturierteDivision(a, b, DivisionsSeite.LINKS, KommutativitaetsStatus.NACHGEWIESEN)

        assertEquals(Division(a, b), assertIs<Division>(ergebnis))
        assertEquals("\\frac{a}{b}", ergebnis.zuLatex())
    }

    @Test
    fun `unbekannte Kommutativitaet behaelt die Seite sichtbar`() {
        val ergebnis = StrukturierteDivision(
            Variable("a"),
            Variable("b"),
            DivisionsSeite.RECHTS,
            struktur = ZahlbereichsIds.QUATERNION,
        )

        assertEquals("algebra.division.rechts", ergebnis.operatorId)
        assertEquals("a\\div_Rb", ergebnis.zuLatex())
    }
}
