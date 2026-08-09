package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class MethodenSignaturProjektionTest {
    @Test
    fun `Signaturknoten trennen Wertevorrat Zielmenge und Argumentanzahl`() {
        val x = Variable("x")
        val y = Variable("y")
        val methode = Methode(
            name = "f",
            parameter = listOf(x, y),
            ausgaben = mapOf("wert" to x),
            zielMengen = mapOf("wert" to ReelleZahlen),
            werteVorräte = mapOf(x.name to KomplexeZahlen, y.name to KomplexeZahlen),
        )
        val eingänge = mapOf("methode" to BedingterWert(methode))

        val wertevorrat = MethodenWertevorratAuswerter.auswerten(
            KnotenAuswertungsKontext(KnotenDaten(art = METHODEN_WERTEVORRAT_ART, name = "Wertevorrat"), eingänge, RechenKontext()),
        ).ausgaben.getValue("menge").objekt
        val zielmenge = MethodenZielmengeSignaturAuswerter.auswerten(
            KnotenAuswertungsKontext(KnotenDaten(art = METHODEN_ZIELMENGE_ART, name = "Zielmenge"), eingänge, RechenKontext()),
        ).ausgaben.getValue("menge").objekt
        val argumentanzahl = MethodenArgumentanzahlAuswerter.auswerten(
            KnotenAuswertungsKontext(KnotenDaten(art = METHODEN_ARGUMENTANZAHL_ART, name = "Argumentanzahl"), eingänge, RechenKontext()),
        ).ausgaben.getValue("anzahl").objekt

        assertEquals(Tupelraum(listOf(KomplexeZahlen, KomplexeZahlen)), wertevorrat)
        assertEquals(ReelleZahlen, zielmenge)
        assertEquals(RationaleZahl.von(2), argumentanzahl)
    }

    @Test
    fun `Tupelprojektion entpackt mehrstellige Methode positionsgetreu`() {
        val x = Variable("x")
        val y = Variable("y")
        val methode = Methode(
            name = "f",
            parameter = listOf(x, y),
            ausgaben = mapOf("wert" to Division(x, y)),
            zielMengen = mapOf("wert" to ReelleZahlen),
            werteVorräte = mapOf(x.name to ReelleZahlen, y.name to ReelleZahlen),
        )
        val knoten = tupelAufrufKnoten()

        val ergebnis = MethodenAufrufAuswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf(
                    "methode" to BedingterWert(methode),
                    "argument-0" to BedingterWert(Tupel(listOf(RationaleZahl.von(2), RationaleZahl.von(4)))),
                ),
                RechenKontext(),
            ),
        )

        assertEquals(RationaleZahl.von(1, 2), ergebnis.ausgaben.getValue("wert").objekt)
        assertEquals("f(2,4)", ergebnis.ausgaben.getValue("wert").latexDarstellung)
    }

    @Test
    fun `Tupelprojektion akzeptiert echtes Einertupel`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            ausgaben = mapOf("wert" to addition(x, RationaleZahl.Eins)),
            zielMengen = mapOf("wert" to ReelleZahlen),
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )

        val ergebnis = MethodenAufrufAuswerter.auswerten(
            KnotenAuswertungsKontext(
                tupelAufrufKnoten(),
                mapOf(
                    "methode" to BedingterWert(methode),
                    "argument-0" to BedingterWert(Tupel(listOf(RationaleZahl.von(4)))),
                ),
                RechenKontext(),
            ),
        )

        assertEquals(RationaleZahl.von(5), ergebnis.ausgaben.getValue("wert").objekt)
    }

    @Test
    fun `Tupelprojektion verpackt Skalar nicht still`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            ausgaben = mapOf("wert" to x),
            zielMengen = mapOf("wert" to ReelleZahlen),
            werteVorräte = mapOf(x.name to ReelleZahlen),
        )

        assertFailsWith<IllegalStateException> {
            MethodenAufrufAuswerter.auswerten(
                KnotenAuswertungsKontext(
                    tupelAufrufKnoten(),
                    mapOf(
                        "methode" to BedingterWert(methode),
                        "argument-0" to BedingterWert(RationaleZahl.von(4)),
                    ),
                    RechenKontext(),
                ),
            )
        }
    }

    @Test
    fun `Tupelprojektion diagnostiziert falsche Tupellaenge`() {
        val x = Variable("x")
        val y = Variable("y")
        val methode = Methode(
            name = "f",
            parameter = listOf(x, y),
            ausgaben = mapOf("wert" to x),
            zielMengen = mapOf("wert" to ReelleZahlen),
            werteVorräte = mapOf(x.name to ReelleZahlen, y.name to ReelleZahlen),
        )

        val fehler = assertFailsWith<IllegalArgumentException> {
            MethodenAufrufAuswerter.auswerten(
                KnotenAuswertungsKontext(
                    tupelAufrufKnoten(),
                    mapOf(
                        "methode" to BedingterWert(methode),
                        "argument-0" to BedingterWert(Tupel(listOf(RationaleZahl.von(4)))),
                    ),
                    RechenKontext(),
                ),
            )
        }
        assertIs<String>(fehler.message)
    }

    private fun tupelAufrufKnoten() = KnotenDaten(
        art = METHODEN_AUFRUF_ART,
        name = "Methode aufrufen",
        parameter = mapOf(
            METHODEN_ANWENDUNG_ERGEBNIS_ART to "mathematik.zahl",
            METHODEN_AUFRUF_ARGUMENTPROJEKTION to METHODEN_ARGUMENTPROJEKTION_TUPEL,
        ),
        anschlüsse = listOf(
            AnschlussDaten(
                name = "methode",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = AnschlussArtId("mathematik.methode"),
                reihenfolge = 0,
            ),
            AnschlussDaten(
                name = "argument-0",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = AnschlussArtId("mathematik.tupel"),
                reihenfolge = 1,
            ),
            AnschlussDaten(
                name = "wert",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = AnschlussArtId("mathematik.zahl"),
            ),
        ),
    )
}
