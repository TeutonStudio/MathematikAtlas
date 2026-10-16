package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import java.io.File
import kotlin.test.*

class MethodenKonsolidierungMigrationTest {
    private val prüfung=GraphPrüfung(AnschlussArtRegister(MathematikAnschlussArten.alle))
    private fun karte(k:KnotenDaten)=KartenDaten(name="Test",knoten=listOf(k))
    private fun alt()=FaltungsKnotenVorlagen.MethodenAnwendungZahl.erzeuge(GraphPunkt.Zero)
    private fun phasen(k:KartenDaten):List<KartenDaten> {
        val text=KartenDatenJson.schreibe(k)
        return listOf(MathematikKartenCodec.lese(text),MathematikKartenCodec.lade(text),MathematikKartenCodec.importiere(text),MathematikKartenMigrationen.vorSpeichern(k),MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(k)))
    }
    @Test fun `alle 43 gespeicherten Altaufrufe werden mit stabilen Anschlussidentitaeten migriert`() {
        var anzahl=0;var dateien=0
        File("src/main/assets/de/TeutonStudio/MathematikKnoten/konzeptkarte").walkTopDown().filter { it.extension=="json" }.forEach { f ->
            val text=f.readText()
            val nodes=org.json.JSONObject(text).optJSONArray("knoten") ?: return@forEach
            if ((0 until nodes.length()).none { nodes.getJSONObject(it).optString("art")==METHODEN_ANWENDUNG_ART }) return@forEach
            val original=KartenDatenJson.lese(text)
            val alt=original.knoten.filter { it.art==METHODEN_ANWENDUNG_ART }
            if(alt.isEmpty()) return@forEach
            dateien++;anzahl+=alt.size
            phasen(original).forEach { aktuell ->
                assertEquals(original.id,aktuell.id)
                alt.forEach { a ->
                    val n=aktuell.knoten.single {it.id==a.id}
                    assertEquals(METHODEN_AUFRUF_ART,n.art,"${f.name}: ${n.parameter[RECHNER_MIGRATION_DIAGNOSE]}")
                    assertEquals(a.anschlüsse.map {it.id}.toSet(),n.anschlüsse.map {it.id}.toSet())
                    assertEquals(a.anschlüsse.map {it.name to it.richtung}.toSet(),n.anschlüsse.map {it.name to it.richtung}.toSet())
                    assertEquals(METHODEN_ERGEBNISPROJEKTION_DIREKT,n.parameter[METHODEN_AUFRUF_ERGEBNISPROJEKTION])
                    assertEquals(n,karte(n).migriereMethodenKonsolidierung().knoten.single())
                    original.verbindungen.filter {it.zu.knotenId==a.id}.forEach {v ->
                        assertEquals(v.zu,aktuell.verbindungen.single {it.id==v.id}.zu)
                    }
                }
            }
        }
        assertEquals(43,anzahl);assertEquals(40,dateien)
    }
    @Test fun `unklare Anwendung bleibt in allen Codecphasen vollstaendig erhalten und gesperrt`() {
        val original=alt().let {it.copy(anschlüsse=it.anschlüsse+it.anschlüsse.last().copy(id=AnschlussId("extra"),name="mehr"))}
        phasen(karte(original)).forEach { k ->
            val n=k.knoten.single();assertEquals(original,n.copy(parameter=n.parameter-RECHNER_MIGRATION_DIAGNOSE))
            assertNotNull(n.parameter[RECHNER_MIGRATION_DIAGNOSE])
            assertNotNull(KartenAuswerter(GesamterMathematikAuswerter.erzeugeRegister()).auswerten(k).knoten.getValue(n.id).fehler)
        }
    }
    @Test fun `alle vier Ordnungswerte migrieren und unbekannte Werte bleiben erhalten`() {
        val basis=MathematikKnotenVorlagen.Kleiner.erzeuge(GraphPunkt.Zero)
        val werte=mapOf("kleiner" to "relation.kleiner","kleinerGleich" to "relation.kleinerGleich","größer" to "relation.groesser","größerGleich" to "relation.groesserGleich")
        werte.forEach { (wert,op) -> phasen(karte(basis.copy(parameter=mapOf("relation" to wert)))).forEach { k ->
            val n=k.knoten.single();assertEquals("mathematik.praedikat",n.art);assertEquals(op,n.parameter["operator"])
            assertEquals(basis.anschlüsse.map {it.id},n.anschlüsse.map {it.id})
        }}
        for(wert in listOf("unbekannt","")) {
            val original=basis.copy(parameter=mapOf("relation" to wert))
            phasen(karte(original)).forEach {k -> val n=k.knoten.single();assertEquals(original,n.copy(parameter=n.parameter-RECHNER_MIGRATION_DIAGNOSE));assertNotNull(n.parameter[RECHNER_MIGRATION_DIAGNOSE])}
        }
    }
    @Test fun `eingebettete Methodenquellen durchlaufen dieselben Codecphasen`() {
        val original=alt()
        val quelle=konfiguriereMethodenQuelle(MethodenQuellKnotenVorlagen.Methode.erzeuge(GraphPunkt.Zero),quelle=original)
        phasen(karte(quelle)).forEach { k ->
            val text=(k.knoten.single().eigenschaften[METHODEN_QUELLE_KNOTEN] as KnotenEigenschaft.Text).wert
            val n=KartenDatenJson.lese(text).knoten.single()
            assertEquals(METHODEN_AUFRUF_ART,n.art);assertEquals(original.id,n.id)
            assertEquals(original.anschlüsse.map {it.id},n.anschlüsse.map {it.id})
        }
        val kaputt=quelle.copy(eigenschaften=quelle.eigenschaften+(METHODEN_QUELLE_KNOTEN to KnotenEigenschaft.Text("Originaldaten?")))
        phasen(karte(kaputt)).forEach {k -> assertEquals(kaputt.eigenschaften,k.knoten.single().eigenschaften);assertNotNull(k.knoten.single().parameter[RECHNER_MIGRATION_DIAGNOSE])}
    }
    @Test fun `fehlende Methode erhaelt verbundene Argumente und feste direkte Projektion`() {
        val k=karte(alt()).migriereMethodenKonsolidierung().knoten.single()
        val q=MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero)
        val v=VerbindungDaten(von=AnschlussVerweis(q.id,q.anschlüsse.single().id),zu=AnschlussVerweis(k.id,k.anschlüsse.single {it.name=="argument"}.id))
        val original=KartenDaten(name="Test",knoten=listOf(q,k),verbindungen=listOf(v))
        val n=synchronisiereMethodenAufrufe(original,KartenAuswertungsErgebnis(emptyMap(),emptyList()),prüfung)
        assertEquals(original,n)
    }
    @Test fun `Signaturumordnung erhaelt Portnamen lokale Werte Kartenfallbacks und IDs`() {
        val x=Variable("x",bindungsId="qx");val y=Variable("y",bindungsId="qy")
        val m=MathematischeMethode("d",listOf(y,x),subtraktion(x,y),ReelleZahlen,mapOf("x" to ReelleZahlen,"y" to ReelleZahlen))
        val basis=FaltungsKnotenVorlagen.MethodeAufrufen.erzeuge(GraphPunkt.Zero)
        val erster=basis.anschlüsse.single {it.name=="argument-0"}.copy(name="lokal",semantischeId="qx",art=MathematikAnschlussArten.Objekt.id)
        val zweiter=erster.copy(id=AnschlussId("qy-port"),name="karte",semantischeId="qy",reihenfolge=2)
        val k=basis.copy(anschlüsse=basis.anschlüsse.filter {it.name!="argument-0"}.map {
                if (it.richtung==AnschlussRichtung.Ausgang) it.copy(art=MathematikAnschlussArten.Objekt.id,vertrag=de.TeutonStudio.TypSystem.AnschlussVertrag()) else it
            }+erster+zweiter,
            parameter=mapOf(METHODEN_AUFRUF_PROJEKTION_FEST to "true",METHODEN_AUFRUF_ARGUMENTPROJEKTION to METHODEN_ARGUMENTPROJEKTION_SEPARIERT,METHODEN_AUFRUF_ERGEBNISPROJEKTION to METHODEN_ERGEBNISPROJEKTION_DIREKT,"standardwert.lokal" to "5"),
            eingangsKartenVerweise=mapOf("karte" to KartenVerweis(KartenId("extern"),7)))
        val e=KartenAuswertungsErgebnis(mapOf(k.id to KnotenAuswertungsErgebnis(emptyMap(),eingänge=mapOf("methode" to BedingterWert(m)))),emptyList())
        val n=synchronisiereMethodenAufrufe(karte(k),e,prüfung).knoten.single()
        assertEquals("5",n.parameter["standardwert.lokal"]);assertEquals(k.eingangsKartenVerweise,n.eingangsKartenVerweise)
        assertEquals(listOf(zweiter.id,erster.id),n.anschlüsse.filter {it.richtung==AnschlussRichtung.Eingang&&it.name!="methode"}.map {it.id})
        assertEquals(setOf("lokal","karte"),n.anschlüsse.filter {it.name!="methode"&&it.richtung==AnschlussRichtung.Eingang}.map {it.name}.toSet())
    }
    @Test fun `Diagnose bewahrt inkompatible Originalkanten auch durch den Strukturabschlussfilter`() {
        val original=alt().let {it.copy(anschlüsse=it.anschlüsse+it.anschlüsse.last().copy(id=AnschlussId("extra"),name="mehr"))}
        val k=karte(original).migriereMethodenKonsolidierung().knoten.single()
        val q=MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero)
        val v=VerbindungDaten(von=AnschlussVerweis(q.id,q.anschlüsse.single().id),zu=AnschlussVerweis(k.id,k.anschlüsse.single {it.richtung==AnschlussRichtung.Eingang&&it.name=="methode"}.id))
        val originalKarte=KartenDaten(name="Test",knoten=listOf(q,k),verbindungen=listOf(v))
        val n=synchronisiereMethodenAufrufe(originalKarte,KartenAuswertungsErgebnis(emptyMap(),emptyList()),prüfung)
        assertEquals(originalKarte,n)
    }
    @Test fun `orientierte Analysis verwendet allgemeine Typen und erhaelt beide Methodenport IDs`() {
        val katalog=de.TeutonStudio.MathematikKnoten.katalog.KanonischerMathematikKnotenKatalog.alle()
        for(art in listOf("mathematik.methodenDifferentieren","mathematik.methodenIntegrieren")) for(orient in listOf(VEKTOR_ORIENTIERUNG_SPALTE,VEKTOR_ORIENTIERUNG_ZEILE)) {
            val k=katalog.single {it.art==art&&it.standardParameter[VEKTOR_ORIENTIERUNG_PARAMETER]==orient}.erzeuge(GraphPunkt.Zero).copy(name="Mein Name")
            assertTrue(istKonfigurierteVektorAnalysis(k));assertTrue(hatGemeinsameAusgangsEinstellungen(k))
            val andere=if(orient==VEKTOR_ORIENTIERUNG_SPALTE) VEKTOR_ORIENTIERUNG_ZEILE else VEKTOR_ORIENTIERUNG_SPALTE
            val n=konfiguriereOrientierungsKnoten(k,andere)
            assertEquals(k.anschlüsse.map {it.id},n.anschlüsse.map {it.id});assertEquals(2,n.anschlüsse.map {it.id}.distinct().size)
            assertEquals(k.name,n.name);assertEquals(art,n.art)
            assertEquals("mathematik.vektor.$andere",n.parameter[methodenErgebnisArtSchlüssel("methode")])
            assertFails {konfiguriereOrientierungsKnoten(k,"unbekannt")}
            val x=Variable("x")
            val v=if(orient==VEKTOR_ORIENTIERUNG_SPALTE) SpaltenVektor(listOf(x)) else ZeilenVektor(listOf(x))
            val raum=Vektorraum(if(orient==VEKTOR_ORIENTIERUNG_SPALTE) VektorOrientierung.Spalte else VektorOrientierung.Zeile,1,ReelleZahlen)
            val m=Methode("f",listOf(x),v,raum,mapOf("x" to ReelleZahlen))
            val e=GesamterMathematikAuswerter.erzeugeRegister().finde(art)!!.auswerten(KnotenAuswertungsKontext(k,mapOf("methode" to BedingterWert(m)),RechenKontext()))
            val ergebnis=e.ausgaben.getValue("methode").objekt as MathematischeMethode
            assertEquals(v::class,ergebnis.einzigeAusgabe().second::class)
        }
    }

    @Test fun `migrierter Aufruf bleibt beim Kopieren Duplizieren und Undo mit festen Kartenquellen verbunden`() {
        val alt=alt().copy(eingangsKartenVerweise=mapOf("methode" to KartenVerweis(KartenId("extern"),7)))
        val k=karte(alt).migriereMethodenKonsolidierung().knoten.single()
        val q=MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero)
        val v=VerbindungDaten(von=AnschlussVerweis(q.id,q.anschlüsse.single().id),zu=AnschlussVerweis(k.id,k.anschlüsse.single {it.name=="argument"}.id),eingangsReihenfolge=3)
        val original=KartenDaten(name="Test",knoten=listOf(q,k),verbindungen=listOf(v))
        val editor=de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand(original,prüfung)
        editor.wähleAlleKnoten();editor.dupliziereAuswahl()
        val kopiert=editor.karte
        val neu=kopiert.knoten.single {it.art==METHODEN_AUFRUF_ART&&it.id!=k.id}
        assertEquals(k.parameter,neu.parameter);assertEquals(k.eingangsKartenVerweise,neu.eingangsKartenVerweise)
        assertTrue(neu.anschlüsse.none {p -> k.anschlüsse.any {it.id==p.id}})
        val neueKante=kopiert.verbindungen.single {it.id!=v.id}
        assertEquals(neu.id,neueKante.zu.knotenId);assertEquals(3,neueKante.eingangsReihenfolge)
        assertTrue(neu.anschlüsse.any {it.id==neueKante.zu.anschlussId})
        editor.rückgängig();assertEquals(original,editor.karte)
        editor.wiederholen();assertEquals(kopiert,editor.karte)
        val zwischenablage=AtlasZwischenablage()
        assertTrue(zwischenablage.kopiere(original,original.knoten.map {it.id}.toSet()))
        val einfügung=assertNotNull(zwischenablage.erzeugeEinfügung(GraphPunkt.Zero))
        val eingefügt=einfügung.knoten.single {it.art==METHODEN_AUFRUF_ART}
        assertEquals(k.eingangsKartenVerweise,eingefügt.eingangsKartenVerweise)
        assertTrue(einfügung.verbindungen.all {it.id!=v.id&&it.zu.knotenId==eingefügt.id})
    }
    @Test fun `historische M1 Konzepte erzeugen kanonische Vorlagen mit ihren vorhandenen Definitionskarten`() {
        val vorlagen=alleMathematikDefinitionsVorlagen()
        val alte=setOf(METHODEN_ANWENDUNG_ART,"mathematik.ordnungsrelation")+HISTORISCHE_METHODEN_ANALYSIS.keys
        assertTrue(vorlagen.none {it.art in alte})
        val einträge=de.TeutonStudio.MathematikKnoten.konzeptknoten.KonzeptKnotenRegister.erstelle(vorlagen)
        val ids=listOf("mathematik.methodenAnwendung|Zahlmethode anwenden|methodenAnwendung.ergebnisArt=mathematik.zahl",
            "mathematik.ordnungsrelation|Kleiner|relation=kleiner",
            "mathematik.spaltenMethodeDifferentieren|Spaltenmethode differentieren|",
            "mathematik.zeilenMethodeIntegrieren|Zeilenmethode integrieren|")
        ids.forEach {id ->
            val e=einträge.single {it.id.wert==id}
            assertTrue(e.knotenVorlagen.isNotEmpty());assertTrue(e.knotenVorlagen.none {it.art in alte})
            assertTrue(e.karten.any {it.primär})
        }
    }

    @Test fun `unpassender fester Einzelresultatvertrag erhaelt Ausgang und Verbindung`() {
        val original = alt().let { k -> k.copy(
            parameter = k.parameter + (METHODEN_ANWENDUNG_ERGEBNIS_ART to "mathematik.zahl"),
            anschlüsse = k.anschlüsse.map { if (it.richtung == AnschlussRichtung.Ausgang) it.copy(art = MathematikAnschlussArten.Zahl.id) else it }) }
        val k = karte(original).migriereMethodenKonsolidierung().knoten.single()
        val ziel = KnotenDaten(art="test", name="Zahlziel", anschlüsse=listOf(AnschlussDaten(
            name="zahl",richtung=AnschlussRichtung.Eingang,kante=AnschlussKante.Links,art=MathematikAnschlussArten.Zahl.id)))
        val v = VerbindungDaten(von=AnschlussVerweis(k.id,k.anschlüsse.single {it.richtung==AnschlussRichtung.Ausgang}.id),
            zu=AnschlussVerweis(ziel.id,ziel.anschlüsse.single().id))
        val m = MathematischeMethode("bool",emptyList(),WahrheitsKonstante(true),WahrheitsMenge)
        val graph = KartenDaten(name="Test",knoten=listOf(k,ziel),verbindungen=listOf(v))
        val e = KartenAuswertungsErgebnis(mapOf(k.id to KnotenAuswertungsErgebnis(emptyMap(),eingänge=mapOf("methode" to BedingterWert(m)))),emptyList())
        val n = synchronisiereMethodenAufrufe(graph,e,prüfung)
        val erhalten = n.knoten.single {it.id==k.id}
        assertTrue(METHODEN_AUFRUF_VERTRAGSFEHLER in erhalten.parameter)
        assertEquals(k.anschlüsse,erhalten.anschlüsse)
        assertEquals(k.parameter[METHODEN_ANWENDUNG_ERGEBNIS_ART],erhalten.parameter[METHODEN_ANWENDUNG_ERGEBNIS_ART])
        assertEquals(graph.verbindungen,n.verbindungen)
    }

    @Test fun `unpassendes belegtes Argument bewahrt Zahlenkante statt Tupelvertrag einzusetzen`() {
        val original = alt().let { k -> k.copy(anschlüsse = k.anschlüsse.map {
            if (it.name == "argument") it.copy(art = MathematikAnschlussArten.Zahl.id) else it }) }
        val k = karte(original).migriereMethodenKonsolidierung().knoten.single()
        val q = MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero)
        val v = VerbindungDaten(von=AnschlussVerweis(q.id,q.anschlüsse.single().id),
            zu=AnschlussVerweis(k.id,k.anschlüsse.single {it.name=="argument"}.id))
        val x = AllgemeinerParameter("x")
        val m = MathematischeMethode("f",listOf(x),RationaleZahl.von(1),ReelleZahlen,
            mapOf("x" to Tupelraum(listOf(ReelleZahlen,ReelleZahlen))))
        val graph = KartenDaten(name="Test",knoten=listOf(k,q),verbindungen=listOf(v))
        val e = KartenAuswertungsErgebnis(mapOf(k.id to KnotenAuswertungsErgebnis(emptyMap(),eingänge=mapOf("methode" to BedingterWert(m)))),emptyList())
        val n = synchronisiereMethodenAufrufe(graph,e,prüfung)
        val erhalten = n.knoten.single {it.id==k.id}
        assertTrue(METHODEN_AUFRUF_VERTRAGSFEHLER in erhalten.parameter)
        assertEquals(k.anschlüsse,erhalten.anschlüsse)
        assertEquals(graph.verbindungen,n.verbindungen)
    }

    @Test fun `unveraenderte historische Zahlenanwendung mit Zahlquelle bleibt nach Synchronisierung gueltig`() {
        val original=alt()
        val q=MathematikKnotenVorlagen.Zahl.erzeuge(GraphPunkt.Zero)
        val v=VerbindungDaten(von=AnschlussVerweis(q.id,q.anschlüsse.single().id),zu=AnschlussVerweis(original.id,original.anschlüsse.single {it.name=="argument"}.id))
        val graph=KartenDaten(name="Test",knoten=listOf(q,original),verbindungen=listOf(v)).migriereMethodenKonsolidierung()
        val x=Variable("x")
        val m=MathematischeMethode("f",listOf(x),x,ReelleZahlen,mapOf("x" to ReelleZahlen))
        val e=KartenAuswertungsErgebnis(mapOf(original.id to KnotenAuswertungsErgebnis(emptyMap(),eingänge=mapOf("methode" to BedingterWert(m),"argument" to BedingterWert(RationaleZahl.von(3))))),emptyList())
        val n=synchronisiereMethodenAufrufe(graph,e,prüfung)
        val k=n.knoten.single {it.id==original.id}
        assertFalse(METHODEN_AUFRUF_VERTRAGSFEHLER in k.parameter)
        assertEquals(graph.verbindungen,n.verbindungen)
        assertEquals(original.anschlüsse.single {it.name=="argument"}.art,k.anschlüsse.single {it.name=="argument"}.art)
    }

}
