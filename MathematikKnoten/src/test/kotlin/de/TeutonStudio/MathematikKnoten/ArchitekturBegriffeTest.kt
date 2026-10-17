package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.konzeptknoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class ArchitekturBegriffeTest {
    private val register=GesamterMathematikAuswerter.erzeugeRegister()
    private fun eval(k:KnotenDaten,e:Map<String,BedingterWert> = emptyMap())=
        register.finde(k.art)!!.auswerten(KnotenAuswertungsKontext(k,e,RechenKontext()))
    private fun port(k:KnotenDaten,richtung:AnschlussRichtung,name:String?=null)=
        AnschlussVerweis(k.id,k.anschlüsse.single {it.richtung==richtung && (name==null || it.name==name)}.id)
    private fun richtung(n:Int,winkel:String="")=RichtungWinkelVorlage.erzeuge(GraphPunkt.Zero).let {it.copy(parameter=it.parameter+mapOf(RICHTUNG_WINKEL_ANZAHL to n.toString(),RICHTUNG_WINKEL_LISTE to winkel))}
    @Test fun `LaTeX Darstellung ist registriert und erhaelt den gesamten bezeugten Wert`() {
        val basis=BedingterWert(Tupel(listOf(RationaleZahl.Eins)),annahmen=setOf(ElementBeziehung(Variable("x"),ReelleZahlen)),
            variablenQuellen=listOf(VariablenQuelle(KnotenId("q"),"x",ReelleZahlen)),werteVorrat=Tupelraum(listOf(ReelleZahlen)),
            komponentenMengen=listOf(ReelleZahlen),latexDarstellung="alt")
        val trans=konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero),"spalte")
        val wert=eval(trans,mapOf("wert" to basis)).ausgaben.getValue("ergebnis")
        val k=MathematikKnotenVorlagen.Darstellungsoptimierung.erzeuge(GraphPunkt.Zero).copy(parameter=mapOf("latex" to " neu "))
        val r=eval(k,mapOf("wert" to wert))
        assertNull(r.fehler)
        assertEquals(setOf("wert"),r.bezeugteAusgaben)
        assertEquals(wert.copy(latexDarstellung="neu"),r.ausgaben.getValue("wert"))
        val fort=r.führeÜbersetzungsHerkunftFort(listOf(wert)).ausgaben.getValue("wert")
        assertSame(wert.objekt,fort.objekt)
        assertEquals(wert.übersetzungen,fort.übersetzungen)
        assertEquals(wert.annahmen,fort.annahmen)
        assertEquals(wert.variablenQuellen,fort.variablenQuellen)
        assertEquals(wert.komponentenMengen,fort.komponentenMengen)
        assertEquals(wert,eval(k.copy(parameter=mapOf("latex" to "  ")),mapOf("wert" to wert)).ausgaben.getValue("wert"))
        assertFails {eval(k)}
    }
    @Test fun `Richtungsquelle Darstellung und Rechnung behalten numerische Genauigkeit im Wire`() {
        val q=richtung(2,"15;30")
        val d=MathematikKnotenVorlagen.Darstellungsoptimierung.erzeuge(GraphPunkt.Zero).copy(parameter=mapOf("latex" to "u"))
        val norm=konfiguriereVektorRechner(VektorRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero),VektorRechnerOperator.NORM)
        val graph=KartenDaten(name="Genauigkeit",knoten=listOf(q,d,norm),verbindungen=listOf(
            VerbindungDaten(von=port(q,AnschlussRichtung.Ausgang),zu=port(d,AnschlussRichtung.Eingang)),
            VerbindungDaten(von=port(d,AnschlussRichtung.Ausgang),zu=port(norm,AnschlussRichtung.Eingang)),
        ))
        val r=KartenAuswerter(register).auswerten(graph)
        assertTrue(r.fehler.isEmpty(),r.fehler.joinToString())
        assertTrue(r.knoten.getValue(q.id).warnungen.any {it.contains("angenähert")})
        assertEquals("u",r.knoten.getValue(d.id).ausgaben.values.single().latexDarstellung)
        for(k in listOf(q,d,norm)) for(w in r.knoten.getValue(k.id).ausgaben.values) {
            assertNull(w.übersetzungen.aktuell)
            assertEquals(ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT,w.übersetzungsKontext().datenGenauigkeit)
            val out=w.alsÜbertragenerMathematischerWert()
            assertEquals(out,ÜbersetzungsCodec.dekodiereWert(ÜbersetzungsCodec.kodieren(out)))
            assertEquals(ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT,out.übersetzungen.kontext!!.datenGenauigkeit)
        }
        val exakt=eval(richtung(0)).ausgaben.values.single()
        assertEquals(SpaltenVektor(listOf(RationaleZahl.Eins)),exakt.objekt)
        assertNotEquals(ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT,exakt.übersetzungsKontext().datenGenauigkeit)
        assertTrue(eval(richtung(0)).warnungen.isEmpty())
    }
    @Test fun `Literal und Operationssnapshot behalten Genauigkeit bei Wire und erneuter Auswertung`() {
        val approx=ÜbersetzungsMetadaten(kontext=ÜbersetzungsKontext(datenGenauigkeit=ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT))
        val eingang=BedingterWert(RationaleZahl.Eins,übersetzungen=approx)
        val literal=FormelAusdruck.Literal("l",RationaleZahl.Eins,FormelTyp.ZAHL)
        val snapshot=GebundeneFormel(literal,emptyList(),emptyMap())
        val wert=BedingterWert(RationaleZahl.Eins,gebundeneFormel=snapshot)
        val fort=KnotenAuswertungsErgebnis(mapOf("wert" to wert),bezeugteAusgaben=setOf("wert"))
            .führeÜbersetzungsHerkunftFort(listOf(eingang)).ausgaben.getValue("wert")
        val literalWire=ÜbersetzungsCodec.dekodiereWert(ÜbersetzungsCodec.kodieren(fort.alsÜbertragenerMathematischerWert()))
        assertEquals(ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT,literalWire.formel!!.werteAus().übersetzungen.kontext!!.datenGenauigkeit)
        assertNull(literalWire.formel!!.werteAus().übersetzungen.aktuell)
        val operation=FormelAusdruck.Operation("op","zahl.addition",listOf(
            FormelArgument("a",0,literal.copy(übersetzungen=approx)),
            FormelArgument("b",1,FormelAusdruck.Literal("r",RationaleZahl.Eins,FormelTyp.ZAHL)),
        ),FormelTyp.ZAHL)
        val formel=GebundeneFormel(operation,emptyList(),emptyMap())
        val out=ÜbertragenerMathematischerWert(RationaleZahl.von(2),approx,formel)
        val decoded=ÜbersetzungsCodec.dekodiereWert(ÜbersetzungsCodec.kodieren(out))
        val ergebnis=decoded.formel!!.werteAus()
        assertEquals(RationaleZahl.von(2),ergebnis.objekt)
        assertEquals(ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT,ergebnis.übersetzungen.kontext!!.datenGenauigkeit)
        assertNull(ergebnis.übersetzungen.aktuell)
        assertTrue(ergebnis.übersetzungen.herkunft.isEmpty())
    }
    @Test fun `neue Rechnung behaelt nur Genauigkeit und Herkunft statt alter Formkontexte`() {
        val kontext=ÜbersetzungsKontext(definitionsRaum=ReelleZahlen,komponentenRäume=listOf(ReelleZahlen),datenGenauigkeit=ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT)
        val alt=BedingterWert(RationaleZahl.Eins,übersetzungen=ÜbersetzungsMetadaten(kontext=kontext))
        val neu=KnotenAuswertungsErgebnis(mapOf("x" to alt.copy(objekt=RationaleZahl.von(2))))
            .führeÜbersetzungsHerkunftFort(listOf(alt)).ausgaben.getValue("x")
        assertEquals(ÜbersetzungsKontext(datenGenauigkeit=ÜbersetzungsGenauigkeit.NUMERISCH_ANGENÄHERT),neu.übersetzungen.kontext)
        val pass=KnotenAuswertungsErgebnis(mapOf("x" to alt),bezeugteAusgaben=setOf("x"))
            .führeÜbersetzungsHerkunftFort(listOf(alt)).ausgaben.getValue("x")
        assertEquals(kontext,pass.übersetzungen.kontext)
    }
    @Test fun `Richtungsgrenzen stimmen mit Winkelraum ueberein und ungueltige Werte werden abgewiesen`() {
        for(n in listOf(0,31,32,MAXIMALE_RICHTUNGS_WINKEL)) {
            val w=assertIs<SpaltenVektor>(eval(richtung(n)).ausgaben.values.single().objekt)
            assertEquals(n+1,w.werte.size)
        }
        for((n,w) in listOf(-1 to "",65 to "",1 to "180",1 to "NaN",1 to "1;2")) assertFails {eval(richtung(n,w))}
        assertFails {eval(richtung(0).let {it.copy(parameter=it.parameter+(RICHTUNG_WINKEL_VORZEICHEN to "0"))})}
    }
    @Test fun `Begriffe beschreiben existierende Vertraege ohne historische Referenzen zu brechen`() {
        val namen=listOf(
            MathematikKnotenVorlagen.Darstellungsoptimierung to ("LaTeX-Darstellung" to "Darstellungsoptimierung"),
            MengenMassKnotenVorlagen.MengenNorm to ("Maß einer Menge" to "Norm einer Menge"),
            MengenraumKnotenVorlagen.Vektorraum to ("Tensorträgermenge erster Stufe" to "Vektorraum Aⁿ"),
            StrukturRechnerKnotenVorlagen.Cauchy to ("Nichtstandard-Cauchyprüfung" to "Cauchy"),
        )
        for((v,n) in namen) {
            assertEquals(n.first,v.name)
            assertEquals(v.copy(name=n.second).stabileKonzeptId(),v.stabileKonzeptId())
            assertEquals(v.copy(name=n.second).stabileVariantenId(),v.stabileVariantenId())
        }
        val raum=eval(MengenraumKnotenVorlagen.Vektorraum.erzeuge(GraphPunkt.Zero),mapOf("grundmenge" to BedingterWert(ReelleZahlen))).ausgaben.values.single().objekt
        assertIs<Tensorraum>(raum)
        assertEquals(listOf(RationaleZahl.von(3)),raum.dimensionen)
    }
    @Test fun `Mass bewahrt Messbarkeitsvoraussetzung und Cauchy fordert eindeutige Metrik`() {
        val k=MengenMassKnotenVorlagen.MengenNorm.erzeuge(GraphPunkt.Zero)
        val a=BenannteMenge("A")
        val r=eval(k,mapOf("menge" to BedingterWert(a),"maß" to BedingterWert(IntegralMass.Allgemein("\\mu"))))
        val fach=normEinerMenge(a,IntegralMass.Allgemein("\\mu"))
        assertEquals(fach.wert,r.ausgaben.values.single().objekt)
        assertTrue(r.ausgaben.values.single().annahmen.containsAll(fach.voraussetzungen))
        assertTrue(fach.voraussetzungen.isNotEmpty())
        val x=Variable("n")
        val u=UnnatuerlichesKartesischesTupel("u","u",GanzeZahlen,Methode("u",listOf(x),x,GanzeZahlen,mapOf("n" to NatürlicheZahlen)))
        assertIs<CauchyErgebnis.NichtAnwendbar>(pruefeCauchy(u))
        assertTrue(assertFails {eval(StrukturRechnerKnotenVorlagen.Cauchy.erzeuge(GraphPunkt.Zero),mapOf("tupel" to BedingterWert(u)))}.message!!.contains("Metrik"))
    }
}
