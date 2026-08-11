package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SvgKnotenTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()

    @Test
    fun `SVG Operatoren besitzen immer SVG Eingang und SVG Ausgang`() {
        SvgOperatoren.alle.forEach { operator ->
            assertNotNull(operator.anschlüsse.singleOrNull {
                it.name == "svg" && it.richtung == AnschlussRichtung.Eingang
            }, operator.titel)
            assertNotNull(operator.anschlüsse.singleOrNull {
                it.name == "svg" && it.richtung == AnschlussRichtung.Ausgang
            }, operator.titel)
        }
    }

    @Test
    fun `unverbundener Kreis beginnt selbststaendig ein neues SVG`() {
        val basis = SvgKnotenVorlagen.Svg.erzeuge(GraphPunkt.Zero)
        val knoten = konfiguriereSvgKnoten(basis, SvgOperatoren.Kreis.id)
        val auswerter = register.finde(SVG_KNOTEN_ART)
        assertNotNull(auswerter)

        val ergebnis = auswerter.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf(
                    "x" to zahl(0),
                    "y" to zahl(0),
                    "radius" to zahl(2),
                ),
                rechenKontext = RechenKontext(),
            ),
        )

        val grafik = ergebnis.ausgaben.getValue("svg").objekt as SvgGrafik
        assertEquals(1, grafik.elemente.size)
        assertTrue(grafik.elemente.single() is SvgKreis)
    }

    @Test
    fun `verbundener SVG Eingang wird vollstaendig weitergereicht und ergaenzt`() {
        val vorhanden = SvgGrafik.standard().mitElement(
            SvgLinie("vorhanden", SvgPunkt(0.0, 0.0), SvgPunkt(10.0, 10.0)),
        )
        val knoten = konfiguriereSvgKnoten(
            SvgKnotenVorlagen.Svg.erzeuge(GraphPunkt.Zero),
            SvgOperatoren.Rechteck.id,
        )

        val ergebnis = register.finde(SVG_KNOTEN_ART)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf(
                    "svg" to BedingterWert(vorhanden),
                    "x" to zahl(-1),
                    "y" to zahl(-1),
                    "breite" to zahl(2),
                    "höhe" to zahl(2),
                ),
                rechenKontext = RechenKontext(),
            ),
        )

        val grafik = ergebnis.ausgaben.getValue("svg").objekt as SvgGrafik
        assertEquals(2, grafik.elemente.size)
        assertEquals("vorhanden", grafik.elemente.first().id)
        assertTrue(grafik.elemente.last() is SvgRechteck)
    }

    @Test
    fun `wiederverwendbarer Stil wird an SVG Ergaenzung uebernommen`() {
        val stilKnoten = SvgKnotenVorlagen.Stil.erzeuge(GraphPunkt.Zero).copy(
            parameter = SvgKnotenVorlagen.Stil.standardParameter + mapOf(
                "füllung" to "red",
                "kontur" to "black",
                "konturBreite" to "3",
            ),
        )
        val stil = register.finde(SVG_STIL_KNOTEN_ART)!!.auswerten(
            KnotenAuswertungsKontext(stilKnoten, emptyMap(), RechenKontext()),
        ).ausgaben.getValue("stil").objekt as SvgStil

        val kreis = konfiguriereSvgKnoten(
            SvgKnotenVorlagen.Svg.erzeuge(GraphPunkt.Zero),
            SvgOperatoren.Kreis.id,
        )
        val grafik = register.finde(SVG_KNOTEN_ART)!!.auswerten(
            KnotenAuswertungsKontext(
                kreis,
                mapOf(
                    "stil" to BedingterWert(stil),
                    "x" to zahl(0),
                    "y" to zahl(0),
                    "radius" to zahl(1),
                ),
                RechenKontext(),
            ),
        ).ausgaben.getValue("svg").objekt as SvgGrafik

        val element = grafik.elemente.single()
        assertEquals("red", element.stil?.füllung)
        assertEquals("black", element.stil?.kontur)
        assertEquals(3.0, element.stil?.konturBreite)
    }

    @Test
    fun `SVG kombinieren fuehrt zwei vollstaendige Aeste zusammen`() {
        val links = SvgGrafik.standard().mitElement(
            SvgLinie("links", SvgPunkt(0.0, 0.0), SvgPunkt(1.0, 1.0)),
        )
        val rechts = SvgGrafik.standard().mitElement(
            SvgKreis("rechts", SvgPunkt(5.0, 5.0), 2.0),
        )
        val knoten = konfiguriereSvgKnoten(
            SvgKnotenVorlagen.Svg.erzeuge(GraphPunkt.Zero),
            SvgOperatoren.Kombinieren.id,
        )

        val grafik = register.finde(SVG_KNOTEN_ART)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten,
                mapOf(
                    "svg" to BedingterWert(links),
                    "zweitesSvg" to BedingterWert(rechts),
                ),
                RechenKontext(),
            ),
        ).ausgaben.getValue("svg").objekt as SvgGrafik

        assertEquals(listOf("links", "rechts"), grafik.elemente.map { it.id })
    }

    private fun zahl(wert: Long) = BedingterWert(RationaleZahl.von(wert))
}
