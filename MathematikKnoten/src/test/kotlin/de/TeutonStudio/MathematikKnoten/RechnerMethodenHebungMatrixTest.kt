package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class RechnerMethodenHebungMatrixTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()

    @Test
    fun `Matrixaddition aus Methode und Konstante liefert Matrixmethode`() {
        val x = Variable("x")
        val matrixMenge = BenannteMenge("R11", "\\mathbb R^{1\\times 1}")
        val f = Methode(
            name = "A",
            parameter = listOf(x),
            vorschrift = Matrix(listOf(listOf(x))),
            zielMenge = matrixMenge,
            werteVorräte = mapOf("x" to ReelleZahlen),
        )
        val konstant = Matrix(listOf(listOf(RationaleZahl.Eins)))
        val knoten = normalisiereRechnerMethodenAnschluesse(
            konfiguriereStrukturRechner(
                StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero),
                StrukturRechnerKnotenFamilie.MATRIX,
                "matrix.addition",
            ),
        )

        val ergebnis = register.finde(MatrixRechner.KNOTEN_ART)!!.auswerten(
            KnotenAuswertungsKontext(
                knoten = knoten,
                eingänge = mapOf(
                    "a" to BedingterWert(f),
                    "b" to BedingterWert(konstant),
                ),
                rechenKontext = RechenKontext(),
            ),
        )

        val methode = assertIs<Methode>(ergebnis.ausgaben.getValue("wert").objekt)
        assertEquals(listOf("x"), methode.parameter.map { it.name })
        val matrix = assertIs<Matrix>(methode.vorschrift)
        assertEquals(1, matrix.zeilen.size)
        assertEquals(1, matrix.zeilen.single().size)
        assertTrue(matrix.zuLatex().contains("x"))
        assertTrue(matrix.zuLatex().contains("1"))
    }
}
