package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PotenzStrukturKnotenTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()

    private fun kontext(
        knoten: KnotenDaten,
        eingaben: Map<String, BedingterWert>,
    ) = KnotenAuswertungsKontext(
        knoten = knoten,
        eingänge = eingaben,
        rechenKontext = RechenKontext(),
    )

    private fun reelleMultiplikation(): Methode {
        val a = Variable("a")
        val b = Variable("b")
        return Methode(
            name = "\\star",
            parameter = listOf(a, b),
            vorschrift = multiplikation(a, b),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("a" to ReelleZahlen, "b" to ReelleZahlen),
        )
    }

    private fun endlicheMultiplikation(
        argumentBereich: MengenAusdruck,
        zielMenge: MengenAusdruck,
    ): Methode {
        val a = Variable("a")
        val b = Variable("b")
        return Methode(
            name = "\\diamond",
            parameter = listOf(a, b),
            vorschrift = a,
            zielMenge = zielMenge,
            werteVorräte = mapOf("a" to argumentBereich, "b" to argumentBereich),
        )
    }

    private val wahreAussage = Gleichheit(RationaleZahl.Eins, RationaleZahl.Eins)

    @Test
    fun `Vorlagenkatalog enthaelt Potenzstruktur-Zeugnisknoten`() {
        val vorlage = alleMathematikKnotenVorlagen().single { it.art == POTENZ_STRUKTUR_KNOTEN_ART }

        assertEquals("struktur", vorlage.anschlüsse.single {
            it.richtung == AnschlussRichtung.Ausgang
        }.name)
        assertTrue(vorlage.anschlüsse.any { it.name == "multiplikation" })
        assertTrue(vorlage.anschlüsse.any { it.name == "assoziativ" })
    }

    @Test
    fun `Strukturknoten erzeugt ausfuehrbares Zeugnis`() {
        val knoten = PotenzStrukturKnotenVorlagen.Struktur.erzeuge(GraphPunkt.Zero)
        val ergebnis = assertNotNull(register.finde(POTENZ_STRUKTUR_KNOTEN_ART)).auswerten(
            kontext(
                knoten,
                mapOf(
                    "traeger" to BedingterWert(ReelleZahlen),
                    "multiplikation" to BedingterWert(reelleMultiplikation()),
                    "abgeschlossen" to BedingterWert(wahreAussage),
                    "assoziativ" to BedingterWert(wahreAussage),
                    "neutral" to BedingterWert(RationaleZahl.Eins),
                    "neutralitaet" to BedingterWert(wahreAussage),
                ),
            ),
        )
        val struktur = assertIs<PotenzStruktur>(ergebnis.ausgaben.getValue("struktur").objekt)

        assertEquals(NachweisStatus.Nachgewiesen, struktur.abgeschlossenheit)
        assertEquals(NachweisStatus.Nachgewiesen, struktur.assoziativitaet)
        assertEquals(NachweisStatus.Nachgewiesen, struktur.neutralitaet)
        assertNotNull(struktur.multiplikationsMethode)
    }

    @Test
    fun `explizites Zeugnis wird vom Potenzknoten tatsaechlich ausgefuehrt`() {
        val strukturKnoten = PotenzStrukturKnotenVorlagen.Struktur.erzeuge(GraphPunkt.Zero)
        val strukturErgebnis = assertNotNull(register.finde(POTENZ_STRUKTUR_KNOTEN_ART)).auswerten(
            kontext(
                strukturKnoten,
                mapOf(
                    "traeger" to BedingterWert(ReelleZahlen),
                    "multiplikation" to BedingterWert(reelleMultiplikation()),
                    "abgeschlossen" to BedingterWert(wahreAussage),
                    "assoziativ" to BedingterWert(wahreAussage),
                    "neutral" to BedingterWert(RationaleZahl.Eins),
                    "neutralitaet" to BedingterWert(wahreAussage),
                ),
            ),
        )
        val strukturWert = strukturErgebnis.ausgaben.getValue("struktur")
        val basis = AlgebraischePotenzKnotenVorlagen.Potenz.erzeuge(GraphPunkt.Zero)
        val potenzKnoten = basis.copy(
            parameter = basis.parameter +
                (POTENZ_STRUKTUR_MODUS_PARAMETER to PotenzStrukturModus.EXPLIZIT.name),
        )
        val potenzErgebnis = assertNotNull(register.finde(ALGEBRAISCHE_POTENZ_KNOTEN_ART)).auswerten(
            kontext(
                potenzKnoten,
                mapOf(
                    "basis" to BedingterWert(RationaleZahl.von(3)),
                    "ordnung" to BedingterWert(RationaleZahl.von(4)),
                    "struktur" to strukturWert,
                ),
            ),
        )

        assertEquals(RationaleZahl.von(81), potenzErgebnis.ausgaben.getValue("wert").objekt)
    }

    @Test
    fun `offene Axiome bleiben strukturierte Bedingungen`() {
        val offen = UnentscheidbareAussage("\\operatorname{assoziativ}(\\star)", "eigene Struktur")
        val knoten = PotenzStrukturKnotenVorlagen.Struktur.erzeuge(GraphPunkt.Zero)
        val ergebnis = assertNotNull(register.finde(POTENZ_STRUKTUR_KNOTEN_ART)).auswerten(
            kontext(
                knoten,
                mapOf(
                    "traeger" to BedingterWert(ReelleZahlen),
                    "multiplikation" to BedingterWert(reelleMultiplikation()),
                    "abgeschlossen" to BedingterWert(wahreAussage),
                    "assoziativ" to BedingterWert(offen),
                ),
            ),
        )
        val struktur = assertIs<PotenzStruktur>(ergebnis.ausgaben.getValue("struktur").objekt)

        assertIs<NachweisStatus.Bedingt>(struktur.assoziativitaet)
        assertTrue(offen in ergebnis.ausgaben.getValue("struktur").annahmen)
    }

    @Test
    fun `offene innere Signatur bleibt Bedingung der Abgeschlossenheit`() {
        val traeger = BenannteMenge("M")
        val knoten = PotenzStrukturKnotenVorlagen.Struktur.erzeuge(GraphPunkt.Zero)
        val ergebnis = assertNotNull(register.finde(POTENZ_STRUKTUR_KNOTEN_ART)).auswerten(
            kontext(
                knoten,
                mapOf(
                    "traeger" to BedingterWert(traeger),
                    "multiplikation" to BedingterWert(reelleMultiplikation()),
                    "abgeschlossen" to BedingterWert(wahreAussage),
                    "assoziativ" to BedingterWert(wahreAussage),
                ),
            ),
        )
        val struktur = assertIs<PotenzStruktur>(ergebnis.ausgaben.getValue("struktur").objekt)
        val status = assertIs<NachweisStatus.Bedingt>(struktur.abgeschlossenheit)

        assertTrue(status.bedingungen.any { it is TeilmengenBeziehung })
        assertTrue(ergebnis.warnungen.any { it.contains("innere Signatur") })
    }

    @Test
    fun `nachweislich zu enger Argumentbereich wird abgelehnt`() {
        val klein = EndlicheMenge(setOf(RationaleZahl.Eins))
        val traeger = EndlicheMenge(setOf(RationaleZahl.Eins, RationaleZahl.von(2)))
        val knoten = PotenzStrukturKnotenVorlagen.Struktur.erzeuge(GraphPunkt.Zero)
        val ergebnis = assertNotNull(register.finde(POTENZ_STRUKTUR_KNOTEN_ART)).auswerten(
            kontext(
                knoten,
                mapOf(
                    "traeger" to BedingterWert(traeger),
                    "multiplikation" to BedingterWert(
                        endlicheMultiplikation(argumentBereich = klein, zielMenge = traeger),
                    ),
                ),
            ),
        )

        assertTrue(ergebnis.ausgaben.isEmpty())
        assertTrue(ergebnis.fehler.orEmpty().contains("liegt nicht im Wertevorrat"))
    }

    @Test
    fun `nachweislich fremde Zielmenge wird abgelehnt`() {
        val traeger = EndlicheMenge(setOf(RationaleZahl.Eins))
        val fremd = EndlicheMenge(setOf(RationaleZahl.von(2)))
        val knoten = PotenzStrukturKnotenVorlagen.Struktur.erzeuge(GraphPunkt.Zero)
        val ergebnis = assertNotNull(register.finde(POTENZ_STRUKTUR_KNOTEN_ART)).auswerten(
            kontext(
                knoten,
                mapOf(
                    "traeger" to BedingterWert(traeger),
                    "multiplikation" to BedingterWert(
                        endlicheMultiplikation(argumentBereich = traeger, zielMenge = fremd),
                    ),
                ),
            ),
        )

        assertTrue(ergebnis.ausgaben.isEmpty())
        assertTrue(ergebnis.fehler.orEmpty().contains("Zielmenge"))
    }

    @Test
    fun `neutrales Element ausserhalb des Traegers wird abgelehnt`() {
        val traeger = EndlicheMenge(setOf(RationaleZahl.Eins))
        val knoten = PotenzStrukturKnotenVorlagen.Struktur.erzeuge(GraphPunkt.Zero)
        val ergebnis = assertNotNull(register.finde(POTENZ_STRUKTUR_KNOTEN_ART)).auswerten(
            kontext(
                knoten,
                mapOf(
                    "traeger" to BedingterWert(traeger),
                    "multiplikation" to BedingterWert(
                        endlicheMultiplikation(argumentBereich = traeger, zielMenge = traeger),
                    ),
                    "neutral" to BedingterWert(RationaleZahl.von(2)),
                    "neutralitaet" to BedingterWert(wahreAussage),
                ),
            ),
        )

        assertTrue(ergebnis.ausgaben.isEmpty())
        assertTrue(ergebnis.fehler.orEmpty().contains("liegt nicht im Träger"))
    }

    @Test
    fun `nichtbinaere Multiplikation wird abgelehnt`() {
        val x = Variable("x")
        val unaer = Methode(
            name = "u",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen),
        )
        val knoten = PotenzStrukturKnotenVorlagen.Struktur.erzeuge(GraphPunkt.Zero)
        val ergebnis = assertNotNull(register.finde(POTENZ_STRUKTUR_KNOTEN_ART)).auswerten(
            kontext(
                knoten,
                mapOf(
                    "traeger" to BedingterWert(ReelleZahlen),
                    "multiplikation" to BedingterWert(unaer),
                ),
            ),
        )

        assertTrue(ergebnis.ausgaben.isEmpty())
        assertTrue(ergebnis.fehler.orEmpty().contains("genau zwei"))
    }
}
