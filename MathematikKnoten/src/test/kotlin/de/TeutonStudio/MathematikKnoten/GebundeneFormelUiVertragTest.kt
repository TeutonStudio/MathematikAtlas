package de.TeutonStudio.MathematikKnoten
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.AtlasZwischenablage
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*
class GebundeneFormelUiVertragTest {
    private fun basis()=ZahlenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
    private fun eval(k:KnotenDaten,e:Map<String,BedingterWert>)=GesamterMathematikAuswerter.erzeugeRegister().finde(k.art)!!.auswerten(KnotenAuswertungsKontext(k,e,RechenKontext()))
    @Test fun `Gleichnamige Quellen werden verschieden gebunden auch nach Kopieren und Laden`() {
        val x=FormelAusdruck.Variable("a","x",typ=FormelTyp.ZAHL);val y=x.copy(id="b")
        val ast=FormelAusdruck.Operation("s","zahl.subtraktion",listOf(FormelArgument("a",0,x),FormelArgument("b",1,y)),FormelTyp.ZAHL)
        val qs=listOf("q1","q2").map {FormelVariablenQuelle(it,"x",FormelVariablenArt.FREI,FormelTyp.ZAHL,ReelleZahlen)}
        val k=konfiguriereGebundeneKnotenFormel(basis(),GebundeneFormel(ast,qs,mapOf("a" to "q1","b" to "q2")))
        val ab=AtlasZwischenablage();ab.kopiere(KartenDaten(name="T",knoten=listOf(k)),setOf(k.id))
        val kopie=ab.erzeugeEinfügung(GraphPunkt.Zero)!!.knoten.single()
        val geladen=MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(KartenDaten(name="T",knoten=listOf(kopie)))).knoten.single()
        for(n in listOf(k,geladen)) {
            val ports=n.anschlüsse.filter {it.richtung==AnschlussRichtung.Eingang}
            assertEquals(2,ports.map {it.name}.distinct().size)
            val e=ports.associate {it.name to BedingterWert(RationaleZahl.von(if(it.semantischeId=="q1") 5 else 2))}
            assertEquals(RationaleZahl.von(3),eval(n,e).ausgaben.getValue("wert").objekt)
        }
    }
    @Test fun `Verschachtelte Visualisierung verlangt explizite Blattprojektion`() {
        val w=Tupel(listOf(Tupel(listOf(RationaleZahl.Eins,RationaleZahl.von(2)))))
        val adapter=de.TeutonStudio.MathematikKnoten.visualisierung.koordinaten.KoordinatenAdapter
        assertIs<de.TeutonStudio.MathematikKnoten.visualisierung.koordinaten.KoordinatenErgebnis.ProjektionErforderlich>(adapter.extrahiere(w,2))
        assertIs<de.TeutonStudio.MathematikKnoten.visualisierung.koordinaten.KoordinatenErgebnis.Darstellbar>(adapter.extrahiere(w,2,ausgabeKoordinaten=listOf(1,2)))
    }
    @Test fun `Herkunft wird im exportierten Formelsnapshot synchronisiert`() {
        val z=ÜbersetzungsDienst.übersetze(ÜbersetzungsAnfrage(Tupel(listOf(RationaleZahl.Eins)),ÜbersetzungsZiel.ZEILE)) as ÜbersetzungsErgebnis.Erfolg
        val eingang=BedingterWert(z.wert,übersetzungen=ÜbersetzungsMetadaten(z.zeugnis,kontext=z.kontext))
        val f=GebundeneFormel(FormelAusdruck.Literal("l",RationaleZahl.Eins,FormelTyp.ZAHL),emptyList(),emptyMap())
        val r=KnotenAuswertungsErgebnis(mapOf("wert" to BedingterWert(RationaleZahl.Eins,gebundeneFormel=f)),formelAusgaben=setOf("wert")).führeÜbersetzungsHerkunftFort(listOf(eingang))
        val w=r.ausgaben.getValue("wert")
        assertNull(w.übersetzungen.aktuell)
        val paket=ÜbersetzungsCodec.dekodiereWert(ÜbersetzungsCodec.kodieren(w.alsÜbertragenerMathematischerWert()))
        assertEquals(listOf(z.zeugnis),(paket.formel!!.wurzel as FormelAusdruck.Literal).übersetzungen.herkunft)
    }
    @Test fun `Portnamen bleiben auch bei generierten Suffixen eindeutig`() {
        val qs=listOf("x","x","x · 1").mapIndexed {i,n ->FormelVariablenQuelle("q$i",n,FormelVariablenArt.FREI,FormelTyp.ZAHL,ReelleZahlen)}
        val f=GebundeneFormel(FormelAusdruck.Literal("l",RationaleZahl.Eins,FormelTyp.ZAHL),qs,emptyMap())
        val k=konfiguriereGebundeneKnotenFormel(basis(),f)
        val ports=k.anschlüsse.filter {it.richtung==AnschlussRichtung.Eingang}
        assertEquals(3,ports.map {it.name}.distinct().size)
        assertEquals(qs.map {it.id},ports.map {it.semantischeId})
    }
    @Test fun `Kontextfehler wird nicht durch Uebernahme ueberschrieben`() {
        val k=basis().copy(eigenschaften=mapOf(FORMEL_KONTEXT_EIGENSCHAFT to KnotenEigenschaft.Text("kaputt")))
        assertFails {ladeGebundeneKnotenFormel(k)}
        assertFails {konfiguriereGebundeneKnotenFormel(k,FormelBearbeitungsEntwurf.aus(FormelAusdruck.Literal("l",RationaleZahl.Eins,FormelTyp.ZAHL)).formel)}
        assertEquals("kaputt",(k.eigenschaften[FORMEL_KONTEXT_EIGENSCHAFT] as KnotenEigenschaft.Text).wert)
    }
    @Test fun `Port IDs ueberleben Umbenennung und Neusortierung`() {
        val e=FormelBearbeitungsEntwurf.aus((FormelLatexCodec.importiere("x+y") as FormelLatexImportErgebnis.Erfolg).ausdruck)
        val k=konfiguriereGebundeneKnotenFormel(basis(),e.formel)
        val q=e.formel.quellen.first();e.quelle(q.copy(anzeigeName="z"));e.verschiebeQuelle(q.id,1)
        val neu=konfiguriereGebundeneKnotenFormel(k,e.formel)
        assertEquals(k.anschlüsse.associate {it.semantischeId to it.id},neu.anschlüsse.associate {it.semantischeId to it.id})
    }
    @Test fun `Rueckfuehrungsmodus nutzt Zeugnis und erhaelt leere Untertupel`() {
        val w=Tupel(listOf(Tupel(emptyList()),Tupel(listOf(RationaleZahl.Eins))))
        val r=ÜbersetzungsDienst.übersetze(ÜbersetzungsAnfrage(w,ÜbersetzungsZiel.ABFLACHEN)) as ÜbersetzungsErgebnis.Erfolg
        val k=konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero),"rueckfuehrung")
        val aus=eval(k,mapOf("wert" to BedingterWert(r.wert,übersetzungen=ÜbersetzungsMetadaten(r.zeugnis,kontext=r.kontext))))
        assertNull(aus.fehler);assertEquals(w,aus.ausgaben.getValue("ergebnis").objekt)
        assertNotNull(eval(k,mapOf("wert" to BedingterWert(r.wert))).übersetzungsDiagnose)
    }
    @Test fun `Entfaltete Formel ertraegt Umbenennen einer vorgeschalteten Quelle`() {
        val f=FormelLatexCodec.importiereGebunden("x+1",listOf(FormelVariablenQuelle("qx","x",FormelVariablenArt.FREI,FormelTyp.ZAHL,ReelleZahlen)))
        val graph=erzeugeFormelKarte(f)
        val quelle=graph.karte.knoten.first {k -> k.id!=graph.wurzel && ladeGebundeneKnotenFormel(k)?.wurzel is FormelAusdruck.Variable}
        val e=FormelBearbeitungsEntwurf(ladeGebundeneKnotenFormel(quelle)!!);e.quelle(e.formel.quellen.single().copy(anzeigeName="z"))
        val geändert=konfiguriereGebundeneKnotenFormel(quelle,e.formel)
        val karte=graph.karte.copy(knoten=graph.karte.knoten.map {if(it.id==quelle.id) geändert else it})
        val r=MathematikKartenLaufzeit().auswerten(karte)
        assertTrue(r.fehler.isEmpty(),r.fehler.joinToString())
        val snapshot=r.knoten.getValue(graph.wurzel).ausgaben.getValue("wert").gebundeneFormel!!
        assertEquals("z",snapshot.quellen.single {it.id=="qx"}.anzeigeName)
    }
    @Test fun `Angeschlossene freie Quelle bleibt im Ergebnispaket und Methodenargument`() {
        val f=FormelLatexCodec.importiereGebunden("x+1",listOf(FormelVariablenQuelle("qx","x",FormelVariablenArt.FREI,FormelTyp.ZAHL,ReelleZahlen)))
        val k=konfiguriereGebundeneKnotenFormel(basis(),f)
        for(y in listOf(Variable("y",bindungsId="qy"),Variable("y"))) {
            val eingang=BedingterWert(y,annahmen=setOf(Ungleichheit(y,RationaleZahl.Null)),variablenQuellen=listOf(VariablenQuelle(KnotenId("y-knoten"),"y",ReelleZahlen,bindungsId="qy")))
            val w=eval(k,mapOf("x" to eingang)).ausgaben.getValue("wert")
            val paket=ÜbersetzungsCodec.dekodiereWert(ÜbersetzungsCodec.kodieren(w.alsÜbertragenerMathematischerWert()))
            val m=requireNotNull(paket.formel).alsMethode("plus1")
            assertEquals(listOf("qy"),paket.formel!!.quellen.map {it.id})
            assertEquals(Tupel(listOf(RationaleZahl.von(3))),m.wendeMitIdentitätenAn(mapOf("qy" to RationaleZahl.von(2))).ergebnis)
            assertFails {m.wendeMitIdentitätenAn(mapOf("qy" to RationaleZahl.Null))}
        }
    }
    @Test fun `Differenzenquotient durchlaeuft Formel Karte Methode Codec und Anwendung`() {
        val r=ReelleZahlen.elementTypAusdruck()
        val sig=MethodenSignatur(listOf(MethodenKomponente("t","t",0,r)),listOf(MethodenKomponente("y","wert",0,r)))
        val qs=listOf(FormelVariablenQuelle("qf","f",FormelVariablenArt.FREI,FormelTyp.METHODE,methodenSignatur=sig),FormelVariablenQuelle("qx","x",FormelVariablenArt.FREI,FormelTyp.ZAHL,ReelleZahlen),FormelVariablenQuelle("qh","h",FormelVariablenArt.FREI,FormelTyp.ZAHL,ReelleZahlen))
        val h=Variable("h",bindungsId="qh")
        val f=FormelLatexCodec.importiereGebunden("\\frac{f(x+h)-f(x)}{h}",qs).copy(annahmen=setOf(Ungleichheit(h,RationaleZahl.Null)))
        val graph=erzeugeFormelKarte(f)
        val geladen=MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(graph.karte))
        val zurück=karteAlsGebundeneFormel(geladen,graph.wurzel)
        assertEquals(qs.map {it.id},zurück.quellen.map {it.id})
        val laufzeit=MathematikKartenLaufzeit();val aus=laufzeit.auswerten(geladen)
        assertTrue(aus.fehler.isEmpty(),aus.fehler.joinToString())
        val wert=aus.knoten.getValue(graph.wurzel).ausgaben.getValue("wert")
        assertNotNull(wert.gebundeneFormel)
        val m=wert.gebundeneFormel!!.alsMethode("Q")
        val t=Variable("t")
        val quadrat=MathematischeMethode("Quadrat",listOf(t),Potenz(t,RationaleZahl.von(2)),ReelleZahlen,mapOf("t" to ReelleZahlen))
        val parameter=mapOf<String,MathematischesObjekt>("qf" to quadrat,"qx" to RationaleZahl.von(3),"qh" to RationaleZahl.von(2))
        assertEquals(Tupel(listOf(RationaleZahl.von(8))),m.wendeMitIdentitätenAn(parameter).ergebnis)
        assertFails {m.wendeMitIdentitätenAn(parameter+("qh" to RationaleZahl.Null))}
        assertEquals(m,MathematischerWertCodec.dekodieren(MathematischerWertCodec.kodieren(m)))
        val formelNode=geladen.knoten.single {it.id==graph.wurzel}
        val plan=planeFormelKnotenZusammenfassen(geladen,formelNode)
        val editor=de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand(geladen,laufzeit.graphPrüfung)
        editor.führeAus(de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion.TeilgraphErsetzen(plan))
        assertEquals(1,editor.karte.knoten.size);editor.rückgängig();assertEquals(geladen,editor.karte);editor.wiederholen();assertEquals(1,editor.karte.knoten.size)
    }

}
