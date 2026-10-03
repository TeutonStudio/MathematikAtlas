package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class EndlicheMengenSicherheitTest {
    private val eins = RationaleZahl.Eins
    private val x = Variable("x")
    private val symbolisch = EndlicheMenge(setOf(x))
    private val konkret = EndlicheMenge(setOf(eins))

    @Test fun `symbolische Ungleichheit beweist keine Nichtmitgliedschaft`() {
        assertNull(ElementBeziehung(eins, symbolisch).entscheide().wahrheitswert)
        assertEquals(Wahrheitswert.Wahr, ElementBeziehung(x, symbolisch).entscheide().wahrheitswert)
        val gebunden = ersetze(symbolisch, mapOf("x" to eins)) as MengenAusdruck
        assertEquals(Wahrheitswert.Wahr, ElementBeziehung(eins, gebunden).entscheide().wahrheitswert)
    }
    @Test fun `unbekannte Treffer bleiben in Mengenoperationen symbolisch`() {
        assertIs<Schnitt>(schneide(listOf(symbolisch, konkret)))
        assertIs<MengenDifferenz>(mengenDifferenz(symbolisch, konkret))
        assertIs<SymmetrischeDifferenz>(symmetrischeDifferenz(symbolisch, konkret))
        assertNull(TeilmengenBeziehung(symbolisch, konkret).entscheide().wahrheitswert)
        assertNull(EchteTeilmengeBeziehung(symbolisch, konkret).entscheide().wahrheitswert)
        assertNull(Disjunktheit(symbolisch, konkret).entscheide().wahrheitswert)
    }
    @Test fun `ungeklaerte Verschiedenheit ergibt keine erfundene Maechtigkeit`() {
        assertFailsWith<IllegalStateException> { mächtigkeit(EndlicheMenge(setOf(x, eins))) }
        assertEquals(EndlicheMächtigkeit(RationaleZahl.von(2)), mächtigkeit(EndlicheMenge(setOf(eins, RationaleZahl.von(2)))))
    }
    @Test fun `konkrete Tupel bleiben entscheidbar ohne sie mit Skalaren zu identifizieren`() {
        val a = Tupel(listOf(eins))
        val b = Tupel(listOf(RationaleZahl.von(2)))
        assertEquals(Wahrheitswert.Lüge, ElementBeziehung(a, EndlicheMenge(setOf(b))).entscheide().wahrheitswert)
        assertEquals(Wahrheitswert.Lüge, ElementBeziehung(eins, EndlicheMenge(setOf(a))).entscheide().wahrheitswert)
    }
    @Test fun `konkrete Wahrheitswerte behalten ihre entscheidbare Maechtigkeit`() {
        val wahr = WahrheitsKonstante(true); val falsch = WahrheitsKonstante(false)
        assertEquals(Wahrheitswert.Lüge, ElementBeziehung(falsch, EndlicheMenge(setOf(wahr))).entscheide().wahrheitswert)
        assertEquals(EndlicheMächtigkeit(RationaleZahl.von(2)), mächtigkeit(EndlicheMenge(setOf(wahr, falsch))))
    }
    @Test fun `rohe Mengenalgebra kombiniert unbekannte Mitgliedschaft konservativ`() {
        assertEquals(Wahrheitswert.Wahr, ElementBeziehung(eins, Vereinigung(listOf(symbolisch, konkret))).entscheide().wahrheitswert)
        assertEquals(Wahrheitswert.Lüge, ElementBeziehung(eins, Schnitt(listOf(symbolisch, LeereMenge))).entscheide().wahrheitswert)
        assertNull(ElementBeziehung(eins, MengenDifferenz(konkret, symbolisch)).entscheide().wahrheitswert)
        assertEquals(Wahrheitswert.Wahr, ElementBeziehung(eins, Schnitt(emptyList(), ReelleZahlen)).entscheide().wahrheitswert)
    }
}
