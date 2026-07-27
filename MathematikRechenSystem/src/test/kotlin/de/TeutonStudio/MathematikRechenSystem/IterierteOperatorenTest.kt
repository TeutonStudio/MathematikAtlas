package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

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
}
