package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class StrukturierteDivisionTest {
    @Test
    fun `rechte Division multipliziert das Inverse rechts`() {
        val a = Variable("a")
        val b = Variable("b")
        val division = StrukturierteDivision(
            a,
            b,
            DivisionsSeite.RECHTS,
            KommutativitaetsStatus.NICHT_KOMMUTATIV,
        )

        assertEquals("a\\div_{R}\\,b", division.zuLatex())
        assertEquals(listOf(a, InversesElement(b)), division.alsGeordnetesProdukt().faktoren)
    }

    @Test
    fun `linke Division multipliziert das Inverse links`() {
        val a = Variable("a")
        val b = Variable("b")
        val division = StrukturierteDivision(
            a,
            b,
            DivisionsSeite.LINKS,
            KommutativitaetsStatus.NICHT_KOMMUTATIV,
        )

        assertEquals("a\\div_{L}\\,b", division.zuLatex())
        assertEquals(listOf(InversesElement(b), a), division.alsGeordnetesProdukt().faktoren)
    }

    @Test
    fun `Seitenwechsel behaelt dieselbe Operator ID`() {
        val rechts = StrukturierteDivision(Variable("a"), Variable("b"), DivisionsSeite.RECHTS)
        val links = rechts.copy(seite = DivisionsSeite.LINKS)

        assertEquals("algebra.division", rechts.operatorId)
        assertEquals(rechts.operatorId, links.operatorId)
        assertEquals(mapOf("divisionsSeite" to "rechts"), rechts.persistenzParameter)
        assertEquals(mapOf("divisionsSeite" to "links"), links.persistenzParameter)
    }

    @Test
    fun `kommutative Division normalisiert zum bestehenden Bruch`() {
        val a = Variable("a")
        val b = RationaleZahl.von(2)
        val ergebnis = strukturierteDivision(
            a,
            b,
            DivisionsSeite.LINKS,
            KommutativitaetsStatus.NACHGEWIESEN,
        )

        assertEquals(Division(a, b), assertIs<Division>(ergebnis))
        assertEquals("\\frac{a}{2}", ergebnis.zuLatex())
    }

    @Test
    fun `unbekannte Invertierbarkeit bleibt strukturierte Voraussetzung`() {
        val ergebnis = StrukturierteDivision(
            Variable("a"),
            Variable("b"),
            DivisionsSeite.RECHTS,
            struktur = ZahlbereichsIds.QUATERNION,
        )

        assertTrue(
            ergebnis.effektiveVoraussetzungen.any {
                it.art == DivisionsVoraussetzungsArt.DIVISOR_INVERTIERBAR
            },
        )
    }

    @Test
    fun `Division durch null kann nicht zum geordneten Produkt werden`() {
        val division = StrukturierteDivision(
            Variable("a"),
            RationaleZahl.Null,
            DivisionsSeite.RECHTS,
        )

        assertTrue(division.istNachweislichUngueltig)
        assertFailsWith<IllegalArgumentException> { division.alsGeordnetesProdukt() }
    }

    @Test
    fun `historische nichtkommutative Division ohne Seite bleibt mehrdeutig`() {
        val ergebnis = migriereHistorischeDivision(
            Variable("a"),
            Variable("b"),
            KommutativitaetsStatus.NICHT_KOMMUTATIV,
        )

        val auswahl = assertIs<DivisionsMigrationErgebnis.SeitenAuswahlErforderlich>(ergebnis)
        assertEquals(DivisionsSeite.entries, auswahl.erlaubteSeiten)
    }

    @Test
    fun `historische kommutative Division wird ohne geratenen Seitenwert uebernommen`() {
        val ergebnis = migriereHistorischeDivision(
            Variable("a"),
            RationaleZahl.von(2),
            KommutativitaetsStatus.NACHGEWIESEN,
        )

        val erfolg = assertIs<DivisionsMigrationErgebnis.Erfolg>(ergebnis)
        assertIs<Division>(erfolg.ausdruck)
        assertEquals(null, erfolg.seite)
        assertTrue(erfolg.kommutativNormalisiert)
    }
}
