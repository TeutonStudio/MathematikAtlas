package de.TeutonStudio.MathematikRechenSystem

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class ÜbersetzungsZeugnisTest {
    private fun z(n: Long) = RationaleZahl.von(n)
    private fun erfolg(w: MathematischesObjekt, ziel: ÜbersetzungsZiel, k: ÜbersetzungsKontext = ÜbersetzungsKontext(), p: Map<String,String> = emptyMap()) =
        assertIs<ÜbersetzungsErgebnis.Erfolg>(ÜbersetzungsDienst.übersetze(ÜbersetzungsAnfrage(w,ziel,k,p)))

    @Test fun `heterogene Komponenten bleiben im exakten Bild und nach Rueckfuehrung eingeschraenkt`() {
        val a=EndlicheMenge(setOf(z(1))); val b=EndlicheMenge(setOf(z(2)))
        val k=ÜbersetzungsKontext(werteBereich=Tupelraum(listOf(a,b)),quellen=listOf(ÜbersetzungsQuelle("id-x","x",a)))
        val r=erfolg(Tupel(listOf(z(1),z(2))),ÜbersetzungsZiel.ZEILE,k)
        assertEquals(NachweisStatus.Nachgewiesen,r.zeugnis.injektiv)
        assertNotEquals(NachweisStatus.Nachgewiesen,r.zeugnis.surjektivAufTräger)
        assertEquals(Wahrheitswert.Wahr,ElementBeziehung(r.wert,r.zeugnis.bildRaum!!).entscheide().wahrheitswert)
        assertEquals(Wahrheitswert.Lüge,ElementBeziehung(ZeilenVektor(listOf(z(2),z(1))),r.zeugnis.bildRaum!!).entscheide().wahrheitswert)
        val zurück=assertIs<ÜbersetzungsErgebnis.Erfolg>(ÜbersetzungsDienst.rückübersetze(r.wert,r.zeugnis))
        assertEquals(Tupel(listOf(z(1),z(2))),zurück.wert)
        assertEquals(k.quellen,zurück.kontext.quellen)
        val t=erfolg(r.wert,ÜbersetzungsZiel.TUPEL,r.kontext)
        assertEquals(listOf(a,b),t.kontext.komponentenRäume)
    }

    @Test fun `Abflachungsplaene erhalten leere Teilstrukturen und Einertupel`() {
        val werte=listOf(Tupel(emptyList()),Tupel(listOf(Tupel(emptyList()))),Tupel(listOf(Tupel(listOf(z(1),z(2))),Tupel(emptyList()),z(3))),Tupel(listOf(Tupel(listOf(Tupel(listOf(z(1))))))))
        for(w in werte) for(ebene in listOf("1","alle")) {
            val r=erfolg(w,ÜbersetzungsZiel.ABFLACHEN,p=mapOf("abflachen.ebenen" to ebene))
            val z=ÜbersetzungsCodec.dekodiereZeugnis(ÜbersetzungsCodec.kodieren(r.zeugnis))
            assertEquals(w,assertIs<ÜbersetzungsErgebnis.Erfolg>(ÜbersetzungsDienst.rückübersetze(r.wert,z)).wert)
            assertNotEquals(NachweisStatus.Nachgewiesen,z.injektiv)
            assertEquals(NachweisStatus.Nachgewiesen,z.informationserhaltung)
        }
    }

    @Test fun `fehlende Bereiche bleiben offen und ungueltige Struktur ist nicht definiert`() {
        val r=erfolg(Tupel(listOf(z(1))),ÜbersetzungsZiel.SPALTE)
        assertNull(r.zeugnis.bildRaum);assertNull(r.zeugnis.zielTräger)
        listOf<MathematischesObjekt>(z(1),Tupel(emptyList()),Tupel(listOf(Tupel(listOf(z(1)))))).forEach {
            assertIs<ÜbersetzungsErgebnis.NichtDefiniert>(ÜbersetzungsDienst.übersetze(ÜbersetzungsAnfrage(it,ÜbersetzungsZiel.SPALTE)))
        }
        assertIs<ÜbersetzungsErgebnis.NichtDefiniert>(ÜbersetzungsDienst.übersetze(ÜbersetzungsAnfrage(Tupel(listOf(z(2))),ÜbersetzungsZiel.ZEILE,ÜbersetzungsKontext(werteBereich=Tupelraum(listOf(EndlicheMenge(setOf(z(1)))))))))
    }

    @Test fun `Periodenverlust und Polarapproximation werden nicht automatisch ausgefuehrt`() {
        val r=erfolg(z(7),ÜbersetzungsZiel.GRAD,ÜbersetzungsKontext(werteBereich=ReelleZahlen))
        assertEquals(NachweisStatus.Widerlegt,r.zeugnis.injektiv)
        assertTrue(r.zeugnis.verloreneInformationen.isNotEmpty())
        assertFalse(r.zeugnis.fürAutomatikGeeignet())
        val polar=erfolg(Tupel(listOf(z(0),z(0))),ÜbersetzungsZiel.POLAR)
        assertEquals(ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT,polar.zeugnis.genauigkeit)
        assertNotEquals(NachweisStatus.Nachgewiesen,polar.zeugnis.injektiv)
        assertIs<ÜbersetzungsErgebnis.NichtDefiniert>(ÜbersetzungsDienst.übersetze(ÜbersetzungsAnfrage(z(7),ÜbersetzungsZiel.GRAD,automatisch=true)))
    }

    @Test fun `unbekannte Regeln bleiben lesbar und geben keine zertifizierte Rueckfuehrung`() {
        val r=erfolg(Tupel(listOf(z(1))),ÜbersetzungsZiel.SPALTE)
        for(z in listOf(r.zeugnis.copy(regelVersion=999),r.zeugnis.copy(regelId="uebersetzung.zukunft"))) {
            val gelesen=ÜbersetzungsCodec.dekodiereZeugnis(ÜbersetzungsCodec.kodieren(z))
            assertEquals(z.regelId,gelesen.regelId);assertEquals(z.regelVersion,gelesen.regelVersion)
            assertNotEquals(NachweisStatus.Nachgewiesen,gelesen.informationserhaltung)
            assertEquals(z.informationserhaltung,gelesen.ungeprüfteBehauptungen?.informationserhaltung)
            assertIs<ÜbersetzungsErgebnis.NichtDefiniert>(ÜbersetzungsDienst.rückübersetze(r.wert,gelesen))
        }
        assertFails { ÜbersetzungsCodec.dekodiereZeugnis(ÜbersetzungsCodec.kodieren(r.zeugnis.copy(identitätsZuordnung=mapOf("unbekannt" to "x")))) }
        assertFails { ÜbersetzungsCodec.dekodiereZeugnis(ÜbersetzungsCodec.kodieren(r.zeugnis.copy(rückführungsplan=StrukturRückführungsplan(r.zeugnis.quelle,listOf(listOf(2)))))) }
    }

    @Test fun `Wert Formel Graph Methode und Export sind ohne Ursprungskarte selbsttragend`() {
        val r=erfolg(Tupel(listOf(z(1))),ÜbersetzungsZiel.SPALTE)
        val m=ÜbersetzungsMetadaten(r.zeugnis,kontext=r.kontext)
        val payload=ÜbertragenerMathematischerWert(r.wert,m)
        assertEquals(payload,ÜbersetzungsCodec.dekodiereWert(ÜbersetzungsCodec.kodieren(payload)))
        val literal=FormelAusdruck.Literal("l",r.wert,FormelTyp.OBJEKT,m)
        assertEquals(literal,StrukturFormelCodec.dekodieren(StrukturFormelCodec.kodieren(literal)))
        assertEquals(literal,assertIs<GraphZuFormelErgebnis.Erfolg>(GraphZuFormel.konvertiere(FormelZuGraph.konvertiere(literal))).wurzel)
        val methode=MathematischeMethode("f",emptyList(),r.wert,Tupelraum(listOf(ReelleZahlen)),übersetzungen=m)
        assertEquals(methode,MathematischerWertCodec.dekodieren(MathematischerWertCodec.kodieren(methode)))
        assertNull(m.alsHerkunft().aktuell);assertEquals(listOf(r.zeugnis),m.alsHerkunft().herkunft)
    }
    @Test fun `regelwidriger Bildraumplan liefert keinen Mitgliedschaftsnachweis`() {
        val q=Tupel(listOf(z(1),z(1)))
        val r=erfolg(q,ÜbersetzungsZiel.SPALTE,ÜbersetzungsKontext(werteBereich=Tupelraum(listOf(ReelleZahlen,EndlicheMenge(setOf(z(1)))))))
        val bild=(r.zeugnis.bildRaum as ÜbersetzungsBildraum).copy(rückführungsplan=StrukturRückführungsplan(r.zeugnis.quelle,listOf(listOf(2),listOf(1))))
        val gelesen=MathematischerWertCodec.dekodieren(MathematischerWertCodec.kodieren(bild)) as ÜbersetzungsBildraum
        assertNotEquals(Wahrheitswert.Wahr,ElementBeziehung(SpaltenVektor(listOf(z(1),z(2))),gelesen).entscheide().wahrheitswert)
    }

    @Test fun `Literalbedingungen wirken auch nach Methodenbildung`() {
        val x=Variable("x",bindungsId="qx")
        val bedingung=Ungleichheit(x,z(0))
        val r=erfolg(Tupel(listOf(x)),ÜbersetzungsZiel.SPALTE,ÜbersetzungsKontext(annahmen=setOf(bedingung)))
        val ast=FormelAusdruck.Literal("l",x,FormelTyp.ZAHL,ÜbersetzungsMetadaten(kontext=r.kontext))
        val f=GebundeneFormel(ast,listOf(FormelVariablenQuelle("qx","x",FormelVariablenArt.FREI,FormelTyp.ZAHL,ReelleZahlen)),emptyMap())
        assertTrue(bedingung in f.werteAus().bedingungen)
        val m=f.alsMethode("f")
        assertFailsWith<IllegalArgumentException> { m.wendeMitIdentitätenAn(mapOf("qx" to z(0))) }
    }

    @Test fun `Strukturwechsel und Rueckfuehrung behalten numerische Herkunft`() {
        val r=erfolg(Tupel(listOf(z(1))),ÜbersetzungsZiel.ZEILE,ÜbersetzungsKontext(datenGenauigkeit=ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT))
        assertEquals(ÜbersetzungsGenauigkeit.EXAKT,r.zeugnis.genauigkeit)
        assertEquals(ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT,r.zeugnis.effektiveGenauigkeit)
        val zurück=assertIs<ÜbersetzungsErgebnis.Erfolg>(ÜbersetzungsDienst.rückübersetze(r.wert,r.zeugnis))
        assertEquals(ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT,zurück.zeugnis.effektiveGenauigkeit)
    }

}
