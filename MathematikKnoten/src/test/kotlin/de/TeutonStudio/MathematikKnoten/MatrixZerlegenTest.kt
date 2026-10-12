package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
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

    @Test fun `Uebersetzer zerlegt beide Richtungen als Vektoren oder Tupel mit stabiler Migration`() {
        val alt = konfiguriereStrukturRechner(StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero),
            StrukturRechnerKnotenFamilie.MATRIX, "matrix.zerlegen")
        val karte = KartenDaten(name="Alt",knoten=listOf(alt))
        val neu = karte.migriereMatrixZerlegenZumÜbersetzer().knoten.single()
        assertEquals(ÜBERSETZER_ART,neu.art)
        assertEquals(alt.anschlüsse.map { it.id },neu.anschlüsse.map { it.id })
        assertEquals(karte.migriereMatrixZerlegenZumÜbersetzer(),karte.migriereMatrixZerlegenZumÜbersetzer().migriereMatrixZerlegenZumÜbersetzer())
        assertTrue(RechnerOperatorRegistry.alle.none { it.operatorId == "matrix.zerlegen" })
        val matrix = Matrix(listOf(listOf(RationaleZahl.von(1),RationaleZahl.von(2),RationaleZahl.von(3)),
            listOf(RationaleZahl.von(4),RationaleZahl.von(5),RationaleZahl.von(6))))
        for(richtung in MatrixZerlegeRichtung.entries) for(tupel in listOf(false,true)) {
            val k = neu.copy(parameter=neu.parameter+mapOf(MATRIX_ZERLEGEN_RICHTUNG to richtung.name,"matrixZerlegen.ausgabe" to if(tupel) "tupel" else "vektor"))
            val e=register.finde(k.art)!!.auswerten(KnotenAuswertungsKontext(k,mapOf("wert" to BedingterWert(matrix)),RechenKontext()))
            assertNull(e.fehler)
            assertEquals(if(richtung==MatrixZerlegeRichtung.ZEILEN) 2 else 3,e.ausgaben.size)
            val erste=e.ausgaben.values.first().objekt
            if(tupel) assertIs<Tupel>(erste) else if(richtung==MatrixZerlegeRichtung.ZEILEN) assertIs<ZeilenVektor>(erste) else assertIs<SpaltenVektor>(erste)
        }
        val methode=matrixZerlegeMethode(matrix,MatrixZerlegeRichtung.SPALTEN,true)
        assertEquals(Tupel(listOf(RationaleZahl.von(2),RationaleZahl.von(5))),methode.wendeAn(listOf(RationaleZahl.von(2))))
    }
    @Test fun `Zerlegeindex bindet keine freie Matrixkomponente`() {
        for(richtung in MatrixZerlegeRichtung.entries) for(tupel in listOf(false,true)) {
            val name=if(richtung==MatrixZerlegeRichtung.ZEILEN) "i" else "j"
            val symbol=Variable(name)
            val methode=matrixZerlegeMethode(Matrix(listOf(listOf(symbol))),richtung,tupel)
            val ergebnis=methode.wendeAn(listOf(RationaleZahl.Eins))
            assertEquals(if(tupel) Tupel(listOf(symbol)) else if(richtung==MatrixZerlegeRichtung.ZEILEN) ZeilenVektor(listOf(symbol)) else SpaltenVektor(listOf(symbol)),ergebnis)
        }
    }

}
