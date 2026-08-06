package de.TeutonStudio.MathematikKnoten

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

    private val wahreAussage = Gleichheit(RationaleZahl.Eins, RationaleZahl.Eins)

    @Test
    fun `Vorlagenkatalog enthaelt Potenzstruktur-Zeugnisknoten`() {
        val vorlage = alleMathematikKnotenVorlagen().single { it.art == POTENZ_STRUKTUR_KNOTEN_ART }

        assertEquals("struktur", vorlage.anschlüsse.single {
            it.richtung == de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung.Ausgang
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
