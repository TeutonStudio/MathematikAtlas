package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class VektorUnterraumIntegrationTest {
    @Test fun `Potenz und Unterraum haben stabile Ports und JSON`() {
        val p=vektorRechnerVorlage(VektorRechnerOperator.POTENZ).erzeuge(GraphPunkt.Zero)
        val u=konfiguriereMengenRechner(MengenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero),MengenRechnerOperator.UNTERRAUM)
        assertEquals(p.anschlüsse.map { it.id },konfiguriereVektorRechner(p,VektorRechnerOperator.POTENZ).anschlüsse.map { it.id })
        assertEquals(u.anschlüsse.map { it.id },konfiguriereMengenRechner(u,MengenRechnerOperator.UNTERRAUM).anschlüsse.map { it.id })
        val karte=KartenDaten(name="Potenz Unterraum",knoten=listOf(p,u))
        val geladen=MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(karte))
        assertEquals(karte.knoten.map { it.anschlüsse.map { a -> a.id } },geladen.knoten.map { it.anschlüsse.map { a -> a.id } })
    }
    @Test fun `Potenzparitaet steuert wirkliche Graphverbindungen`() {
        val p=vektorRechnerVorlage(VektorRechnerOperator.POTENZ).erzeuge(GraphPunkt.Zero)
        val skalar=MathematikKnotenVorlagen.Addition.erzeuge(GraphPunkt.Zero)
        val vektor=vektorRechnerVorlage(VektorRechnerOperator.NORM).erzeuge(GraphPunkt.Zero)
        val graph=MathematikKartenLaufzeit().graphPrüfung
        fun ref(k:KnotenDaten,name:String,richtung:AnschlussRichtung)=AnschlussVerweis(k.id,k.anschlüsse.single { it.name==name && it.richtung==richtung }.id)
        val out=ref(p,"ergebnis",AnschlussRichtung.Ausgang)
        val karte=KartenDaten(name="Parität",knoten=listOf(p,skalar,vektor))
        assertIs<VerbindungsPrüfung.Erlaubt>(graph.prüfe(karte,out,ref(skalar,"a",AnschlussRichtung.Eingang)))
        val ungerade=p.copy(parameter=p.parameter+(VEKTOR_POTENZ_EXPONENT to "3"))
        assertIs<VerbindungsPrüfung.Erlaubt>(graph.prüfe(karte.copy(knoten=listOf(ungerade,skalar,vektor)),out,ref(vektor,"vektor",AnschlussRichtung.Eingang)))
    }
    @Test fun `Unterraumauswerter berechnet affin und weist innere Luecke ab`() {
        val u=konfiguriereMengenRechner(MengenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero),MengenRechnerOperator.UNTERRAUM)
        val laufzeit=MathematikKartenLaufzeit()
        fun t(vararg n:Long)=BedingterWert(Tupel(n.map(RationaleZahl::von)))
        val auswerter=GesamterMathematikAuswerter.erzeugeRegister().finde(MengenRechner.KNOTEN_ART)!!
        val r=auswerter.auswerten(KnotenAuswertungsKontext(u,mapOf("a.1" to t(1,0),"c" to t(0,2)),RechenKontext()))
        val menge=assertIs<UnterraumMenge>(r.ausgaben.getValue("menge").objekt)
        assertEquals(Wahrheitswert.Wahr,ElementBeziehung(Tupel(listOf(RationaleZahl.von(4),RationaleZahl.von(2))),menge).entscheide().wahrheitswert)
    }
    @Test fun `Geometrieanker verschiebt gesamte Menge und lehnt komplexes Ziel ab`() {
        val raum=EuklidischerRaum("E",2)
        fun punkt(name:String,x:Long,y:Long)=GeometriePunkt(name,raum,Tupel(listOf(RationaleZahl.von(x),RationaleZahl.von(y))))
        val strecke=GeometrieStrecke(punkt("a",0,0),punkt("b",2,0))
        val k=konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero),"koordinatenmenge").copy(parameter=mapOf("geometrie.anker" to "mittelpunkt"))
        val auswerter=GesamterMathematikAuswerter.erzeugeRegister().finde(ÜBERSETZER_ART)!!
        val r=auswerter.auswerten(KnotenAuswertungsKontext(k,mapOf("wert" to BedingterWert(strecke)),RechenKontext()))
        val menge=r.ausgaben.getValue("ergebnis").objekt as MengenAusdruck
        assertEquals(Wahrheitswert.Wahr,ElementBeziehung(Tupel(listOf(RationaleZahl.von(-1),RationaleZahl.Null)),menge).entscheide().wahrheitswert)
        assertFails { auswerter.auswerten(KnotenAuswertungsKontext(k,mapOf("wert" to BedingterWert(strecke),"zielpunkt" to BedingterWert(Tupel(listOf(KomplexeZahl(RationaleZahl.Null,RationaleZahl.Eins),RationaleZahl.Null)))),RechenKontext())) }
        val ziel=punkt("z",4,0)
        val verschoben=auswerter.auswerten(KnotenAuswertungsKontext(k,mapOf("wert" to BedingterWert(strecke),"zielpunkt" to BedingterWert(ziel)),RechenKontext())).ausgaben.getValue("ergebnis").objekt as MengenAusdruck
        assertEquals(Wahrheitswert.Wahr,ElementBeziehung(Tupel(listOf(RationaleZahl.von(3),RationaleZahl.Null)),verschoben).entscheide().wahrheitswert)
    }
}
