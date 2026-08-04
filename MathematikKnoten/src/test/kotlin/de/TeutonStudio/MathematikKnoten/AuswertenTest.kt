package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussVerweis
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungDaten
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KartenAuswerter
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikKartenAdapter.VariablenQuelle
import de.TeutonStudio.MathematikRechenSystem.kern.EndlicheMenge
import de.TeutonStudio.MathematikRechenSystem.kern.Division
import de.TeutonStudio.MathematikRechenSystem.kern.Gleichheit
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.RechenKontext
import de.TeutonStudio.MathematikRechenSystem.kern.WahrheitsKonstante
import de.TeutonStudio.MathematikRechenSystem.kern.Variable
import de.TeutonStudio.MathematikRechenSystem.kern.ReelleZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.NatürlicheZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.KomplexeZahlen
import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import de.TeutonStudio.MathematikRechenSystem.kern.AllgemeinerParameter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFailsWith

class AuswertenTest {
    @Test
    fun `falsche Aussagen geben Lüge aus`() {
        val auswerten = MathematikKnotenVorlagen.Auswerten.erzeuge(GraphPunkt.Zero)
        val aussage = Gleichheit(RationaleZahl.von(2), EndlicheMenge(setOf(RationaleZahl.von(1), RationaleZahl.von(2), RationaleZahl.von(3))))
        val auswerter = StandardMathematikAuswerter.erzeugeRegister().finde(auswerten.art)!!

        val ergebnis = auswerter.auswerten(
            KnotenAuswertungsKontext(auswerten, mapOf("term" to BedingterWert(aussage)), RechenKontext()),
        )

        val auswertung = assertIs<WahrheitsKonstante>(ergebnis.ausgaben.getValue("term").objekt)
        assertEquals(false, auswertung.wert)
    }

    @Test
    fun `wahre Aussagen geben Wahr aus`() {
        val auswerten = MathematikKnotenVorlagen.Auswerten.erzeuge(GraphPunkt.Zero)
        val aussage = Gleichheit(RationaleZahl.von(2), RationaleZahl.von(2))
        val auswerter = StandardMathematikAuswerter.erzeugeRegister().finde(auswerten.art)!!

        val ergebnis = auswerter.auswerten(
            KnotenAuswertungsKontext(auswerten, mapOf("term" to BedingterWert(aussage)), RechenKontext()),
        )

        val auswertung = assertIs<WahrheitsKonstante>(ergebnis.ausgaben.getValue("term").objekt)
        assertEquals(true, auswertung.wert)
    }

    @Test
    fun `Term zu Methode leitet allgemeine freie Variablen und ihre Reihenfolge ab`() {
        val knoten = MathematikKnotenVorlagen.TermZuMethode.erzeuge(GraphPunkt.Zero).copy(
            parameter = mapOf("name" to "g", "argumentReihenfolge" to "y,x"),
        )
        val auswerter = StandardMathematikAuswerter.erzeugeRegister().finde(knoten.art)!!
        val xQuelle = KnotenId("x-quelle")
        val yQuelle = KnotenId("y-quelle")
        val ergebnis = auswerter.auswerten(KnotenAuswertungsKontext(
            knoten,
            mapOf(
                "term" to BedingterWert(
                    Gleichheit(Variable("x"), Variable("y")),
                    variablenQuellen = listOf(
                        VariablenQuelle(xQuelle, "x", NatürlicheZahlen),
                        VariablenQuelle(yQuelle, "y", ReelleZahlen),
                    ),
                ),
            ),
            RechenKontext(),
            mapOf(xQuelle to 0, yQuelle to 1),
        ))

        val methode = assertIs<Methode>(ergebnis.ausgaben.getValue("methode").objekt)
        assertEquals(listOf(Variable("y"), Variable("x")), methode.parameter)
        assertEquals(ReelleZahlen, methode.werteVorräte.getValue("y"))
        assertEquals(NatürlicheZahlen, methode.werteVorräte.getValue("x"))
        assertEquals(EndlicheMenge(setOf(WahrheitsKonstante(true), WahrheitsKonstante(false))), methode.einzigeZielMenge)
        assertIs<Gleichheit>(methode.vorschrift)
    }

