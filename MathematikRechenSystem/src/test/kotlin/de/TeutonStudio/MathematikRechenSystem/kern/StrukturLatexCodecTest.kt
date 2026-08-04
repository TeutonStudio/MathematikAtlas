package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class StrukturLatexCodecTest {
    private val eins = RationaleZahl.von(1)
    private val zwei = RationaleZahl.von(2)
    private val drei = RationaleZahl.von(3)

    @Test
    fun `Matrix und beide Vektororientierungen bleiben kanonische pmatrix`() {
        assertEquals(
            "\\begin{pmatrix}1 & 2 \\\\ 3 & 1\\end{pmatrix}",
            Matrix(listOf(listOf(eins, zwei), listOf(drei, eins))).zuStrukturLatex(),
        )
        assertEquals(
            "\\begin{pmatrix}1 & 2 & 3\\end{pmatrix}",
            ZeilenVektor(listOf(eins, zwei, drei)).zuStrukturLatex(),
        )
        assertEquals(
            "\\begin{pmatrix}1 \\\\ 2 \\\\ 3\\end{pmatrix}",
            SpaltenVektor(listOf(eins, zwei, drei)).zuStrukturLatex(),
        )
    }

    @Test
    fun `Tupel wird einzeilig gerendert ohne seinen Typ zu verlieren`() {
        val tupel: MathematischesObjekt = Tupel(listOf(eins, zwei, drei))

        assertEquals("\\begin{pmatrix}1 & 2 & 3\\end{pmatrix}", tupel.zuStrukturLatex())
        assertIs<Tupel>(tupel)
    }

    @Test
    fun `verschachtelte Tupel bleiben einzelne Komponenten`() {
        val tupel = Tupel(listOf(eins, Tupel(listOf(zwei, drei)), Variable("x")))

        assertEquals(
            "\\begin{pmatrix}1 & \\begin{pmatrix}2 & 3\\end{pmatrix} & x\\end{pmatrix}",
            tupel.zuStrukturLatex(),
        )
    }
}
