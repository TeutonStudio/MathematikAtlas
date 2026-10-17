package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.AtlasZwischenablage
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.katalog.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import de.TeutonStudio.TypSystem.TypInferenzRegel
import kotlin.test.*

class UebersetzungsKonsolidierungTest {
    private val register=GesamterMathematikAuswerter.erzeugeRegister()
    private fun z(n:Long)=RationaleZahl.von(n)
    private fun ziel(z:String)=konfiguriereÜbersetzer(ÜbersetzungsKnotenVorlagen.Übersetzer.erzeuge(GraphPunkt.Zero),z)
    private fun eval(k:KnotenDaten,w:BedingterWert,port:String="wert")=register.finde(k.art)!!.auswerten(KnotenAuswertungsKontext(k,mapOf(port to w),RechenKontext()))
    private fun phasen(k:KartenDaten)=listOf(MathematikKartenMigrationen::nachDekodierung,MathematikKartenMigrationen::nachLaden,MathematikKartenMigrationen::vorSpeichern,
        { a:KartenDaten -> MathematikKartenCodec.lese(KartenDatenJson.schreibe(a)) },
        { a:KartenDaten -> MathematikKartenCodec.lade(KartenDatenJson.schreibe(a)) },
        { a:KartenDaten -> MathematikKartenCodec.importiere(KartenDatenJson.schreibe(a)) }).map {it(k)}
    private val altVorlagen get()=listOf(MathematikKnotenVorlagen.TupelZuSpalte,MathematikKnotenVorlagen.TupelZuZeile,
        GeometrieKnotenVorlagen.SpalteZuTupel,GeometrieKnotenVorlagen.ZeileZuTupel,ÜbersetzungsKnotenVorlagen.Abflachen)

