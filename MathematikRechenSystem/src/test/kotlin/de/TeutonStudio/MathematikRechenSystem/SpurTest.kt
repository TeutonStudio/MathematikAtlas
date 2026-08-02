package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.Matrix
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.Tensor
import de.TeutonStudio.MathematikRechenSystem.kern.Variable
import de.TeutonStudio.MathematikRechenSystem.kern.addition
import de.TeutonStudio.MathematikRechenSystem.kern.spur
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SpurTest {
    @Test
    fun `Spur summiert die Hauptdiagonale einer quadratischen Matrix`() {
        val matrix = Matrix(
            listOf(
                listOf(zahl(1), zahl(2), zahl(3)),
                listOf(zahl(4), zahl(5), zahl(6)),
                listOf(zahl(7), zahl(8), zahl(9)),
            ),
        )

        assertEquals(zahl(15), spur(matrix))
    }

    @Test
    fun `Einmal-eins Matrix liefert ihren einzigen Eintrag`() {
        assertEquals(zahl(7), spur(Matrix(listOf(listOf(zahl(7))))))
    }

    @Test
    fun `Symbolische Diagonale bleibt eine gemeinsame Addition`() {
        val a = Variable("a")
        val b = Variable("b")
        val matrix = Matrix(
            listOf(
                listOf(a, zahl(2)),
                listOf(zahl(3), b),
            ),
        )

        assertEquals(addition(a, b), spur(matrix))
        assertEquals("a + b", spur(matrix).zuLatex())
    }

    @Test
    fun `Rechteckige Matrix wird mit ihrer Form abgewiesen`() {
        val fehler = assertFailsWith<IllegalArgumentException> {
            spur(
                Matrix(
                    listOf(
                        listOf(zahl(1), zahl(2), zahl(3)),
                        listOf(zahl(4), zahl(5), zahl(6)),
                    ),
                ),
            )
        }

        assertEquals(
            "Die Spur ist nur für quadratische Matrizen definiert; erhalten wurde 2×3.",
            fehler.message,
        )
    }

    @Test
    fun `Tensor hoeherer Stufe wird abgewiesen`() {
        val tensor = Tensor(List(3) { 2 }, List(8) { zahl((it + 1).toLong()) })

        val fehler = assertFailsWith<IllegalArgumentException> { spur(tensor) }

        assertEquals("Der Eingang ist keine Matrix; erwartet wird ein Tensor zweiter Stufe.", fehler.message)
    }

    private fun zahl(wert: Long): RationaleZahl = RationaleZahl.von(wert)
}
