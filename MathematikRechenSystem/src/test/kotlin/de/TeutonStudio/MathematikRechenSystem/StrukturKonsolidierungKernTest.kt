package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class StrukturKonsolidierungKernTest {
    private fun z(n:Long)=RationaleZahl.von(n)
    private fun quelle(v:OrientierterVektor,b:FundamentalerZahlbereich=FundamentalerZahlbereich.REELL)=VektorQuelle.Vektor(v,
        KartesischerKoordinatenVertrag(v.werte.size,b,"standard","kartesisch",true))
    @Test fun `reelle Norm ist gemeinsam exakt und Quellenbereiche folgen IDs`() {
        val v=SpaltenVektor(listOf(z(3),z(4)))
        assertEquals(z(5),vereinfache(v.radius()))
        val r=assertIs<VektorRechnerErgebnis.ZahlWert>(VektorRechner.erzeuge(VektorRechnerAnfrage(VektorRechnerOperator.NORM,listOf(quelle(v)))))
        assertEquals(v.radius(),r.wert)
        val x=Variable("x",bindungsId="reell")
        val y=Variable("x",bindungsId="komplex")
        val bereiche=mapOf("reell" to ReelleZahlen,"komplex" to KomplexeZahlen,"x" to KomplexeZahlen)
        assertEquals(ReelleZahlen,inferiereZahlenWertevorrat(x,bereiche))
        assertTrue(x in reelleEuklidischeNorm(listOf(x),bereiche).enthalteneVariablen())
        assertFails {reelleEuklidischeNorm(listOf(x,y),bereiche)}
        assertFails {reelleEuklidischeNorm(listOf(Variable("unbekannt")))}
        assertFails {reelleEuklidischeNorm(listOf(KomplexeZahl(z(0),z(1))))}
    }
    @Test fun `eingeschraenkte reelle Bereiche und typisierte Vektorkomponenten bleiben auswertbar`() {
        val x=Variable("x",bindungsId="x-id")
        val intervall=ReellesIntervall(z(0),true,z(10),false)
        for(bereich in listOf(intervall,BeschraenkteZahlmenge(FundamentalerZahlbereich.REELL,z(0),false),Schnitt(listOf(ReelleZahlen,intervall)))) {
            assertEquals(ReelleZahlen,inferiereZahlenWertevorrat(x,mapOf("x-id" to bereich)))
            assertTrue(x in reelleEuklidischeNormAnwendung(listOf(x),mapOf("x-id" to bereich)).wert.enthalteneVariablen())
        }
        val n0=NichtnegativeGanzeZahlenSemantik.menge
        val beschränkt=BeschraenkteZahlmenge(FundamentalerZahlbereich.NATUERLICH_MIT_NULL,z(0),true,z(10),true)
        assertEquals(n0,maximaleZahlenGrundmenge(listOf(NatürlicheZahlen,n0)))
        assertEquals(n0,maximaleZahlenGrundmenge(listOf(n0,NatürlicheZahlen)))
        assertEquals(n0,inferiereZahlenWertevorrat(x,mapOf("x-id" to beschränkt)))
        assertIs<VektorRechnerErgebnis.ZahlWert>(VektorRechner.erzeuge(VektorRechnerAnfrage(VektorRechnerOperator.NORM,
            listOf(quelle(SpaltenVektor(listOf(x)),FundamentalerZahlbereich.NATUERLICH_MIT_NULL)),variablenVorräte=mapOf("x-id" to beschränkt))))
        val v=AllgemeinerParameter("v",bindungsId="v-id")
        val bereiche=mapOf("v-id" to Vektorraum(VektorOrientierung.Spalte,2,ReelleZahlen))
        val werte=(1L..2L).map {SymbolischeZahlKomponente(v,z(it))}
        assertIs<VektorRechnerErgebnis.ZahlWert>(VektorRechner.erzeuge(VektorRechnerAnfrage(VektorRechnerOperator.NORM,
            listOf(quelle(SpaltenVektor(werte))),variablenVorräte=bereiche)))
        assertFails {reelleEuklidischeNormAnwendung(listOf(SymbolischeZahlKomponente(v,z(3))),bereiche)}
        assertFails {reelleEuklidischeNormAnwendung(werte,mapOf("v-id" to Vektorraum(VektorOrientierung.Spalte,2,KomplexeZahlen)))}
    }
    @Test fun `normabhaengige Operationen lehnen nichtreelle Vertraege ab`() {
        val v=SpaltenVektor(listOf(z(1),z(2)))
        for(op in listOf(VektorRechnerOperator.NORM,VektorRechnerOperator.NORMALISIERUNG,VektorRechnerOperator.WINKEL,
            VektorRechnerOperator.WINKEL_ZU_ACHSE,VektorRechnerOperator.DISTANZ)) {
            val n=if(op in listOf(VektorRechnerOperator.WINKEL,VektorRechnerOperator.DISTANZ)) 2 else 1
            assertIs<VektorRechnerErgebnis.Ungueltig>(VektorRechner.erzeuge(VektorRechnerAnfrage(op,List(n){quelle(v,FundamentalerZahlbereich.KOMPLEX)})))
        }
        assertIs<VektorRechnerErgebnis.Ungueltig>(VektorRechner.erzeuge(VektorRechnerAnfrage(VektorRechnerOperator.NORM,
            listOf(quelle(SpaltenVektor(listOf(KomplexeZahl(z(0),z(1)))),FundamentalerZahlbereich.REELL)))))
        val x=Variable("x",bindungsId="x-real")
        assertIs<VektorRechnerErgebnis.ZahlWert>(VektorRechner.erzeuge(VektorRechnerAnfrage(VektorRechnerOperator.NORM,
            listOf(quelle(SpaltenVektor(listOf(x)))))))
    }
    @Test fun `Kreuzprodukt verwendet dieselben Komponenten mit erhaltenen Orientierungsvertraegen`() {
        for(zeile in listOf(false,true)) {
            fun v(werte:List<ZahlAusdruck>):OrientierterVektor=if(zeile) ZeilenVektor(werte) else SpaltenVektor(werte)
            val a=v(listOf(z(1),z(0),z(0)));val b=v(listOf(z(0),z(1),z(0)))
            val erwartet=v(listOf(z(0),z(0),z(1)))
            assertEquals(erwartet,kreuzprodukt(a,b))
            assertEquals(erwartet,assertIs<VektorRechnerErgebnis.VektorWert>(VektorRechner.erzeuge(VektorRechnerAnfrage(
                VektorRechnerOperator.KREUZPRODUKT,listOf(quelle(a),quelle(b))))).wert)
            assertEquals(v(listOf(z(0),z(0),z(0))),kreuzprodukt(a,a))
        }
        assertFails {kreuzproduktKomponenten(listOf(z(1)),listOf(z(1)))}
        val a=SpaltenVektor(listOf(z(1),z(0),z(0)))
        assertIs<VektorRechnerErgebnis.Ungueltig>(VektorRechner.erzeuge(VektorRechnerAnfrage(VektorRechnerOperator.KREUZPRODUKT,
            listOf(quelle(a,FundamentalerZahlbereich.QUATERNION),quelle(a,FundamentalerZahlbereich.QUATERNION)))))
    }
    @Test fun `Spur und Diagonalprojektionen verwenden Rang zwei und rechteckige Verankerung`() {
        val m=Matrix(listOf(listOf(z(1),z(2),z(3)),listOf(z(4),z(5),z(6))))
        val operand=MatrixOperand("matrix",m,FundamentalerZahlbereich.REELL)
        for(form in VektorStrukturAusgabe.entries) {
            val r=MatrixRechner.erzeuge(MatrixRechnerAnfrage(MatrixRechnerOperator.NEBENDIAGONALE,listOf(operand),diagonalAusgabe=form))
            val wert=when(r) {is MatrixRechnerErgebnis.TupelWert -> r.wert;is MatrixRechnerErgebnis.VektorWert -> r.wert;else -> error(r.toString())}
            assertEquals(projiziereMatrixDiagonale(Tupel(listOf(z(3),z(5))),form),wert)
        }
        assertIs<MatrixRechnerErgebnis.Ungueltig>(MatrixRechner.erzeuge(MatrixRechnerAnfrage(MatrixRechnerOperator.SPUR,listOf(operand))))
        assertEquals(z(7),assertIs<MatrixRechnerErgebnis.ZahlWert>(MatrixRechner.erzeuge(MatrixRechnerAnfrage(MatrixRechnerOperator.SPUR,
            listOf(MatrixOperand("m",Matrix(listOf(listOf(z(7)))),FundamentalerZahlbereich.REELL))))).wert)
        val tensor=Tensor(listOf(2,3),(1L..6L).map(::z))
        assertEquals(m,tensor.alsRangZweiMatrix())
        assertEquals(Tupel(listOf(z(3),z(5))),matrixDiagonale(tensor,MatrixDiagonalArt.NEBENDIAGONALE))
        assertFails {SpaltenVektor(listOf(z(1))).alsRangZweiMatrix()}
    }
    @Test fun `beide Diagonalmatrixmodi erhalten Zeile Spalte und Tupel als einzelne Struktur`() {
        for(art in MatrixDiagonalArt.entries) for(w in listOf(Tupel(listOf(z(2),z(3))),ZeilenVektor(listOf(z(2),z(3))),SpaltenVektor(listOf(z(2),z(3))))) {
            val r=assertIs<MatrixRechnerErgebnis.MatrixWert>(MatrixRechner.erzeuge(MatrixRechnerAnfrage(
                MatrixRechnerOperator.DIAGONALMATRIX_ERZEUGEN,emptyList(),diagonalWerte=w,diagonalArt=art)))
            assertEquals(Tupel(listOf(z(2),z(3))),matrixDiagonale(r.wert,art))
            assertEquals(diagonalmatrixErzeugen(w,art),r.wert)
        }
        assertFails {diagonalmatrixErzeugen(Tupel(emptyList()))}
    }
    @Test fun `Transposition erhaelt Struktur und beliebige Tensorpermutationen`() {
        val v=SpaltenVektor(listOf(z(1),z(2)))
        assertIs<ZeilenVektor>(transponiereStruktur(v));assertEquals(v,transponiereStruktur(transponiereStruktur(v)))
        val m=Matrix(listOf(listOf(z(1),z(2))))
        assertEquals(m,transponiereStruktur(transponiereStruktur(m)))
        val t=Tensor(listOf(2,1,3),(1L..6L).map(::z))
        val neu=assertIs<Tensor>(transponiereStruktur(t,listOf(2,0,1)))
        assertEquals(listOf(3,2,1),neu.dimensionen)
        assertEquals(t,transponiereStruktur(neu,listOf(1,2,0)))
        assertFails {transponiereStruktur(t,listOf(0,0,1))}
        assertIs<Tensor>(transponiereStruktur(Tensor(listOf(2),listOf(z(1),z(2)))))
        assertEquals(listOf(z(0),z(1),z(0)),einheitsVektorWerte(3,2))
    }
    @Test fun `Norm prueft partielle reelle Ausdruecke und traegt offene Bedingungen`() {
        assertFails {reelleEuklidischeNorm(listOf(ArcSinus(z(2))))}
        assertFails {reelleEuklidischeNorm(listOf(Division(z(1),z(0))))}
        assertFails {reelleEuklidischeNorm(listOf(Betrag(Division(z(1),z(0)))))}
        val x=Variable("x")
        assertFails {reelleEuklidischeNorm(listOf(Division(z(1),x)),mapOf("x" to ReelleZahlen))}
        val bedingung=Ungleichheit(x,z(0))
        val r=reelleEuklidischeNormAnwendung(listOf(Division(z(1),x)),mapOf("x" to ReelleZahlen))
        assertTrue(bedingung in r.bedingungen)
        assertEquals(r.wert,reelleEuklidischeNorm(listOf(Division(z(1),x)),mapOf("x" to ReelleZahlen),setOf(bedingung)))
        val v=SpaltenVektor(listOf(Division(z(1),x)))
        val k=assertIs<VektorRechnerErgebnis.ZahlWert>(VektorRechner.erzeuge(VektorRechnerAnfrage(VektorRechnerOperator.NORM,
            listOf(quelle(v)),variablenVorräte=mapOf("x" to ReelleZahlen))))
        assertTrue(bedingung in k.bedingungen)
        val a=SpaltenVektor(listOf(Betrag(x),Sinus(x)))
        assertIs<VektorRechnerErgebnis.Ungueltig>(VektorRechner.erzeuge(VektorRechnerAnfrage(VektorRechnerOperator.NORM,listOf(quelle(a)))))
        assertIs<VektorRechnerErgebnis.ZahlWert>(VektorRechner.erzeuge(VektorRechnerAnfrage(VektorRechnerOperator.NORM,
            listOf(quelle(SpaltenVektor(listOf(Betrag(x))))))))
    }

}
