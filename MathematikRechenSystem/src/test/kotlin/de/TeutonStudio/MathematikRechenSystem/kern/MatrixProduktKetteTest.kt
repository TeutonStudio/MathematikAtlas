package de.TeutonStudio.MathematikRechenSystem.kern

import java.math.BigDecimal
import kotlin.test.*

class MatrixProduktKetteTest {
    private fun z(n: Long) = RationaleZahl.von(n)
    private fun m(n: Long) = Matrix(listOf(listOf(z(n))))
    @Test fun `zwei bis sechs Faktoren und erster Dimensionsfehler`() {
        for (n in 2..6) {
            val kette = matrixProduktKette(List(n) { m(2) })
            assertEquals(n - 1, kette.zwischenprodukte.size)
            assertEquals(m(1L shl n), kette.ergebnis)
        }
        assertFailsWith<IllegalArgumentException> { matrixProduktKette(listOf(m(1))) }
        val fehler = assertFailsWith<IllegalArgumentException> {
            matrixProduktKette(listOf(m(1), m(2), Matrix(listOf(listOf(z(1)), listOf(z(2))))))
        }
        assertTrue(fehler.message!!.contains("Faktor 3"))
        assertTrue(fehler.message!!.contains("1×1"))
        assertTrue(fehler.message!!.contains("2×1"))
    }
    @Test fun `Zwischenprodukte sind separate Belege der Linksassoziation`() {
        val a = Matrix(listOf(listOf(z(1), z(2))))
        val b = Matrix(listOf(listOf(z(3)), listOf(z(4))))
        val c = Matrix(listOf(listOf(z(5), z(6), z(7))))
        val kette = matrixProduktKette(listOf(a, b, c))
        assertEquals(a * b, kette.zwischenprodukte[0])
        assertEquals((a * b) * c, kette.zwischenprodukte[1])
        assertEquals(1, kette.zwischenprodukte[0].spaltenAnzahl)
        assertEquals(3, (b * c).spaltenAnzahl)
    }
    @Test fun `Quaternionen behalten nichtkommutative Eintragsreihenfolge`() {
        val i = Variable("i"); val j = Variable("j")
        fun q(i: Int, j: Int, k: Int) = DomaenenWert.Quaternion(BigDecimal.ZERO, i.toBigDecimal(), j.toBigDecimal(), k.toBigDecimal())
        val kontext = DomaenenKontext(mapOf("i" to q(1,0,0), "j" to q(0,1,0)))
        val ij = matrixProdukt(listOf(Matrix(listOf(listOf(i))), Matrix(listOf(listOf(j)))))
        val ji = matrixProdukt(listOf(Matrix(listOf(listOf(j))), Matrix(listOf(listOf(i)))))
        assertEquals(DomaenenErgebnis.Wert(q(0,0,1)), DomaenenAuswerter.wert(ij.zeilen[0][0], kontext))
        assertEquals(DomaenenErgebnis.Wert(q(0,0,-1)), DomaenenAuswerter.wert(ji.zeilen[0][0], kontext))
    }
    @Test fun `orientierte Faktoren benutzen denselben geordneten Kern`() {
        assertEquals(m(11), matrixProdukt(listOf(ZeilenVektor(listOf(z(1), z(2))), SpaltenVektor(listOf(z(3), z(4))))))
    }
}
