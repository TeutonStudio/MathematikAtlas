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
        val basis = ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        val polynom = konfiguriereErweitertenZahlenRechner(
            basis,
            ErweiterterZahlenOperator.POLYNOM,
        )
        val ergebnis = register.finde(ZAHLENRECHNER_ART)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten = polynom,
                eingänge = mapOf(
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
                rechenKontext = RechenKontext(),
            ),
        )

        val wert = ergebnis.ausgaben.getValue("wert").objekt
        assertIs<ZahlAusdruck>(wert)
        assertFalse(wert is Methode)
        assertEquals(RationaleZahl.von(17), wert)
    }

    @Test
    fun `historische Vektorkoeffizienten bleiben im Zahlenrechner verwendbar`() {
        val basis = ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        val polynom = konfiguriereErweitertenZahlenRechner(
            basis,
            ErweiterterZahlenOperator.POLYNOM,
        )
        val ergebnis = register.finde(ZAHLENRECHNER_ART)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten = polynom,
                eingänge = mapOf(
                    "koeffizienten" to BedingterWert(
                        SpaltenVektor(listOf(RationaleZahl.Eins, RationaleZahl.von(2))),
                    ),
                    "argument" to BedingterWert(Variable("x")),
                ),
                rechenKontext = RechenKontext(),
            ),
        )

        val wert = assertIs<ZahlAusdruck>(ergebnis.ausgaben.getValue("wert").objekt)
        assertTrue("x" in wert.zuLatex())
    }

    @Test
    fun `alter Vektor zu Polynom Knoten ist nicht mehr erzeugbar`() {
        assertFalse(alleMathematikKnotenVorlagen().any { it.art == "mathematik.vektorZuPolynom" })
    }
}
