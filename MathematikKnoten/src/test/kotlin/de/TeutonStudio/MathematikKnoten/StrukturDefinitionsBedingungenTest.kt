package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class StrukturDefinitionsBedingungenTest {
    @Test fun `Inverse und Division erhalten Quellannahmen und Definitionsbedingungen`() {
        val x=Variable("x"); val quelle=Ungleichheit(Variable("a"),RationaleZahl.Null)
        val register=GesamterMathematikAuswerter.erzeugeRegister()
        for (op in listOf("matrix.inverse","matrix.hadamardDivision")) {
            val k=konfiguriereStrukturRechner(StrukturFormelRechnerVorlagen.Matrix.erzeuge(GraphPunkt.Zero),de.TeutonStudio.MathematikRechenSystem.kern.StrukturRechnerKnotenFamilie.MATRIX,op)
            val a=BedingterWert(Matrix(listOf(listOf(if(op.endsWith("inverse")) x else RationaleZahl.Eins))),setOf(quelle))
            val b=BedingterWert(Matrix(listOf(listOf(x))))
            val inputs=k.anschlüsse.filter { it.richtung==AnschlussRichtung.Eingang }.sortedBy { it.reihenfolge }.mapIndexed { i,p -> p.name to if(i==0) a else b }.toMap()
            val result=register.finde(k.art)!!.auswerten(KnotenAuswertungsKontext(k,inputs,RechenKontext()))
            assertNull(result.fehler,result.fehler)
            val ausgabe=result.ausgaben.values.single()
            assertTrue(quelle in ausgabe.annahmen)
            assertTrue(ausgabe.annahmen.any { it != quelle && "x" in it.enthalteneMethodenParameter().map { p -> p.name } },ausgabe.annahmen.toString())
        }
    }
}
