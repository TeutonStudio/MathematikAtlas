package de.TeutonStudio.MathematikKnoten

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LatexFormelTest {
    @Test
    fun `zerlegt zwei mal zwei Matrix in Zeilen und Spalten`() {
        val analyse = analysiereLatexMatrix(
            "\\begin{pmatrix}a_{11} & a_{12} \\\\ a_{21} & a_{22}\\end{pmatrix}",
        )

        val formel = assertIs<LatexMatrixAnalyse.Erfolg>(analyse).formel
        assertEquals(
            listOf(
                listOf("a_{11}", "a_{12}"),
                listOf("a_{21}", "a_{22}"),
            ),
            formel.zeilen,
        )
    }

    @Test
    fun `erkennt Zeilenvektor und Spaltenvektor`() {
        val zeile = assertIs<LatexMatrixAnalyse.Erfolg>(
            analysiereLatexMatrix("\\begin{pmatrix}x_1 & x_2 & x_3\\end{pmatrix}"),
        ).formel
        val spalte = assertIs<LatexMatrixAnalyse.Erfolg>(
            analysiereLatexMatrix("\\begin{pmatrix}x_1 \\\\ x_2 \\\\ x_3\\end{pmatrix}"),
        ).formel

        assertEquals(listOf(listOf("x_1", "x_2", "x_3")), zeile.zeilen)
        assertEquals(listOf(listOf("x_1"), listOf("x_2"), listOf("x_3")), spalte.zeilen)
    }

    @Test
    fun `trennt nicht innerhalb von Gruppen und Brüchen`() {
        val analyse = analysiereLatexMatrix(
            "\\begin{pmatrix}\\frac{a&b}{c} & x_{n} \\\\ \\frac{1}{2} & f(x)\\end{pmatrix}",
        )

        val formel = assertIs<LatexMatrixAnalyse.Erfolg>(analyse).formel
        assertEquals("\\frac{a&b}{c}", formel.zeilen[0][0])
        assertEquals("x_{n}", formel.zeilen[0][1])
        assertEquals("\\frac{1}{2}", formel.zeilen[1][0])
    }

    @Test
    fun `verschachtelte Tupelmatrix bleibt eine einzelne äußere Zelle`() {
        val innere = "\\begin{pmatrix}2 & 3 \\\\ 4 & 5\\end{pmatrix}"
        val analyse = analysiereLatexMatrix(
            "\\begin{pmatrix}1 & $innere & x\\end{pmatrix}",
        )

        val formel = assertIs<LatexMatrixAnalyse.Erfolg>(analyse).formel
        assertEquals(1, formel.zeilen.size)
        assertEquals(3, formel.zeilen.single().size)
        assertEquals(innere, formel.zeilen.single()[1])
    }

    @Test
    fun `erhält Präfix Suffix und beide Display-Begrenzer`() {
        val dollar = assertIs<LatexMatrixAnalyse.Erfolg>(
            analysiereLatexMatrix("\$\$A=\\begin{pmatrix}1 & 2\\end{pmatrix}\\in M\$\$"),
        ).formel
        val eckig = assertIs<LatexMatrixAnalyse.Erfolg>(
            analysiereLatexMatrix("\\[A=\\begin{pmatrix}1 & 2\\end{pmatrix}\\in M\\]"),
        ).formel

        assertEquals("A=", dollar.vorher)
        assertEquals("\\in M", dollar.nachher)
        assertEquals(dollar, eckig)
    }

    @Test
    fun `meldet fehlendes Ende und ungleiche Spaltenzahlen`() {
        val ohneEnde = assertIs<LatexMatrixAnalyse.Fehler>(
            analysiereLatexMatrix("\\begin{pmatrix}1 & 2"),
        )
        val ungleich = assertIs<LatexMatrixAnalyse.Fehler>(
            analysiereLatexMatrix("\\begin{pmatrix}1 & 2 \\\\ 3\\end{pmatrix}"),
        )

        assertEquals("Die Matrix besitzt kein \\end{pmatrix}.", ohneEnde.diagnose)
        assertEquals("Matrixzeilen besitzen unterschiedliche Spaltenzahlen: 2, 1.", ungleich.diagnose)
    }

    @Test
    fun `gewöhnliche Inline Formel bleibt ohne Matrixanalyse`() {
        assertIs<LatexMatrixAnalyse.KeineMatrix>(analysiereLatexMatrix("x_1 + x_2"))
        assertEquals("x_1 + x_2", entferneLatexDisplayBegrenzer("x_1 + x_2"))
    }
}
