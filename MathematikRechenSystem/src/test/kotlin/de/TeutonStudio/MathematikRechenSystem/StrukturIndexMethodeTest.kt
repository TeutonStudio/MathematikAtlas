package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class StrukturIndexMethodeTest {
    private fun z(n: Long) = RationaleZahl.von(n)
    @Test fun `Symbolische Komponenten erhalten den Skalarkoerper ihrer Matrixquelle`() {
        val x=TypisiertesElement("X","mathematik.matrix",strukturForm=listOf(z(2),z(2)))
        for(bereich in listOf(RationaleZahlen,ReelleZahlen,KomplexeZahlen,FundamentalerZahlbereich.QUATERNION.alsMenge())) {
            val vorrat=mapOf("X" to Matrizenraum(2,2,bereich))
            assertEquals(bereich,inferiereZahlenWertevorrat(SymbolischeZahlKomponente(x,z(1),listOf(z(2))),vorrat))
            assertEquals(Tupelraum(listOf(bereich)),strukturIndexMethode(x,vorrat).zielMenge)
        }
    }
    @Test fun `Indexmethoden bewahren Tupelverschachtelung und symbolische Komponenten`() {
        val verschachtelt = Tupel(listOf(z(2), z(3)))
        val quelle = Tupel(listOf(Variable("x"), verschachtelt))
        val m = strukturIndexMethode(quelle, mapOf("x" to ReelleZahlen))
        assertEquals(Variable("x"), m.wendeAn(listOf(z(1))))
        assertEquals(verschachtelt, m.wendeAn(listOf(z(2))))
        assertEquals(1, m.mathematischeSignatur.ergebnisse.size)
    }
    @Test fun `Vektor Matrix und Tensor verwenden einen Index je Achse`() {
        val v = SpaltenVektor(listOf(z(2), z(3)))
        assertEquals(z(3), strukturIndexMethode(v).wendeAn(listOf(z(2))))
        val matrix = Matrix(listOf(listOf(z(1), z(2)), listOf(z(3), z(4))))
        assertEquals(z(3), strukturIndexMethode(matrix).wendeAn(listOf(z(2), z(1))))
        val tensor = Tensor(listOf(2, 1, 2), listOf(z(1), z(2), z(3), z(4)))
        assertEquals(z(4), strukturIndexMethode(tensor).wendeAn(listOf(z(2), z(1), z(2))))
    }
    @Test fun `Indexgrenzen Argumentanzahl und Typen werden validiert`() {
        val m = strukturIndexMethode(Tupel(listOf(z(2))))
        listOf(emptyList(), listOf(z(1), z(1)), listOf(z(0)), listOf(z(-1)), listOf(z(2)), listOf(Variable("i")), listOf(Tupel(emptyList()))).forEach {
            assertFails { m.wendeAn(it) }
        }
        assertFails { strukturIndexMethode(z(2)) }
    }
    @Test fun `Substitution wertet mehrdimensionale Zugriffe aus`() {
        val matrix = Matrix(listOf(listOf(z(1), z(2))))
        val zugriff = IndexKomponente(matrix, listOf(Variable("i"), Variable("j")))
        assertEquals(z(2), ersetze(zugriff, mapOf("i" to z(1), "j" to z(2))))
    }
    @Test fun `Matrixarithmetik promoviert natürliche Quellen in den Ergebnisbereich`() {
        val n=FundamentalerZahlbereich.NATUERLICH_POSITIV
        fun operand(x:Long)=MatrixOperand("a",Matrix(listOf(listOf(z(x)))),n)
        val differenz=assertIs<MatrixRechnerErgebnis.MatrixWert>(MatrixRechner.erzeuge(MatrixRechnerAnfrage(MatrixRechnerOperator.SUBTRAKTION,listOf(operand(2),operand(5)))))
        assertEquals(FundamentalerZahlbereich.GANZ,differenz.zahlbereich)
        assertEquals(z(-3),vereinfache(differenz.wert.zeilen[0][0]))
        val skaliert=assertIs<MatrixRechnerErgebnis.MatrixWert>(MatrixRechner.erzeuge(MatrixRechnerAnfrage(MatrixRechnerOperator.SKALARMULTIPLIKATION,listOf(operand(1)),listOf(Division(z(1),z(2))))))
        assertEquals(FundamentalerZahlbereich.RATIONAL,skaliert.zahlbereich)
        val det=assertIs<MatrixRechnerErgebnis.ZahlWert>(MatrixRechner.erzeuge(MatrixRechnerAnfrage(MatrixRechnerOperator.DETERMINANTE,listOf(MatrixOperand("a",Matrix(listOf(listOf(z(1),z(2)),listOf(z(3),z(4)))),n)))))
        assertEquals(FundamentalerZahlbereich.GANZ,det.zahlbereich)
        assertEquals(z(-2),vereinfache(det.wert))
        val invers=assertIs<MatrixRechnerErgebnis.MatrixWert>(MatrixRechner.erzeuge(MatrixRechnerAnfrage(MatrixRechnerOperator.INVERSE,listOf(operand(2)))))
        assertEquals(FundamentalerZahlbereich.RATIONAL,invers.zahlbereich)
        val potenz=assertIs<MatrixRechnerErgebnis.MatrixWert>(MatrixRechner.erzeuge(MatrixRechnerAnfrage(MatrixRechnerOperator.POTENZ,listOf(operand(2)),listOf(z(-1)))))
        assertEquals(FundamentalerZahlbereich.RATIONAL,potenz.zahlbereich)
    }

    @Test fun `Gebundene Matrixindizes verwenden Formvorbild ohne dessen Werte einzusetzen`() {
        val akkumulator=TypisiertesElement("acc","mathematik.matrix")
        val neutral=Matrix(listOf(listOf(z(1),z(0)),listOf(z(0),z(1))))
        val methode=strukturIndexMethode(akkumulator,formVorbild=neutral)
        val term=methode.wendeAn(listOf(z(1),z(2)))
        assertTrue(term.enthalteneMethodenParameter().any { it.name=="acc" })
        val aktuell=Matrix(listOf(listOf(z(2),z(3)),listOf(z(4),z(5))))
        assertEquals(z(3),ersetze(term,mapOf("acc" to aktuell)))
    }

    @Test fun `Leeres Tupel besitzt leeren Indexbereich und neutrale Reduktionen`() {
        val methode=strukturIndexMethode(Tupel(emptyList()))
        assertEquals(EndlicheIndexMenge(z(0)),methode.werteVorräte.values.single())
        assertEquals(Tupel(emptyList()),strukturAusIndexMethode(methode,"tupel"))
        assertFails { methode.wendeAn(listOf(z(1))) }
        val numerisch=methode.copy(vorschrift=z(7),zielMenge=Tupelraum(listOf(RationaleZahlen)))
        assertEquals(z(0),summiereIndexParameter(numerisch,numerisch.parameter.single().name))
        assertEquals(z(1),summiereIndexParameter(numerisch,numerisch.parameter.single().name,"produkt"))
    }

}
