package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class FaltungsAuswerterTest {
    @Test
    fun `endliche Summenfaltung verwendet Index Akkumulator und neutrales Element`() {
        val paar = "test-faltung"
        val indexMenge = EndlicheMenge(setOf(RationaleZahl.von(1), RationaleZahl.von(2)))
        val konstruktor = KnotenDaten(
            id = KnotenId("konstruktor"),
            art = FALTUNGSKONSTRUKTOR_ART,
            name = "Faltungskonstruktor",
            parameter = mapOf(
                FALTUNG_PAAR to paar,
                FALTUNG_OPERATOR to "summe",
                FALTUNG_INDEXNAME to "i",
                FALTUNG_AKKUMULATORNAME to "a",
            ),
            anschlüsse = listOf(
                AnschlussDaten(name = "indexmenge", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = AnschlussArtId("mathematik.menge")),
                AnschlussDaten(name = "neutral", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = AnschlussArtId("mathematik.zahl")),
                AnschlussDaten(name = "index", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = AnschlussArtId("mathematik.zahl")),
                AnschlussDaten(name = "akkumulator", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = AnschlussArtId("mathematik.zahl")),
            ),
        )
        val gebunden = FaltungskonstruktorAuswerter.auswerten(
            KnotenAuswertungsKontext(
                konstruktor,
                mapOf(
                    "indexmenge" to BedingterWert(indexMenge),
                    "neutral" to BedingterWert(RationaleZahl.Null, zielMenge = ReelleZahlen),
                ),
                RechenKontext(),
            ),
        )
        val index = assertIs<Variable>(gebunden.ausgaben.getValue("index").objekt)
        val akkumulator = assertIs<Variable>(gebunden.ausgaben.getValue("akkumulator").objekt)
        val körper = BedingterWert(
            objekt = addition(akkumulator, index),
            zielMenge = ReelleZahlen,
            variablenQuellen = gebunden.ausgaben.values.flatMap { it.variablenQuellen },
        )
        val definator = KnotenDaten(
            id = KnotenId("definator"),
            art = FALTUNGSDEFINATOR_ART,
            name = "Faltungsdefinator",
            parameter = mapOf(FALTUNG_PAAR to paar, FALTUNG_OPERATOR to "summe"),
        )

        val ergebnis = FaltungsdefinatorAuswerter.auswerten(
            KnotenAuswertungsKontext(
                definator,
                mapOf("nächsterAkkumulator" to körper),
                RechenKontext(),
            ),
        )

        assertEquals(RationaleZahl.von(3), ergebnis.ausgaben.getValue("wert").objekt)
        assertEquals(emptyList(), ergebnis.ausgaben.getValue("wert").variablenQuellen)
    }

    @Test
    fun `Methoden Anwendung wertet konkrete einwertige Methode aus`() {
        val x = Variable("x")
        val methode = Methode(
            "f", listOf(x), mapOf("wert" to addition(x, RationaleZahl.Eins)),
            mapOf("wert" to ReelleZahlen), mapOf(x.name to ReelleZahlen),
        )
        val knoten = KnotenDaten(
            art = METHODEN_ANWENDUNG_ART,
            name = "Methode anwenden",
            parameter = mapOf(METHODEN_ANWENDUNG_ERGEBNIS_ART to "mathematik.zahl"),
            anschlüsse = listOf(
                AnschlussDaten(name = "methode", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = AnschlussArtId("mathematik.funktion.zahl"), reihenfolge = 0),
                AnschlussDaten(name = "argument", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = AnschlussArtId("mathematik.zahl"), reihenfolge = 1),
                AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = AnschlussArtId("mathematik.zahl")),
            ),
        )

        val ergebnis = MethodenAnwendungAuswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf(
                    "methode" to BedingterWert(methode),
                    "argument" to BedingterWert(RationaleZahl.von(2)),
                ),
                RechenKontext(),
            ),
        )

        assertEquals(RationaleZahl.von(3), ergebnis.ausgaben.getValue("wert").objekt)
        assertEquals(ReelleZahlen, ergebnis.ausgaben.getValue("wert").zielMenge)
    }

    @Test
    fun `allgemeiner Methodenaufruf ignoriert unverbundenen Platzhalter bei einstelliger Methode`() {
        val x = Variable("x")
        val methode = Methode(
            "f", listOf(x), mapOf("wert" to addition(x, RationaleZahl.Eins)),
            mapOf("wert" to ReelleZahlen), mapOf(x.name to ReelleZahlen),
        )
        val knoten = methodenAufrufKnoten()

        val ergebnis = MethodenAnwendungAuswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf(
                    "methode" to BedingterWert(methode),
                    "argument1" to BedingterWert(RationaleZahl.von(4)),
                ),
                RechenKontext(),
            ),
        )

        assertEquals(RationaleZahl.von(5), ergebnis.ausgaben.getValue("wert").objekt)
        assertEquals("${methode.zuLatex()}(4)", ergebnis.ausgaben.getValue("wert").latexDarstellung)
    }

    @Test
    fun `allgemeiner Methodenaufruf erhält Argumentreihenfolge bei mehrstelliger Methode`() {
        val x = Variable("x")
        val y = Variable("y")
        val methode = Methode(
            "f",
            listOf(x, y),
            mapOf("wert" to Division(x, y)),
            mapOf("wert" to ReelleZahlen),
            mapOf(x.name to ReelleZahlen, y.name to ReelleZahlen),
        )
        val knoten = methodenAufrufKnoten()

        val ergebnis = MethodenAnwendungAuswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf(
                    "methode" to BedingterWert(methode),
                    "argument1" to BedingterWert(RationaleZahl.von(2)),
                    "argument2" to BedingterWert(RationaleZahl.von(4)),
                ),
                RechenKontext(),
            ),
        )

        assertEquals(RationaleZahl.von(1, 2), ergebnis.ausgaben.getValue("wert").objekt)
        assertEquals("${methode.zuLatex()}(2,4)", ergebnis.ausgaben.getValue("wert").latexDarstellung)
    }

    @Test
    fun `allgemeiner Methodenaufruf bleibt mit symbolischem Argument eine Menge`() {
        val methode = TypisiertesElement("A", "mathematik.funktion", "A")
        val i = Variable("i")
        val knoten = methodenAufrufKnoten("mathematik.menge")

        val ergebnis = MethodenAnwendungAuswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf(
                    "methode" to BedingterWert(methode),
                    "argument1" to BedingterWert(i, werteVorrat = NatürlicheZahlen),
                ),
                RechenKontext(),
            ),
        )

        val wert = assertIs<MengenParameter>(ergebnis.ausgaben.getValue("wert").objekt)
        assertEquals("A(i)", wert.zuLatex())
        assertEquals("A(i)", ergebnis.ausgaben.getValue("wert").latexDarstellung)
    }

    @Test
    fun `allgemeiner Methodenaufruf verwendet denselben Auswerter wie bestehende Anwendung`() {
        assertSame(MethodenAnwendungAuswerter, MathematikAuswerterRegister().finde(METHODEN_AUFRUF_ART))
    }

    private fun methodenAufrufKnoten(ergebnisArt: String = "mathematik.zahl") = KnotenDaten(
        art = METHODEN_AUFRUF_ART,
        name = "Methode aufrufen",
        parameter = mapOf(METHODEN_ANWENDUNG_ERGEBNIS_ART to ergebnisArt),
        anschlüsse = listOf(
            AnschlussDaten(name = "methode", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = AnschlussArtId("mathematik.funktion"), reihenfolge = 0),
            AnschlussDaten(name = "argument1", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = AnschlussArtId("mathematik.objekt"), reihenfolge = 1, kannSichErweitern = true),
            AnschlussDaten(name = "argument2", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = AnschlussArtId("mathematik.objekt"), reihenfolge = 2, kannSichErweitern = true),
            AnschlussDaten(name = "wert", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = AnschlussArtId(ergebnisArt)),
        ),
    )
}
