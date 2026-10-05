package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class MatrixZerlegenTest {
    private val register = GesamterMathematikAuswerter.erzeugeRegister()

    @Test
    fun `Zeilen und Spalten werden orientiert und geordnet ausgegeben`() {
        val basis = StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero)
        val matrix = Matrix(listOf(
            listOf(RationaleZahl.von(1), RationaleZahl.von(2)),
            listOf(RationaleZahl.von(3), RationaleZahl.von(4)),
        ))
        MatrixZerlegeRichtung.entries.forEach { richtung ->
            val konfiguriert = konfiguriereStrukturRechner(
                basis,
                StrukturRechnerKnotenFamilie.MATRIX,
                MatrixRechnerOperator.ZERLEGEN.stabileId,
            ).copy(parameter = basis.parameter + mapOf(
                RECHNER_OPERATOR_PARAMETER to MatrixRechnerOperator.ZERLEGEN.stabileId,
                MATRIX_ZERLEGEN_RICHTUNG to richtung.name,
            ))
            val ergebnis = register.finde(MatrixRechner.KNOTEN_ART)!!.auswerten(
                KnotenAuswertungsKontext(
                    konfiguriert,
                    mapOf("matrix" to BedingterWert(matrix)),
                    RechenKontext(),
                ),
            )
            assertEquals(2, ergebnis.ausgaben.size)
            if (richtung == MatrixZerlegeRichtung.ZEILEN) {
                assertIs<ZeilenVektor>(ergebnis.ausgaben.getValue("zeile1").objekt)
            } else {
                assertIs<SpaltenVektor>(ergebnis.ausgaben.getValue("spalte1").objekt)
            }
        }
    }

    @Test
    fun `symbolische Zeilenzahl liefert Zeilenmethode`() {
        val basis = StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero)
        val knoten = konfiguriereStrukturRechner(
            basis,
            StrukturRechnerKnotenFamilie.MATRIX,
            MatrixRechnerOperator.ZERLEGEN.stabileId,
        )
        val matrix = TypisiertesElement(
            "A",
            "mathematik.matrix",
            strukturForm = listOf(Variable("m"), RationaleZahl.von(3)),
        )
        val ergebnis = register.finde(MatrixRechner.KNOTEN_ART)!!.auswerten(
            KnotenAuswertungsKontext(knoten, mapOf("matrix" to BedingterWert(matrix)), RechenKontext()),
        )
        val methode = assertIs<MathematischeMethode>(ergebnis.ausgaben.getValue("methode").objekt)
        assertEquals(EndlicheIndexMenge(Variable("m")), methode.werteVorräte.getValue("i"))
        val ziel = assertIs<StrukturErgebnisMenge>(methode.zielMenge)
        assertEquals("mathematik.vektor.zeile", ziel.anschlussArt)
        assertEquals(listOf(RationaleZahl.von(3)), ziel.form)
    }
}