    @Test
    fun `Term zu Methode lehnt widersprüchliche Wertevorräte gleicher Variablen ab`() {
        val knoten = MathematikKnotenVorlagen.TermZuMethode.erzeuge(GraphPunkt.Zero)
        val auswerter = StandardMathematikAuswerter.erzeugeRegister().finde(knoten.art)!!

        assertFailsWith<IllegalArgumentException> {
            auswerter.auswerten(KnotenAuswertungsKontext(
                knoten,
                mapOf("term" to BedingterWert(
                    Variable("x"),
                    variablenQuellen = listOf(
                        VariablenQuelle(KnotenId("a"), "x", NatürlicheZahlen),
                        VariablenQuelle(KnotenId("b"), "x", ReelleZahlen),
                    ),
                )),
                RechenKontext(),
            ))
        }
    }

    @Test
    fun `Term zu Methode leitet die Zielmenge ohne alten Inspectorwert ab`() {
        val knoten = MathematikKnotenVorlagen.TermZuMethode.erzeuge(GraphPunkt.Zero).copy(
            parameter = mapOf("name" to "f", "zielmenge" to "C", "argumentReihenfolge" to ""),
        )
        val auswerter = StandardMathematikAuswerter.erzeugeRegister().finde(knoten.art)!!
        val xQuelle = KnotenId("x-quelle")

        val ergebnis = auswerter.auswerten(KnotenAuswertungsKontext(
            knoten,
            mapOf("term" to BedingterWert(
                Division(Variable("x"), RationaleZahl.von(2)),
                variablenQuellen = listOf(VariablenQuelle(xQuelle, "x", NatürlicheZahlen)),
            )),
            RechenKontext(),
            mapOf(xQuelle to 0),
        ))

        val methode = assertIs<Methode>(ergebnis.ausgaben.getValue("methode").objekt)
        assertEquals(RationaleZahlen, methode.einzigeZielMenge)
    }

    @Test
    fun `Term zu Methode leitet die Zielmenge für komplexe Variablen ab`() {
        val knoten = MathematikKnotenVorlagen.TermZuMethode.erzeuge(GraphPunkt.Zero).copy(
            parameter = mapOf("name" to "f", "zielmenge" to "N", "argumentReihenfolge" to ""),
        )
        val auswerter = StandardMathematikAuswerter.erzeugeRegister().finde(knoten.art)!!
        val xQuelle = KnotenId("x-quelle")

        val ergebnis = auswerter.auswerten(KnotenAuswertungsKontext(
            knoten,
            mapOf("term" to BedingterWert(
                Variable("x"),
                variablenQuellen = listOf(VariablenQuelle(xQuelle, "x", KomplexeZahlen)),
            )),
            RechenKontext(),
            mapOf(xQuelle to 0),
        ))

        val methode = assertIs<Methode>(ergebnis.ausgaben.getValue("methode").objekt)
        assertEquals(KomplexeZahlen, methode.einzigeZielMenge)
    }

    @Test
    fun `Term zu Methode leitet die Zielmenge für Aussagen ab`() {
        val knoten = MathematikKnotenVorlagen.TermZuMethode.erzeuge(GraphPunkt.Zero).copy(
            parameter = mapOf("name" to "f", "zielmenge" to "N", "argumentReihenfolge" to ""),
        )
        val auswerter = StandardMathematikAuswerter.erzeugeRegister().finde(knoten.art)!!
        val xQuelle = KnotenId("x-quelle")

        val ergebnis = auswerter.auswerten(KnotenAuswertungsKontext(
            knoten,
            mapOf("term" to BedingterWert(
                Gleichheit(Variable("x"), Variable("x")),
                variablenQuellen = listOf(VariablenQuelle(xQuelle, "x", NatürlicheZahlen)),
            )),
            RechenKontext(),
            mapOf(xQuelle to 0),
        ))

        val methode = assertIs<Methode>(ergebnis.ausgaben.getValue("methode").objekt)
        assertEquals(EndlicheMenge(setOf(WahrheitsKonstante(true), WahrheitsKonstante(false))), methode.einzigeZielMenge)
    }

