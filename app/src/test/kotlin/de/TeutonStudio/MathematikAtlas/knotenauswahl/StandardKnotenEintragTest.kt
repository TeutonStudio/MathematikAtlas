package de.TeutonStudio.MathematikAtlas.knotenauswahl

import de.TeutonStudio.MathematikAtlas.statischeKnotenVorlagen
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.katalog.OperatorKnotenSuchindex
import kotlin.test.*

class StandardKnotenEintragTest {
    private fun v(name: String,art: String=name,kategorie: String="Zahlen") = KnotenVorlage(art,name,kategorie,"Beschreibung",anschlüsse=emptyList())
    private fun result(b: List<StandardKnotenEintrag>,text: String="",tab: String="Alle",kompatibel: (KnotenVorlage)->Boolean={ true },aktionen: Boolean=true) = standardKnotenErgebnis(b,text,SuchtextHerkunft.GETIPPT,tab,kompatibel,aktionen,aktionen,aktionen,if(aktionen) null else "Ausgang:test")
    @Test fun `wirklicher Standardkatalog besitzt eindeutige stabile Eintragsidentitaeten`() {
        val e=standardVorlagenEinträge(statischeKnotenVorlagen())
        val doppelt=e.groupBy { it.schluessel }.filterValues { it.size>1 }
        assertTrue(doppelt.isEmpty(),doppelt.mapValues { it.value.map { v -> v.name+" "+v.vorlage?.standardParameter } }.toString())
        assertEquals(e.map { it.schluessel },standardVorlagenEinträge(statischeKnotenVorlagen()).map { it.schluessel })
    }
    @Test fun `anzeigename und Standardwertwechsel veraendern die Identitaet nicht`() {
        val a=v("A","typ");val b=a.copy(name="Neu",standardParameter=mapOf("radius" to "20"))
        assertEquals(StandardKnotenEintrag.vorlage(a).schluessel,StandardKnotenEintrag.vorlage(b).schluessel)
        assertNotEquals(StandardKnotenEintrag.vorlage(a,"op-a").schluessel,StandardKnotenEintrag.vorlage(a,"op-b").schluessel)
    }
    @Test fun `Kartenidentitaet ist von ausgewaehlter Version getrennt`() {
        val a=v("Karte","gruppe").copy(kartenVerweis=KartenVerweis(KartenId("karte"),1))
        assertEquals("karte/karte",StandardKnotenEintrag.vorlage(a).schluessel)
        assertEquals(StandardKnotenEintrag.vorlage(a).schluessel,StandardKnotenEintrag.vorlage(a.copy(kartenVerweis=a.kartenVerweis!!.copy(version=2))).schluessel)
    }
    @Test fun `alte alphabetische Reihenfolge und Sammelaktionsposition bleiben erhalten`() {
        val r=result(listOf(v("Beta"),v("Alpha")).map { StandardKnotenEintrag.vorlage(it) })
        assertEquals(listOf("Endliche Faltung","Mengendefinition","Alpha","Beta"),r.zeilen.mapNotNull { it.eintrag?.name })
        assertEquals(4,r.aktiverTab.anzahl)
    }
    @Test fun `Tabausweichen und deaktivierte Sammelaktionen sind unterschiedliche Ergebniszustaende`() {
        val r=result(listOf(StandardKnotenEintrag.vorlage(v("Treffer"))),tab="Matrizen",aktionen=false)
        assertEquals("Matrizen",r.zustand.gewünschterTab);assertEquals("Alle",r.zustand.aktiverTab)
        assertEquals(3,r.aktiverTab.anzahl);assertEquals(2,r.zustand.angebote.count { !it.einfügbar })
        assertEquals(5,r.zustand.textTreffer);assertEquals(1,r.zustand.einfügbareTreffer)
    }
    @Test fun `Texttreffer vor und nach Anschlussfilter werden nicht vermischt`() {
        val r=result(listOf(v("Treffer1"),v("Treffer2")).map { StandardKnotenEintrag.vorlage(it) },"Treffer",kompatibel={ it.name=="Treffer1" })
        assertEquals(2,r.zustand.textTreffer);assertEquals(1,r.zustand.einfügbareTreffer);assertEquals(1,r.zustand.angebote.size)
    }
    @Test fun `derselbe Eintrag behaelt in verschiedenen Tabs seinen Schluessel`() {
        val b=listOf(StandardKnotenEintrag.vorlage(v("Matrix","matrix","Matrizen")))
        val all=result(b,"Matrix");val tab=result(b,"Matrix","Matrizen")
        assertEquals(all.zustand.angebote,tab.zustand.angebote)
    }
    @Test fun `Spaltenvektorsuche weicht mit wirklichem Katalog auf Tupel Sammelaktionen aus`() {
        val basis=statischeKnotenVorlagen()
        val query="Spaltenvektor erzeugen"
        val r=result(standardVorlagenEinträge(basis,OperatorKnotenSuchindex.sucheMitMetadaten(query,basis)),query)
        assertEquals("Alle",r.zustand.gewünschterTab)
        assertEquals("Tupel",r.zustand.aktiverTab)
        val spalte=r.zeilen.mapNotNull { it.eintrag }.single { it.schluessel=="aktion/tupel-spalte" }
        assertTrue(spalte.einfügbar)
        assertEquals("Spaltenvektor erzeugen",spalte.name)
    }
    @Test fun `Sammelaktion kann frisches Tupel mit Spaltenadapter verbinden`() {
        val tupel=de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen.Tupel.erzeuge(GraphPunkt.Zero)
        val adapter=de.TeutonStudio.MathematikKnoten.MathematikKnotenVorlagen.TupelZuSpalte.erzeuge(GraphPunkt.Zero)
        val karte=KartenDaten(name="Sammelauswahl",knoten=listOf(tupel,adapter))
        val quelle=AnschlussVerweis(tupel.id,tupel.anschlüsse.first { it.name=="tupel" }.id)
        val ziel=AnschlussVerweis(adapter.id,adapter.anschlüsse.first { it.name=="tupel" }.id)
        val prüfung=de.TeutonStudio.MathematikKnoten.MathematikKartenLaufzeit().graphPrüfung.prüfe(karte,quelle,ziel)
        assertIs<de.TeutonStudio.KnotenKartenVerwalter.logik.VerbindungsPrüfung.Erlaubt>(prüfung,prüfung.toString())
    }
    @Test fun `Operatorindex liefert stabile Metadaten bei unveraenderten Suchtreffern`() {
        val b=statischeKnotenVorlagen()
        for(text in listOf("spur","vereinigung","+")) {
            val neu=OperatorKnotenSuchindex.sucheMitMetadaten(text,b);assertTrue(neu.isNotEmpty())
            assertTrue(neu.all { it.operatorId.isNotBlank() })
            fun signatur(v: KnotenVorlage)=Triple(v.art,v.name,v.standardParameter)
            assertEquals(OperatorKnotenSuchindex.suche(text,b).map(::signatur),neu.map { signatur(it.vorlage) })
        }
    }
    @Test fun `Uebersetzungsziele in Eigenschaften bleiben getrennte stabile Auswahlvarianten`() {
        val basis=de.TeutonStudio.MathematikKnoten.ÜbersetzungsKnotenVorlagen.Übersetzer
        val vorlagen=listOf("tupel","zeile","spalte","abflachen").map { z ->
            val k=de.TeutonStudio.MathematikKnoten.konfiguriereÜbersetzer(basis.erzeuge(GraphPunkt.Zero),z)
            basis.copy(name="Gleicher Titel",anschlüsse=k.anschlüsse,standardParameter=k.parameter,standardEigenschaften=k.eigenschaften)
        }
        val einträge=standardVorlagenEinträge(vorlagen)
        assertEquals(4,einträge.size);assertEquals(4,einträge.map {it.schluessel}.distinct().size)
        assertEquals(4,vorlagen.map {stabileKnotenVariante(it.erzeuge(GraphPunkt.Zero))}.distinct().size)
    }

}
