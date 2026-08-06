package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
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

    private fun intervall(links: Long = 0, rechts: Long = 1) = ReellesIntervall(
        RationaleZahl.von(links),
        linksOffen = false,
        RationaleZahl.von(rechts),
        rechtsOffen = false,
    )

    private fun quadratischeMethode(): Methode = Methode(
        name = "f",
        parameter = listOf(x),
        vorschrift = Potenz(x, RationaleZahl.von(2)),
        zielMenge = ReelleZahlen,
        werteVorräte = mapOf("x" to ReelleZahlen),
    )

    @Test
    fun `Vorlagenkatalog enthaelt genau einen gemeinsamen Integralknoten`() {
        val vorlagen = alleMathematikKnotenVorlagen().filter { it.art == INTEGRAL_KNOTEN_ART }

        assertEquals(1, vorlagen.size)
        assertEquals(IntegralAusgabeform.METHODE.name, vorlagen.single().standardParameter[INTEGRAL_AUSGABEFORM_PARAMETER])
    }

    @Test
    fun `Moduswechsel erhaelt Menge Mass und Ausgang aber trennt Methodenrolle`() {
        val methode = IntegralKnotenVorlagen.Integral.erzeuge(GraphPunkt.Zero)
        val gemeinsameIds = listOf("menge", "mass", "wert").associateWith { name ->
            methode.anschlüsse.single { it.name == name }.id
        }
        val methodenId = methode.anschlüsse.single { it.name == "methode" }.id

        val term = konfiguriereIntegralKnoten(methode, IntegralAusgabeform.TERM)

        gemeinsameIds.forEach { (name, id) ->
            assertEquals(id, term.anschlüsse.single { it.name == name }.id)
        }
        assertTrue(term.anschlüsse.none { it.id == methodenId })
        assertTrue(term.anschlüsse.any { it.name == "variable" })
        assertTrue(term.anschlüsse.any { it.name == "term" })
    }

    @Test
    fun `Diagnose meldet verbundene Methodenrolle vor Modustrennung`() {
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
    fun `Methodenmodus wertet quadratische Methode auf Intervall exakt aus`() {
        val knoten = IntegralKnotenVorlagen.Integral.erzeuge(GraphPunkt.Zero)
        val ergebnis = assertNotNull(register.finde(INTEGRAL_KNOTEN_ART)).auswerten(
            kontext(
                knoten,
                mapOf(
                    "menge" to BedingterWert(intervall(0, 2)),
                    "methode" to BedingterWert(quadratischeMethode()),
                ),
            ),
        )

        assertEquals(RationaleZahl.von(8, 3), ergebnis.ausgaben.getValue("wert").objekt)
        assertTrue(ergebnis.warnungen.any { it.contains("EXAKT") })
        assertTrue(ergebnis.schritte.any { it.regelId == "analysis.integral.hauptsatz" })
    }

    @Test
    fun `Termmodus bindet Variable und Quellen ID`() {
        val basis = IntegralKnotenVorlagen.Integral.erzeuge(GraphPunkt.Zero)
        val knoten = konfiguriereIntegralKnoten(basis, IntegralAusgabeform.TERM).copy(
            parameter = basis.parameter + mapOf(
                INTEGRAL_AUSGABEFORM_PARAMETER to IntegralAusgabeform.TERM.name,
                INTEGRAL_QUELLEN_IDS_PARAMETER to "quelle.x",
            ),
        )
        val ergebnis = assertNotNull(register.finde(INTEGRAL_KNOTEN_ART)).auswerten(
            kontext(
                knoten,
                mapOf(
                    "variable" to BedingterWert(x),
                    "menge" to BedingterWert(intervall()),
                    "term" to BedingterWert(Potenz(x, RationaleZahl.von(2))),
                ),
            ),
        )

        assertEquals(RationaleZahl.von(1, 3), ergebnis.ausgaben.getValue("wert").objekt)
        assertTrue(ergebnis.warnungen.any { it.contains("Maß") })
    }

    @Test
    fun `Zaehlen summiert endlichen Bereich exakt`() {
        val basis = IntegralKnotenVorlagen.Integral.erzeuge(GraphPunkt.Zero)
        val knoten = konfiguriereIntegralKnoten(basis, IntegralAusgabeform.TERM).copy(
            parameter = basis.parameter + mapOf(
                INTEGRAL_AUSGABEFORM_PARAMETER to IntegralAusgabeform.TERM.name,
                INTEGRAL_MASS_MODUS_PARAMETER to IntegralMassModus.ZAEHLMASS.name,
            ),
        )
        val menge = EndlicheMenge(
            setOf(RationaleZahl.Eins, RationaleZahl.von(2), RationaleZahl.von(3)),
        )
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

        assertEquals(RationaleZahl.von(6), ergebnis.ausgaben.getValue("wert").objekt)
        assertTrue(ergebnis.warnungen.any { it.contains("Zählmaß") })
    }

    @Test
    fun `Auto Mass auf allgemeiner reeller Menge bleibt Fehler statt dx zu raten`() {
        val knoten = IntegralKnotenVorlagen.Integral.erzeuge(GraphPunkt.Zero)
        val ergebnis = assertNotNull(register.finde(INTEGRAL_KNOTEN_ART)).auswerten(
            kontext(
                knoten,
                mapOf(
                    "menge" to BedingterWert(ReelleZahlen),
                    "methode" to BedingterWert(quadratischeMethode()),
                ),
            ),
        )

        assertTrue(ergebnis.ausgaben.isEmpty())
        assertTrue(ergebnis.fehler.orEmpty().contains("Maß"))
    }

    @Test
    fun `Mehrdimensionaler Term bleibt gueltig symbolisch mit Variablentupel`() {
        val y = Variable("y")
        val basis = IntegralKnotenVorlagen.Integral.erzeuge(GraphPunkt.Zero)
        val knoten = konfiguriereIntegralKnoten(basis, IntegralAusgabeform.TERM).copy(
            parameter = basis.parameter + mapOf(
                INTEGRAL_AUSGABEFORM_PARAMETER to IntegralAusgabeform.TERM.name,
                INTEGRAL_QUELLEN_IDS_PARAMETER to "quelle.x,quelle.y",
            ),
        )
        val bereich = KartesischesProdukt(listOf(intervall(), intervall(-1, 1)))
        val ergebnis = assertNotNull(register.finde(INTEGRAL_KNOTEN_ART)).auswerten(
            kontext(
                knoten,
                mapOf(
                    "variable" to BedingterWert(Tupel(listOf(x, y))),
                    "menge" to BedingterWert(bereich),
                    "term" to BedingterWert(addition(x, y)),
                ),
            ),
        )

        val integral = assertIs<StrukturiertesIntegral>(ergebnis.ausgaben.getValue("wert").objekt)
        assertEquals(listOf("quelle.x", "quelle.y"), integral.bindungen.map { it.quellenId })
        assertTrue(ergebnis.warnungen.any { it.contains("SYMBOLISCH") })
    }

    @Test
    fun `historische Integraltypen migrieren idempotent`() {
        val alteMethode = KnotenDaten(
            art = "mathematik.integralMethode",
            name = "Alt",
            position = GraphPunkt.Zero,
            parameter = mapOf("kurz" to "false"),
        )
        val alterTerm = KnotenDaten(
            art = "mathematik.integralTerm",
            name = "Altterm",
            position = GraphPunkt(1f, 1f),
            parameter = mapOf("quellenId" to "quelle.alt"),
        )
        val karte = KartenDaten(name = "Alt", knoten = listOf(alteMethode, alterTerm))

        val migriert = karte.migriereIntegralKnoten()

        assertEquals(setOf(INTEGRAL_KNOTEN_ART), migriert.knoten.map { it.art }.toSet())
        assertEquals(IntegralMethodenDarstellung.VOLLSTAENDIG.name, migriert.knoten[0].parameter[INTEGRAL_METHODEN_DARSTELLUNG_PARAMETER])
        assertEquals(IntegralAusgabeform.TERM.name, migriert.knoten[1].parameter[INTEGRAL_AUSGABEFORM_PARAMETER])
        assertEquals("quelle.alt", migriert.knoten[1].parameter[INTEGRAL_QUELLEN_IDS_PARAMETER])
        assertEquals(migriert, migriert.migriereIntegralKnoten())
    }
}
