package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MethodenArgumenteAuswerterTest {
    private val x = Variable("x")
    private val y = Variable("y")
    private val methode = Methode(
        name = "f",
        parameter = listOf(x, y),
        ausgaben = mapOf("wert" to addition(x, y)),
        zielMengen = mapOf("wert" to ReelleZahlen),
        werteVorräte = mapOf(x.name to ReelleZahlen, y.name to GanzeZahlen),
    )

    @Test
    fun `Tupelprojektion liefert bindbare Parameter mit stabiler Herkunft`() {
        val knoten = KnotenDaten(
            art = METHODEN_ARGUMENTE_ART,
            name = "Methodenargumente",
            parameter = mapOf(METHODEN_ARGUMENTE_PROJEKTION to METHODEN_ARGUMENTPROJEKTION_TUPEL),
        )

        val ergebnis = MethodenArgumenteAuswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf("methode" to BedingterWert(methode)),
                rechenKontext = RechenKontext(),
            ),
        )
        val argumentWert = ergebnis.ausgaben.getValue("argumente")
        val tupel = assertIs<Tupel>(argumentWert.objekt)

        assertEquals(listOf(x, y), tupel.elemente)
        assertTrue(
            argumentWert.variablenQuellen.contains(
                VariablenQuelle(knoten.id, "x", ReelleZahlen, alsMethodenParameter = true),
            ),
        )
        assertTrue(
            argumentWert.variablenQuellen.contains(
                VariablenQuelle(knoten.id, "y", GanzeZahlen, alsMethodenParameter = true),
            ),
        )
    }

    @Test
    fun `separierte Projektion liefert dieselben Parameter mit Wertevorrat plus Dimension`() {
        val knoten = KnotenDaten(
            art = METHODEN_ARGUMENTE_ART,
            name = "Methodenargumente",
            parameter = mapOf(METHODEN_ARGUMENTE_PROJEKTION to METHODEN_ARGUMENTPROJEKTION_SEPARIERT),
        )

        val ergebnis = MethodenArgumenteAuswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf("methode" to BedingterWert(methode)),
                rechenKontext = RechenKontext(),
            ),
        )

        assertEquals(setOf("x", "y", "dimension"), ergebnis.ausgaben.keys)
        assertEquals(x, ergebnis.ausgaben.getValue("x").objekt)
        assertEquals(y, ergebnis.ausgaben.getValue("y").objekt)
        assertEquals(ReelleZahlen, ergebnis.ausgaben.getValue("x").werteVorrat)
        assertEquals(GanzeZahlen, ergebnis.ausgaben.getValue("y").werteVorrat)
        assertEquals(RationaleZahl.von(2), ergebnis.ausgaben.getValue("dimension").objekt)
        assertTrue(
            ergebnis.ausgaben.getValue("x").variablenQuellen.contains(
                VariablenQuelle(knoten.id, "x", ReelleZahlen, alsMethodenParameter = true),
            ),
        )
        assertTrue(
            ergebnis.ausgaben.getValue("y").variablenQuellen.contains(
                VariablenQuelle(knoten.id, "y", GanzeZahlen, alsMethodenParameter = true),
            ),
        )
    }

    @Test
    fun `Tupelprojektion kann unmittelbar als symbolischer Methodenaufruf verwendet werden`() {
        val parameter = Variable("i")
        val einstelligeMethode = Methode(
            name = "g",
            parameter = listOf(parameter),
            ausgaben = mapOf("wert" to addition(parameter, RationaleZahl.Eins)),
            zielMengen = mapOf("wert" to ReelleZahlen),
            werteVorräte = mapOf(parameter.name to ReelleZahlen),
        )
        val argumentKnoten = KnotenDaten(
            art = METHODEN_ARGUMENTE_ART,
            name = "Methodenargumente",
            parameter = mapOf(METHODEN_ARGUMENTE_PROJEKTION to METHODEN_ARGUMENTPROJEKTION_TUPEL),
        )
        val argumente = MethodenArgumenteAuswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten = argumentKnoten,
                eingänge = mapOf("methode" to BedingterWert(einstelligeMethode)),
                rechenKontext = RechenKontext(),
            ),
        ).ausgaben.getValue("argumente")

        val aufrufKnoten = KnotenDaten(
            art = METHODEN_AUFRUF_ART,
            name = "Methode aufrufen",
            parameter = mapOf(
                METHODEN_AUFRUF_ARGUMENTPROJEKTION to METHODEN_ARGUMENTPROJEKTION_TUPEL,
                METHODEN_AUFRUF_ERGEBNISPROJEKTION to METHODEN_ERGEBNISPROJEKTION_TUPEL,
                METHODEN_ANWENDUNG_ERGEBNIS_ART to "mathematik.tupel",
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
                    name = "argumente",
                    richtung = AnschlussRichtung.Eingang,
                    kante = AnschlussKante.Links,
                    art = AnschlussArtId("mathematik.tupel"),
                    reihenfolge = 1,
                ),
                AnschlussDaten(
                    name = "wert",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = AnschlussArtId("mathematik.tupel"),
                    reihenfolge = 2,
                ),
            ),
        )

        val aufruf = MethodenAufrufAuswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten = aufrufKnoten,
                eingänge = mapOf(
                    "methode" to BedingterWert(einstelligeMethode),
                    "argumente" to argumente,
                ),
                rechenKontext = RechenKontext(),
            ),
        ).ausgaben.getValue("wert")

        val ergebnisTupel = assertIs<Tupel>(aufruf.objekt)
        assertEquals(addition(parameter, RationaleZahl.Eins), ergebnisTupel.elemente.single())
        assertTrue(
            aufruf.variablenQuellen.contains(
                VariablenQuelle(argumentKnoten.id, "i", ReelleZahlen, alsMethodenParameter = true),
            ),
        )
    }

    @Test
    fun `unbekannter Methodenwert darf keine erfundene Signatur erzeugen`() {
        val knoten = KnotenDaten(
            art = METHODEN_ARGUMENTE_ART,
            name = "Methodenargumente",
        )

        assertFailsWith<IllegalStateException> {
            MethodenArgumenteAuswerter.auswerten(
                KnotenAuswertungsKontext(
                    knoten = knoten,
                    eingänge = mapOf("methode" to BedingterWert(AllgemeinerParameter("f"))),
                    rechenKontext = RechenKontext(),
                ),
            )
        }
    }
}
