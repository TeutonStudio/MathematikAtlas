package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class StrukturIndexierungTest {
    @Test
    fun `endliche symbolische Indexmenge akzeptiert Variable aber keine Null`() {
        assertEquals("\\left\\{1,\\ldots,n\\right\\}", EndlicheIndexMenge(Variable("n")).zuLatex())
        assertFailsWith<IllegalArgumentException> { EndlicheIndexMenge(RationaleZahl.Null) }
        assertFailsWith<IllegalArgumentException> { EndlicheIndexMenge(RationaleZahl.von(-1)) }
        assertFailsWith<IllegalArgumentException> { EndlicheIndexMenge(RationaleZahl.von(3, 2)) }
    }

    @Test
    fun `symbolische Vektormethode materialisiert konkrete Komponente und prueft Grenzen`() {
        val x = TypisiertesElement(
            "x",
            "mathematik.vektor.spalte",
            strukturForm = listOf(RationaleZahl.von(3)),
        )
        val i = Variable("i")
        val methode = MathematischeMethode(
            name = "komponenten",
            parameter = listOf(i),
            vorschrift = strukturKomponente(x, i, numerisch = true),
            zielMenge = KomplexeZahlen,
            werteVorräte = mapOf("i" to EndlicheIndexMenge(strukturAchsenLaenge(x, 0))),
        )
        assertEquals(SymbolischeZahlKomponente(x, RationaleZahl.von(2)), methode.wendeAn(listOf(RationaleZahl.von(2))))
        assertFailsWith<IllegalArgumentException> { methode.wendeAn(listOf(RationaleZahl.Null)) }
        assertFailsWith<IllegalArgumentException> { methode.wendeAn(listOf(RationaleZahl.von(4))) }

        val konkret = SpaltenVektor(listOf(RationaleZahl.von(7), RationaleZahl.von(9)))
        assertEquals(RationaleZahl.von(9), strukturKomponente(konkret, RationaleZahl.von(2), numerisch = true))
    }

    @Test
    fun `Tensorordnung drei wird entlang jeder Achse zur Matrix`() {
        val tensor = Tensor(listOf(2, 2, 2), (1L..8L).map(RationaleZahl::von))
        repeat(3) { achse ->
            val schnitt = strukturSchnitt(
                tensor,
                RationaleZahl.Eins,
                StrukturZugriffsArt.Schnitt,
                achse,
                "mathematik.matrix",
            )
            assertIs<Matrix>(schnitt)
            assertEquals(2, schnitt.zeilenAnzahl)
            assertEquals(2, schnitt.spaltenAnzahl)
        }
    }

    @Test
    fun `Matrixzeilen und spalten behalten ihre Orientierung`() {
        val matrix = Matrix(listOf(
            listOf(RationaleZahl.von(1), RationaleZahl.von(2)),
            listOf(RationaleZahl.von(3), RationaleZahl.von(4)),
        ))
        assertEquals(
            ZeilenVektor(listOf(RationaleZahl.von(3), RationaleZahl.von(4))),
            strukturSchnitt(matrix, RationaleZahl.von(2), StrukturZugriffsArt.Zeile, 0, "mathematik.vektor.zeile"),
        )
        assertEquals(
            SpaltenVektor(listOf(RationaleZahl.von(2), RationaleZahl.von(4))),
            strukturSchnitt(matrix, RationaleZahl.von(2), StrukturZugriffsArt.Spalte, 1, "mathematik.vektor.spalte"),
        )
    }
}
