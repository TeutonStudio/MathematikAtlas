package de.TeutonStudio.MathematikKnoten

import kotlin.test.Test
import kotlin.test.assertEquals

class LatexFormelTest {
    @Test
    fun `Display Begrenzung wird entfernt und Matrix bleibt vollständig erhalten`() {
        val matrix = "A=\\begin{pmatrix}a_{11}&a_{12}\\\\a_{21}&a_{22}\\end{pmatrix}"

        assertEquals(matrix, entferneLatexDisplayBegrenzer("\\[$matrix\\]"))
        assertEquals(matrix, entferneLatexDisplayBegrenzer("\$\$$matrix\$\$"))
    }

    @Test
    fun `Fallunterscheidungen bleiben als echte Latex Umgebung erhalten`() {
        val methode = "f:\\begin{cases}\\mathbb{R}\\longrightarrow\\mathbb{R}\\\\x\\mapsto x^2\\end{cases}"

        assertEquals(methode, entferneLatexDisplayBegrenzer(methode))
    }

    @Test
    fun `gewöhnliche Inline Formel bleibt unverändert`() {
        assertEquals("x_1 + x_2", entferneLatexDisplayBegrenzer("x_1 + x_2"))
    }
}
