package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FalkSchemaTest {
    private fun variable(name: String) = Variable(name)

    @Test
    fun `2 mal 3 mit 3 mal 2 liefert die gewählte Zeile Spalte und geordnete Summanden`() {
        val a = Matrix(
            listOf(
                listOf(variable("a11"), variable("a12"), variable("a13")),
                listOf(variable("a21"), variable("a22"), variable("a23")),
            ),
        )
        val b = Matrix(
            listOf(
                listOf(variable("b11"), variable("b12")),
                listOf(variable("b21"), variable("b22")),
                listOf(variable("b31"), variable("b32")),
            ),
        )

        val modell = assertIs<DetailliertesFalkSchemaErgebnis.Gültig>(
            detailliertesFalkSchema(a, b, zeilenIndex = 1, spaltenIndex = 1),
        ).modell

        assertEquals(listOf("a21", "a22", "a23"), modell.summanden.map { it.linkerFaktor.zuLatex() })
        assertEquals(listOf("b12", "b22", "b32"), modell.summanden.map { it.rechterFaktor.zuLatex() })
        assertEquals("a21\\cdot b12 + a22\\cdot b22 + a23\\cdot b32", modell.summenLatex())
        assertEquals(2, modell.ergebnis.zeilenAnzahl)
        assertEquals(2, modell.ergebnis.spaltenAnzahl)
    }

    @Test
    fun `Faktorenreihenfolge wird auch bei nichtkommutativen Symbolen nicht vertauscht`() {
        val a = Matrix(listOf(listOf(variable("p"), variable("q"))))
        val b = Matrix(listOf(listOf(variable("r")), listOf(variable("s"))))

        val modell = assertIs<DetailliertesFalkSchemaErgebnis.Gültig>(
            detailliertesFalkSchema(a, b),
        ).modell

        assertEquals("p\\cdot r + q\\cdot s", modell.summenLatex())
        assertEquals("p", modell.summanden.first().linkerFaktor.zuLatex())
        assertEquals("r", modell.summanden.first().rechterFaktor.zuLatex())
    }

    @Test
    fun `inkompatible Dimensionen erzeugen eine konkrete Diagnose statt eines Ergebnisrasters`() {
        val a = Matrix(listOf(listOf(variable("a"), variable("b"))))
        val b = Matrix(listOf(listOf(variable("c"), variable("d"))))

        val fehler = assertIs<DetailliertesFalkSchemaErgebnis.Inkompatibel>(
            detailliertesFalkSchema(a, b),
        )

        assertEquals(2, fehler.linkeSpalten)
        assertEquals(1, fehler.rechteZeilen)
        assertEquals(
            "Die Matrizen sind nicht multiplizierbar: Die linke Matrix besitzt 2 Spalten, die rechte 1 Zeilen.",
            fehler.meldung,
        )
    }
}
