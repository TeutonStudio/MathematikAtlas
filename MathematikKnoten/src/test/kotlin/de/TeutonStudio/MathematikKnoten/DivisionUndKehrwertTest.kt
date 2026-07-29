package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class DivisionUndKehrwertTest {
    private val auswerter = KartenAuswerter(GesamterMathematikAuswerter.erzeugeRegister())

    @Test fun `Kehrwert wird als Potenz minus eins ausgewertet`() {
        val vier = zahl("vier", "4")
        val kehrwert = MathematikKnotenVorlagen.Kehrwert.erzeuge(GraphPunkt(300f, 0f)).copy(id = KnotenId("kehrwert"))
        val karte = KartenDaten(
            name = "Kehrwert",
            knoten = listOf(vier, kehrwert),
            verbindungen = listOf(VerbindungDaten(von = ausgang(vier), zu = eingang(kehrwert, "zahl"))),
        )

        val ergebnis = auswerter.auswerten(karte)

        assertTrue(ergebnis.fehler.isEmpty(), ergebnis.fehler.joinToString())
        assertEquals(RationaleZahl.von(1, 4), ergebnis.knoten.getValue(kehrwert.id).ausgaben.getValue("wert").objekt)
    }

    @Test fun `Kehrwert von null wird mit verständlichem Fehler abgelehnt`() {
        val nullKnoten = zahl("null", "0")
        val kehrwert = MathematikKnotenVorlagen.Kehrwert.erzeuge(GraphPunkt(300f, 0f)).copy(id = KnotenId("kehrwert"))
        val karte = KartenDaten(
            name = "Kehrwert von null",
            knoten = listOf(nullKnoten, kehrwert),
            verbindungen = listOf(VerbindungDaten(von = ausgang(nullKnoten), zu = eingang(kehrwert, "zahl"))),
        )

        val ergebnis = auswerter.auswerten(karte)

        assertTrue(ergebnis.fehler.single().contains("Kehrwert von 0"))
    }

    @Test fun `Division verwendet bei nichtnull Divisor den Quotienten`() {
        val (karte, division) = divisionsKarte("8", "2", "99")

        val ergebnis = auswerter.auswerten(karte)

        assertTrue(ergebnis.fehler.isEmpty(), ergebnis.fehler.joinToString())
        assertEquals(RationaleZahl.von(4), ergebnis.knoten.getValue(division.id).ausgaben.getValue("wert").objekt)
    }

    @Test fun `Division verwendet bei Divisor null den dritten Eingang`() {
        val (karte, division) = divisionsKarte("8", "0", "99")

        val ergebnis = auswerter.auswerten(karte)

        assertTrue(ergebnis.fehler.isEmpty(), ergebnis.fehler.joinToString())
        assertEquals(RationaleZahl.von(99), ergebnis.knoten.getValue(division.id).ausgaben.getValue("wert").objekt)
    }

    @Test fun `Unbekannter Divisor erhält beide Fälle symbolisch`() {
        val dividend = zahl("dividend", "8")
        val divisor = MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt(0f, 170f)).copy(
            id = KnotenId("divisor"),
            parameter = mapOf("name" to "y", "werteVorrat" to "R"),
        )
        val ersatz = zahl("ersatz", "99", 340f)
        val division = MathematikKnotenVorlagen.Division.erzeuge(GraphPunkt(620f, 100f)).copy(id = KnotenId("division"))
        val karte = KartenDaten(
            name = "Symbolische Division",
            knoten = listOf(dividend, divisor, ersatz, division),
            verbindungen = listOf(
                VerbindungDaten(von = ausgang(dividend), zu = eingang(division, "dividend")),
                VerbindungDaten(von = ausgang(divisor), zu = eingang(division, "divisor")),
                VerbindungDaten(von = ausgang(ersatz), zu = eingang(division, "fallsNennerNull")),
            ),
        )

        val ergebnis = auswerter.auswerten(karte)

        assertTrue(ergebnis.fehler.isEmpty(), ergebnis.fehler.joinToString())
        val wert = ergebnis.knoten.getValue(division.id).ausgaben.getValue("wert").objekt
        assertIs<FallAusdruck>(wert)
        assertEquals(RationaleZahl.von(99), wert.wahr)
        assertEquals(Gleichheit(Variable("y"), RationaleZahl.Null), wert.aussage)
    }

    @Test fun `Aliase bleiben in der Divisionsfallformel erhalten`() {
        val dividend = zahl("dividend", "8")
        val divisor = zahl("divisor", "2", 170f)
        val ersatz = zahl("ersatz", "99", 340f)
        val division = MathematikKnotenVorlagen.Division.erzeuge(GraphPunkt(620f, 100f)).copy(id = KnotenId("division"))
        val karte = KartenDaten(
            name = "Division mit Aliasen",
            knoten = listOf(dividend, divisor, ersatz, division),
            verbindungen = listOf(
                VerbindungDaten(von = ausgang(dividend), zu = eingang(division, "dividend")),
                VerbindungDaten(von = ausgang(divisor), zu = eingang(division, "divisor")),
                VerbindungDaten(von = ausgang(ersatz), zu = eingang(division, "fallsNennerNull")),
            ),
        )
        val vorgaben = mapOf(
            dividend.id to mapOf("wert" to BedingterWert(RationaleZahl.von(8), latexDarstellung = "x")),
            divisor.id to mapOf("wert" to BedingterWert(RationaleZahl.von(2), latexDarstellung = "y")),
            ersatz.id to mapOf("wert" to BedingterWert(RationaleZahl.von(99), latexDarstellung = "e")),
        )

        val ergebnis = auswerter.auswerten(karte, vorgaben)
        val latex = ergebnis.knoten.getValue(division.id).ausgaben.getValue("wert").anzeigeLatex()

        assertTrue(latex.contains("e,&y=0"), latex)
        assertTrue(latex.contains("\\frac{x}{y}"), latex)
        assertTrue(latex.contains("y\\ne0"), latex)
    }

    private fun divisionsKarte(dividendText: String, divisorText: String, ersatzText: String): Pair<KartenDaten, KnotenDaten> {
        val dividend = zahl("dividend", dividendText)
        val divisor = zahl("divisor", divisorText, 170f)
        val ersatz = zahl("ersatz", ersatzText, 340f)
        val division = MathematikKnotenVorlagen.Division.erzeuge(GraphPunkt(620f, 100f)).copy(id = KnotenId("division"))
        return KartenDaten(
            name = "Division",
            knoten = listOf(dividend, divisor, ersatz, division),
            verbindungen = listOf(
                VerbindungDaten(von = ausgang(dividend), zu = eingang(division, "dividend")),
                VerbindungDaten(von = ausgang(divisor), zu = eingang(division, "divisor")),
                VerbindungDaten(von = ausgang(ersatz), zu = eingang(division, "fallsNennerNull")),
            ),
        ) to division
    }

    private fun zahl(id: String, wert: String, y: Float = 0f) = MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt(0f, y)).copy(
        id = KnotenId(id),
        parameter = mapOf("wert" to wert),
    )

    private fun ausgang(knoten: KnotenDaten) = AnschlussVerweis(
        knoten.id,
        knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }.id,
    )

    private fun eingang(knoten: KnotenDaten, name: String) = AnschlussVerweis(
        knoten.id,
        knoten.anschlüsse.single { it.richtung == AnschlussRichtung.Eingang && it.name == name }.id,
    )
}