    @Test fun `alle Altarten migrieren in jeder Phase mit gerichteten Rollen und Identitaeten`() {
        for(v in altVorlagen) {
            val basis=v.erzeuge(GraphPunkt.Zero)
            val input=basis.anschlüsse.single {it.richtung==AnschlussRichtung.Eingang}
            val output=basis.anschlüsse.single {it.richtung==AnschlussRichtung.Ausgang}
            val ref=KartenVerweis(KartenId("extern"),7)
            val alt=basis.copy(name="Mein Adapter",parameter=basis.parameter+mapOf("standardwert.${input.name}" to "3","methodenAusgang.${output.name}.typ" to "erhalten"),
                eigenschaften=basis.eigenschaften+(KNOTEN_FORM_EIGENSCHAFT to KnotenEigenschaft.Text("eigene-form")),
                eingangsKartenVerweise=mapOf(input.name to ref),
                anschlüsse=basis.anschlüsse.map {if(it.richtung==AnschlussRichtung.Ausgang) it.copy(artFolgtEingang=input.name,typInferenz=TypInferenzRegel.FolgtEingang(input.name)) else it})
            val verbindung=VerbindungDaten(von=AnschlussVerweis(KnotenId("quelle"),AnschlussId("aus")),zu=AnschlussVerweis(alt.id,input.id),eingangsReihenfolge=5,eingangsFallback=input.name+"@karte:"+input.id.wert)
            val karte=KartenDaten(name="Alt",knoten=listOf(alt),verbindungen=listOf(verbindung))
            for(neu in phasen(karte)) {
                val k=neu.knoten.single()
                assertEquals(ÜBERSETZER_ART,k.art,k.parameter[RECHNER_MIGRATION_DIAGNOSE]);assertEquals(alt.id,k.id);assertEquals(alt.name,k.name)
                assertEquals(alt.anschlüsse.map {it.id},k.anschlüsse.map {it.id})
                assertEquals("3",k.parameter["standardwert.wert"])
                assertEquals("erhalten",k.parameter["methodenAusgang.ergebnis.typ"])
                assertEquals(mapOf("wert" to ref),k.eingangsKartenVerweise)
                assertEquals("wert",k.anschlüsse.last().artFolgtEingang)
                assertEquals(TypInferenzRegel.FolgtEingang("wert"),k.anschlüsse.last().typInferenz)
                assertEquals(alt.eigenschaften[KNOTEN_FORM_EIGENSCHAFT],k.eigenschaften[KNOTEN_FORM_EIGENSCHAFT])
                assertEquals(verbindung.copy(eingangsFallback="wert@karte:"+input.id.wert),neu.verbindungen.single())
                assertEquals(neu,neu.migriereÜbersetzungsKonsolidierung())
                assertEquals(neu,MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(neu)))
            }
        }
    }

    @Test fun `mehrdeutige Originale bekommen vor Darstellung und Normalisierung nur Diagnose`() {
        for(v in altVorlagen) {
            val basis=v.erzeuge(GraphPunkt.Zero).copy(eigenschaften=emptyMap())
            val extra=basis.anschlüsse.first().copy(id=AnschlussId("extra"))
            val defekte=listOf(basis.copy(anschlüsse=basis.anschlüsse+extra),basis.copy(parameter=basis.parameter+mapOf("standardwert.${basis.anschlüsse.first().name}" to "a","standardwert.wert" to "b"))) +
                if(v.art==TUPEL_ABFLACHEN_ART) listOf(basis.copy(parameter=mapOf("abflachen.ebenen" to "2"))) else emptyList()
            for(alt in defekte) for(neu in phasen(KartenDaten(name="Defekt",knoten=listOf(alt)))) {
                val k=neu.knoten.single();assertNotNull(k.parameter[RECHNER_MIGRATION_DIAGNOSE])
                assertEquals(alt,k.copy(parameter=alt.parameter));assertEquals(neu,neu.migriereÜbersetzungsKonsolidierung())
                val r=register.finde(k.art)!!.auswerten(KnotenAuswertungsKontext(k,emptyMap(),RechenKontext()))
                assertTrue(r.ausgaben.isEmpty());assertTrue(r.fehler.orEmpty().startsWith("Migrationsdiagnose:"))
            }
        }
        val alt=MathematikKnotenVorlagen.TupelZuSpalte.erzeuge(GraphPunkt.Zero)
        val p=alt.anschlüsse.first()
        val kanten=List(2) {i -> VerbindungDaten(von=AnschlussVerweis(KnotenId("q$i"),AnschlussId("a$i")),zu=AnschlussVerweis(alt.id,p.id))}
        assertNotNull(KartenDaten(name="Doppelt",knoten=listOf(alt),verbindungen=kanten).migriereÜbersetzungsKonsolidierung().knoten.single().parameter[RECHNER_MIGRATION_DIAGNOSE])
    }

    @Test fun `rekursiv und eine Ebene behalten leere Strukturen Blaetter und Rueckfuehrung`() {
        val v=SpaltenVektor(listOf(z(3),z(4)))
        val menge=EndlicheMenge(setOf(z(5)))
        val werte=listOf(Tupel(emptyList()),Tupel(listOf(z(1))),Tupel(listOf(Tupel(emptyList()),Tupel(listOf(z(1),Tupel(listOf(z(2))))),v,menge)))
        for(t in werte) for(tiefe in listOf("rekursiv","alle","1")) {
            val k=ziel("abflachen").let {it.copy(parameter=it.parameter+("abflachen.ebenen" to tiefe))}
            val r=eval(k,BedingterWert(t));assertNull(r.fehler)
            val out=r.ausgaben.getValue("ergebnis");val result=assertIs<Tupel>(out.objekt)
            assertEquals(if(tiefe=="1") strukturEineEbeneAbflachen(t) else tupelAbflachen(t),result)
            val zeugnis=assertNotNull(out.übersetzungen.aktuell)
            assertEquals(t,assertNotNull(zeugnis.rückführungsplan).rückführen(result.elemente))
            assertEquals(NachweisStatus.Nachgewiesen,zeugnis.informationserhaltung)
            assertFalse(zeugnis.fürAutomatikGeeignet())
            val export=out.alsÜbertragenerMathematischerWert()
            assertEquals(export,ÜbersetzungsCodec.dekodiereWert(ÜbersetzungsCodec.kodieren(export)))
        }
    }

    @Test fun `Strukturzeugnisse erhalten Voraussetzungen Quellen Komponenten und Formelwerte`() {
        val q=VariablenQuelle(KnotenId("x"),"x",ReelleZahlen,bindungsId="x-id")
        val x=Variable("x",bindungsId="x-id");val bed=Ungleichheit(x,z(0))
        val t=Tupel(listOf(Tupel(listOf(x,z(2))),Tupel(emptyList())))
        val raum=Tupelraum(listOf(Tupelraum(listOf(ReelleZahlen,ReelleZahlen)),Tupelraum(emptyList())))
        val w=BedingterWert(t,annahmen=setOf(bed),zielMenge=raum,komponentenMengen=raum.komponenten,variablenQuellen=listOf(q))
        val snapshot=w.alsGebundeneErgebnisFormel()
        val out=eval(ziel("abflachen"),w.copy(gebundeneFormel=snapshot)).ausgaben.getValue("ergebnis")
        assertEquals(listOf(q),out.variablenQuellen);assertTrue(bed in out.annahmen)
        assertEquals(listOf(ReelleZahlen,ReelleZahlen),out.komponentenMengen)
        assertEquals(out.objekt,assertIs<FormelAusdruck.Literal>(out.gebundeneFormel!!.wurzel).wert)
        val zeile=eval(ziel("zeile"),out).ausgaben.getValue("ergebnis")
        assertEquals(ZeilenVektor(listOf(x,z(2))),zeile.objekt)
        assertTrue(assertNotNull(out.übersetzungen.aktuell) in zeile.übersetzungen.herkunft)
        assertTrue(bed in zeile.annahmen)
        assertNotNull(eval(ziel("abflachen"),w.copy(annahmen=setOf(Ungleichheit(z(1),z(1))))).fehler)
    }

    @Test fun `Konvertierung flacht nicht implizit ab und unbekannte Tiefe wird abgewiesen`() {
        for(w in listOf(Tupel(emptyList()),Tupel(listOf(Tupel(listOf(z(1))))),Tupel(listOf(WahrheitsKonstante(true)))))
            assertNotNull(eval(ziel("spalte"),BedingterWert(w)).übersetzungsDiagnose)
        assertNotNull(eval(ziel("abflachen").let {it.copy(parameter=it.parameter+("abflachen.ebenen" to "0"))},BedingterWert(Tupel(emptyList()))).fehler)
        assertEquals(SpaltenVektor(listOf(z(1))),eval(ziel("spalte").let {it.copy(parameter=it.parameter+("abflachen.ebenen" to "0"))},BedingterWert(Tupel(listOf(z(1))))).ausgaben.getValue("ergebnis").objekt)
    }

    @Test fun `Suchbegriffe und Definitionen erzeugen ausschliesslich konfigurierte Uebersetzer`() {
        val katalog=KanonischerMathematikKnotenKatalog.alle()
        assertEquals(124,katalog.map {it.art}.toSet().size)
        assertTrue(katalog.none {it.art in übersetzungsKonsolidierungAltarten})
        assertTrue(alleMathematikDefinitionsVorlagen().none {it.art in übersetzungsKonsolidierungAltarten})
        for((suchtext,z) in listOf("Spalte zu Tupel" to "tupel","Zeile zu Tupel" to "tupel","Tupel zu Spalte" to "spalte","Tupel zu Zeile" to "zeile","Abflachen" to "abflachen")) {
            assertTrue(OperatorKnotenSuchindex.suche(suchtext,katalog).any {it.art==ÜBERSETZER_ART && übersetzerZiel(it.erzeuge(GraphPunkt.Zero))==z})
        }
    }

    @Test fun `kopierte migrierte Adapter remappen nur lokale Identitaeten`() {
        val alt=MathematikKnotenVorlagen.TupelZuSpalte.erzeuge(GraphPunkt.Zero).copy(eingangsKartenVerweise=mapOf("tupel" to KartenVerweis(KartenId("extern"),3)))
        val karte=KartenDaten(name="Kopie",knoten=listOf(alt)).migriereÜbersetzungsKonsolidierung()
        val ablage=AtlasZwischenablage();assertTrue(ablage.kopiere(karte,setOf(alt.id)))
        val kopie=ablage.erzeugeEinfügung(GraphPunkt(100f,100f))!!.knoten.single()
        assertNotEquals(alt.id,kopie.id);assertEquals(karte.knoten.single().eingangsKartenVerweise,kopie.eingangsKartenVerweise)
        assertTrue(kopie.anschlüsse.none {it.id in alt.anschlüsse.map {p->p.id}})
    }
    @Test fun `unveraenderliche Altkarte laeuft laden auswerten speichern wiederladen`() {
        val text=javaClass.getResource("/konsolidierung-m3/verbundene-altadapter-v10.json")!!.readText()
        val alt=KartenDatenJson.lese(text)
        for(neu in phasen(alt)) {
            assertEquals(alt.id,neu.id);assertEquals(alt.version,neu.version)
            assertEquals(alt.verbindungen,neu.verbindungen)
            assertEquals(alt.knoten.map {it.anschlüsse.map {p->p.id}},neu.knoten.map {it.anschlüsse.map {p->p.id}})
            val r=KartenAuswerter(register).auswerten(neu,mapOf(KnotenId("quelle") to mapOf("wert" to BedingterWert(Tupel(listOf(z(1),z(2)))))))
            for(id in listOf("flachSpalte","flachZeile")) {
                val out=r.knoten.getValue(KnotenId(id));assertNull(out.fehler)
                assertEquals(Tupel(listOf(z(1),z(2))),out.ausgaben.getValue("ergebnis").objekt)
            }
            assertEquals(neu,MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(neu)))
        }
    }

    @Test fun `gleichnamige Komponentenquellen behalten getrennte Zahlenbereiche ohne Raumannotation`() {
        val komplex=Variable("x",bindungsId="cx");val reell=Variable("x",bindungsId="rx")
        val w=BedingterWert(Tupel(listOf(Tupel(listOf(komplex)),reell)),variablenQuellen=listOf(
            VariablenQuelle(KnotenId("c"),"x",KomplexeZahlen,bindungsId="cx"),
            VariablenQuelle(KnotenId("r"),"x",ReelleZahlen,bindungsId="rx")))
        val out=eval(ziel("abflachen"),w).ausgaben.getValue("ergebnis")
        assertEquals(Tupel(listOf(komplex,reell)),out.objekt)
        assertEquals(Tupelraum(listOf(KomplexeZahlen,ReelleZahlen)),out.zielMenge)
        assertEquals(w.variablenQuellen,out.variablenQuellen)
        val orientiert=eval(ziel("zeile"),out).ausgaben.getValue("ergebnis")
        assertEquals(ZeilenVektor(listOf(komplex,reell)),orientiert.objekt)
        assertEquals(w.variablenQuellen,orientiert.variablenQuellen)
    }

    @Test fun `Orientierungsparameter ist nur bei konfigurierbarer Tupelkonvertierung aktiv`() {
        val alt=GeometrieKnotenVorlagen.ZeileZuTupel.erzeuge(GraphPunkt.Zero).let {it.copy(parameter=it.parameter+(VEKTOR_ORIENTIERUNG_PARAMETER to "inaktiv-alt"))}
        val neu=KartenDaten(name="Zeile",knoten=listOf(alt)).migriereÜbersetzungsKonsolidierung().knoten.single()
        assertEquals(ÜBERSETZER_ART,neu.art);assertEquals("tupel",übersetzerZiel(neu))
        assertEquals("inaktiv-alt",neu.parameter[VEKTOR_ORIENTIERUNG_PARAMETER])
        assertEquals(Tupel(listOf(z(1))),eval(neu,BedingterWert(ZeilenVektor(listOf(z(1))))).ausgaben.getValue("ergebnis").objekt)
        val unklar=MathematikKnotenVorlagen.TupelZuSpalte.erzeuge(GraphPunkt.Zero).let {it.copy(parameter=it.parameter+(VEKTOR_ORIENTIERUNG_PARAMETER to "unbekannt"))}
        assertNotNull(KartenDaten(name="Unklar",knoten=listOf(unklar)).migriereÜbersetzungsKonsolidierung().knoten.single().parameter[RECHNER_MIGRATION_DIAGNOSE])
    }

    @Test fun `historischer Zeilenalias respektiert ausdruecklich gespeicherte Spaltenorientierung`() {
        val alt=MathematikKnotenVorlagen.TupelZuSpalte.erzeuge(GraphPunkt.Zero).let {it.copy(art="mathematik.tupelZuZeile",parameter=it.parameter+(VEKTOR_ORIENTIERUNG_PARAMETER to VEKTOR_ORIENTIERUNG_SPALTE))}
        val neu=KartenDaten(name="Alias",knoten=listOf(alt)).migriereÜbersetzungsKonsolidierung().knoten.single()
        assertEquals("spalte",übersetzerZiel(neu))
        val w=BedingterWert(Tupel(listOf(z(1))))
        val vorher=eval(alt,w,"tupel").ausgaben.getValue("vektor")
        val nachher=eval(neu,w).ausgaben.getValue("ergebnis")
        assertEquals(SpaltenVektor(listOf(z(1))),nachher.objekt)
        assertEquals(vorher.objekt,nachher.objekt)
        assertEquals(vorher.übersetzungen.aktuell!!.regelId,nachher.übersetzungen.aktuell!!.regelId)
    }

}
