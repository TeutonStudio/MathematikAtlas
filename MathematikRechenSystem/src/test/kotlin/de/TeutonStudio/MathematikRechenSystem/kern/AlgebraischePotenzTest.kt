package de.TeutonStudio.MathematikRechenSystem.kern

import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AlgebraischePotenzTest {
    private val reelleStruktur = StandardPotenzStrukturen.zahlbereich(FundamentalerZahlbereich.REELL)

    @Test
    fun `Ordnung null verwendet das neutrale Element der konkreten Struktur`() {
        val ergebnis = assertIs<PotenzAuswertung.Wert>(
            werteNatuerlichePotenzAus(
                basis = RationaleZahl.von(7),
                ordnung = IterationsOrdnung.Konkret(0),
                struktur = reelleStruktur,
                multiplikation = standardZahlMultiplikation,
            ),
        )

        assertEquals(RationaleZahl.Eins, ergebnis.wert)
        assertEquals(ReelleZahlen, ergebnis.traeger)
    }

    @Test
    fun `fehlendes neutrales Element betrifft Ordnung null aber nicht Ordnung eins`() {
        val halbgruppe = reelleStruktur.copy(
            id = "test.halbgruppe",
            neutralesElement = null,
            neutralitaet = NachweisStatus.Unvollstaendig,
        )

        val nullte = assertIs<PotenzAuswertung.Ungueltig>(
            werteNatuerlichePotenzAus(
                RationaleZahl.von(3),
                IterationsOrdnung.Konkret(0),
                halbgruppe,
                standardZahlMultiplikation,
            ),
        )
        val erste = assertIs<PotenzAuswertung.Wert>(
            werteNatuerlichePotenzAus(
                RationaleZahl.von(3),
                IterationsOrdnung.Konkret(1),
                halbgruppe,
                standardZahlMultiplikation,
            ),
        )

        assertEquals("neutrales_element_fehlt", nullte.code)
        assertEquals(RationaleZahl.von(3), erste.wert)
    }

    @Test
    fun `offene Strukturaxiome bleiben Aussagen statt Meldungstext`() {
        val assoziativ = UnentscheidbareAussage("\\operatorname{assoziativ}(\\cdot)", "Testmonoid")
        val offeneStruktur = reelleStruktur.copy(
            id = "test.offen",
            assoziativitaet = NachweisStatus.Bedingt(listOf(assoziativ)),
        )
        val ergebnis = assertIs<PotenzAuswertung.Bedingt>(
            werteNatuerlichePotenzAus(
                Variable("a"),
                IterationsOrdnung.Konkret(3),
                offeneStruktur,
                standardZahlMultiplikation,
            ),
        )

        assertTrue(assoziativ in ergebnis.voraussetzungen)
        assertEquals("{a}^{3}", ergebnis.potenz.zuLatex())
    }

    @Test
    fun `widerlegte Assoziativitaet lehnt klammerungsfreie hoehere Potenz ab`() {
        val nichtAssoziativ = reelleStruktur.copy(
            id = "test.nichtassoziativ",
            assoziativitaet = NachweisStatus.Widerlegt,
        )

        val ergebnis = assertIs<PotenzAuswertung.Ungueltig>(
            werteNatuerlichePotenzAus(
                RationaleZahl.von(2),
                IterationsOrdnung.Konkret(2),
                nichtAssoziativ,
                standardZahlMultiplikation,
            ),
        )

        assertEquals("assoziativitaet_widerlegt", ergebnis.code)
    }

    @Test
    fun `konkrete grosse Ordnung verwendet Exponentiation durch Quadrieren`() {
        var multiplikationen = 0
        val zaehlend: StrukturMultiplikation = { links, rechts ->
            multiplikationen++
            standardZahlMultiplikation(links, rechts)
        }
        val ergebnis = assertIs<PotenzAuswertung.Wert>(
            werteNatuerlichePotenzAus(
                RationaleZahl.von(2),
                IterationsOrdnung.Konkret(100),
                reelleStruktur,
                zaehlend,
            ),
        )

        assertEquals(RationaleZahl.von(BigInteger.ONE.shiftLeft(100)), ergebnis.wert)
        assertTrue(multiplikationen <= 10, "Exponentiation durch Quadrieren sollte logarithmisch bleiben.")
    }

    @Test
    fun `symbolische Ordnung materialisiert keine Produktkette und traegt Annahmen`() {
        val n = Variable("n")
        val natuerlich = UnentscheidbareAussage("n\\in\\mathbb N_0", "Iterationsordnung")
        val ergebnis = assertIs<PotenzAuswertung.Bedingt>(
            werteNatuerlichePotenzAus(
                Variable("a"),
                IterationsOrdnung.Symbolisch(n, setOf(natuerlich)),
                reelleStruktur,
                standardZahlMultiplikation,
            ),
        )

        assertEquals("{a}^{n}", ergebnis.potenz.zuLatex())
        assertTrue(natuerlich in ergebnis.voraussetzungen)
        assertTrue(ergebnis.potenz.basis is Variable)
    }

    @Test
    fun `quadratische Matrix nullte Potenz liefert typisierte Einheitsmatrix`() {
        val matrix = Matrix(
            listOf(
                listOf(RationaleZahl.von(2), RationaleZahl.Null),
                listOf(RationaleZahl.Null, RationaleZahl.von(3)),
            ),
        )
        val struktur = StandardPotenzStrukturen.matrix(matrix)
        val ergebnis = assertIs<PotenzAuswertung.Wert>(
            werteNatuerlichePotenzAus(
                matrix,
                IterationsOrdnung.Konkret(0),
                struktur,
                standardMatrixMultiplikation,
            ),
        )

        assertEquals(einheitsMatrixFuerPotenz(2), ergebnis.wert)
        assertEquals(Matrizenraum(2, 2, RationaleZahlen), ergebnis.traeger)
    }

    @Test
    fun `quadratische Matrix wird mit demselben Potenzalgorithmus ausgewertet`() {
        val matrix = Matrix(
            listOf(
                listOf(RationaleZahl.von(2), RationaleZahl.Null),
                listOf(RationaleZahl.Null, RationaleZahl.von(3)),
            ),
        )
        val ergebnis = assertIs<PotenzAuswertung.Wert>(
            werteNatuerlichePotenzAus(
                matrix,
                IterationsOrdnung.Konkret(5),
                StandardPotenzStrukturen.matrix(matrix),
                standardMatrixMultiplikation,
            ),
        )

        assertEquals(
            Matrix(
                listOf(
                    listOf(RationaleZahl.von(32), RationaleZahl.Null),
                    listOf(RationaleZahl.Null, RationaleZahl.von(243)),
                ),
            ),
            ergebnis.wert,
        )
    }

    @Test
    fun `rechteckige Matrix erlaubt Ordnung eins aber weder null noch zwei`() {
        val matrix = Matrix(
            listOf(
                listOf(RationaleZahl.Eins, RationaleZahl.von(2), RationaleZahl.von(3)),
                listOf(RationaleZahl.von(4), RationaleZahl.von(5), RationaleZahl.von(6)),
            ),
        )
        val struktur = StandardPotenzStrukturen.matrix(matrix)

        val erste = assertIs<PotenzAuswertung.Wert>(
            werteNatuerlichePotenzAus(
                matrix,
                IterationsOrdnung.Konkret(1),
                struktur,
                standardMatrixMultiplikation,
            ),
        )
        val nullte = assertIs<PotenzAuswertung.Ungueltig>(
            werteNatuerlichePotenzAus(
                matrix,
                IterationsOrdnung.Konkret(0),
                struktur,
                standardMatrixMultiplikation,
            ),
        )
        val zweite = assertIs<PotenzAuswertung.Ungueltig>(
            werteNatuerlichePotenzAus(
                matrix,
                IterationsOrdnung.Konkret(2),
                struktur,
                standardMatrixMultiplikation,
            ),
        )

        assertEquals(matrix, erste.wert)
        assertEquals("neutrales_element_fehlt", nullte.code)
        assertEquals("abgeschlossenheit_widerlegt", zweite.code)
    }

    @Test
    fun `symbolische rechteckige Matrixpotenz wird auf Ordnung eins eingeschraenkt`() {
        val matrix = Matrix(
            listOf(
                listOf(RationaleZahl.Eins, RationaleZahl.von(2)),
            ),
        )
        val n = Variable("n")
        val ergebnis = assertIs<PotenzAuswertung.Bedingt>(
            werteNatuerlichePotenzAus(
                matrix,
                IterationsOrdnung.Symbolisch(n),
                StandardPotenzStrukturen.matrix(matrix),
                standardMatrixMultiplikation,
            ),
        )

        assertTrue(ergebnis.voraussetzungen.any { it == Gleichheit(n, RationaleZahl.Eins) })
    }

    @Test
    fun `Tupel und Vektoren erhalten keine erfundene Standardmultiplikation`() {
        val tupel = StandardPotenzStrukturen.aufloesen(
            Tupel(listOf(RationaleZahl.Eins, RationaleZahl.von(2))),
        )
        val vektor = StandardPotenzStrukturen.aufloesen(
            SpaltenVektor(listOf(RationaleZahl.Eins, RationaleZahl.von(2))),
        )

        assertIs<PotenzStrukturAufloesung.NichtEindeutig>(tupel)
        assertIs<PotenzStrukturAufloesung.NichtEindeutig>(vektor)
    }

    @Test
    fun `Punktweise Methodenpotenz behaelt Wertevorrat und Zielmenge`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = addition(x, RationaleZahl.Eins),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen),
        )
        val ergebnis = assertIs<MethodenPotenzAuswertung.Wert>(
            wertePunktweiseMethodenPotenzAus(
                methode,
                IterationsOrdnung.Konkret(2),
                reelleStruktur,
                standardZahlMultiplikation,
            ),
        )

        assertEquals(methode.werteVorräte, ergebnis.methode.werteVorräte)
        assertEquals(methode.zielMenge, ergebnis.methode.zielMenge)
        assertEquals(
            multiplikation(methode.vorschrift as ZahlAusdruck, methode.vorschrift as ZahlAusdruck),
            ergebnis.methode.vorschrift,
        )
    }

    @Test
    fun `Methodenpotenz bleibt von Selbstkomposition getrennt`() {
        val x = Variable("x")
        val methode = Methode(
            name = "f",
            parameter = listOf(x),
            vorschrift = x,
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf("x" to ReelleZahlen),
        )
        val potenz = PunktweiseMethodenPotenz(methode, IterationsOrdnung.Konkret(2), reelleStruktur)
        val komposition = IterierterAusdruck(
            methode,
            IterationsArt.SELBSTKOMPOSITION,
            IterationsOrdnung.Konkret(2),
        )

        assertEquals("{f}^{2}", potenz.zuLatex())
        assertEquals("{f}^{\\langle 2\\rangle}", komposition.zuLatex())
        assertNotEquals(potenz.operatorId, komposition.operatorId)
    }

    @Test
    fun `Basis ausserhalb des Strukturtraegers wird abgelehnt`() {
        val matrix = Matrix(listOf(listOf(RationaleZahl.Eins)))
        val ergebnis = assertIs<PotenzAuswertung.Ungueltig>(
            werteNatuerlichePotenzAus(
                matrix,
                IterationsOrdnung.Konkret(1),
                reelleStruktur,
                standardZahlMultiplikation,
            ),
        )

        assertEquals("basis_nicht_im_traeger", ergebnis.code)
    }
}
