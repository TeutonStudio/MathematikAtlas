package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class TupelMethodenOperatorenTest {
    private val x = Variable("x", bindungsId = "x-id")
    private val y = Variable("y", bindungsId = "y-id")
    private fun z(n: Long) = RationaleZahl.von(n)
    private fun f() = MathematischeMethode("f", listOf(x, y), Tupel(listOf(addition(multiplikation(z(2), x), y, z(1)), addition(x, y))),
        Tupelraum(listOf(ReelleZahlen, ReelleZahlen)), mapOf("x" to ReelleZahlen, "y" to ReelleZahlen), listOf("a", "b"))

    @Test fun `affine Tupelmethode wird exakt invertiert`() {
        val f = f(); val invers = f.invertiereRationalAffin()
        val bild = f.wendeMitBedingungenAn(Tupel(listOf(z(2), z(3)))).ergebnis
        assertEquals(Tupel(listOf(z(2), z(3))), invers.wendeMitBedingungenAn(bild).ergebnis)
        assertEquals(f.signatur.ergebnisse.map { it.id }, invers.signatur.argumente.map { it.id })
        assertFailsWith<IllegalStateException> { f.copy(vorschrift = Tupel(listOf(x, x))).invertiereRationalAffin() }
        assertFailsWith<IllegalStateException> { f.copy(vorschrift = Tupel(listOf(multiplikation(x, y), y))).invertiereRationalAffin() }
    }
    @Test fun `partielle Ableitung aller Komponenten erhaelt Argumentidentitaeten`() {
        val f = f(); val d = f.leiteNachArgumentAb("x-id")
        assertEquals(f.signatur.argumente, d.signatur.argumente)
        assertEquals(Tupel(listOf(z(2), z(1))), d.wendeMitBedingungenAn(Tupel(listOf(z(4), z(5)))).ergebnis)
    }
    @Test fun `Polynomintegration fuehrt unabhängige Konstanten`() {
        val f = f()
        val konstanten = listOf(Variable("C1", bindungsId = "c1"), Variable("C2", bindungsId = "c2"))
        val stamm = f.integrierePolynomiell("x-id", konstanten)
        assertTrue(stamm.vorschrift.enthalteneMethodenParameter().containsAll(konstanten))
        val rück = stamm.leiteNachArgumentAb("x-id")
        assertEquals(f.wendeMitBedingungenAn(Tupel(listOf(z(4), z(5)))).ergebnis, rück.wendeMitBedingungenAn(Tupel(listOf(z(4), z(5)))).ergebnis)
    }
    @Test fun `numerische Jacobi Matrix respektiert Komponentenreihenfolge`() {
        val j = assertIs<MethodenNumerikErgebnis.Approximation<List<List<Double>>>>(f().jacobiNumerisch(listOf(2.0, 3.0)))
        assertEquals(2.0, j.wert[0][0], 1e-6); assertEquals(1.0, j.wert[0][1], 1e-6)
        assertEquals(1.0, j.wert[1][0], 1e-6); assertEquals(1.0, j.wert[1][1], 1e-6)
        val abs = MathematischeMethode("abs", listOf(x), Betrag(x), ReelleZahlen, mapOf("x" to ReelleZahlen))
        assertIs<MethodenNumerikErgebnis.Fehler>(abs.jacobiNumerisch(listOf(0.0)))
    }
    @Test fun `gedaempfte lokale Inversion liefert Kandidat und respektiert Budget`() {
        val invers = assertIs<MethodenNumerikErgebnis.Approximation<List<Double>>>(f().invertiereLokalNumerisch(listOf(8.0, 5.0), listOf(0.0, 0.0), listOf(-10.0, -10.0), listOf(10.0, 10.0)))
        assertEquals(2.0, invers.wert[0], 1e-6); assertEquals(3.0, invers.wert[1], 1e-6)
        assertIs<MethodenNumerikErgebnis.Fehler>(f().jacobiNumerisch(listOf(0.0, 0.0), MethodenNumerikOptionen(maximaleAuswertungen = 1)))
    }
    @Test fun `bestimmte Integration berechnet alle Komponenten mit Orientierung`() {
        val integral = assertIs<MethodenNumerikErgebnis.Approximation<List<Double>>>(f().integriereBestimmtNumerisch("x-id", mapOf("y-id" to 3.0), 0.0, 2.0))
        assertEquals(12.0, integral.wert[0], 1e-8); assertEquals(8.0, integral.wert[1], 1e-8)
        val rück = assertIs<MethodenNumerikErgebnis.Approximation<List<Double>>>(f().integriereBestimmtNumerisch("x-id", mapOf("y-id" to 3.0), 2.0, 0.0))
        assertEquals(-12.0, rück.wert[0], 1e-8)
    }

    @Test fun `Tensorableitungen und Indexmethoden sind verlustfrei speicherbar`() {
        val tensor = MathematischeMethode("T", listOf(x), Tensor(listOf(1, 1), listOf(x)), Tensorraum(ReelleZahlen, listOf(z(1), z(1))), mapOf("x" to ReelleZahlen))
        val ableitung = tensor.leiteNachArgumentAb("x-id")
        assertEquals(ableitung, MathematischerWertCodec.dekodieren(MathematischerWertCodec.kodieren(ableitung)))
        val index = MathematischeMethode("index", listOf(x), x, ReelleZahlen, mapOf("x" to EndlicheIndexMenge(z(3))))
        assertEquals(index, MathematischerWertCodec.dekodieren(MathematischerWertCodec.kodieren(index)))
    }
}
