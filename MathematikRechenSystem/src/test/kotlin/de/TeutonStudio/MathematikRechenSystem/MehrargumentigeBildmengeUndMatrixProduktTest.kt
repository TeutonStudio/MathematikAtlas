package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class MehrargumentigeBildmengeUndMatrixProduktTest {
    @Test
    fun `orientierte Vektoren werden im Matrixprodukt als einspaltige und einzeilige Matrizen interpretiert`() {
        val zeile = ZeilenVektor(listOf(zahl(1), zahl(2), zahl(3)))
        val spalte = SpaltenVektor(listOf(zahl(4), zahl(5), zahl(6)))

        assertEquals(
            listOf(listOf(zahl(32))),
            matrixProdukt(zeile, spalte).zeilen,
        )
        assertEquals(
            listOf(
                listOf(zahl(4), zahl(8), zahl(12)),
                listOf(zahl(5), zahl(10), zahl(15)),
                listOf(zahl(6), zahl(12), zahl(18)),
            ),
            matrixProdukt(spalte, zeile).zeilen,
        )

        val matrix = Matrix(
            listOf(
                listOf(zahl(1), zahl(2), zahl(3)),
                listOf(zahl(4), zahl(5), zahl(6)),
            ),
        )
        assertEquals(
            listOf(listOf(zahl(32)), listOf(zahl(77))),
            matrixProdukt(matrix, spalte).zeilen,
        )
    }

    @Test
    fun `mehrstellige Bildmenge wertet Tupel als Gesamtargumente aus`() {
        val x = Variable("x")
        val y = Variable("y")
        val methode = Methode(
            name = "f",
            parameter = listOf(x, y),
            vorschrift = addition(x, y),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen, y.name to ReelleZahlen),
        )
        val definitionsMenge = EndlicheMenge(
            setOf(
                Tupel(listOf(zahl(1), zahl(2))),
                Tupel(listOf(zahl(3), zahl(4))),
            ),
        )

        assertEquals(
            EndlicheMenge(setOf(zahl(3), zahl(7))),
            bildeAb(definitionsMenge, methode),
        )
    }

    @Test
    fun `Tupel bleibt bei einstelliger Methode genau ein Argument`() {
        val a = AllgemeinerParameter("a")
        val identität = Methode(
            name = "id",
            parameter = listOf(a),
            vorschrift = a,
            zielMenge = Tupelraum(listOf(ReelleZahlen, ReelleZahlen)),
            werteVorräte = mapOf(a.name to Tupelraum(listOf(ReelleZahlen, ReelleZahlen))),
        )
        val tupel = Tupel(listOf(zahl(1), zahl(2)))

        assertEquals(
            EndlicheMenge(setOf(tupel)),
            bildeAb(EndlicheMenge(setOf(tupel)), identität),
        )
    }

    @Test
    fun `mehrstellige Methode lehnt Nicht-Tupel als Gesamtargument ab`() {
        val x = Variable("x")
        val y = Variable("y")
        val methode = Methode(
            name = "f",
            parameter = listOf(x, y),
            vorschrift = addition(x, y),
            zielMenge = ReelleZahlen,
            werteVorräte = mapOf(x.name to ReelleZahlen, y.name to ReelleZahlen),
        )

        val fehler = assertFailsWith<IllegalStateException> {
            bildeAb(EndlicheMenge(setOf(zahl(1))), methode)
        }
        assertContains(fehler.message.orEmpty(), "Tupel")
    }

    private fun zahl(wert: Long): RationaleZahl = RationaleZahl.von(wert)
}
