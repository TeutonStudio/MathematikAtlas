package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

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
    fun `Methoden Anwendung wertet konkrete einwertige Funktion aus`() {
        val x = Variable("x")
        val methode = Funktion(
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
}
