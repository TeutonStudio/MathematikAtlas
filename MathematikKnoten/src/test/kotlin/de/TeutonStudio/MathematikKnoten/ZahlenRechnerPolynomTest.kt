package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class ZahlenRechnerPolynomTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()

    @Test
    fun `Polynomzustand verwendet Koordinaten und Argument als Eingänge`() {
        val basis = ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        val polynom = konfiguriereErweitertenZahlenRechner(
            basis,
            ErweiterterZahlenOperator.POLYNOM,
        )
        val eingänge = polynom.anschlüsse
            .filter { it.richtung == AnschlussRichtung.Eingang }
            .sortedBy { it.reihenfolge }
        val ausgang = polynom.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }

        assertEquals("zahl.polynom", polynom.parameter[ZAHLENRECHNER_OPERATOR])
        assertEquals(listOf("koeffizienten", "argument"), eingänge.map { it.name })
        assertEquals(MathematikAnschlussArten.Objekt.id, eingänge[0].art)
        assertEquals(
            setOf(MathematikAnschlussArten.Tupel.id, MathematikAnschlussArten.Vektor.id),
            eingänge[0].zulässigeArten,
        )
        assertEquals(MathematikAnschlussArten.Zahl.id, eingänge[1].art)
        assertEquals(MathematikAnschlussArten.Zahl.id, ausgang.art)
        assertNull(ausgang.artPriorisiertEingänge)
    }

    @Test
    fun `Polynomzustand setzt das Argument ein und liefert einen Zahlterm`() {
        val polynom = polynomKnoten()
        val ergebnis = wertePolynomDirektAus(
            polynom,
            mapOf(
                "koeffizienten" to BedingterWert(
                    Tupel(
                        listOf(
                            RationaleZahl.Eins,
                            RationaleZahl.von(2),
                            RationaleZahl.von(3),
                        ),
                    ),
                ),
                "argument" to BedingterWert(RationaleZahl.von(2)),
            ),
        )

        val wert = ergebnis.ausgaben.getValue("wert").objekt
        assertIs<ZahlAusdruck>(wert)
        assertFalse(wert is Methode)
        assertEquals(RationaleZahl.von(17), wert)
        assertNull(ergebnis.fehler)
    }

    @Test
    fun `historische Vektorkoeffizienten bleiben im Zahlenrechner verwendbar`() {
        val polynom = polynomKnoten()
        val ergebnis = wertePolynomDirektAus(
            polynom,
            mapOf(
                "koeffizienten" to BedingterWert(
                    SpaltenVektor(listOf(RationaleZahl.Eins, RationaleZahl.von(2))),
                ),
                "argument" to BedingterWert(Variable("x")),
            ),
        )

        val wert = assertIs<ZahlAusdruck>(ergebnis.ausgaben.getValue("wert").objekt)
        assertTrue("x" in wert.zuLatex())
        assertNull(ergebnis.fehler)
    }

    @Test
    fun `fehlende Polynomkoeffizienten liefern kontrollierten Knotenfehler`() {
        val ergebnis = wertePolynomDirektAus(
            polynomKnoten(),
            mapOf("argument" to BedingterWert(RationaleZahl.von(2))),
        )

        assertTrue(ergebnis.ausgaben.isEmpty())
        assertEquals("Die Koeffizienten fehlen.", ergebnis.fehler)
    }

    @Test
    fun `leere Koeffizientenfolge liefert kontrollierten Knotenfehler`() {
        val ergebnis = wertePolynomDirektAus(
            polynomKnoten(),
            mapOf(
                "koeffizienten" to BedingterWert(Tupel(emptyList())),
                "argument" to BedingterWert(RationaleZahl.von(2)),
            ),
        )

        assertEquals("Ein Polynom benötigt mindestens einen Koeffizienten.", ergebnis.fehler)
    }

    @Test
    fun `nichtnumerischer Koeffizient liefert kontrollierten Knotenfehler`() {
        val ergebnis = wertePolynomDirektAus(
            polynomKnoten(),
            mapOf(
                "koeffizienten" to BedingterWert(Tupel(listOf(RationaleZahl.Eins, ReelleZahlen))),
                "argument" to BedingterWert(RationaleZahl.von(2)),
            ),
        )

        assertEquals("Koeffizient 2 ist keine Zahl.", ergebnis.fehler)
    }

    @Test
    fun `nichtnumerisches Polynomargument liefert kontrollierten Knotenfehler`() {
        val ergebnis = wertePolynomDirektAus(
            polynomKnoten(),
            mapOf(
                "koeffizienten" to BedingterWert(Tupel(listOf(RationaleZahl.Eins))),
                "argument" to BedingterWert(Tupel(listOf(RationaleZahl.Eins))),
            ),
        )

        assertEquals("Das Polynomargument ist kein Zahlterm.", ergebnis.fehler)
    }

    @Test
    fun `alter Vektor zu Polynom Knoten ist nicht mehr erzeugbar`() {
        assertFalse(alleMathematikKnotenVorlagen().any { it.art == "mathematik.vektorZuPolynom" })
    }

    private fun polynomKnoten() = konfiguriereErweitertenZahlenRechner(
        ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero),
        ErweiterterZahlenOperator.POLYNOM,
    )

    private fun wertePolynomDirektAus(
        knoten: KnotenDaten,
        eingänge: Map<String, BedingterWert>,
    ) = register.finde(ZAHLENRECHNER_ART)!!.auswerten(
        KnotenAuswertungsKontext(
            knoten = knoten,
            eingänge = eingänge,
            rechenKontext = RechenKontext(),
        ),
    )
}