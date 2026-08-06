package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class VorzeichenVerzweigungTest {
    private val x = Variable("x")
    private val y = Variable("y")

    @Test
    fun `plus minus bedeutet geordnet plus dann minus`() {
        val verzweigung = VorzeichenVerzweigung(
            VorzeichenReihenfolge.PLUS_MINUS,
            x,
            VerzweigungsQuellenId("s1"),
        )

        assertEquals("\\pm\\,x", verzweigung.zuLatex())
        assertEquals(listOf(x, negation(x)), verzweigung.entfalte().komponenten)
    }

    @Test
    fun `minus plus bedeutet geordnet minus dann plus`() {
        val verzweigung = VorzeichenVerzweigung(
            VorzeichenReihenfolge.MINUS_PLUS,
            x,
            VerzweigungsQuellenId("s1"),
        )

        assertEquals("\\mp\\,x", verzweigung.zuLatex())
        assertEquals(listOf(negation(x), x), verzweigung.entfalte().komponenten)
    }

    @Test
    fun `gleiche Quelle koppelt Zweige positionsweise`() {
        val quelle = VerzweigungsQuellenId("gekoppelt")
        val links = VorzeichenVerzweigung(VorzeichenReihenfolge.PLUS_MINUS, x, quelle).entfalte()
        val rechts = VorzeichenVerzweigung(VorzeichenReihenfolge.PLUS_MINUS, y, quelle).entfalte()
        val produkt = multipliziereVerzweigungen(links, rechts)

        assertEquals(2, produkt.zweige.size)
        assertEquals(listOf(multiplikation(x, y), multiplikation(x, y)), produkt.komponenten)
    }

    @Test
    fun `verschiedene Quellen bilden stabiles kartesisches Produkt`() {
        val links = VorzeichenVerzweigung(
            VorzeichenReihenfolge.PLUS_MINUS,
            x,
            VerzweigungsQuellenId("aelter"),
        ).entfalte()
        val rechts = VorzeichenVerzweigung(
            VorzeichenReihenfolge.PLUS_MINUS,
            y,
            VerzweigungsQuellenId("juenger"),
        ).entfalte()
        val produkt = multipliziereVerzweigungen(links, rechts)

        assertEquals(4, produkt.zweige.size)
        assertEquals(
            listOf(
                multiplikation(x, y),
                negation(multiplikation(x, y)),
                negation(multiplikation(x, y)),
                multiplikation(x, y),
            ),
            produkt.komponenten,
        )
        assertEquals(
            listOf(VerzweigungsQuellenId("aelter"), VerzweigungsQuellenId("juenger")),
            produkt.quellenReihenfolge,
        )
    }

    @Test
    fun `doppelte Null bleibt im Tupel und verschwindet erst in der Loesungsmenge`() {
        val nullZweige = VorzeichenVerzweigung(
            VorzeichenReihenfolge.PLUS_MINUS,
            RationaleZahl.Null,
            VerzweigungsQuellenId("null"),
        ).entfalte()

        assertEquals(2, nullZweige.komponenten.size)
        assertEquals(listOf(RationaleZahl.Null, RationaleZahl.Null), nullZweige.komponenten)
        assertEquals(setOf(RationaleZahl.Null), nullZweige.alsLoesungsMenge().elemente)
    }

    @Test
    fun `quadratische Loesung substituiert vor der Hauptwurzel`() {
        val a = Variable("a")
        val loesung = loeseQuadratischePotenzMitVorzeichenSubstitution(x, a)

        assertEquals(
            listOf(
                QuadratischerLoesungsSchrittArt.VORZEICHEN_SUBSTITUTION,
                QuadratischerLoesungsSchrittArt.HAUPTWURZEL,
                QuadratischerLoesungsSchrittArt.RUECKSUBSTITUTION,
            ),
            loesung.schritte.map { it.art },
        )
        assertEquals(listOf(Wurzel(a), negation(Wurzel(a))), loesung.geordnetesErgebnis.komponenten)
        assertEquals(1, loesung.bedingungen.size)
        assertIs<Vergleich>(loesung.bedingungen.single())
        assertTrue(loesung.schritte.first().latex.contains("\\pm\\,\\varphi"))
    }
}
