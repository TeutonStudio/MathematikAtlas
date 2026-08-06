package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AlgebraischePotenzTest {
    private val struktur = StandardPotenzStrukturen.zahlbereich(ZahlbereichsIds.REELL)

    @Test
    fun `Ordnung null verwendet das neutrale Element der Struktur`() {
        val ergebnis = werteNatuerlichePotenzAus(
            basis = RationaleZahl.von(7),
            ordnung = IterationsOrdnung.Konkret(0),
            struktur = struktur,
            multiplikation = standardZahlMultiplikation,
        )

        assertEquals(RationaleZahl.Eins, assertIs<PotenzAuswertung.Wert>(ergebnis).wert)
    }

    @Test
    fun `Ordnung eins bewahrt die Basis ohne Multiplikation`() {
        val basis = Variable("a")
        val ergebnis = werteNatuerlichePotenzAus(
            basis,
            IterationsOrdnung.Konkret(1),
            struktur,
            standardZahlMultiplikation,
        )

        assertEquals(basis, assertIs<PotenzAuswertung.Wert>(ergebnis).wert)
    }

    @Test
    fun `konkrete Zahlpotenz wird assoziativ durch Quadrieren ausgewertet`() {
        val ergebnis = werteNatuerlichePotenzAus(
            RationaleZahl.von(2),
            IterationsOrdnung.Konkret(10),
            struktur,
            standardZahlMultiplikation,
        )

        assertEquals(RationaleZahl.von(1024), assertIs<PotenzAuswertung.Wert>(ergebnis).wert)
    }

    @Test
    fun `fehlende Assoziativitaet bleibt sichtbare Bedingung`() {
        val offeneStruktur = struktur.copy(assoziativitaet = NachweisStatus.Unvollstaendig)
        val ergebnis = assertIs<PotenzAuswertung.Bedingt>(
            werteNatuerlichePotenzAus(
                Variable("a"),
                IterationsOrdnung.Konkret(3),
                offeneStruktur,
                standardZahlMultiplikation,
            ),
        )

        assertTrue(ergebnis.voraussetzungen.any { it.contains("Assoziativität") })
        assertEquals("{a}^{3}", ergebnis.potenz.zuLatex())
    }

    @Test
    fun `symbolische Ordnung materialisiert keine Produktkette`() {
        val n = Variable("n")
        val ergebnis = assertIs<PotenzAuswertung.Symbolisch>(
            werteNatuerlichePotenzAus(
                Variable("a"),
                IterationsOrdnung.Symbolisch(n),
                struktur,
                standardZahlMultiplikation,
            ),
        )

        assertEquals("{a}^{n}", ergebnis.potenz.zuLatex())
    }

    @Test
    fun `Punktweise Methodenpotenz bleibt von Selbstkomposition getrennt`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen),
        )
        val potenz = PunktweiseMethodenPotenz(methode, IterationsOrdnung.Konkret(2), struktur)
        val komposition = IterierterAusdruck(methode, IterationsArt.SELBSTKOMPOSITION, IterationsOrdnung.Konkret(2))

        assertEquals("{f}^{2}", potenz.zuLatex())
        assertEquals("{f}^{\\langle 2\\rangle}", komposition.zuLatex())
        assertTrue(potenz.operatorId != komposition.operatorId)
    }
}
