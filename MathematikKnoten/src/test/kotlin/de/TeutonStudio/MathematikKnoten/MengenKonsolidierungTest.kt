package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.katalog.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import de.TeutonStudio.TypSystem.TypInferenzRegel
import kotlin.test.*

class MengenKonsolidierungTest {
    private val register=GesamterMathematikAuswerter.erzeugeRegister()
    private fun graph(n:Int)=konfiguriereEndlicheMenge(MathematikKnotenVorlagen.EndlicheMenge.erzeuge(GraphPunkt.Zero),ENDLICHE_MENGE_GRAPH,n)
    private fun eval(k:KnotenDaten,werte:Map<String,BedingterWert>)=register.findeNachMigration(k.art).auswerten(KnotenAuswertungsKontext(k,werte,RechenKontext()))
    private fun z(n:Long)=RationaleZahl.von(n)
    private val altVorlagen get()=listOf(MathematikKnotenVorlagen.Einzelmenge,MengenraumKnotenVorlagen.Potenzmenge,MengenraumKnotenVorlagen.Abbildungsmenge)
    private fun phasen(k:KartenDaten)=listOf(MathematikKartenMigrationen::nachDekodierung,MathematikKartenMigrationen::nachLaden,MathematikKartenMigrationen::vorSpeichern,
        {a:KartenDaten->MathematikKartenCodec.lese(KartenDatenJson.schreibe(a))},
        {a:KartenDaten->MathematikKartenCodec.lade(KartenDatenJson.schreibe(a))},
        {a:KartenDaten->MathematikKartenCodec.importiere(KartenDatenJson.schreibe(a))}).map {it(k)}

