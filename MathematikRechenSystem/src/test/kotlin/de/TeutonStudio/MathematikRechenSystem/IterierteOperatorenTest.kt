package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class IterierteOperatorenTest {
    private val k = Variable("k")
    private val einsBisDrei = EndlicheMenge(setOf(RationaleZahl.von(1), RationaleZahl.von(2), RationaleZahl.von(3)))

    @Test
    fun `zahlwertige Funktion behält Zielmenge bei partieller Bindung`() {
        val x = Variable("x")
        val f = Funktion("f", listOf(k, x), mapOf("wert" to addition(k, x)), mapOf("wert" to ReelleZahlen))

        val gebunden = f.binde(mapOf("x" to RationaleZahl.von(2)))

        assertEquals(ReelleZahlen, gebunden.funktion.zielMengeFür("wert"))
        assertEquals(RationaleZahl.von(5), gebunden.binde(mapOf("k" to RationaleZahl.von(3))).auswerten().getValue("wert"))
    }

    @Test
    fun `mengenwerte werden bei Anwendung substituiert`() {
        val grundMenge = EndlicheMenge((1L..4L).map(RationaleZahl::von).toSet())
        val methode = Funktion(
            "A", listOf(k), mapOf("menge" to EndlicheMenge(setOf(k, addition(k, RationaleZahl.Eins)))),
            mapOf("menge" to grundMenge),
        )

        assertEquals(
            EndlicheMenge(setOf(RationaleZahl.von(2), RationaleZahl.von(3))),
            methode.wendeAn(mapOf("k" to RationaleZahl.von(2))).getValue("menge"),
        )
    }

    @Test
    fun `iterierte Summe und Produkt werten endliche Indexmengen aus`() {
        val quadrat = Funktion("f", listOf(k), mapOf("wert" to Potenz(k, RationaleZahl.von(2))), mapOf("wert" to ReelleZahlen))
        val identität = Funktion("g", listOf(k), mapOf("wert" to k), mapOf("wert" to ReelleZahlen))

        assertEquals(RationaleZahl.von(14), iterierteSumme(quadrat, einsBisDrei))
        assertEquals(RationaleZahl.von(24), iteriertesProdukt(identität, EndlicheMenge((1L..4L).map(RationaleZahl::von).toSet())))
    }

    @Test
    fun `iterierte Mengenoperationen verwenden die deklarierte Zielmenge`() {
        val grundMenge = EndlicheMenge((1L..4L).map(RationaleZahl::von).toSet())
        val methode = Funktion(
            "A", listOf(k), mapOf("menge" to EndlicheMenge(setOf(k, addition(k, RationaleZahl.Eins)))),
            mapOf("menge" to grundMenge),
        )

        assertEquals(grundMenge, iterierteVereinigung(methode, einsBisDrei))
        assertEquals(EndlicheMenge(setOf(RationaleZahl.von(2))), iterierterSchnitt(methode, EndlicheMenge(setOf(RationaleZahl.von(1), RationaleZahl.von(2)))))
        assertEquals(grundMenge, methode.grundMenge)
    }

    @Test
    fun `leere Indexmengen verwenden die korrekten neutralen Elemente`() {
        val zahlen = Funktion("f", listOf(k), mapOf("wert" to k), mapOf("wert" to ReelleZahlen))
        val grundMenge = EndlicheMenge(setOf(RationaleZahl.von(1), RationaleZahl.von(2), RationaleZahl.von(3)))
        val mengen = Funktion("A", listOf(k), mapOf("menge" to EndlicheMenge(setOf(k))), mapOf("menge" to grundMenge))

        assertEquals(RationaleZahl.Null, iterierteSumme(zahlen, LeereMenge))
        assertEquals(RationaleZahl.Eins, iteriertesProdukt(zahlen, LeereMenge))
        assertEquals(LeereMenge, iterierteVereinigung(mengen, LeereMenge))
        assertEquals(grundMenge, iterierterSchnitt(mengen, LeereMenge))
    }

    @Test
    fun `ungültige Methoden und Zielmengen werden verständlich abgelehnt`() {
        val ohneZielmenge = Funktion("f", listOf(k), mapOf("wert" to k))
        val zweiParameter = Funktion("g", listOf(k, Variable("j")), mapOf("wert" to k), mapOf("wert" to ReelleZahlen))
        val mehrereAusgaben = Funktion("h", listOf(k), mapOf("a" to k, "b" to k), mapOf("a" to ReelleZahlen, "b" to ReelleZahlen))
        val außerhalb = Funktion(
            "A", listOf(k), mapOf("menge" to EndlicheMenge(setOf(RationaleZahl.von(4)))),
            mapOf("menge" to EndlicheMenge(setOf(RationaleZahl.von(1)))),
        )

        assertFailsWith<IllegalStateException> { iterierteSumme(ohneZielmenge, LeereMenge) }
        assertFailsWith<IllegalArgumentException> { iterierteSumme(zweiParameter, LeereMenge) }
        assertFailsWith<IllegalArgumentException> { iterierteSumme(mehrereAusgaben, LeereMenge) }
        assertFailsWith<IllegalStateException> { iterierteVereinigung(außerhalb, EndlicheMenge(setOf(RationaleZahl.von(3)))) }
    }

    @Test
    fun `parameterabhängige Zielmenge wird als Grundmenge abgelehnt`() {
        val abhängigeZielmenge = Funktion(
            "A", listOf(k), mapOf("menge" to EndlicheMenge(setOf(k))),
            mapOf("menge" to EndlicheMenge(setOf(k))),
        )

        val fehler = assertFailsWith<IllegalArgumentException> {
            iterierterSchnitt(abhängigeZielmenge, LeereMenge)
        }

        assertTrue(fehler.message.orEmpty().contains("darf nicht vom Iterationsparameter 'k' abhängen"))
        assertTrue(fehler.message.orEmpty().contains("leeren Schnitt"))
    }

    @Test
    fun `endliche Mengen werden gegen Zahlbereiche als Teilmengen validiert`() {
        val gültig = Funktion(
            "A", listOf(k), mapOf("menge" to EndlicheMenge(setOf(k, RationaleZahl.von(2)))),
            mapOf("menge" to NatürlicheZahlen),
        )
        val ungültig = Funktion(
            "B", listOf(k), mapOf("menge" to EndlicheMenge(setOf(RationaleZahl.von(-1)))),
            mapOf("menge" to NatürlicheZahlen),
        )

        assertEquals(
            EndlicheMenge(setOf(RationaleZahl.von(1), RationaleZahl.von(2))),
            iterierteVereinigung(gültig, EndlicheMenge(setOf(RationaleZahl.von(1)))),
        )
        val fehler = assertFailsWith<IllegalStateException> {
            iterierteVereinigung(ungültig, EndlicheMenge(setOf(RationaleZahl.von(1))))
        }
        assertTrue(fehler.message.orEmpty().contains("Die Methode 'B' liefert für k = 1"))
        assertTrue(fehler.message.orEmpty().contains("Grundmenge \\mathbb{N}"))
    }

    @Test
    fun `symbolische Indexmenge und Set-Reihenfolge bleiben deterministisch`() {
        val methode = Funktion("f", listOf(k), mapOf("wert" to Potenz(k, RationaleZahl.von(2))), mapOf("wert" to ReelleZahlen))
        val symbolisch = iterierteSumme(methode, BenannteMenge("I"))
        val erste = iterierteSumme(methode, EndlicheMenge(linkedSetOf(RationaleZahl.von(3), RationaleZahl.von(1), RationaleZahl.von(2))))
        val zweite = iterierteSumme(methode, EndlicheMenge(linkedSetOf(RationaleZahl.von(1), RationaleZahl.von(2), RationaleZahl.von(3))))

        assertIs<IterierteSumme>(symbolisch)
        assertEquals(erste, zweite)
        assertEquals(erste.zuLatex(), zweite.zuLatex())
    }

    @Test
    fun `symbolisch unentscheidbare Zielmengenbeziehung bleibt erhalten`() {
        val methode = Funktion("A", listOf(k), mapOf("menge" to BenannteMenge("A_k")), mapOf("menge" to BenannteMenge("G")))

        assertEquals(BenannteMenge("A_k"), iterierteVereinigung(methode, EndlicheMenge(setOf(RationaleZahl.von(1)))))
    }

    @Test
    fun `symbolische Grundmenge bleibt für symbolischen Schnitt abgeleitet`() {
        val grundMenge = BenannteMenge("G")
        val methode = Funktion("A", listOf(k), mapOf("menge" to BenannteMenge("A_k")), mapOf("menge" to grundMenge))

        val schnitt = assertIs<IterierterSchnitt>(iterierterSchnitt(methode, BenannteMenge("I")))

        assertEquals(grundMenge, schnitt.grundMenge)
        assertEquals(grundMenge, methode.grundMengeFürMengenAusgabe())
    }

    @Test
    fun `mengeniterationen bleiben für unterschiedliche Indexreihenfolgen deterministisch`() {
        val grundMenge = EndlicheMenge((1L..4L).map(RationaleZahl::von).toSet())
        val methode = Funktion(
            "A", listOf(k), mapOf("menge" to EndlicheMenge(setOf(k, addition(k, RationaleZahl.Eins)))),
            mapOf("menge" to grundMenge),
        )
        val ersteIndexmenge = EndlicheMenge(linkedSetOf(RationaleZahl.von(3), RationaleZahl.von(1), RationaleZahl.von(2)))
        val zweiteIndexmenge = EndlicheMenge(linkedSetOf(RationaleZahl.von(1), RationaleZahl.von(2), RationaleZahl.von(3)))

        val ersteVereinigung = iterierteVereinigung(methode, ersteIndexmenge)
        val zweiteVereinigung = iterierteVereinigung(methode, zweiteIndexmenge)
        val ersterSchnitt = iterierterSchnitt(methode, ersteIndexmenge)
        val zweiterSchnitt = iterierterSchnitt(methode, zweiteIndexmenge)

        assertEquals(ersteVereinigung, zweiteVereinigung)
        assertEquals(ersteVereinigung.hashCode(), zweiteVereinigung.hashCode())
        assertEquals(ersteVereinigung.zuLatex(), zweiteVereinigung.zuLatex())
        assertEquals(ersterSchnitt, zweiterSchnitt)
        assertEquals(ersterSchnitt.hashCode(), zweiterSchnitt.hashCode())
        assertEquals(ersterSchnitt.zuLatex(), zweiterSchnitt.zuLatex())
    }

    @Test
    fun `Zahlfunktionen bleiben für Summen gültig aber erhalten keine Mengengrundmenge`() {
        val zahlen = Funktion("f", listOf(k), mapOf("wert" to k), mapOf("wert" to ReelleZahlen))

        assertEquals(RationaleZahl.von(3), iterierteSumme(zahlen, EndlicheMenge(setOf(RationaleZahl.von(1), RationaleZahl.von(2)))))
        assertFailsWith<IllegalArgumentException> { zahlen.grundMenge }
    }

    @Test
    fun `Variablenanalyse durchläuft Mengen Aussagen Vektoren und Iterationen`() {
        val mengenMethode = Funktion("A", listOf(k), mapOf("menge" to EndlicheMenge(setOf(k))), mapOf("menge" to BenannteMenge("G")))
        val ausdruck = Konjunktion(listOf(
            Gleichheit(Matrix(listOf(listOf(k))), Matrix(listOf(listOf(RationaleZahl.Eins)))),
            TeilmengenBeziehung(IterierterSchnitt(mengenMethode, EndlicheMenge(setOf(Variable("i")))), BenannteMenge("G")),
        ))

        assertEquals(setOf("k", "i"), ausdruck.enthalteneVariablen().map { it.name }.toSet())
    }
}
