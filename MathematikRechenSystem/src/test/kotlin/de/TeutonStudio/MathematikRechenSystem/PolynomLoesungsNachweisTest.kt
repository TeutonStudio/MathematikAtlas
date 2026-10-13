package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*
import org.junit.Test

class PolynomLoesungsNachweisTest {
    private fun q(n:Long)=RationaleZahl.von(n)
    @Test fun `Sturm beweist Wurzeln Ziffern und Vollstaendigkeit`() {
        val c=listOf(q(-2),q(0),q(1))
        val r=loeseReellesPolynom(c,PolynomNumerikKonfiguration(12))
        assertEquals(LoesungsNachweis.Bewiesen,r.vollstaendigkeit)
        assertEquals(listOf("-1,414213562373","1,414213562373"),r.loesungen.map { it.dezimalstellen!!.single() })
        r.loesungen.forEach { assertTrue(pruefeReellesWurzelIntervall(c,it.komponenten.single())) }
        assertFalse(pruefeReellesWurzelIntervall(c,RationalesIntervall(q(-2),q(2))))
        assertFalse(pruefeReellesWurzelIntervall(c,RationalesIntervall(q(3),q(4))))
    }
    @Test fun `Dezimalgrenzen Mehrfachwurzeln und Nullpolynom`() {
        val r=loeseReellesPolynom(listOf(q(1),q(-2),q(1)),PolynomNumerikKonfiguration(100))
        assertEquals(2,r.loesungen.single().vielfachheit)
        assertEquals("1,"+"0".repeat(100),r.loesungen.single().dezimalstellen!!.single())
        assertEquals(1,loeseReellesPolynom(listOf(q(0))).dimension)
        assertEquals(LoesungsNachweis.Widerlegt,loeseReellesPolynom(listOf(q(1))).existenz)
        assertEquals("-1,23",abgeschnitteneDezimalzahl(RationaleZahl.von(-1239,1000),2))
        assertNull(zertifizierteDezimalzahl(RationalesIntervall(RationaleZahl.von(999,1000),RationaleZahl.von(1001,1000)),2))
    }
    @Test fun `Komplexe Scheiben sind geprueft und nicht nur kleine Residuen`() {
        val c=listOf(KomplexRational(q(1)),KomplexRational(q(0)),KomplexRational(q(1)))
        assertTrue(pruefeKomplexeWurzelScheibe(c,KomplexRational(q(0),q(1)),RationaleZahl.von(1,10)))
        assertFalse(pruefeKomplexeWurzelScheibe(c,KomplexRational(q(0)),q(2)))
        val r=loeseKomplexesPolynom(c,PolynomNumerikKonfiguration(8))
        assertEquals(LoesungsNachweis.Bewiesen,r.vollstaendigkeit)
        assertEquals(setOf(listOf("0,00000000","1,00000000"),listOf("0,00000000","-1,00000000")),r.loesungen.map { it.dezimalstellen }.toSet())
    }
    @Test fun `Matrix linearer Fall hat korrekte Rangkriterien und Familien`() {
        val zero=Matrix(listOf(listOf(q(0),q(0)),listOf(q(0),q(0))))
        val b=Matrix(listOf(listOf(q(1),q(0)),listOf(q(0),q(0))))
        val r=loesePolynomGleichungsSystem(matrixPolynomProblem(zero,b,zero))
        assertEquals(LoesungsNachweis.Bewiesen,r.vollstaendigkeit)
        assertEquals(2,r.dimension)
        assertEquals(2,r.parameter.size)
        val impossible=Matrix(listOf(listOf(q(0),q(1)),listOf(q(0),q(0))))
        assertEquals(LoesungsNachweis.Widerlegt,loesePolynomGleichungsSystem(matrixPolynomProblem(zero,b,impossible)).existenz)
    }
    @Test fun `Vektorgleichung liefert kein falsches isoliertes Matrixergebnis`() {
        val a=SpaltenVektor(listOf(q(1),q(0))); val z=SpaltenVektor(listOf(q(0),q(0)))
        val r=loesePolynomGleichungsSystem(matrixPolynomProblem(a,z,z))
        assertEquals(LoesungsNachweis.Offen,r.vollstaendigkeit)
        assertNull(r.dimension)
    }
    @Test fun `Nichtkommutative Koeffizienten werden rechts eingesetzt`() {
        val a=Matrix(listOf(listOf(q(1),q(2)),listOf(q(0),q(1))))
        val b=Matrix(listOf(listOf(q(0),q(1)),listOf(q(1),q(0))))
        val c=Matrix(listOf(listOf(q(1),q(0)),listOf(q(0),q(1))))
        val p=matrixPolynomProblem(a,b,c)
        val x=Matrix(listOf(listOf(q(1),q(0)),listOf(q(2),q(1))))
        val binding=p.variablen.zip(x.zeilen.flatten()).associate { it.first.name to it.second }
        val expected=(x*x)*a; val xb=x*b
        val values=p.komponenten.map { vereinfache(ersetze(it,binding)) }
        assertEquals(expected.zeilen.flatten().zip(xb.zeilen.flatten()).zip(c.zeilen.flatten()).map { (v,c0) -> vereinfache(addition(v.first,v.second,c0)) },values)
    }
    @Test fun `Budgetende behauptet keine Unloesbarkeit`() {
        val r=loeseReellesPolynom(listOf(q(-2),q(0),q(1)),budget=PolynomRechenBudget(1))
        assertEquals(LoesungsNachweis.Offen,r.existenz)
        assertEquals(LoesungsNachweis.Offen,r.vollstaendigkeit)
    }
    @Test fun `Reelle Matrixquadrate koennen minus I ergeben`() {
        val zero=Matrix(listOf(listOf(q(0),q(0)),listOf(q(0),q(0))))
        val identity=Matrix(listOf(listOf(q(1),q(0)),listOf(q(0),q(1))))
        assertEquals(LoesungsNachweis.Widerlegt,loeseReellesPolynom(listOf(q(1),q(0),q(1))).existenz)
        val r=loesePolynomGleichungsSystem(matrixPolynomProblem(identity,zero,identity))
        assertEquals(LoesungsNachweis.Bewiesen,r.existenz)
        assertEquals(LoesungsNachweis.Offen,r.vollstaendigkeit)
        assertNull(r.dimension)
        assertTrue(r.loesungen.any { it.komponenten.map { v -> v.minimum }==listOf(q(0),q(-1),q(1),q(0)) })
    }
    @Test fun `Eigenwerte komplexer Matrizen verwenden exaktes charakteristisches Polynom`() {
        val a=Matrix(listOf(listOf(KomplexeZahl(q(0),q(1)),q(2)),listOf(q(0),q(3))))
        val c=komplexesCharakteristischesPolynom(a)
        assertEquals(listOf(KomplexRational(q(0),q(3)),KomplexRational(q(-3),q(-1)),KomplexRational(q(1))),c)
        val r=loesePolynomGleichungsSystem(eigenwertAnfrage(a)!!,PolynomNumerikKonfiguration(6))
        assertEquals(LoesungsNachweis.Bewiesen,r.vollstaendigkeit)
        assertEquals(2,r.loesungen.size)
    }
    @Test fun `Familienmitglied wird gegen alle Originalkomponenten geprueft`() {
        val z=Matrix(listOf(listOf(q(0),q(0)),listOf(q(0),q(0))))
        val b=Matrix(listOf(listOf(q(1),q(0)),listOf(q(0),q(0))))
        val p=matrixPolynomProblem(z,b,z)
        val r=loesePolynomGleichungsSystem(p)
        val m=wertePolynomFamilieAus(p,r,r.parameter.associate { it.name to RationaleZahl.von(1,3) },4)
        assertEquals(listOf("0,0000","0,3333","0,0000","0,3333"),m.dezimalstellen)
        assertFailsWith<IllegalArgumentException> { wertePolynomFamilieAus(p,r,emptyMap(),4) }
    }
    @Test fun `Annahmen beschraenken numerische Loesungen`() {
        val x=Variable("x")
        val term=subtraktion(Potenz(x,q(2)),q(2))
        val p=PolynomGleichungsProblem(Gleichheit(term,q(0)),listOf(x),listOf(term),false,
            bedingungen=listOf(Vergleich(x,VergleichsArt.Größer,q(0))))
        val r=loesePolynomGleichungsSystem(p)
        assertEquals(LoesungsNachweis.Bewiesen,r.vollstaendigkeit)
        assertEquals(listOf("1,414213"),r.loesungen.map { it.dezimalstellen!!.single() })
    }
    @Test fun `Komplexe irrationalen Komponenten werden auf 30 Stellen nachgewiesen`() {
        val c=listOf(KomplexRational(q(0),q(-1)),KomplexRational(q(0)),KomplexRational(q(1)))
        val r=loeseKomplexesPolynom(c,PolynomNumerikKonfiguration(30))
        assertEquals(LoesungsNachweis.Bewiesen,r.vollstaendigkeit)
        assertTrue(r.loesungen.all { it.dezimalstellen!=null })
        assertTrue(r.loesungen.all { it.dezimalstellen!!.all { t -> t.removePrefix("-")=="0,707106781186547524400844362104" } })
        assertTrue(r.suchschritte.size in 1..200)
    }

    @Test fun `Komplexe Dezimalgrenze mit irrationaler anderer Komponente`() {
        // (z-1)^2+2: Re(z)=1 exakt, Im(z)=±sqrt(2).
        val c=listOf(KomplexRational(q(3)),KomplexRational(q(-2)),KomplexRational(q(1)))
        val r=loeseKomplexesPolynom(c,PolynomNumerikKonfiguration(12))
        assertEquals(LoesungsNachweis.Bewiesen,r.vollstaendigkeit)
        assertTrue(r.loesungen.all { it.dezimalstellen?.first()=="1,000000000000" })
        assertEquals(setOf("1,414213562373","-1,414213562373"),r.loesungen.map { it.dezimalstellen!![1] }.toSet())
    }

}
