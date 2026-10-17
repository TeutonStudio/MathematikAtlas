package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.konzeptknoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class QuellenKonsolidierungTest {
    private val register=GesamterMathematikAuswerter.erzeugeRegister()
    private fun parameter(bereich:WertebereichKonfiguration=WertebereichKonfiguration.Zahl(),name:String="a")=
        konfiguriereAllgemeinenParameterBereich(MathematikKnotenVorlagen.AllgemeinerParameter.erzeuge(GraphPunkt.Zero).let {it.copy(parameter=it.parameter+("name" to name))},bereich)
    private fun wert(k:KnotenDaten,eingänge:Map<String,BedingterWert> = emptyMap()):BedingterWert {
        val out=register.finde(k.art)!!.auswerten(KnotenAuswertungsKontext(k,eingänge,RechenKontext()))
        assertNull(out.fehler)
        return out.ausgaben.values.single()
    }
    private fun methode(eingang:BedingterWert):MathematischeMethode {
        val k=konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero),"methode")
        return assertIs<MathematischeMethode>(wert(k,mapOf("wert" to eingang)).objekt)
    }
    private fun phasen(a:KartenDaten):List<KartenDaten> {
        val text=KartenDatenJson.schreibe(a)
        return listOf(MathematikKartenCodec.lese(text),MathematikKartenCodec.lade(text),MathematikKartenCodec.importiere(text),MathematikKartenMigrationen.vorSpeichern(a))
    }
    @Test fun `gemeinsame Grundrauminformation erhaelt unterschiedliche Quellenrollen und Darstellungen`() {
        for(bereich in listOf(WertebereichKonfiguration.Zahl(),WertebereichKonfiguration.Tupel(),WertebereichKonfiguration.Vektor(),WertebereichKonfiguration.Matrix())) {
            val p=parameter(bereich);val pw=wert(p);val raum=bereich.zuMenge()
            val v=MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero)
            val vw=wert(v,mapOf("grundraum" to BedingterWert(raum)))
            assertEquals(raum,pw.werteVorrat);assertEquals(raum,vw.werteVorrat)
            assertEquals(LösungsQuellenRolle.Parameter,pw.variablenQuellen.single().lösungsRolle)
            assertEquals(LösungsQuellenRolle.Variable,vw.variablenQuellen.single().lösungsRolle)
            assertEquals(raum.elementTypAusdruck(),quellenGrundraumInformation(raum).typ)
            when(bereich) {
                is WertebereichKonfiguration.Tupel -> {assertIs<AllgemeinerParameter>(pw.objekt);assertIs<Tupel>(vw.objekt)}
                is WertebereichKonfiguration.Vektor -> {assertIs<TypisiertesElement>(pw.objekt);assertIs<SpaltenVektor>(vw.objekt)}
                is WertebereichKonfiguration.Matrix -> {assertIs<TypisiertesElement>(pw.objekt);assertIs<Matrix>(vw.objekt)}
                else -> {assertIs<Variable>(pw.objekt);assertIs<Variable>(vw.objekt)}
            }
        }
    }
    @Test fun `Umbenennen erhaelt Quellenidentitaet und Clipboardkopie erzeugt eigene Quelle`() {
        for(bereich in listOf(WertebereichKonfiguration.Zahl(),WertebereichKonfiguration.Tupel(),WertebereichKonfiguration.Vektor(),WertebereichKonfiguration.Matrix())) {
            val p=parameter(bereich);val alt=wert(p)
            val umbenannt=p.copy(parameter=p.parameter+("name" to "b"))
            val neu=wert(umbenannt)
            assertEquals(alt.variablenQuellen.single().identität,neu.variablenQuellen.single().identität)
            assertEquals(alt.variablenQuellen.single().strukturBindung!!.id,neu.variablenQuellen.single().strukturBindung!!.id)
            if(alt.objekt is TypisiertesElement) assertEquals((alt.objekt as TypisiertesElement).name,(neu.objekt as TypisiertesElement).name)
            val clipboard=AtlasZwischenablage();assertTrue(clipboard.kopiere(KartenDaten(name="Kopie",knoten=listOf(p)),setOf(p.id)))
            val kopie=clipboard.erzeugeEinfügung(GraphPunkt.Zero)!!.knoten.single()
            assertNotEquals(p.id,kopie.id)
            assertNotEquals(alt.variablenQuellen.single().identität,wert(kopie).variablenQuellen.single().identität)
        }
        val v=MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero)
        val erster=wert(v,mapOf("grundraum" to BedingterWert(ReelleZahlen)))
        val zweiter=wert(v.copy(parameter=v.parameter+("name" to "y")),mapOf("grundraum" to BedingterWert(ReelleZahlen)))
        assertEquals(erster.variablenQuellen.single().identität,zweiter.variablenQuellen.single().identität)
    }
    @Test fun `gleichnamige Parameter und externe Quelle bleiben nach ID getrennt bindbar`() {
        val a=wert(parameter());val b=wert(parameter(WertebereichKonfiguration.Zahl("C")))
        val term=BedingterWert(Tupel(listOf(a.objekt as MathematischesObjekt,b.objekt as MathematischesObjekt)),variablenQuellen=a.variablenQuellen+b.variablenQuellen)
        val f=methode(term)
        assertEquals(2,f.parameter.size);assertEquals(2,f.parameter.map {it.name}.distinct().size)
        val ai=a.variablenQuellen.single().strukturBindung!!.id;val bi=b.variablenQuellen.single().strukturBindung!!.id
        assertEquals(setOf(ai,bi),f.argumentIdentitäten.values.toSet())
        val resultat=f.wendeMitIdentitätenAn(mapOf(ai to RationaleZahl.von(2),bi to KomplexeZahl(RationaleZahl.Null,RationaleZahl.Eins))).ergebnis
        assertEquals(Tupel(listOf(Tupel(listOf(RationaleZahl.von(2),KomplexeZahl(RationaleZahl.Null,RationaleZahl.Eins))))),resultat)
        val extern=Variable("a")
        val gemischt=methode(BedingterWert(Tupel(listOf(a.objekt as MathematischesObjekt,extern)),variablenQuellen=a.variablenQuellen+
            VariablenQuelle(KnotenId("extern"),"a",ReelleZahlen)))
        assertEquals(2,gemischt.parameter.size);assertEquals(setOf(ai,"extern"),gemischt.argumentIdentitäten.values.toSet())
        assertEquals(2,gemischt.parameter.map {it.name}.distinct().size)
    }
    @Test fun `historische Argumentidentitaet allgemeiner Parameter bleibt erhalten`() {
        val k=parameter()
        val historisch=BedingterWert(Variable("a"),werteVorrat=ReelleZahlen,
            variablenQuellen=listOf(VariablenQuelle(k.id,"a",ReelleZahlen,lösungsRolle=LösungsQuellenRolle.Parameter)))
        assertEquals(mapOf("a" to k.id.wert),methode(historisch).argumentIdentitäten)
        assertEquals(methode(historisch).argumentIdentitäten,methode(wert(k)).argumentIdentitäten)
        assertEquals(k.id.wert,wert(k).variablenQuellen.single().bindungsId)
    }
    @Test fun `strukturierter allgemeiner Parameter bleibt ein logisches Argument`() {
        for((bereich,konkret) in listOf(
            WertebereichKonfiguration.Tupel() to Tupel(listOf(RationaleZahl.Eins,RationaleZahl.von(2))),
            WertebereichKonfiguration.Tupel(emptyList()) to Tupel(emptyList()),
            WertebereichKonfiguration.Vektor() to SpaltenVektor(listOf(RationaleZahl.Eins,RationaleZahl.von(2))),
            WertebereichKonfiguration.Matrix() to Matrix(listOf(listOf(RationaleZahl.Eins,RationaleZahl.Null),listOf(RationaleZahl.Null,RationaleZahl.Eins))))) {
            val p=wert(parameter(bereich));val f=methode(p)
            assertEquals(1,f.parameter.size)
            assertEquals(bereich.zuMenge(),f.werteVorräte.values.single())
            assertEquals(Tupel(listOf(konkret)),f.wendeMitIdentitätenAn(mapOf(f.argumentIdentitäten.values.single() to konkret)).ergebnis)
        }
    }
    @Test fun `gleichnamige Variable und Parameter werden beim Aufloesen nicht gleichgesetzt`() {
        val v=MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero).let {it.copy(parameter=it.parameter+("name" to "a"))}
        val vw=wert(v,mapOf("grundraum" to BedingterWert(ReelleZahlen)));val pw=wert(parameter())
        val relation=BedingterWert(Gleichheit(vw.objekt as ZahlAusdruck,pw.objekt as ZahlAusdruck),variablenQuellen=vw.variablenQuellen+pw.variablenQuellen)
        val k=MathematikKnotenVorlagen.GleichungLösen.erzeuge(GraphPunkt.Zero)
        val out=register.finde(k.art)!!.auswerten(KnotenAuswertungsKontext(k,mapOf("relation" to relation),RechenKontext()))
        assertEquals(1,out.lösungsVariablen.size);assertEquals(1,out.lösungsParameter.size)
        assertNotEquals(out.lösungsVariablen.single().variable.name,out.lösungsParameter.single().name)
        assertEquals(pw.variablenQuellen.single().bindungsId,out.lösungsParameter.single().bindungsId)
    }
    @Test fun `strukturierte Quellen besitzen selbsttragenden Wire und Formelbindungsvertrag`() {
        for((bereich,konkret) in listOf(
            WertebereichKonfiguration.Tupel() to Tupel(listOf(RationaleZahl.Eins,RationaleZahl.von(2))),
            WertebereichKonfiguration.Tupel(emptyList()) to Tupel(emptyList()),
            WertebereichKonfiguration.Vektor() to SpaltenVektor(listOf(RationaleZahl.Eins,RationaleZahl.von(2))),
            WertebereichKonfiguration.Matrix() to Matrix(listOf(listOf(RationaleZahl.Eins,RationaleZahl.Null),listOf(RationaleZahl.Null,RationaleZahl.Eins))))) {
            val p=wert(parameter(bereich))
            val export=p.alsÜbertragenerMathematischerWert()
            val formel=assertNotNull(export.formel)
            val id=p.variablenQuellen.single().strukturBindung!!.id
            assertEquals(id,formel.quellen.single().id)
            assertEquals(konkret,formel.werteAus(mapOf(id to konkret)).objekt)
            val zurück=ÜbersetzungsCodec.dekodiereWert(ÜbersetzungsCodec.kodieren(export))
            assertEquals(p.objekt,zurück.wert)
            val zurückFormel=assertNotNull(zurück.formel)
            assertEquals(konkret,zurückFormel.werteAus(mapOf(id to konkret)).objekt)
            val f=zurückFormel.alsMethode("f")
            assertEquals(Tupelraum(listOf(bereich.zuMenge())),f.zielRaum)
            assertEquals(1,f.signatur.ergebnisse.size)
            assertEquals(Tupel(listOf(konkret)),f.wendeMitIdentitätenAn(mapOf(id to konkret)).ergebnis)
            assertFails {f.wendeMitIdentitätenAn(mapOf(id to RationaleZahl.Eins))}
        }
    }
    @Test fun `Wire Formel und Methodenbildung halten gleichnamige C und R Quellen getrennt`() {
        val komplex=wert(parameter(WertebereichKonfiguration.Zahl("C")))
        val reell=wert(parameter(WertebereichKonfiguration.Zahl("R")))
        val term=BedingterWert(addition(komplex.objekt as ZahlAusdruck,reell.objekt as ZahlAusdruck),
            variablenQuellen=komplex.variablenQuellen+reell.variablenQuellen)
        val export=ÜbersetzungsCodec.dekodiereWert(ÜbersetzungsCodec.kodieren(term.alsÜbertragenerMathematischerWert()))
        val f=assertNotNull(export.formel).alsMethode("f")
        val cId=komplex.variablenQuellen.single().bindungsId!!
        val rId=reell.variablenQuellen.single().bindungsId!!
        assertEquals(setOf(cId,rId),f.argumentIdentitäten.values.toSet())
        assertEquals(2,f.parameter.map {it.name}.distinct().size)
        assertEquals(Tupelraum(listOf(KomplexeZahlen)),f.zielRaum)
        val i=KomplexeZahl(RationaleZahl.Null,RationaleZahl.Eins)
        assertEquals(Tupel(listOf(i)),f.wendeMitIdentitätenAn(mapOf(cId to i,rId to RationaleZahl.Null)).ergebnis)
    }
    @Test fun `sichtbarer Name darf fremde QuellenID nicht als Bereichsschluessel ueberschreiben`() {
        val komplexKnoten=parameter(WertebereichKonfiguration.Zahl("C"))
        val komplex=wert(komplexKnoten)
        val reell=wert(parameter(WertebereichKonfiguration.Zahl("R"),komplexKnoten.id.wert))
        assertEquals(KomplexeZahlen,listOf(komplex,reell).rechnerQuellBereiche()[komplexKnoten.id.wert])
        val term=BedingterWert(addition(komplex.objekt as ZahlAusdruck,reell.objekt as ZahlAusdruck),variablenQuellen=komplex.variablenQuellen+reell.variablenQuellen)
        val f=term.alsGebundeneErgebnisFormel().alsMethode("f")
        assertEquals(Tupelraum(listOf(KomplexeZahlen)),f.zielRaum)
    }
    @Test fun `Matrixvariable behaelt interne Identitaet und exportierte Komponentenbindung beim Umbenennen`() {
        val raum=Matrizenraum(2,2,ReelleZahlen)
        val basis=MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero)
        val alt=wert(basis,mapOf("grundraum" to BedingterWert(raum)))
        val neu=wert(basis.copy(parameter=basis.parameter+("name" to "B")),mapOf("grundraum" to BedingterWert(raum)))
        assertEquals(alt.variablenQuellen.single().identität,neu.variablenQuellen.single().identität)
        assertEquals(alt.variablenQuellen.single().name,neu.variablenQuellen.single().name)
        val id=neu.variablenQuellen.single().strukturBindung!!.id
        val konkret=Matrix(listOf(listOf(RationaleZahl.Eins,RationaleZahl.Null),listOf(RationaleZahl.Null,RationaleZahl.Eins)))
        val export=ÜbersetzungsCodec.dekodiereWert(ÜbersetzungsCodec.kodieren(neu.alsÜbertragenerMathematischerWert()))
        assertEquals(konkret,assertNotNull(export.formel).werteAus(mapOf(id to konkret)).objekt)
    }
    @Test fun `defekte Bereichsdaten bleiben in jeder Codecphase erhalten und sind nicht auswertbar`() {
        val basis=parameter()
        val ungültig=listOf<KnotenEigenschaft>(KnotenEigenschaft.Text("defekt"),KnotenEigenschaft.Objekt(mapOf("art" to KnotenEigenschaft.Text("unbekannt"))),
            KnotenEigenschaft.Objekt(mapOf("art" to KnotenEigenschaft.Text("vektor"),"dimension" to KnotenEigenschaft.Text("2"))),
            KnotenEigenschaft.Objekt(mapOf("art" to KnotenEigenschaft.Text("vektor"),"orientierung" to KnotenEigenschaft.Text("unbekannt"))),
            KnotenEigenschaft.Objekt(mapOf("art" to KnotenEigenschaft.Text("zahl"),"grundmenge" to KnotenEigenschaft.Text("unbekannt"))),
            KnotenEigenschaft.Objekt(mapOf("art" to KnotenEigenschaft.Text("tupel"),"komponenten" to KnotenEigenschaft.Liste(emptyList()))))
        for(config in ungültig) {
            val alt=basis.copy(eigenschaften=basis.eigenschaften+(WertebereichKonfiguration.EIGENSCHAFT to config))
            for(karte in phasen(KartenDaten(name="Alt",knoten=listOf(alt)))) {
                val neu=karte.knoten.single();assertEquals(alt,neu.copy(parameter=neu.parameter-RECHNER_MIGRATION_DIAGNOSE))
                assertTrue(neu.parameter.getValue(RECHNER_MIGRATION_DIAGNOSE).startsWith(QUELLEN_BEREICH_DIAGNOSE_PREFIX))
                assertEquals(karte,karte.diagnostiziereQuellenBereiche())
                assertNotNull(KartenAuswerter(register).auswerten(karte).knoten.getValue(neu.id).fehler)
                val repariert=konfiguriereAllgemeinenParameterBereich(neu,WertebereichKonfiguration.Tupel(emptyList()))
                assertFalse(RECHNER_MIGRATION_DIAGNOSE in repariert.parameter)
                assertEquals(Tupelraum(emptyList()),wert(repariert).werteVorrat)
            }
        }
        assertEquals(WertebereichKonfiguration.Standard,WertebereichKonfiguration.vonEigenschaft(null))
        assertEquals(WertebereichKonfiguration.Standard,WertebereichKonfiguration.vonEigenschaft(KnotenEigenschaft.Objekt(mapOf("art" to KnotenEigenschaft.Text("zahl")))))
        val leer=WertebereichKonfiguration.Tupel(emptyList())
        assertEquals(leer,WertebereichKonfiguration.vonEigenschaft(leer.zuEigenschaft()))
    }
    @Test fun `Bereichsreparatur loest keine unabhaengige Identitaetsdiagnose`() {
        val p=parameter()
        val original=KartenDaten(name="Doppelte ID",knoten=listOf(p,p.copy(name="Zweite Quelle")))
        val diagnostiziert=MathematikKartenCodec.lade(KartenDatenJson.schreibe(original))
        assertEquals(2,diagnostiziert.knoten.size)
        val neu=diagnostiziert.knoten.map {k ->
            assertTrue(k.parameter.getValue(RECHNER_MIGRATION_DIAGNOSE).startsWith(QUELLEN_IDENTITAET_DIAGNOSE_PREFIX))
            konfiguriereAllgemeinenParameterBereich(k,WertebereichKonfiguration.Matrix())
        }
        assertTrue(neu.all {it.parameter[RECHNER_MIGRATION_DIAGNOSE]?.startsWith(QUELLEN_IDENTITAET_DIAGNOSE_PREFIX)==true})
        assertNotNull(KartenAuswerter(register).auswerten(diagnostiziert.copy(knoten=neu)).knoten.getValue(p.id).fehler)
    }
    @Test fun `ungueltige aktive historische Variablenbereiche werden vor Portnormalisierung bewahrt`() {
        val basis=MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero).let {it.copy(eigenschaften=emptyMap(),anschlüsse=it.anschlüsse.filter {p->p.richtung==AnschlussRichtung.Ausgang})}
        for(parameter in listOf(mapOf("werteVorrat" to "?"),mapOf(VARIABLE_STRUKTUR to "?"),mapOf(VARIABLE_STRUKTUR to "tupel",VARIABLE_DIMENSION to "?"),
            mapOf(VARIABLE_BEREICH_MODUS to "?"),mapOf(VARIABLE_BEREICH_MODUS to VARIABLE_BEREICH_INTERVALL,VARIABLE_INTERVALL_LINKS_OFFEN to "?"))) {
            val alt=basis.copy(parameter=basis.parameter+parameter)
            for(karte in phasen(KartenDaten(name="Alt",knoten=listOf(alt)))) {
                val neu=karte.knoten.single();assertEquals(alt,neu.copy(parameter=neu.parameter-RECHNER_MIGRATION_DIAGNOSE))
                assertNotNull(neu.parameter[RECHNER_MIGRATION_DIAGNOSE]);assertTrue(karte.verbindungen.isEmpty())
            }
        }
        val aktiv=MathematikKnotenVorlagen.Variable.erzeuge(GraphPunkt.Zero).let {it.copy(parameter=it.parameter+mapOf("werteVorrat" to "?",VARIABLE_DIMENSION to "?",VARIABLE_STRUKTUR to "?"))}
        assertEquals(aktiv,phasen(KartenDaten(name="Aktiv",knoten=listOf(aktiv))).first().knoten.single())
    }
    @Test fun `eingebettete defekte Quelle wird ebenfalls unveraendert diagnostiziert`() {
        val alt=parameter().let {it.copy(eigenschaften=it.eigenschaften+(WertebereichKonfiguration.EIGENSCHAFT to KnotenEigenschaft.Text("defekt")))}
        val text=KartenDatenJson.schreibe(KartenDaten(name="Innen",knoten=listOf(alt)))
        val host=MethodenQuellKnotenVorlagen.Methode.erzeuge(GraphPunkt.Zero).let {it.copy(eigenschaften=it.eigenschaften+(METHODEN_QUELLE_KNOTEN to KnotenEigenschaft.Text(text)))}
        for(phase in phasen(KartenDaten(name="Außen",knoten=listOf(host)))) {
            val außen=phase.knoten.single()
            val innen=KartenDatenJson.lese((außen.eigenschaften.getValue(METHODEN_QUELLE_KNOTEN) as KnotenEigenschaft.Text).wert).knoten.single()
            assertEquals(alt,innen.copy(parameter=innen.parameter-RECHNER_MIGRATION_DIAGNOSE))
            assertNotNull(innen.parameter[RECHNER_MIGRATION_DIAGNOSE])
            assertEquals(host.id,außen.id)
        }
    }
    @Test fun `Bereichswechsel und Reparatur sind ein Undo Schritt und Tiefe wird ganzheitlich validiert`() {
        val p=parameter();val editor=KartenEditorZustand(KartenDaten(name="Bereich",knoten=listOf(p)),GraphPrüfung(AnschlussArtRegister(MathematikAnschlussArten.alle)))
        val matrix=konfiguriereAllgemeinenParameterBereich(p,WertebereichKonfiguration.Matrix())
        val laufzeit=MathematikKartenLaufzeit()
        val plan=planeKonfiguration(editor.karte,matrix,laufzeit.graphPrüfung,laufzeit::auswerten)
        editor.führeAus(KartenAktion.KartenInhaltErsetzen(plan.karte))
        assertEquals(matrix,editor.karte.knoten.single());editor.rückgängig();assertEquals(p,editor.karte.knoten.single())
        var tief:WertebereichKonfiguration=WertebereichKonfiguration.Zahl()
        repeat(63) {tief=WertebereichKonfiguration.Menge(tief)}
        val defekt=p.copy(eigenschaften=p.eigenschaften+(WertebereichKonfiguration.EIGENSCHAFT to KnotenEigenschaft.Text("defekt")))
        val original=phasen(KartenDaten(name="Reparatur",knoten=listOf(defekt))).first()
        val reparaturEditor=KartenEditorZustand(original,laufzeit.graphPrüfung)
        val repariert=konfiguriereAllgemeinenParameterBereich(original.knoten.single(),WertebereichKonfiguration.Tupel(emptyList()))
        val reparaturPlan=planeKonfiguration(original,repariert,laufzeit.graphPrüfung,laufzeit::auswerten)
        reparaturEditor.führeAus(KartenAktion.KartenInhaltErsetzen(reparaturPlan.karte))
        assertFalse(RECHNER_MIGRATION_DIAGNOSE in reparaturEditor.karte.knoten.single().parameter)
        reparaturEditor.rückgängig();assertEquals(original,reparaturEditor.karte)
        assertNotNull(konfiguriereAllgemeinenParameterBereich(p,tief))
        assertFails {konfiguriereAllgemeinenParameterBereich(p,WertebereichKonfiguration.Menge(tief))}
    }
    @Test fun `Falsch bleibt nullstellig und historische Konzeptreferenzen bleiben aufloesbar`() {
        val v=MathematikKnotenVorlagen.Lüge
        assertEquals("mathematik.lüge",v.art);assertEquals("Falsch",v.name)
        assertTrue(v.anschlüsse.none {it.richtung==AnschlussRichtung.Eingang})
        assertEquals("mathematik.lüge|Lüge",v.stabileVariantenId().wert)
        assertEquals("\\mathcal{Falsch}",wert(v.erzeuge(GraphPunkt.Zero)).objekt.let {(it as WahrheitsKonstante).zuLatex()})
        val konzepte=KonzeptKnotenRegister.erstelle(alleMathematikDefinitionsVorlagen())
        val falsch=konzepte.single {it.id.wert=="mathematik.lüge|Lüge|"}
        assertEquals("Falsch",falsch.titel);assertTrue("Lüge" in falsch.aliase);assertTrue(falsch.karten.isNotEmpty())
    }
}
