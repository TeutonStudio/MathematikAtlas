package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class StrukturRechnerTest {
    @Test
    fun `Skalarprodukt ignoriert Vektororientierung und akzeptiert Tupel`() {
        val spalte = SpaltenVektor(listOf(zahl(1), zahl(2)))
        val zeile = ZeilenVektor(listOf(zahl(3), zahl(4)))
        val vektorErgebnis = standardSkalarprodukt(spalte, zeile)
        assertEquals(zahl(11), assertIs<StrukturPruefung.Gueltig<ZahlAusdruck>>(vektorErgebnis).wert)

        val tupelErgebnis = standardSkalarprodukt(
            Tupel(listOf(zahl(1), zahl(2))),
            Tupel(listOf(zahl(3), zahl(4))),
        )
        assertEquals(zahl(11), assertIs<StrukturPruefung.Gueltig<ZahlAusdruck>>(tupelErgebnis).wert)
    }

    @Test
    fun `Tensorprodukt bewahrt Links-rechts-Reihenfolge und addiert Stufen`() {
        val links = SpaltenVektor(listOf(zahl(1), zahl(2)))
        val rechts = ZeilenVektor(listOf(zahl(3), zahl(4)))
        val produkt = assertIs<StrukturPruefung.Gueltig<MathematischesObjekt>>(
            tensorprodukt(links, rechts),
        ).wert

        assertEquals(
            Matrix(
                listOf(
                    listOf(zahl(3), zahl(4)),
                    listOf(zahl(6), zahl(8)),
                ),
            ),
            produkt,
        )
        assertEquals(listOf(2, 2), assertIs<Matrix>(produkt).tensorForm)
    }

    @Test
    fun `Zahlen besitzen im Tensorvertrag Form eins und Stufe eins`() {
        val dimensionen = assertIs<StrukturPruefung.Gueltig<TensorDimensionenErgebnis>>(
            tensorDimensionen(zahl(7)),
        ).wert

        assertEquals(listOf(1), dimensionen.form)
        assertEquals(zahl(1), dimensionen.stufe)
        assertEquals(Tupel(listOf(zahl(1))), dimensionen.dimensionen)
    }

    @Test
    fun `Falk Schema beschreibt ausgewaehlten Matrixeintrag`() {
        val links = Matrix(listOf(listOf(zahl(1), zahl(2)), listOf(zahl(3), zahl(4))))
        val rechts = Matrix(listOf(listOf(zahl(5), zahl(6)), listOf(zahl(7), zahl(8))))

        assertEquals(
            "c_{01}=\\sum_{k=0}^{1}a_{0 k}b_{k1}",
            falkSchema(links, rechts, 0, 1).eintragsFormelLatex(),
        )
    }

    @Test
    fun `Beschraenkte Zahlmenge rendert links Grenze Relation und rechts Relation Grenze`() {
        val bereich = beschraenkteZahlmenge(
            FundamentalerZahlbereich.REELL,
            zahl(2),
            true,
            zahl(5),
            false,
        )

        assertEquals("{}^{2\\leq}\\mathbb R^{<5}", bereich.zuLatex())
        assertTrue(!bereich.zuLatex().contains("{}^{\\leq2}"))
    }

    @Test
    fun `Schnitt eines reellen Bereichs mit ganzen Zahlen tauscht den Traeger`() {
        val reell = beschraenkteZahlmenge(
            FundamentalerZahlbereich.REELL,
            zahl(2),
            true,
            zahl(5),
            true,
        )
        val geschnitten = normalisiereZahlmengenSchnitt(listOf(reell, GanzeZahlen))

        val ganz = assertIs<BeschraenkteZahlmenge>(geschnitten)
        assertEquals(FundamentalerZahlbereich.GANZ, ganz.traeger)
        assertEquals("{}^{2\\leq}\\mathbb Z^{\\leq5}", ganz.zuLatex())
    }

    @Test
    fun `Natuerliche Zahlen und Indizes beginnen bei eins`() {
        assertEquals(
            Wahrheitswert.Lüge,
            ElementBeziehung(zahl(0), NatürlicheZahlen).entscheide().wahrheitswert,
        )
        assertEquals(
            Wahrheitswert.Wahr,
            ElementBeziehung(zahl(1), NatürlicheZahlen).entscheide().wahrheitswert,
        )

        val n = Variable("n")
        val methode = Methode(
            name = "u",
            parameter = listOf(n),
            vorschrift = n,
            zielMenge = RationaleZahlen,
            werteVorräte = mapOf(n.name to NatürlicheZahlen),
        )
        val tupel = UnnatuerlichesKartesischesTupel("u", "u", RationaleZahlen, methode)

        assertFailsWith<IllegalArgumentException> { tupel.standardKomponente(zahl(0)) }
        assertEquals(zahl(1), tupel.standardKomponente(zahl(1)))
    }

    @Test
    fun `Hypererweiterung materialisiert keine unendliche Komponentenliste`() {
        val n = Variable("n")
        val methode = Methode(
            name = "u",
            parameter = listOf(n),
            vorschrift = zahl(1),
            zielMenge = RationaleZahlen,
            werteVorräte = mapOf(n.name to NatürlicheZahlen),
        )
        val tupel = UnnatuerlichesKartesischesTupel(
            "u",
            "u",
            RationaleZahlen,
            methode,
            nachweislichKonstant = true,
        )
        val hyper = tupel.hyperErweiterung()
        val komponente = hyper.komponente(HyperNatuerlicherIndex("H"))

        assertEquals("{}^*u_{H}", komponente.zuLatex())
        val cauchy = assertIs<CauchyErgebnis.AussageWert>(pruefeCauchy(tupel)).aussage
        assertEquals(Wahrheitswert.Wahr, cauchy.entscheide().wahrheitswert)
    }

    @Test
    fun `Quantor bindet nur die Variable mit derselben stabilen ID`() {
        val x = LogischeVariable("var-x", "x", EndlicheMenge(setOf(zahl(1), zahl(2))))
        val y = LogischeVariable("var-y", "x", EndlicheMenge(setOf(zahl(1))))
        val atom = LogischesAtom(Gleichheit(Variable("x"), Variable("x")), setOf(x, y))
        val quantifiziert = QuantifizierterAusdruck(AussagenSatzOperator.ALLQUANTOR, x, atom)

        assertEquals(setOf(y), quantifiziert.freieVariablen)
        assertEquals(setOf(x), quantifiziert.gebundeneVariablen)
    }

    private fun zahl(wert: Long): RationaleZahl = RationaleZahl.von(wert)
}
