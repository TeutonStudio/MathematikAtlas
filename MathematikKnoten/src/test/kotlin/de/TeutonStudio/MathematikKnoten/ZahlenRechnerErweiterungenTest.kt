package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class ZahlenRechnerErweiterungenTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()

    @Test
    fun `erweiterte Operatoren besitzen stabile eindeutige IDs`() {
        assertEquals(
            ErweiterterZahlenOperator.entries.size,
            ErweiterterZahlenOperator.entries.map { it.stabileId }.distinct().size,
        )
        assertTrue(ErweiterterZahlenOperator.entries.all { it.stabileId.startsWith("zahl.") })
    }

    @Test
    fun `Formelzustand erzeugt Eingänge aus freien Variablen`() {
        val basis = ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        val formel = konfiguriereZahlenRechnerFormel(basis, "x^2+y")
        val eingänge = formel.anschlüsse
            .filter { it.richtung == AnschlussRichtung.Eingang }
            .sortedBy { it.reihenfolge }

        assertEquals(ZAHLENRECHNER_FORMEL_ID, formel.parameter[ZAHLENRECHNER_OPERATOR])
        assertEquals(listOf("x", "y"), eingänge.map { it.name })
        assertEquals("x,y", formel.parameter[ZAHLENRECHNER_FORMEL_VARIABLEN])
        assertEquals("Formel", formel.name)
    }

    @Test
    fun `Formelzustand zeigt Variablen trotz eingesetzter Werte`() {
        val basis = ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        val formel = konfiguriereZahlenRechnerFormel(basis, "x+y")
        val auswerter = register.finde(ZAHLENRECHNER_ART)!!
        val ergebnis = auswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten = formel,
                eingänge = mapOf(
                    "x" to BedingterWert(RationaleZahl.von(2)),
                    "y" to BedingterWert(RationaleZahl.von(3)),
                ),
                rechenKontext = RechenKontext(),
            ),
        )

        val ausgabe = ergebnis.ausgaben.getValue("wert")
        val sichtbaresLatex = ausgabe.latexDarstellung.orEmpty()
        val eingesetztesLatex = ausgabe.objekt.zuStrukturLatex()

        assertTrue("x" in sichtbaresLatex)
        assertTrue("y" in sichtbaresLatex)
        assertTrue("+" in sichtbaresLatex)
        assertFalse("2" in sichtbaresLatex)
        assertFalse("3" in sichtbaresLatex)
        assertTrue("2" in eingesetztesLatex)
        assertTrue("3" in eingesetztesLatex)
    }

    @Test
    fun `Tangenszustand wird durch erweiterten Auswerter dargestellt`() {
        val basis = ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        val tangens = konfiguriereErweitertenZahlenRechner(basis, ErweiterterZahlenOperator.TANGENS)
        val ergebnis = register.finde(ZAHLENRECHNER_ART)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten = tangens,
                eingänge = mapOf("a" to BedingterWert(Variable("x"))),
                rechenKontext = RechenKontext(),
            ),
        )

        assertTrue(ergebnis.ausgaben.getValue("wert").latexDarstellung.orEmpty().startsWith("\\tan"))
        assertTrue(ergebnis.warnungen.any { "cos(x)" in it })
    }

    @Test
    fun `Tangenszustand hebt Zahlenfunktionen punktweise mit Definitionsbedingung`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen),
        )
        val basis = ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
        val tangens = konfiguriereErweitertenZahlenRechner(basis, ErweiterterZahlenOperator.TANGENS)
        val ergebnis = register.finde(ZAHLENRECHNER_ART)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten = tangens,
                eingänge = mapOf("a" to BedingterWert(methode)),
                rechenKontext = RechenKontext(),
            ),
        )

        val punktweise = assertIs<Methode>(ergebnis.ausgaben.getValue("wert").objekt)
        assertEquals(listOf("x"), punktweise.parameter.map { it.name })
        assertTrue(punktweise.methodenSignatur().werteVorrat.zuLatex().contains("\\cos"))
        assertTrue(ergebnis.warnungen.any { it.startsWith("Signatur:") })
    }
}
