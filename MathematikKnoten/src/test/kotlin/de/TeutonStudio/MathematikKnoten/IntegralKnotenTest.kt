package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IntegralKnotenTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()
    private val x = Variable("x")

    private fun kontext(
        knoten: KnotenDaten,
        eingaben: Map<String, BedingterWert> = emptyMap(),
    ) = KnotenAuswertungsKontext(
        knoten = knoten,
        eingänge = eingaben,
        rechenKontext = RechenKontext(),
    )

    private fun intervall() = ReellesIntervall(
        RationaleZahl.Null,
        false,
        RationaleZahl.von(2),
        false,
    )

    @Test
    fun `Vorlagenkatalog enthaelt genau einen gemeinsamen Integralknoten`() {
        val vorlagen = alleMathematikKnotenVorlagen().filter { it.art == INTEGRAL_KNOTEN_ART }

        assertEquals(1, vorlagen.size)
        assertEquals(
            IntegralAusgabeform.METHODE.name,
            vorlagen.single().standardParameter[INTEGRAL_AUSGABEFORM_PARAMETER],
        )
    }

    @Test
    fun `Moduswechsel erhaelt gemeinsame Menge und Mass IDs aber nicht semantisch fremde Handles`() {
        val methodenKnoten = IntegralKnotenVorlagen.Integral.erzeuge(GraphPunkt.Zero)
        val mengeId = methodenKnoten.anschlüsse.single { it.name == "menge" }.id
        val massId = methodenKnoten.anschlüsse.single { it.name == "mass" }.id
        val methodeId = methodenKnoten.anschlüsse.single { it.name == "methode" }.id

        val termKnoten = konfiguriereIntegralKnoten(methodenKnoten, IntegralAusgabeform.TERM)

        assertEquals(mengeId, termKnoten.anschlüsse.single { it.name == "menge" }.id)
        assertEquals(massId, termKnoten.anschlüsse.single { it.name == "mass" }.id)
        assertTrue(termKnoten.anschlüsse.none { it.id == methodeId })
        assertEquals(
            setOf("variable", "menge", "term", "mass", "wert"),
            termKnoten.anschlüsse.map { it.name }.toSet(),
        )
        val zurueck = konfiguriereIntegralKnoten(termKnoten, IntegralAusgabeform.METHODE)
        assertNotEquals(methodeId, zurueck.anschlüsse.single { it.name == "methode" }.id)
    }

    @Test
    fun `Diagnose meldet verbundenes Methodenhandle vor Termwechsel`() {
        val knoten = IntegralKnotenVorlagen.Integral.erzeuge(GraphPunkt.Zero)
        val methodeId = knoten.anschlüsse.single { it.name == "methode" }.id

        val diagnose = diagnostiziereIntegralModusWechsel(
            knoten,
            IntegralAusgabeform.TERM,
            setOf(methodeId),
        )

        assertTrue(diagnose.trenntVerbindungen)
        assertEquals(setOf(methodeId), diagnose.verbundeneEntfernteAnschlussIds)
    }

    @Test
    fun `Methodenmodus wertet einfaches Riemann Integral exakt aus`() {
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = Potenz(x, RationaleZahl.von(2)),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen),
        )
        val knoten = IntegralKnotenVorlagen.Integral.erzeuge(GraphPunkt.Zero)
        val ergebnis = assertNotNull(register.finde(INTEGRAL_KNOTEN_ART)).auswerten(
            kontext(
                knoten,
                mapOf(
                    "menge" to BedingterWert(intervall()),
                    "methode" to BedingterWert(methode),
                ),
            ),
        )

        assertEquals(RationaleZahl.von(8, 3), ergebnis.ausgaben.getValue("wert").objekt)
        assertTrue(ergebnis.warnungen.any { it.contains("EXAKT") })
        assertTrue(ergebnis.warnungen.any { it.contains("\\lambda") })
    }

    @Test
    fun `Termmodus summiert mit Zaehlen ueber endliche Menge`() {
        val basis = IntegralKnotenVorlagen.Integral.erzeuge(GraphPunkt.Zero)
        val knoten = konfiguriereIntegralKnoten(basis, IntegralAusgabeform.TERM)
        val menge = EndlicheMenge(setOf(RationaleZahl.Eins, RationaleZahl.von(2)))
        val ergebnis = assertNotNull(register.finde(INTEGRAL_KNOTEN_ART)).auswerten(
            kontext(
                knoten,
                mapOf(
                    "variable" to BedingterWert(x),
                    "menge" to BedingterWert(menge),
                    "term" to BedingterWert(x),
                ),
            ),
        )

        assertEquals(RationaleZahl.von(3), ergebnis.ausgaben.getValue("wert").objekt)
        assertTrue(ergebnis.warnungen.any { it.contains("\\#") })
    }

    @Test
    fun `mehrdimensionaler Termmodus bindet Variablentupel an Produktbereich`() {
        val y = Variable("y")
        val basis = IntegralKnotenVorlagen.Integral.erzeuge(GraphPunkt.Zero)
        val knoten = konfiguriereIntegralKnoten(basis, IntegralAusgabeform.TERM).copy(
            parameter = basis.parameter + mapOf(
                INTEGRAL_AUSGABEFORM_PARAMETER to IntegralAusgabeform.TERM.name,
                INTEGRAL_QUELLEN_IDS_PARAMETER to "quelle.x,quelle.y",
            ),
        )
        val produkt = KartesischesProdukt(listOf(intervall(), intervall()))
        val ergebnis = assertNotNull(register.finde(INTEGRAL_KNOTEN_ART)).auswerten(
            kontext(
                knoten,
                mapOf(
                    "variable" to BedingterWert(Tupel(listOf(x, y))),
                    "menge" to BedingterWert(produkt),
                    "term" to BedingterWert(addition(x, y)),
                ),
            ),
        )

        val integral = assertIs<StrukturiertesIntegral>(ergebnis.ausgaben.getValue("wert").objekt)
        assertEquals(2, integral.bereich.dimension)
        assertEquals(listOf("quelle.x", "quelle.y"), integral.volumenElement.quellenIds)
        assertTrue(integral.zuLatex().contains("dx\\cdotdy"))
    }

    @Test
    fun `explizites nichtstandard Mass bleibt bedingt und materialisiert keine Partition`() {
        val basis = IntegralKnotenVorlagen.Integral.erzeuge(GraphPunkt.Zero)
        val knoten = konfiguriereIntegralKnoten(basis, IntegralAusgabeform.TERM).copy(
            parameter = basis.parameter + mapOf(
                INTEGRAL_AUSGABEFORM_PARAMETER to IntegralAusgabeform.TERM.name,
                INTEGRAL_MASS_MODUS_PARAMETER to IntegralMassModus.NICHTSTANDARD.name,
            ),
        )
        val ergebnis = assertNotNull(register.finde(INTEGRAL_KNOTEN_ART)).auswerten(
            kontext(
                knoten,
                mapOf(
                    "variable" to BedingterWert(x),
                    "menge" to BedingterWert(intervall()),
                    "term" to BedingterWert(x),
                ),
            ),
        )

        assertIs<NichtstandardIntegralDarstellung>(ergebnis.ausgaben.getValue("wert").objekt)
        assertTrue(ergebnis.ausgaben.getValue("wert").annahmen.size >= 3)
        assertTrue(ergebnis.warnungen.any { it.contains("BEDINGT") })
    }

    @Test
    fun `historische Integralvarianten migrieren idempotent`() {
        val methodeAlt = KnotenDaten(
            art = "mathematik.integralMethode",
            name = "Alt",
            position = GraphPunkt.Zero,
            parameter = mapOf("kurz" to "false"),
        )
        val termAlt = KnotenDaten(
            art = "mathematik.integralTerm",
            name = "Altterm",
            position = GraphPunkt(1f, 1f),
            parameter = mapOf("quellenId" to "quelle.alt"),
        )
        val migriert = KartenDaten(name = "Alt", knoten = listOf(methodeAlt, termAlt))
            .migriereIntegralKnoten()

        assertEquals(setOf(INTEGRAL_KNOTEN_ART), migriert.knoten.map { it.art }.toSet())
        assertEquals(
            IntegralMethodenDarstellung.VOLLSTAENDIG.name,
            migriert.knoten[0].parameter[INTEGRAL_METHODEN_DARSTELLUNG_PARAMETER],
        )
        assertEquals(
            IntegralAusgabeform.TERM.name,
            migriert.knoten[1].parameter[INTEGRAL_AUSGABEFORM_PARAMETER],
        )
        assertEquals("quelle.alt", migriert.knoten[1].parameter[INTEGRAL_QUELLEN_IDS_PARAMETER])
        assertEquals(migriert, migriert.migriereIntegralKnoten())
    }
}
