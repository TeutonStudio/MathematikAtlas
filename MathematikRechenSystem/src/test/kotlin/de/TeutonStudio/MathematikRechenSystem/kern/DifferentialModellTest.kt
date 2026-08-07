package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DifferentialModellTest {
    private fun quadratischeMethode(): Methode {
        val x = Variable("x")
        return Methode(name="f", parameter=listOf(x), vorschrift=Potenz(x, RationaleZahl.von(2)), zielMenge=ReelleZahlen, werteVorräte=mapOf("x" to ReelleZahlen))
    }
    @Test fun `Argumentindizes beginnen bei eins`() { assertFailsWith<IllegalArgumentException> { DifferentialOperator.Partiell(0) }; val methode=quadratischeMethode(); assertFailsWith<IllegalArgumentException> { partielleAbleitung(methode,2) }; assertEquals("\\partial_{1}f",partielleAbleitung(methode,1).zuLatex()) }
    @Test fun `Methodendifferential verwendet eingeschraenkte Identitaet`() {
        val d = MethodenDifferentialGleichung(quadratischeMethode(), ReelleZahlen)
        assertEquals(ReelleZahlen, d.identitaetsDifferential.werteVorrat)
        assertEquals(DifferentialOperator.Total, d.identitaetsDifferential.operator)
        assertTrue(d.zuLatex().startsWith("df="))
        assertTrue(d.zuLatex().endsWith("\\cdot${d.identitaetsDifferential.zuLatex()}"))
    }
    @Test fun `Termdifferential behaelt Differentialvariable und Quellen ID`() { val x=Variable("x"); val d=bildeDifferentialTerm(Potenz(x,RationaleZahl.von(2)),x,quellenId="argument-f-x"); assertEquals("argument-f-x",d.quellenId); assertEquals("dx",d.differentialVariable.zuLatex()); assertTrue(d.zuLatex().contains("\\cdotdx")) }
    @Test fun `konkrete Ordnungen rendern roemisch und Ordnung null bleibt unveraendert`() { val m=quadratischeMethode(); assertEquals("f^{\\mathrm{I}}",totaleAbleitung(m,DifferentialOrdnung.Konkret(1)).zuLatex()); assertEquals("f^{\\mathrm{II}}",totaleAbleitung(m,DifferentialOrdnung.Konkret(2)).zuLatex()); assertEquals("f^{\\mathrm{IV}}",totaleAbleitung(m,DifferentialOrdnung.Konkret(4)).zuLatex()); val n=differenziereMethodeStrukturiert(m,DifferentialOrdnung.Konkret(0)); assertEquals(m,n.methode); assertEquals(DifferentialUnterstuetzungsStatus.BERECHNET,n.status) }
    @Test fun `symbolische Ordnung bleibt strukturierter Methodenausdruck`() { val a=UnentscheidbareAussage("n\\in\\mathbb N_0","Differentialordnung"); val e=differenziereMethodeStrukturiert(quadratischeMethode(),DifferentialOrdnung.Symbolisch(Variable("n"),setOf(a))); assertEquals(DifferentialUnterstuetzungsStatus.SYMBOLISCH,e.status); assertEquals("f^{(n)}",e.methode.name); assertIs<AbleitungsMethodenAusdruck>(e.methode.vorschrift); assertEquals(setOf(a),e.voraussetzungen) }
    @Test fun `zweite Ableitung eines quadratischen Terms wird konkret berechnet`() { val e=differenziereMethodeStrukturiert(quadratischeMethode(),DifferentialOrdnung.Konkret(2)); assertEquals(DifferentialUnterstuetzungsStatus.BERECHNET,e.status); assertEquals(RationaleZahl.von(2),e.methode.vorschrift); assertEquals(ReelleZahlen,e.methode.zielMenge); assertEquals("D_{2}(f)",e.werteVorrat.zuLatex()) }
    @Test fun `partielle Ableitung verwendet sichtbaren formalen Argumentindex`() { val x=Variable("x"); val y=Variable("y"); val m=Methode("g",listOf(x,y),multiplikation(x,y),ReelleZahlen,mapOf("x" to ReelleZahlen,"y" to ReelleZahlen)); val e=differenziereMethodeStrukturiert(m,DifferentialOrdnung.Konkret(1),DifferentialOperator.Partiell(2)); assertEquals(DifferentialUnterstuetzungsStatus.BERECHNET,e.status); assertEquals(x,e.methode.vorschrift); assertTrue(e.zielRaum.zuLatex().contains("\\mathcal L")) }
    @Test fun `mehrdimensionale totale Ableitung bleibt strukturiert statt partiell geraten zu werden`() { val x=Variable("x"); val y=Variable("y"); val m=Methode("g",listOf(x,y),addition(x,y),ReelleZahlen,mapOf("x" to ReelleZahlen,"y" to ReelleZahlen)); val e=differenziereMethodeStrukturiert(m,DifferentialOrdnung.Konkret(1),DifferentialOperator.Total); assertEquals(DifferentialUnterstuetzungsStatus.NOCH_NICHT_IMPLEMENTIERT,e.status); assertIs<AbleitungsMethodenAusdruck>(e.methode.vorschrift); assertTrue(e.zielRaum.zuLatex().contains("\\mathcal L")); assertTrue(e.voraussetzungen.isNotEmpty()) }
    @Test fun `mehrere oeffentliche Ausgaenge behalten Tupelvertrag`() { val x=Variable("x"); val m=Methode("F",listOf(x),Tupel(listOf(x,Potenz(x,RationaleZahl.von(2)))),Tupelraum(listOf(ReelleZahlen,ReelleZahlen)),mapOf("x" to ReelleZahlen),listOf("linear","quadratisch")); val e=differenziereMethodeStrukturiert(m,DifferentialOrdnung.Symbolisch(Variable("n"))); val v=assertIs<Tupel>(e.methode.vorschrift); assertEquals(2,v.elemente.size); assertIs<Tupelraum>(e.methode.zielMenge); assertEquals(m.ausgabeNamen,e.methode.ausgabeNamen) }
    @Test fun `komplexes Differentialmodell bleibt als noch nicht implementiert getrennt`() { val e=differenziereMethodeStrukturiert(quadratischeMethode(),DifferentialOrdnung.Konkret(1),begriff=DifferentialBegriff.KOMPLEX); assertEquals(DifferentialUnterstuetzungsStatus.NOCH_NICHT_IMPLEMENTIERT,e.status); assertTrue(e.voraussetzungen.isNotEmpty()) }
    @Test fun `totale und partielle Ableitung stimmen nur eindimensional automatisch ueberein`() { val x=Variable("x"); val y=Variable("y"); val e=Methode("f",listOf(x),x,ReelleZahlen,mapOf("x" to ReelleZahlen)); val m=Methode("g",listOf(x,y),x,ReelleZahlen,mapOf("x" to ReelleZahlen,"y" to ReelleZahlen)); assertTrue(eindimensionaleAbleitungenStimmenUeberein(e)); assertFalse(eindimensionaleAbleitungenStimmenUeberein(m)) }
}