    @Test fun `alle Mengenaltarten erhalten Identitaeten Rollen Defaults und feste Kartenfallbacks in jeder Phase`() {
        for(v in altVorlagen) {
            val basis=v.erzeuge(GraphPunkt.Zero)
            val input=basis.anschlüsse.first {it.richtung==AnschlussRichtung.Eingang}
            val ziel=when(input.name) {"element"->"element1";"grundmenge"->"a";else->input.name}
            val extern=KartenVerweis(KartenId("extern"),7)
            val alt=basis.copy(name="Meine Menge",parameter=basis.parameter+("standardwert.${input.name}" to "3"),
                eingangsKartenVerweise=mapOf(input.name to extern),
                anschlüsse=basis.anschlüsse.map {if(it.richtung==AnschlussRichtung.Ausgang && basis.art!="mathematik.einzelmenge") it.copy(artFolgtEingang=input.name,typInferenz=TypInferenzRegel.FolgtEingang(input.name)) else it})
            val kante=VerbindungDaten(von=AnschlussVerweis(KnotenId("quelle"),AnschlussId("aus")),zu=AnschlussVerweis(alt.id,input.id),eingangsReihenfolge=6,eingangsFallback=input.name+"@karte:"+input.id.wert)
            for(neu in phasen(KartenDaten(name="Alt",knoten=listOf(alt),verbindungen=listOf(kante)))) {
                val k=neu.knoten.single()
                assertFalse(k.art in mengenKonsolidierungAltarten,k.parameter[RECHNER_MIGRATION_DIAGNOSE])
                assertEquals(alt.id,k.id);assertEquals(alt.name,k.name)
                assertEquals(alt.anschlüsse.map {it.id}.toSet(),k.anschlüsse.map {it.id}.toSet())
                assertEquals("3",k.parameter["standardwert.$ziel"])
                assertEquals(mapOf(ziel to extern),k.eingangsKartenVerweise)
                if(basis.art!="mathematik.einzelmenge") assertEquals(ziel,k.anschlüsse.single {it.richtung==AnschlussRichtung.Ausgang}.artFolgtEingang)
                assertEquals(kante.copy(eingangsFallback=ziel+"@karte:"+input.id.wert),neu.verbindungen.single())
                assertEquals(neu,neu.migriereMengenKonsolidierung())
                assertEquals(neu,MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(neu)))
            }
        }
    }

    @Test fun `mehrdeutige Mengenaltarten bleiben mit Diagnose unveraendert und unauswertbar`() {
        for(v in altVorlagen) {
            val basis=v.erzeuge(GraphPunkt.Zero)
            val defekte=listOf(basis.copy(anschlüsse=basis.anschlüsse+ basis.anschlüsse.first().copy(id=AnschlussId("extra"))),
                basis.copy(anschlüsse=basis.anschlüsse.mapIndexed {i,p->if(i==0) p.copy(name="unbekannt") else p}),
                basis.copy(anschlüsse=basis.anschlüsse.mapIndexed {i,p->if(i==0) p.copy(belegung=AnschlussBelegung.Mehrfach) else p}))
            for(alt in defekte) for(neu in phasen(KartenDaten(name="Defekt",knoten=listOf(alt)))) {
                val k=neu.knoten.single();assertNotNull(k.parameter[RECHNER_MIGRATION_DIAGNOSE])
                assertEquals(alt,k.copy(parameter=alt.parameter))
                assertTrue(eval(k,emptyMap()).ausgaben.isEmpty());assertNotNull(eval(k,emptyMap()).fehler)
            }
        }
    }

    @Test fun `Graphmodus unterscheidet ausdruecklich leere Menge Pflichtwert und ganze Strukturwerte`() {
        assertEquals(LeereMenge,eval(graph(0),emptyMap()).ausgaben.getValue("menge").objekt)
        assertNotNull(eval(graph(1),emptyMap()).fehler)
        val methode=MathematischeMethode("f",listOf(Variable("x")),Variable("x"),ReelleZahlen)
        for(objekt in listOf(Tupel(emptyList()),Tupel(listOf(z(1),Tupel(listOf(z(2))))),EndlicheMenge(setOf(z(3))),methode)) {
            val r=eval(graph(1),mapOf("element1" to BedingterWert(objekt)))
            assertNull(r.fehler);assertEquals(EndlicheMenge(setOf(objekt)),r.ausgaben.getValue("menge").objekt)
        }
        assertNotNull(eval(graph(1),mapOf("element1" to BedingterWert(object:AtlasWert{}))).fehler)
    }

    @Test fun `Deduplikation verliert keine Voraussetzungen Quellidentitaeten oder Uebersetzungsherkunft`() {
        val x=Variable("x",bindungsId="a")
        val qa=VariablenQuelle(KnotenId("a"),"x",ReelleZahlen,bindungsId="a")
        val qb=VariablenQuelle(KnotenId("b"),"x",KomplexeZahlen,bindungsId="b")
        val bed=Ungleichheit(x,z(0))
        val r=eval(graph(2),mapOf("element1" to BedingterWert(z(1),variablenQuellen=listOf(qa)),
            "element2" to BedingterWert(z(1),annahmen=setOf(bed),variablenQuellen=listOf(qb))))
        assertNull(r.fehler)
        val out=r.ausgaben.getValue("menge")
        assertEquals(EndlicheMenge(setOf(z(1))),out.objekt)
        assertEquals(listOf(qa,qb),out.variablenQuellen);assertTrue(bed in out.annahmen);assertTrue(r.warnungen.isNotEmpty())
        assertFalse("x" in out.reelleVariablen)
        assertNotNull(eval(graph(1),mapOf("element1" to BedingterWert(z(1),annahmen=setOf(Ungleichheit(z(1),z(1)))))).fehler)
        assertNotNull(eval(graph(0),mapOf("fremd" to BedingterWert(x,annahmen=setOf(bed),variablenQuellen=listOf(qa)))).fehler)
    }

    @Test fun `inaktive lokale Eintraege und alte Operatorparameter veraendern keine Graphports`() {
        val k=graph(2).copy(parameter=graph(2).parameter+mapOf(ENDLICHE_MENGE_KONFIGURATION_PARAMETER to "defekt","operator" to "menge.vereinigung"))
        val neu=MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(KartenDaten(name="Graph",knoten=listOf(k)))).knoten.single()
        assertEquals(k.anschlüsse,neu.anschlüsse)
        assertNull(eval(neu,mapOf("element1" to BedingterWert(z(1)),"element2" to BedingterWert(z(2)))).fehler)
        assertNotNull(eval(neu.copy(parameter=neu.parameter+(ENDLICHE_MENGE_ANZAHL to "unbekannt")),emptyMap()).fehler)
        assertNotNull(eval(neu.copy(parameter=neu.parameter+(ENDLICHE_MENGE_EINGABE to "unbekannt")),emptyMap()).fehler)
    }

    @Test fun `Mengenoperatoren bewahren leere und symbolische Grundmengen ohne Bildmengenverwechslung`() {
        for(a in listOf<MengenAusdruck>(LeereMenge,ReelleZahlen,EndlicheMenge(setOf(z(1))))) {
            val pot=konfiguriereMengenRechner(MengenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero),MengenRechnerOperator.POTENZMENGE)
            assertEquals(Potenzmenge(a),eval(pot,mapOf("a" to BedingterWert(a))).ausgaben.getValue("menge").objekt)
            for(b in listOf<MengenAusdruck>(LeereMenge,ReelleZahlen)) {
                val abb=konfiguriereMengenRechner(pot,MengenRechnerOperator.ABBILDUNGSMENGE)
                assertEquals(Abbildungsmenge(a,b),eval(abb,mapOf("zielmenge" to BedingterWert(a),"argumentmenge" to BedingterWert(b))).ausgaben.getValue("menge").objekt)
            }
        }
    }

    @Test fun `Mengenbegriffe erzeugen unmittelbar kanonische Konfigurationen`() {
        val katalog=KanonischerMathematikKnotenKatalog.alle()
        assertEquals(120,katalog.map {it.art}.toSet().size)
        assertTrue(katalog.none {it.art in mengenKonsolidierungAltarten})
        assertTrue(alleMathematikDefinitionsVorlagen().none {it.art in mengenKonsolidierungAltarten})
        assertTrue(OperatorKnotenSuchindex.suche("Einzelmenge",katalog).any {it.art==ENDLICHE_MENGE_ART && it.standardParameter[ENDLICHE_MENGE_EINGABE]==ENDLICHE_MENGE_GRAPH && it.standardParameter[ENDLICHE_MENGE_ANZAHL]=="1"})
        for(id in listOf("menge.potenzmenge","menge.abbildungsmenge")) assertTrue(OperatorKnotenSuchindex.suche(id,katalog).any {it.art==MengenRechner.KNOTEN_ART && it.standardParameter[MENGENRECHNER_OPERATOR_PARAMETER]==id})
    }

    @Test fun `gespeicherte kanonische Graphkarten mit falschem Ausgangsvertrag werden diagnostiziert`() {
        val basis=graph(1)
        val output=basis.anschlüsse.single {it.richtung==AnschlussRichtung.Ausgang}
        val falsch=listOf(output.copy(art=MathematikAnschlussArten.Zahl.id),
            output.copy(vertrag=de.TeutonStudio.TypSystem.AnschlussVertrag(de.TeutonStudio.TypSystem.TypAusdruck.Atom(MathematischeTypen.Zahl))),
            output.copy(typInferenz=TypInferenzRegel.TupelAus(listOf("element1"))))
        val knoten=falsch.map {p->basis.copy(anschlüsse=basis.anschlüsse.map {if(it.id==output.id) p else it})}+
            basis.copy(anschlüsse=basis.anschlüsse+output.copy(id=AnschlussId("neutral"),richtung=AnschlussRichtung.Neutral))
        for(k in knoten) {
            val geladen=MathematikKartenCodec.lade(KartenDatenJson.schreibe(KartenDaten(name="Falscher Ausgang",knoten=listOf(k)))).knoten.single()
            val r=eval(geladen,mapOf("element1" to BedingterWert(z(1))))
            assertTrue(r.ausgaben.isEmpty());assertNotNull(r.fehler)
        }
    }

    @Test fun `acht echte Altkartefixtures erhalten alle Mengenidentitaeten in jeder Codecphase`() {
        val inventar=org.json.JSONArray(requireNotNull(javaClass.getResource("/konsolidierung-m4/originale.json")).readText())
        assertEquals(8,inventar.length())
        for(i in 0 until inventar.length()) {
            val datei=inventar.getJSONObject(i).getString("path").substringAfterLast('/')
            val text=requireNotNull(javaClass.getResource("/konsolidierung-m4/$datei")).readText()
            val alt=KartenDatenJson.lese(text)
            val betroffen=alt.knoten.filter {it.art in mengenKonsolidierungAltarten}
            assertEquals(1,betroffen.size)
            for(neu in phasen(alt)) {
                for(k in betroffen) {
                    val ziel=neu.knoten.single {it.id==k.id}
                    assertFalse(ziel.art in mengenKonsolidierungAltarten,ziel.parameter[RECHNER_MIGRATION_DIAGNOSE])
                    assertEquals(k.anschlüsse.map {it.id}.toSet(),ziel.anschlüsse.map {it.id}.toSet())
                    assertEquals(k.name,ziel.name)
                    val ids=k.anschlüsse.map {it.id}.toSet()
                    assertEquals(alt.verbindungen.filter {it.zu.knotenId==k.id && it.zu.anschlussId in ids || it.von.knotenId==k.id && it.von.anschlussId in ids}.map {it.id}.toSet(),
                        neu.verbindungen.filter {it.zu.knotenId==k.id || it.von.knotenId==k.id}.map {it.id}.toSet())
                }
                assertEquals(neu,neu.migriereMengenKonsolidierung())
            }
        }
    }

    @Test fun `Mengenkonstruktion behaelt Elementzeugnis nur als Herkunft und prueft semantische Elementtypen`() {
        val adapter=konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero),"abflachen")
        val übersetzt=eval(adapter,mapOf("wert" to BedingterWert(Tupel(listOf(Tupel(listOf(z(1),z(2)))))))) .ausgaben.getValue("ergebnis")
        val zeugnis=assertNotNull(übersetzt.übersetzungen.aktuell)
        val out=eval(graph(1),mapOf("element1" to übersetzt)).ausgaben.getValue("menge")
        assertNull(out.übersetzungen.aktuell);assertTrue(zeugnis in out.übersetzungen.herkunft)
        val zahlenMenge=graph(1).let {k->k.copy(anschlüsse=k.anschlüsse.map {p->if(p.richtung!=AnschlussRichtung.Ausgang) p else p.copy(
            vertrag=de.TeutonStudio.TypSystem.AnschlussVertrag(de.TeutonStudio.TypSystem.TypAusdruck.Parameterisiert(MathematischeTypen.Menge,listOf(de.TeutonStudio.TypSystem.TypAusdruck.Atom(MathematischeTypen.Zahl)))))})}
        assertNotNull(eval(zahlenMenge,mapOf("element1" to übersetzt)).fehler)
        assertNull(eval(zahlenMenge,mapOf("element1" to BedingterWert(z(1)))).fehler)
        assertNotNull(eval(graph(1),mapOf("element1" to BedingterWert(object:Methode {override val name="Engine ohne Mathematik"}))).fehler)
    }

    @Test fun `mathematische Methoden sind ueber echte Graphverbindungen ganze Mengenelemente`() {
        val ziel=graph(1)
        val quelle=KnotenDaten(art="test.methodenquelle",name="Methode",anschlüsse=listOf(AnschlussDaten(name="wert",richtung=AnschlussRichtung.Ausgang,kante=AnschlussKante.Rechts,art=MathematikAnschlussArten.Methode.id)))
        val von=AnschlussVerweis(quelle.id,quelle.anschlüsse.single().id)
        val zu=AnschlussVerweis(ziel.id,ziel.anschlüsse.single {it.name=="element1"}.id)
        val karte=KartenDaten(name="Methode als Element",knoten=listOf(quelle,ziel))
        assertEquals(de.TeutonStudio.KnotenKartenVerwalter.logik.VerbindungsPrüfung.Erlaubt,MathematikKartenLaufzeit().graphPrüfung.prüfe(karte,von,zu))
        val verbunden=karte.copy(verbindungen=listOf(VerbindungDaten(von=von,zu=zu)))
        val mathematisch=MathematischeMethode("f",listOf(Variable("x")),Variable("x"),ReelleZahlen)
        for(methode in listOf<Methode>(mathematisch,object:Methode {override val name="Script ohne Mathematik"})) {
            val r=KartenAuswerter(register).auswerten(verbunden,mapOf(quelle.id to mapOf("wert" to BedingterWert(methode))))
            if(methode===mathematisch) {assertTrue(r.fehler.isEmpty(),r.fehler.toString());assertEquals(EndlicheMenge(setOf(mathematisch)),r.knoten.getValue(ziel.id).ausgaben.getValue("menge").objekt)}
            else {assertTrue(r.fehler.isNotEmpty());assertTrue(r.knoten.getValue(ziel.id).ausgaben.isEmpty())}
        }
    }

    @Test fun `eingebettete Mengenaltquelle migriert rekursiv und doppelte Einzelbelegung bleibt Original`() {
        val alt=MathematikKnotenVorlagen.Einzelmenge.erzeuge(GraphPunkt.Zero)
        val inner=KartenDaten(name="Innen",version=7,knoten=listOf(alt))
        val quelle=KnotenDaten(art="test.quelle",name="Eingebettet",eigenschaften=mapOf(METHODEN_QUELLE_KNOTEN to KnotenEigenschaft.Text(KartenDatenJson.schreibe(inner))))
        for(neu in phasen(KartenDaten(name="Außen",knoten=listOf(quelle)))) {
            val text=(neu.knoten.single().eigenschaften.getValue(METHODEN_QUELLE_KNOTEN) as KnotenEigenschaft.Text).wert
            val k=KartenDatenJson.lese(text).knoten.single()
            assertEquals(ENDLICHE_MENGE_ART,k.art);assertEquals(alt.id,k.id);assertEquals(alt.anschlüsse.map {it.id},k.anschlüsse.map {it.id})
        }
        val input=alt.anschlüsse.single {it.richtung==AnschlussRichtung.Eingang}
        val kanten=List(2) {i->VerbindungDaten(von=AnschlussVerweis(KnotenId("q$i"),AnschlussId("out$i")),zu=AnschlussVerweis(alt.id,input.id),eingangsReihenfolge=i)}
        for(neu in phasen(KartenDaten(name="Mehrdeutig",knoten=listOf(alt),verbindungen=kanten))) {
            val k=neu.knoten.single();assertNotNull(k.parameter[RECHNER_MIGRATION_DIAGNOSE]);assertEquals(alt,k.copy(parameter=alt.parameter));assertEquals(kanten,neu.verbindungen)
        }
    }
}