    @Test
    fun `Term zu Methode übernimmt Variablenherkunft über den verbundenen Termgraph`() {
        val x = MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero).copy(id = KnotenId("x"), parameter = mapOf("name" to "x", "werteVorrat" to "N"))
        val y = MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero).copy(id = KnotenId("y"), parameter = mapOf("name" to "y", "werteVorrat" to "R"))
        val summe = MathematikKnotenVorlagen.Addition.erzeuge(GraphPunkt.Zero).copy(id = KnotenId("summe"))
        val methode = MathematikKnotenVorlagen.TermZuMethode.erzeuge(GraphPunkt.Zero).copy(id = KnotenId("methode"))
        fun kante(von: de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten, ausgang: String, zu: de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten, eingang: String) = VerbindungDaten(
            von = AnschlussVerweis(von.id, von.anschlüsse.first { it.name == ausgang }.id),
            zu = AnschlussVerweis(zu.id, zu.anschlüsse.first { it.name == eingang }.id),
        )
        val karte = KartenDaten(
            name = "Term",
            knoten = listOf(x, y, summe, methode),
            verbindungen = listOf(kante(x, "wert", summe, "a"), kante(y, "wert", summe, "b"), kante(summe, "wert", methode, "term")),
        )

        val ergebnis = KartenAuswerter(StandardMathematikAuswerter.erzeugeRegister()).auswerten(karte)
        val funktion = assertIs<Methode>(ergebnis.knoten.getValue(methode.id).ausgaben.getValue("methode").objekt)

        assertEquals(listOf("x", "y"), funktion.parameter.map { it.name })
        assertEquals(NatürlicheZahlen, funktion.werteVorräte.getValue("x"))
        assertEquals(ReelleZahlen, funktion.werteVorräte.getValue("y"))
        assertEquals(ReelleZahlen, funktion.einzigeZielMenge)
    }

    @Test fun `Allgemeiner Parameter wird zu einem allgemeinen Methodenargument`() {
        val parameter = MathematikKnotenVorlagen.AllgemeinerParameter.erzeuge(GraphPunkt.Zero).copy(
            id = KnotenId("parameter"), parameter = mapOf("name" to "a", "werteVorrat" to "R"),
        )
        val methode = MathematikKnotenVorlagen.TermZuMethode.erzeuge(GraphPunkt.Zero).copy(id = KnotenId("methode"))
        val karte = KartenDaten(
            name = "Allgemeine Methode",
            knoten = listOf(parameter, methode),
            verbindungen = listOf(VerbindungDaten(
                von = AnschlussVerweis(parameter.id, parameter.anschlüsse.single().id),
                zu = AnschlussVerweis(methode.id, methode.anschlüsse.first { it.name == "term" }.id),
            )),
        )

        val ergebnis = KartenAuswerter(StandardMathematikAuswerter.erzeugeRegister()).auswerten(karte)
        val funktion = assertIs<Methode>(ergebnis.knoten.getValue(methode.id).ausgaben.getValue("methode").objekt)

        assertEquals(listOf(AllgemeinerParameter("a")), funktion.parameter)
        assertEquals(ReelleZahlen, funktion.werteVorräte.getValue("a"))
    }

    @Test fun `Allgemeiner Parameter liefert seinen typisierten Wertebereich als Zielmenge`() {
        val bereich = WertebereichKonfiguration.Menge(
            WertebereichKonfiguration.Tupel(listOf(WertebereichKonfiguration.Zahl("N"), WertebereichKonfiguration.Zahl("R"))),
        )
        val parameter = MathematikKnotenVorlagen.AllgemeinerParameter.erzeuge(GraphPunkt.Zero).copy(
            id = KnotenId("parameter"),
            eigenschaften = mapOf(WertebereichKonfiguration.EIGENSCHAFT to bereich.zuEigenschaft()),
        )
        val methode = MathematikKnotenVorlagen.TermZuMethode.erzeuge(GraphPunkt.Zero).copy(id = KnotenId("methode"))
        val karte = KartenDaten(
            name = "Typisierter Parameter",
            knoten = listOf(parameter, methode),
            verbindungen = listOf(VerbindungDaten(
                von = AnschlussVerweis(parameter.id, parameter.anschlüsse.single().id),
                zu = AnschlussVerweis(methode.id, methode.anschlüsse.first { it.name == "term" }.id),
            )),
        )

        val ergebnis = KartenAuswerter(StandardMathematikAuswerter.erzeugeRegister()).auswerten(karte)
        val funktion = assertIs<Methode>(ergebnis.knoten.getValue(methode.id).ausgaben.getValue("methode").objekt)

        assertEquals(bereich.elementBereich.zuMenge(), funktion.einzigeZielMenge)
        assertEquals(bereich.zuMenge(), funktion.werteVorräte.getValue("a"))
    }

    @Test fun `Abbild erwartet einen allgemeinen Funktionsanschluss`() {
        val abbild = MathematikKnotenVorlagen.Abbild.erzeuge(GraphPunkt.Zero)

        assertEquals(MathematikAnschlussArten.Methode.id, abbild.anschlüsse.first { it.name == "methode" }.art)
    }
}
