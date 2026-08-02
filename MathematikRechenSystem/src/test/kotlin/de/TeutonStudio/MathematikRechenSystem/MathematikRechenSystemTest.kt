package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class MathematikRechenSystemTest {
    @Test fun rationaleZahlenSindExakt() {
        assertEquals(RationaleZahl.von(5, 6), RationaleZahl.von(1, 2) + RationaleZahl.von(1, 3))
    }

    @Test fun additionWirdAbgeflacht() {
        val x = Variable("x")
        val term = addition(addition(x, RationaleZahl.von(2)), RationaleZahl.von(3))
        assertEquals(addition(x, RationaleZahl.von(5)), term)
        assertTrue(term is Addition)
        assertEquals(2, term.summanden.size)
    }

    @Test fun extremwerteWertenRationaleZahlenExaktAusUndBleibenSonstSymbolisch() {
        assertEquals(RationaleZahl.von(7), maximum(RationaleZahl.von(-2), RationaleZahl.von(7), RationaleZahl.von(3)))
        assertEquals(RationaleZahl.von(-2), minimum(RationaleZahl.von(-2), RationaleZahl.von(7), RationaleZahl.von(3)))
        assertEquals(RationaleZahl.von(4), maximum(RationaleZahl.von(4), RationaleZahl.von(4)))
        val x = Variable("x")
        assertEquals(x, minimum(x, x))
        assertEquals("\\max\\left\\{x,3\\right\\}", maximum(Variable("x"), RationaleZahl.von(3)).zuLatex())
    }

    @Test fun extremwerteBenötigenMindestensZweiOperanden() {
        assertFailsWith<IllegalArgumentException> { maximum(listOf(RationaleZahl.Eins)) }
        assertFailsWith<IllegalArgumentException> { minimum(emptyList()) }
    }

    @Test fun reellheitsnachweisRespektiertDefinitionsbedingungenPartiellerOperationen() {
        assertFalse(istNachweisbarReell(Division(RationaleZahl.Eins, RationaleZahl.Null)))
        assertFalse(istNachweisbarReell(NatürlicherLogarithmus(RationaleZahl.von(-1))))
        assertFalse(istNachweisbarReell(Logarithmus(RationaleZahl.von(10), RationaleZahl.von(-1))))

        val x = Variable("x")
        val y = Variable("y")
        assertFalse(istNachweisbarReell(Division(x, y), variableIstReell = { true }))
        assertTrue(istNachweisbarReell(Division(x, y), { true }, setOf(Ungleichheit(y, RationaleZahl.Null))))
        assertTrue(istNachweisbarReell(NatürlicherLogarithmus(x), { true }, setOf(Vergleich(x, VergleichsArt.Größer, RationaleZahl.Null))))
    }

    @Test fun zahlenwertebereicheWerdenKonservativAbgeleitet() {
        val x = Variable("x")

        assertEquals(NatürlicheZahlen, inferiereZahlenWertevorrat(RationaleZahl.von(2)))
        assertEquals(GanzeZahlen, inferiereZahlenWertevorrat(RationaleZahl.von(-1)))
        assertEquals(RationaleZahlen, inferiereZahlenWertevorrat(Division(x, RationaleZahl.von(2)), mapOf("x" to NatürlicheZahlen)))
        assertEquals(KomplexeZahlen, inferiereZahlenWertevorrat(KomplexeZahl(RationaleZahl.Null, RationaleZahl.Eins)))
        assertEquals(ReelleZahlen, maximaleZahlenGrundmenge(listOf(NatürlicheZahlen, ReelleZahlen)))
    }

    @Test fun allgemeineZielmengenWerdenStrukturellAbgeleitet() {
        assertEquals(
            EndlicheMenge(setOf(WahrheitsKonstante(true), WahrheitsKonstante(false))),
            inferiereZielmenge(Gleichheit(Variable("x"), Variable("x")), mapOf("x" to NatürlicheZahlen)),
        )
        assertEquals(
            NatürlicheZahlen,
            inferiereZielmenge(EndlicheMenge(setOf(RationaleZahl.von(1), RationaleZahl.von(2)))),
        )
        assertEquals(
            Tupelraum(listOf(NatürlicheZahlen, ReelleZahlen)),
            inferiereZielmenge(Tupel(listOf(RationaleZahl.von(1), Pi))),
        )
        assertEquals(
            Vektorraum(VektorOrientierung.Spalte, 2, ReelleZahlen),
            inferiereZielmenge(SpaltenVektor(listOf(RationaleZahl.von(1), Pi))),
        )
        assertEquals(
            Matrizenraum(1, 2, RationaleZahlen),
            inferiereZielmenge(Matrix(listOf(listOf(RationaleZahl.von(1), RationaleZahl.von(1, 2))))),
        )
        val index = Variable("k")
        val mengenMethode = Methode(
            "A",
            listOf(index),
            mapOf("menge" to EndlicheMenge(setOf(index))),
            mapOf("menge" to NatürlicheZahlen),
            mapOf("k" to NatürlicheZahlen),
        )
        assertEquals(
            Folgenraum(NatürlicheZahlen),
            inferiereZielmenge(IteriertesKartesischesProdukt(mengenMethode, NatürlicheZahlen)),
        )
    }

    @Test fun funktionKannTeilweiseGebundenWerden() {
        val x = Variable("x"); val y = Variable("y")
        val f = Methode("f", listOf(x, y), mapOf("wert" to addition(x, y)))
        val g = f.binde(mapOf("x" to RationaleZahl.von(4)))
        assertEquals(listOf(y), g.freieParameter)
        assertEquals(RationaleZahl.von(7), g.binde(mapOf("y" to RationaleZahl.von(3))).auswerten())
    }

    @Test fun abbildAkzeptiertAllgemeineParameterUndBeliebigeEndlicheElemente() {
        val a = AllgemeinerParameter("a")
        val identität = Methode("id", listOf(a), mapOf("wert" to a))
        val elemente = EndlicheMenge(setOf(
            Tupel(listOf(RationaleZahl.von(1), RationaleZahl.von(2))),
            WahrheitsKonstante(true),
        ))

        assertEquals(elemente, bildeAb(elemente, identität))
    }

    @Test fun abbildBenötigtEineEinwertigeMethode() {
        val a = AllgemeinerParameter("a")
        val menge = EndlicheMenge(setOf(WahrheitsKonstante(true)))

        assertFailsWith<IllegalArgumentException> {
            bildeAb(menge, Methode("f", listOf(a, AllgemeinerParameter("b")), mapOf("wert" to a)))
        }
        assertFailsWith<IllegalArgumentException> {
            bildeAb(menge, Methode("g", listOf(a), mapOf("links" to a, "rechts" to a)))
        }
    }

    @Test fun matrixWirdExaktInvertiert() {
        val m = Matrix(listOf(listOf(RationaleZahl.von(2), RationaleZahl.von(0)), listOf(RationaleZahl.von(0), RationaleZahl.von(4))))
        val inv = m.inverseRational()
        assertEquals(RationaleZahl.von(1, 2), inv.zeilen[0][0])
        assertEquals(RationaleZahl.von(1, 4), inv.zeilen[1][1])
    }

    @Test fun matrixAusMethodeVerwendetNullbasierteZeilenUndSpaltenindizes() {
        val zeile = Variable("i"); val spalte = Variable("j")
        val methode = Methode("f", listOf(zeile, spalte), mapOf("wert" to addition(multiplikation(RationaleZahl.von(10), zeile), spalte)))

        val matrix = matrixAusMethode(methode, höhe = 2, breite = 3)

        assertEquals(
            listOf(
                listOf(RationaleZahl.von(0), RationaleZahl.von(1), RationaleZahl.von(2)),
                listOf(RationaleZahl.von(10), RationaleZahl.von(11), RationaleZahl.von(12)),
            ),
            matrix.zeilen,
        )
    }

    @Test fun matrixAusMethodeBenötigtZweiParameterUndEineZahlAusgabe() {
        val x = Variable("x")
        assertFailsWith<IllegalArgumentException> { matrixAusMethode(Methode("f", listOf(x), mapOf("wert" to x)), 1, 1) }
        assertFailsWith<IllegalArgumentException> { matrixAusMethode(Methode("g", listOf(x, Variable("y")), mapOf("wert" to Tupel(listOf(x)))), 1, 1) }
    }

    @Test fun lineareGleichungWirdGelöst() {
        val x = Variable("x")
        val gleichung = Gleichheit(addition(multiplikation(RationaleZahl.von(2), x), RationaleZahl.von(4)), RationaleZahl.von(10))
        assertEquals(listOf(RationaleZahl.von(3)), löseLinear(gleichung, x).lösungen)
    }

    @Test fun unentscheidbareAussageBleibtOhneWahrheitswert() {
        val e = UnentscheidbareAussage("G", "Axiomensystem S").entscheide()
        assertNull(e.wahrheitswert)
        assertIs<EntscheidungsStatus.Unentscheidbar>(e.status)
    }

    @Test fun wurzelLiefertEineKomplexeHauptwurzel() {
        val wurzel = assertIs<KomplexeZahl>(wurzel(RationaleZahl.von(-1)))
        assertEquals(RationaleZahl.Null, wurzel.realteil)
        assertEquals(RationaleZahl.Eins, wurzel.imaginärteil)
    }

    @Test fun kartesischesProduktEndlicherMengenBildetTupel() {
        val produkt = assertIs<EndlicheMenge>(kartesischesProdukt(listOf(
            EndlicheMenge(setOf(RationaleZahl.von(1), RationaleZahl.von(2))),
            EndlicheMenge(setOf(RationaleZahl.von(3))),
        )))
        assertEquals(setOf(Tupel(listOf(RationaleZahl.von(1), RationaleZahl.von(3))), Tupel(listOf(RationaleZahl.von(2), RationaleZahl.von(3)))), produkt.elemente)
    }

    @Test fun echteTeilmengeUndDisjunktheitWerdenEntschieden() {
        val a = EndlicheMenge(setOf(RationaleZahl.von(1)))
        val b = EndlicheMenge(setOf(RationaleZahl.von(1), RationaleZahl.von(2)))
        assertEquals(Wahrheitswert.Wahr, EchteTeilmengeBeziehung(a, b).entscheide().wahrheitswert)
        assertEquals(Wahrheitswert.Wahr, Disjunktheit(a, EndlicheMenge(setOf(RationaleZahl.von(3)))).entscheide().wahrheitswert)
    }

    @Test fun komplexeZahlAusTupelUndKonjugation() {
        val z = komplexAusKartesisch(Tupel(listOf(RationaleZahl.von(2), RationaleZahl.Null)))
        assertEquals(RationaleZahl.von(2), z.realteil)
        assertEquals(RationaleZahl.von(-1), konjugiere(KomplexeZahl(RationaleZahl.Null, RationaleZahl.Eins)).imaginärteil)
    }

    @Test fun orientierteVektorenVerhindernFalscheSkalarprodukte() {
        val spalte = SpaltenVektor(listOf(RationaleZahl.von(1), RationaleZahl.von(2)))
        assertEquals(RationaleZahl.von(5), spalte.skalarprodukt(spalte))
        assertFailsWith<IllegalArgumentException> { spalte.skalarprodukt(ZeilenVektor(spalte.werte)) }
    }

    @Test fun koeffizientenErzeugenEinPolynomInAufsteigenderEingabereihenfolge() {
        val polynom = polynomAusKoeffizienten(listOf(RationaleZahl.von(2), RationaleZahl.von(-3), RationaleZahl.von(5)), Variable("x"))

        assertEquals("5 \\cdot {x}^{2} + -3 \\cdot x + 2", polynom.zuLatex())
        assertEquals(RationaleZahl.Null, polynomAusKoeffizienten(listOf(RationaleZahl.Null, RationaleZahl.Null), Variable("x")))
        assertEquals(RationaleZahl.von(7), polynomAusKoeffizienten(listOf(RationaleZahl.von(7)), Variable("x")))
        assertEquals(Variable("x"), polynomAusKoeffizienten(listOf(RationaleZahl.Null, RationaleZahl.Eins), Variable("x")))
    }

    @Test fun polynomKoeffizientenDuerfenDieUnbestimmteNichtEnthalten() {
        assertFailsWith<IllegalArgumentException> {
            polynomAusKoeffizienten(listOf(Variable("y")), Variable("y"))
        }
        assertFailsWith<IllegalArgumentException> {
            polynomAusKoeffizienten(emptyList(), Variable("x"))
        }
    }

    @Test fun vektorenUndMatrizenVerwendenPmatrixLatexMitZeilenUndSpaltenTrennern() {
        val a = Variable("a_1")
        val b = Variable("a_2")

        assertEquals("\\begin{pmatrix}a_1 \\\\ a_2\\end{pmatrix}", SpaltenVektor(listOf(a, b)).zuLatex())
        assertEquals("\\begin{pmatrix}a_1 & a_2\\end{pmatrix}", ZeilenVektor(listOf(a, b)).zuLatex())
        assertEquals(
            "\\begin{pmatrix}a_1 & a_2 \\\\ a_2 & a_1\\end{pmatrix}",
            Matrix(listOf(listOf(a, b), listOf(b, a))).zuLatex(),
        )
    }
}
