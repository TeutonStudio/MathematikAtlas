package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class MethodenAufrufDifferenzenquotientTest {
    private fun z(n:Long)=RationaleZahl.von(n)
    @Test fun `Altaufruf Laden Auswertung Synchronisierung Speichern und Import erhalten den Differenzenquotientenvertrag`() {
        val typ=ReelleZahlen.elementTypAusdruck()
        val sig=MethodenSignatur(listOf(MethodenKomponente("t","t",0,typ)),listOf(MethodenKomponente("r","r",0,typ)))
        val quellen=listOf(
            FormelVariablenQuelle("qf","f",FormelVariablenArt.FREI,FormelTyp.METHODE,Abbildungsmenge(Tupelraum(listOf(ReelleZahlen)),Tupelraum(listOf(ReelleZahlen))),sig.typAusdruck,methodenSignatur=sig),
            FormelVariablenQuelle("qx","x",FormelVariablenArt.FREI,FormelTyp.ZAHL,ReelleZahlen),
            FormelVariablenQuelle("qh","h",FormelVariablenArt.FREI,FormelTyp.ZAHL,ReelleZahlen))
        val q=FormelLatexCodec.importiereGebunden("\\frac{f(x+h)-f(x)}{h}",quellen).alsMethode("Q")
        val t=Variable("t");val quadrat=MathematischeMethode("f",listOf(t),Potenz(t,z(2)),ReelleZahlen,mapOf("t" to ReelleZahlen))
        fun quelle(name:String,art:AnschlussArtId)=KnotenDaten(art="mathematik.kartenEingang",name=name,
            anschlüsse=listOf(AnschlussDaten(name="wert",richtung=AnschlussRichtung.Ausgang,kante=AnschlussKante.Rechts,art=art,
                vertrag=de.TeutonStudio.TypSystem.AnschlussVertrag(if(art==MathematikAnschlussArten.Zahl.id) typ else de.TeutonStudio.TypSystem.TypAusdruck.Unbekannt))))
        val methodenquelle=quelle("Q",MathematikAnschlussArten.Methode.id)
        val f=quelle("f",MathematikAnschlussArten.Methode.id)
        val x=quelle("x",MathematikAnschlussArten.Zahl.id);val h=quelle("h",MathematikAnschlussArten.Zahl.id)
        val basis=FaltungsKnotenVorlagen.MethodenAnwendungZahl.erzeuge(GraphPunkt.Zero)
        val argument=basis.anschlüsse.single {it.name=="argument"}
        val alt=basis.copy(anschlüsse=basis.anschlüsse.filter {it.name!="argument"}+listOf(f,x,h).mapIndexed {i,k ->
            argument.copy(id=AnschlussId("argument-$i"),name=k.name,art=k.anschlüsse.single().art,reihenfolge=i+1)})
        val verbindungen=(listOf(methodenquelle to "methode",f to "f",x to "x",h to "h")).map { (k,name) ->
            VerbindungDaten(von=AnschlussVerweis(k.id,k.anschlüsse.single().id),zu=AnschlussVerweis(alt.id,alt.anschlüsse.single {it.name==name&&it.richtung==AnschlussRichtung.Eingang}.id))}
        val original=KartenDaten(name="Q",knoten=listOf(methodenquelle,f,x,h,alt),verbindungen=verbindungen)
        val register=GesamterMathematikAuswerter.erzeugeRegister()
        val auswerter=KartenAuswerter(register)
        fun auswerten(karte:KartenDaten,schritt:MathematischesObjekt)=auswerter.auswerten(karte,mapOf(
            methodenquelle.id to mapOf("wert" to BedingterWert(q)),f.id to mapOf("wert" to BedingterWert(quadrat)),
            x.id to mapOf("wert" to BedingterWert(z(3))),h.id to mapOf("wert" to BedingterWert(schritt))))
        val geladen=MathematikKartenCodec.lade(KartenDatenJson.schreibe(original))
        val erster=auswerten(geladen,z(2))
        assertTrue(erster.fehler.isEmpty(),erster.fehler.toString())
        assertEquals(z(8),erster.knoten.getValue(alt.id).ausgaben.getValue("wert").objekt)
        val sync=synchronisiereMethodenAufrufe(geladen,erster,GraphPrüfung(AnschlussArtRegister(MathematikAnschlussArten.alle), MathematischeTypen.typSystem))
        assertEquals(verbindungen.map {it.id}.toSet(),sync.verbindungen.map {it.id}.toSet(), sync.knoten.joinToString("\n") {it.name+": "+it.anschlüsse.toString()+" "+it.parameter.toString()})
        val text=MathematikKartenCodec.schreibe(sync)
        for(karte in listOf(MathematikKartenCodec.lese(text),MathematikKartenCodec.lade(text),MathematikKartenCodec.importiere(text))) {
            assertEquals(z(8),auswerten(karte,z(2)).knoten.getValue(alt.id).ausgaben.getValue("wert").objekt)
            assertNotNull(auswerten(karte,z(0)).knoten.getValue(alt.id).fehler)
            val symbol=Variable("h")
            val wert=auswerten(karte,symbol).knoten.getValue(alt.id).ausgaben.getValue("wert")
            assertTrue(Ungleichheit(symbol,z(0)) in wert.annahmen)
        }
    }
}
