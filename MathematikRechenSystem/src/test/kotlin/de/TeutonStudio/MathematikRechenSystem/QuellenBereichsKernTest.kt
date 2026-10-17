package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class QuellenBereichsKernTest {
    @Test fun `Null ist nicht positiv natuerlich und N0 bleibt bei Inferenz erhalten`() {
        assertEquals(Wahrheitswert.Lüge,ElementBeziehung(RationaleZahl.Null,NatürlicheZahlen).entscheide().wahrheitswert)
        assertEquals(NichtnegativeGanzeZahlenSemantik.menge,inferiereZahlenWertevorrat(RationaleZahl.Null))
        assertEquals(NatürlicheZahlen,inferiereZahlenWertevorrat(RationaleZahl.Eins))
        assertEquals(GanzeZahlen,inferiereZahlenWertevorrat(RationaleZahl.von(-1)))
        val division=StrukturierteDivision(Variable("a"),RationaleZahl.Eins,DivisionsSeite.RECHTS,struktur=ZahlbereichsIds.NATUERLICH_MIT_NULL)
        assertEquals(NichtnegativeGanzeZahlenSemantik.menge,inferiereZahlenWertevorrat(division))
        val länge=StrukturAchsenLaenge(Tupel(emptyList()),0)
        assertEquals(RationaleZahl.Null,strukturAchsenLaenge(Tupel(emptyList()),0))
        assertEquals(NichtnegativeGanzeZahlenSemantik.menge,inferiereZahlenWertevorrat(länge))
        val parameter=AllgemeinerParameter("a",bindungsId="quelle")
        assertEquals(ReelleZahlen,inferiereZielmenge(parameter,mapOf("a" to KomplexeZahlen,"quelle" to ReelleZahlen)))
    }
    @Test fun `alter falscher N Zielbereich bleibt im Wire erhalten und wird bei Anwendung abgewiesen`() {
        val alt=MathematischeMethode("Null",emptyList(),RationaleZahl.Null,NatürlicheZahlen)
        val text=ÜbersetzungsCodec.kodieren(ÜbertragenerMathematischerWert(alt))
        val geladen=assertIs<MathematischeMethode>(ÜbersetzungsCodec.dekodiereWert(text).wert)
        assertEquals(alt,geladen)
        assertTrue(assertFailsWith<IllegalArgumentException> {geladen.wendeMitIdentitätenAn(emptyMap())}.message!!.contains("Zielmenge"))
        assertFails {geladen.wendeMathematischAlsTupelAn(Tupel(emptyList()))}
        assertEquals(NatürlicheZahlen,geladen.zielMenge)
        assertEquals(Tupel(listOf(RationaleZahl.Null)),alt.copy(zielMenge=NichtnegativeGanzeZahlenSemantik.menge).wendeMitIdentitätenAn(emptyMap()).ergebnis)
        assertEquals(Tupel(listOf(RationaleZahl.Eins)),alt.copy(vorschrift=RationaleZahl.Eins).wendeMitIdentitätenAn(emptyMap()).ergebnis)
    }
    @Test fun `abhaengige numerische Zielmenge wird nach KomponentenID substituiert`() {
        val x=Variable("x",bindungsId="x-id")
        val ziel=ReellesIntervall(x,false,RationaleZahl.von(10),false)
        val f=MathematischeMethode("f",listOf(x),RationaleZahl.von(3),ziel,mapOf("x" to ReelleZahlen))
        assertTrue(f.wendeMitIdentitätenAn(mapOf("x-id" to RationaleZahl.Eins)).bedingungen.isEmpty())
        assertFails {f.wendeMitIdentitätenAn(mapOf("x-id" to RationaleZahl.von(5)))}
    }
    @Test fun `offene numerische Ergebnisbedingung bleibt erhalten ohne Mengenfamilien umzudeuten`() {
        val a=Variable("a",bindungsId="a")
        val f=MathematischeMethode("offen",emptyList(),a,NatürlicheZahlen)
        assertTrue(ElementBeziehung(a,NatürlicheZahlen) in f.wendeMitIdentitätenAn(emptyMap()).bedingungen)
        val menge=EndlicheMenge(setOf(RationaleZahl.Eins))
        val familie=MathematischeMethode("A",emptyList(),menge,ReelleZahlen)
        assertEquals(Tupel(listOf(menge)),familie.wendeMitIdentitätenAn(emptyMap()).ergebnis)
        assertTrue(familie.wendeMitIdentitätenAn(emptyMap()).bedingungen.isEmpty())
        // Auch historische direkte Methoden ohne Argumentbereich bleiben verwendbar.
        val historisch=MathematischeMethode("f",listOf(Variable("x")),RationaleZahl.Eins,NatürlicheZahlen)
        assertEquals(Tupel(listOf(RationaleZahl.Eins)),historisch.wendeMathematischAlsTupelAn(Tupel(listOf(RationaleZahl.Null))))
    }
    @Test fun `offene MathematikCapability prueft denselben numerischen Vertrag genau nach einer Ausfuehrung`() {
        var aufrufe=0
        val m=object:MathematischAuswertbareMethode {
            override val name="cap"
            override val mathematischeSignatur=MathematischeMethodenSignatur(emptyList(),listOf(MathematischeErgebnisKomponente("r","r",0,NatürlicheZahlen)))
            override fun wendeMathematischAn(argumente:Map<String,MathematischesObjekt>):MathematischesObjekt {aufrufe++;return RationaleZahl.Null}
            override fun zuLatex()="cap"
        }
        assertFailsWith<IllegalArgumentException> {m.wendeMitIdentitätenAn(emptyMap())}
        assertEquals(1,aufrufe)
    }
    @Test fun `lineare Bereichsmitgliedschaft trennt konkrete Formen und offene Strukturparameter`() {
        val vr=Vektorraum(VektorOrientierung.Spalte,2,ReelleZahlen)
        val mr=Matrizenraum(1,2,ReelleZahlen)
        val eins=RationaleZahl.Eins
        val spalte=SpaltenVektor(listOf(eins,eins))
        val matrix=Matrix(listOf(listOf(eins,eins)))
        assertEquals(Wahrheitswert.Wahr,ElementBeziehung(spalte,vr).entscheide().wahrheitswert)
        assertEquals(Wahrheitswert.Wahr,ElementBeziehung(matrix,mr).entscheide().wahrheitswert)
        for (falsch in listOf(eins,Variable("x"),spalte.transponiert(),SpaltenVektor(listOf(eins)),matrix,Tupel(listOf(eins,eins)))) {
            assertEquals(Wahrheitswert.Lüge,ElementBeziehung(falsch,vr).entscheide().wahrheitswert)
        }
        for (falsch in listOf(eins,spalte,Matrix(listOf(listOf(eins))),Tensor(listOf(1,2),listOf(eins,eins)))) {
            assertEquals(Wahrheitswert.Lüge,ElementBeziehung(falsch,mr).entscheide().wahrheitswert)
        }
        assertEquals(Wahrheitswert.Lüge,ElementBeziehung(SpaltenVektor(listOf(eins,RationaleZahl.von(-1))),vr.copy(skalarMenge=NatürlicheZahlen)).entscheide().wahrheitswert)
        assertNull(ElementBeziehung(SpaltenVektor(listOf(eins,Variable("x"))),vr).entscheide().wahrheitswert)
        assertNull(ElementBeziehung(Matrix(listOf(listOf(eins,Variable("x")))),mr).entscheide().wahrheitswert)
        assertNull(ElementBeziehung(AllgemeinerParameter("a"),vr).entscheide().wahrheitswert)
        assertNull(ElementBeziehung(AllgemeinerParameter("a"),mr).entscheide().wahrheitswert)
    }

}
