package de.TeutonStudio.MathematikRechenSystem.kern

import de.TeutonStudio.TypSystem.TypAusdruck
import kotlin.test.*

class MatrizenTensorraumTest {
    private fun z(n: Long) = RationaleZahl.von(n)
    private val raum = Tensorraum(ReelleZahlen, listOf(z(1), z(2)), matrixForm = true)

    @Test fun `Matrixform bleibt von allgemeinen Tensoren zweiter Stufe unterscheidbar`() {
        assertEquals(MathematischeTypen.Matrix, assertIs<TypAusdruck.Parameterisiert>(raum.elementTypAusdruck()).konstruktor)
        assertEquals(MathematischeTypen.Tensor, assertIs<TypAusdruck.Parameterisiert>(raum.copy(matrixForm = false).elementTypAusdruck()).konstruktor)
        assertFailsWith<IllegalArgumentException> { Tensorraum(ReelleZahlen, listOf(z(2)), matrixForm = true) }
        assertFailsWith<IllegalArgumentException> { raum.copy(dimensionen = listOf(z(0), z(1))) }
    }

    @Test fun `Mitgliedschaft und Topologie bleiben fuer migrierte Matrizenraeume gleich`() {
        val matrix = Matrix(listOf(listOf(z(1), z(2))))
        assertEquals(Wahrheitswert.Wahr, ElementBeziehung(matrix, raum).entscheide().wahrheitswert)
        assertEquals(Wahrheitswert.Lüge, ElementBeziehung(Matrix(listOf(listOf(z(1)))), raum).entscheide().wahrheitswert)
        assertEquals(Wahrheitswert.Lüge, ElementBeziehung(Matrix(listOf(listOf(KomplexeZahl(z(0),z(1)), z(2)))), raum).entscheide().wahrheitswert)
        assertEquals(Wahrheitswert.Wahr, Gleichheit(raum, Matrizenraum(1, 2, ReelleZahlen)).entscheide().wahrheitswert)
        assertNotNull(StandardTopologieRegister.fuer(raum))
        assertNull(StandardTopologieRegister.fuer(raum.copy(elementMenge = GanzeZahlen)))
    }

    @Test fun `symbolische Achsen behalten ihre Bindung bei Substitution und Wertcodec`() {
        val n = Variable("n", bindungsId = "n-id")
        val symbolisch = raum.copy(dimensionen = listOf(n, z(2)))
        val geladen = assertIs<Tensorraum>(MathematischerWertCodec.dekodieren(MathematischerWertCodec.kodieren(symbolisch)))
        assertEquals(symbolisch, geladen)
        assertEquals(raum, ersetze(geladen, mapOf("n-id" to z(1))))
        assertIs<FehlendeObermenge>(bereichFürTyp(symbolisch.elementTypAusdruck()))
        assertEquals(Matrizenraum(1, 2, ReelleZahlen), bereichFürTyp(raum.elementTypAusdruck()))
        val historisch = Matrizenraum(2, 3, KomplexeZahlen)
        assertEquals(historisch, MathematischerWertCodec.dekodieren(MathematischerWertCodec.kodieren(historisch)))
        val tensor = raum.copy(matrixForm = false)
        assertEquals(tensor, MathematischerWertCodec.dekodieren(MathematischerWertCodec.kodieren(tensor)))
    }
}
