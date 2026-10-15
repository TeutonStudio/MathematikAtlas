package de.TeutonStudio.MathematikAtlas.knotenauswahl

import kotlin.test.*

class KnotenNutzungsVerwalterTest {
    private var zeit=1_800_000_000_000L
    private var ids=0
    private fun manager(d: KnotenNutzungsDaten = KnotenNutzungsDaten(),g: NutzungsGrenzen = NutzungsGrenzen()) = KnotenAuswahlNutzungsVerwalter(d,{zeit},{ "id-${ids++}" },g)
    private fun suche(text: String,tab: String="Alle",aktiv: String=tab,einfügbar: Boolean=true) = KnotenSuchZustand(text,if(text.isBlank()) SuchtextHerkunft.OHNE_FILTER else SuchtextHerkunft.GETIPPT,1,if(einfügbar) 1 else 0,tab,aktiv,listOf(KnotenAngebot("vorlage/test/standard",einfügbar)))
    private fun wahl(z: KnotenSuchZustand,id: String="wahl",key: String="vorlage/test/standard") = KnotenAuswahlAuftrag(id,"s",key,1,z)
    @Test fun `oeffnen und abbrechen zaehlt keine Nutzung`() {
        val m=manager();m.öffnen("s","frei","3.2.2",suche(""));m.schließen("s",suche(""),SitzungsAbschluss.ABBRUCH)
        assertTrue(m.daten.statistik.eintraege.isEmpty());assertEquals(SitzungsAbschluss.ABBRUCH,m.daten.sitzungen.single().abschluss)
    }
    @Test fun `rasches Tippen wird nur im Entwurf gehalten und Auswahl leert Debounce nicht`() {
        val m=manager();m.öffnen("s","frei","v",suche(""))
        m.aktualisieren("s",suche("d"));m.aktualisieren("s",suche("diagonal"))
        assertEquals(1,m.daten.sitzungen.single().suchschritte.size)
        m.auswählen(wahl(suche("diagonal")),true,listOf(ErzeugterKnotenTyp("matrix")))
        val s=m.daten.sitzungen.single();assertEquals(listOf("","diagonal"),s.suchschritte.map { it.zustand.originaltext })
        assertEquals(s.suchschritte.last().id,s.auswahlen.single().suchschritt);assertEquals(1,m.daten.statistik.eintraege.values.single().anzahl)
    }
    @Test fun `Umformulieren und Rueckkehr bleiben geordnet Neudarstellung nicht`() {
        val m=manager();m.öffnen("s","frei","v",suche("diagonal"))
        m.protokollieren("s",suche("diagonal"),SuchAnlass.SUCHRUHE)
        m.protokollieren("s",suche("diagonalmatrix"),SuchAnlass.SUCHRUHE)
        m.protokollieren("s",suche("diagonal"),SuchAnlass.SUCHRUHE)
        assertEquals(listOf("diagonal","diagonalmatrix","diagonal"),m.daten.sitzungen.single().suchschritte.map { it.zustand.originaltext })
    }
    @Test fun `erfolglose Suchen und Reset werden keinem spaeteren Treffer als Synonym zugeordnet`() {
        val m=manager();m.öffnen("s","frei","v",suche("ellipsoid").copy(textTreffer=0,einfügbareTreffer=0,angebote=emptyList()))
        m.protokollieren("s",suche(""),SuchAnlass.ZURUECKSETZEN)
        m.auswählen(wahl(suche("")),true,listOf(ErzeugterKnotenTyp("matrix")))
        assertTrue(m.daten.direkteSuchAuswahlen().isEmpty());assertEquals(2,m.daten.sitzungen.single().suchschritte.size)
    }
    @Test fun `mathematische Einzeichenanfragen bleiben verschieden`() {
        assertEquals(listOf("∪","=","×","π"),listOf("∪","=","×","π").map(::normalisiereSuchtext))
        assertNotEquals(normalisiereSuchtext("×"),normalisiereSuchtext("*"))
        assertNotEquals(normalisiereSuchtext("Π"),normalisiereSuchtext("π"))
    }
    @Test fun `deaktivierte Ergebnisse Tabausweichen und Sichtbarkeit bleiben getrennt`() {
        val z=suche("x","Matrizen","Alle",false).copy(textTreffer=4,anschlussFilter="Eingang:zahl")
        val m=manager();m.öffnen("s","anschluss","v",z)
        m.sichtbar("s",z,setOf("kopf/tabs","vorlage/test/standard"));m.sichtbar("s",z,setOf("vorlage/test/standard"))
        val schritt=m.daten.sitzungen.single().suchschritte.single()
        assertEquals(4,schritt.zustand.textTreffer);assertEquals(0,schritt.zustand.einfügbareTreffer)
        assertEquals("Matrizen",schritt.zustand.gewünschterTab);assertEquals("Alle",schritt.zustand.aktiverTab)
        assertEquals(setOf("vorlage/test/standard"),schritt.sichtbar)
    }
    @Test fun `Sammelaktion ist eine Auswahl mit mehreren Typen und doppelte Auftraege zaehlen nicht`() {
        val m=manager();val z=suche("Menge");m.öffnen("s","frei","v",z)
        val a=wahl(z,key="aktion/mengendefinition");val typen=listOf(ErzeugterKnotenTyp("konstruktor"),ErzeugterKnotenTyp("definator"))
        m.auswählen(a,true,typen);m.auswählen(a,true,typen);m.schließen("s",z,SitzungsAbschluss.ABBRUCH)
        assertEquals(1,m.daten.statistik.eintraege.values.single().anzahl);assertEquals(2,m.daten.sitzungen.single().auswahlen.single().erzeugt.size)
        assertEquals(SitzungsAbschluss.AUSWAHL,m.daten.sitzungen.single().abschluss)
    }
    @Test fun `Ablehnung bleibt ohne Nutzung und derselbe Auftrag wird nicht nachtraeglich zum Erfolg`() {
        val m=manager();val z=suche("x");m.öffnen("s","frei","v",z)
        m.auswählen(wahl(z),false,emptyList());m.auswählen(wahl(z),true,listOf(ErzeugterKnotenTyp("x")))
        assertTrue(m.daten.statistik.eintraege.isEmpty());assertFalse(m.daten.sitzungen.single().auswahlen.single().erfolgreich)
    }
    @Test fun `Neustart markiert offene Sitzung als unterbrochen statt abgebrochen`() {
        val m=manager();m.öffnen("s","frei","v",suche("π"))
        val neu=manager(m.daten)
        assertEquals(SitzungsAbschluss.UNTERBROCHEN,neu.daten.sitzungen.single().abschluss)
    }
    @Test fun `Loeschen verhindert Wiederbelebung durch alte Such und Auswahlauftraege`() {
        val m=manager();val z=suche("π");m.öffnen("s","frei","v",z);m.löschen()
        m.protokollieren("s",z,SuchAnlass.SUCHRUHE);m.auswählen(wahl(z),true,listOf(ErzeugterKnotenTyp("x")))
        assertTrue(m.daten.sitzungen.isEmpty());assertTrue(m.daten.statistik.eintraege.isEmpty())
    }
    @Test fun `Suchtextabschaltung entfernt alte Texte und direkte Suchbeziehungen`() {
        val m=manager();val z=suche("privater Begriff");m.öffnen("s","frei","v",z);m.auswählen(wahl(z),true,listOf(ErzeugterKnotenTyp("x")))
        assertEquals(1,m.daten.direkteSuchAuswahlen().size)
        m.einstellungen(KnotenNutzungsEinstellungen(suchtexte=false))
        assertTrue(m.daten.sitzungen.flatMap { it.suchschritte }.all { it.zustand.originaltext.isEmpty() && it.normalisiert==null })
        assertTrue(m.daten.direkteSuchAuswahlen().isEmpty());assertEquals(1,m.daten.statistik.eintraege.values.single().anzahl)
    }
    @Test fun `ohne Suchtexte bleiben Umformulierungen bei gleicher Ergebnislage getrennt`() {
        val m=manager(KnotenNutzungsDaten(einstellungen=KnotenNutzungsEinstellungen(suchtexte=false)))
        val a=suche("keinTrefferA").copy(textTreffer=0,einfügbareTreffer=0,angebote=emptyList())
        val b=a.copy(originaltext="keinTrefferB")
        m.öffnen("s","frei","v",a)
        m.protokollieren("s",a,SuchAnlass.SUCHRUHE)
        m.protokollieren("s",b,SuchAnlass.SUCHRUHE)
        m.sichtbar("s",b,emptySet())
        m.protokollieren("s",a,SuchAnlass.SUCHRUHE)
        m.schließen("s",a,SitzungsAbschluss.ABBRUCH)
        val schritte=m.daten.sitzungen.single().suchschritte
        assertEquals(3,schritte.size)
        assertTrue(schritte.all { !it.textGespeichert && it.zustand.originaltext.isEmpty() && it.normalisiert==null })
    }
    @Test fun `Abschalten beendet offene Sitzung und sammelt keine neue`() {
        val m=manager();m.öffnen("s","frei","v",suche("x"));m.einstellungen(KnotenNutzungsEinstellungen(erfassen=false))
        m.öffnen("neu","frei","v",suche("y"));assertEquals(1,m.daten.sitzungen.size);assertEquals(SitzungsAbschluss.DEAKTIVIERT,m.daten.sitzungen.single().abschluss)
    }
    @Test fun `Altersgrenze loescht Suchbeziehungen aber erhaelt reine Eintragszaehler`() {
        val m=manager();val z=suche("x");m.öffnen("s","frei","v",z);m.auswählen(wahl(z),true,listOf(ErzeugterKnotenTyp("x")))
        zeit+=91*86_400_000L;m.bereinigen()
        assertTrue(m.daten.sitzungen.isEmpty());assertEquals(1,m.daten.statistik.eintraege.values.single().anzahl)
    }
}
