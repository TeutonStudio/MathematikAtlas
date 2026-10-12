package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.test.*

class VektorUnterraumGeometrieTest {
    private fun z(n: Long) = RationaleZahl.von(n)
    private fun t(vararg n:Long) = Tupel(n.map(::z))
    @Test fun `Potenzparitaet und produktfreie Identitaeten`() {
        val v=SpaltenVektor(listOf(z(2),z(3)))
        val q=VektorQuelle.Vektor(v,KartesischerKoordinatenVertrag(2,FundamentalerZahlbereich.REELL,"standard","kartesisch",true))
        val erwartet=listOf(RationaleZahl.Eins,v,z(13),SpaltenVektor(listOf(z(26),z(39))),z(169))
        for(p in 0..4) {
            val r=VektorRechner.erzeuge(VektorRechnerAnfrage(VektorRechnerOperator.POTENZ,listOf(q),exponent=z(p.toLong()),skalarprodukt=if(p<2) { _,_->error("Produkt darf nicht ausgewertet werden") } else null))
            val wert=when(r) { is VektorRechnerErgebnis.ZahlWert->r.wert;is VektorRechnerErgebnis.VektorWert->r.wert;else->error(r.toString()) }
            assertEquals(erwartet[p],wert)
        }
        assertIs<VektorRechnerErgebnis.Ungueltig>(VektorRechner.erzeuge(VektorRechnerAnfrage(VektorRechnerOperator.POTENZ,listOf(q),exponent=z(-1))))
    }
    @Test fun `Unterraum Rang Mitgliedschaft Affin und Substitution`() {
        val u=UnterraumMenge(listOf(listOf(z(1),z(0),z(0)),listOf(z(2),z(0),z(0))),listOf(z(0),z(0),z(1)))
        assertEquals(1,u.rangOderNull())
        assertEquals(Wahrheitswert.Wahr,ElementBeziehung(t(5,0,1),u).entscheide().wahrheitswert)
        assertEquals(Wahrheitswert.Lüge,ElementBeziehung(t(5,0,0),u).entscheide().wahrheitswert)
        val x=Variable("x")
        val symbol=UnterraumMenge(listOf(listOf(x,z(0))))
        assertNull(symbol.rangOderNull())
        val f=Methode("U",listOf(x),symbol,Potenzmenge(Tupelraum(listOf(ReelleZahlen,ReelleZahlen))),mapOf("x" to ReelleZahlen))
        val angewendet=assertIs<UnterraumMenge>(f.wendeAn(listOf(z(2))))
        assertEquals(z(2),angewendet.richtungen.single().first())
        assertEquals(1,angewendet.rangOderNull())
        assertEquals(Wahrheitswert.Wahr,ElementBeziehung(t(0,0),UnterraumMenge(emptyList(),dimension=2)).entscheide().wahrheitswert)
    }
    @Test fun `Geometrie Schwerpunkt im verschobenen R3 Polygon und symbolische Kreisebene`() {
        val r=EuklidischerRaum("E",3)
        fun p(name:String,vararg a:Long)=GeometriePunkt(name,r,t(*a))
        val dreieck=GeometriePolygon(listOf(p("a",0,0,1),p("b",1,0,1),p("c",0,1,1)))
        val m=geometrieMittelpunkt(dreieck)
        assertEquals(z(1),m.elemente[2])
        assertEquals(vereinfache(Division(z(1),z(3))),m.elemente[0])
        val kreis=GeometrieKreislinie(p("o",0,0,0),p("r",1,0,0))
        assertNull(ElementBeziehung(t(0,1,0),geometrieKoordinatenMenge(kreis)).entscheide().wahrheitswert)
        val punkt=p("p",1,2,3)
        assertEquals(Wahrheitswert.Wahr,ElementBeziehung(t(1,2,3),geometrieKoordinatenMenge(punkt)).entscheide().wahrheitswert)
    }
    @Test fun `Referenztexte sind kein Beweis fuer eigene Methoden`() {
        val u=Variable("u");val v=Variable("v")
        val f=Methode("f",listOf(u,v),addition(multiplikation(u,v),z(1)),ReelleZahlen,mapOf("u" to ReelleZahlen,"v" to ReelleZahlen))
        val a=pruefeSkalarprodukt(f,FundamentalerZahlbereich.REELL,SkalarproduktLinearitaet.RECHTSLINEAR,SkalarproduktNachweisReferenzen("l","s","p"))
        assertEquals(NachweisStatus.Unvollstaendig,a.pruefung.status)
        assertNull(a.pruefung.zeugnis)
        val standard=f.copy(vorschrift=multiplikation(u,v))
        assertEquals(NachweisStatus.Nachgewiesen,pruefeSkalarprodukt(standard,FundamentalerZahlbereich.REELL,SkalarproduktLinearitaet.RECHTSLINEAR,SkalarproduktNachweisReferenzen()).pruefung.status)
        assertEquals(NachweisStatus.Widerlegt,pruefeSkalarprodukt(standard.copy(vorschrift=negation(multiplikation(u,v))),FundamentalerZahlbereich.REELL,SkalarproduktLinearitaet.RECHTSLINEAR,SkalarproduktNachweisReferenzen()).pruefung.status)
    }
}
