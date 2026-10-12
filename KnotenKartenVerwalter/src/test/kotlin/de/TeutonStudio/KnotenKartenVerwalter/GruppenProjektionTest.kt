package de.TeutonStudio.KnotenKartenVerwalter

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.*
import kotlin.test.*

class GruppenProjektionTest {
    private val art = AnschlussArtId("test")
    private val prüfung = GraphPrüfung(AnschlussArtRegister(listOf(AnschlussArt(art, "Test"))))
    private fun k(id: String, x: Float = 100f, y: Float = 120f) = KnotenDaten(
        id = KnotenId(id), art = "test", name = id, position = GraphPunkt(x, y), größe = GraphGröße(100f, 70f),
        anschlüsse = listOf(
            AnschlussDaten(id = AnschlussId("$id-in"), name = "in", richtung = AnschlussRichtung.Eingang, kante = AnschlussKante.Links, art = art),
            AnschlussDaten(id = AnschlussId("$id-out"), name = "out", richtung = AnschlussRichtung.Ausgang, kante = AnschlussKante.Rechts, art = art),
        ))
    private fun ein(k: KnotenDaten) = AnschlussVerweis(k.id, k.anschlüsse[0].id)
    private fun aus(k: KnotenDaten) = AnschlussVerweis(k.id, k.anschlüsse[1].id)
    private fun g(id: String, ids: Set<KnotenId>, eingeklappt: Boolean = false, eltern: VisuelleGruppenId? = null,
                  x: Float = 50f, y: Float = 50f, b: Float = 500f, h: Float = 400f) = VisuelleKnotenGruppeDaten(
        id = VisuelleGruppenId(id), knotenIds = ids, titel = id, eingeklappt = eingeklappt, elternGruppenId = eltern,
        position = GraphPunkt(x, y), größe = GraphGröße(b, h))

    @Test fun `Faltung bewahrt Originalgraph und bildet Grenzen und Fanout exakt ab`() {
        val a = k("a", -200f); val b = k("b"); val c = k("c", 280f); val d = k("d", 900f); val e = k("e", 900f, 300f)
        val verbindungen = listOf(VerbindungDaten(von = aus(a), zu = ein(b)), VerbindungDaten(von = aus(b), zu = ein(c)),
            VerbindungDaten(von = aus(c), zu = ein(d)), VerbindungDaten(von = aus(c), zu = ein(e)))
        val gruppe = g("g", setOf(b.id, c.id), true)
        val karte = KartenDaten(name = "Test", knoten = listOf(a,b,c,d,e), verbindungen = verbindungen, visuelleGruppen = listOf(gruppe))
        val p = karte.gruppenProjektion()
        assertEquals(4, karte.verbindungen.size)
        assertEquals(3, p.karte.verbindungen.size)
        assertEquals(setOf(a.id,d.id,e.id), p.karte.knoten.filter { it.art != GRUPPEN_BLOCK_ART }.map { it.id }.toSet())
        assertEquals(3, p.karte.knoten.single { it.art == GRUPPEN_BLOCK_ART }.anschlüsse.size)
        p.karte.verbindungen.forEach { v ->
            val original = verbindungen.single { it.id == v.id }
            assertEquals(original.von, p.original(v.von)); assertEquals(original.zu, p.original(v.zu))
        }
        assertEquals(p.originalAnschlüsse, karte.gruppenProjektion().originalAnschlüsse)
    }

    @Test fun `freie Anschlüsse erscheinen intern belegte Einzeleingänge nicht`() {
        val a=k("a");val b=k("b",280f)
        val karte=KartenDaten(name="Test",knoten=listOf(a,b),verbindungen=listOf(VerbindungDaten(von=aus(a),zu=ein(b))),
            visuelleGruppen=listOf(g("g",setOf(a.id,b.id),true)))
        val refs=karte.gruppenProjektion().originalAnschlüsse.values.toSet()
        assertEquals(setOf(ein(a),aus(a),aus(b)),refs)
    }

    @Test fun `äußerer Block verbirgt eingeklappte Nachfahren und deren interne Kanten`() {
        val a=k("a");val b=k("b",280f)
        val eltern=g("eltern",emptySet(),true,b=900f,h=700f)
        val kind=g("kind",setOf(a.id,b.id),true,eltern.id)
        val karte=KartenDaten(name="Test",knoten=listOf(a,b),visuelleGruppen=listOf(eltern,kind))
        assertEquals(listOf(eltern.id),karte.gruppenProjektion().blöcke.values.toList())
        assertEquals(listOf(kind.id),karte.gruppenProjektion(eltern.id).blöcke.values.toList())
    }

    @Test fun `räumliche Hierarchie nimmt tiefste eindeutige Mitgliedschaft`() {
        val a=k("a",160f,220f)
        val eltern=g("eltern",emptySet(),b=1000f,h=900f)
        val kind=g("kind",emptySet(),x=120f,y=150f,b=400f,h=300f)
        val karte=KartenDaten(name="Test",knoten=listOf(a),visuelleGruppen=listOf(eltern,kind)).mitAutomatischenVisuellenGruppenMitgliedschaften()
        assertEquals(eltern.id,karte.visuelleGruppen.last().elternGruppenId)
        assertEquals(setOf(a.id),karte.visuelleGruppen.last().knotenIds)
        assertTrue(karte.visuelleGruppen.first().knotenIds.isEmpty())
        karte.prüfeGruppenHierarchie()
    }

    @Test fun `verschachtelter Inhalt bewegt sich genau einmal und Auflösen erhält ihn`() {
        val a=k("a");val parent=g("p",emptySet(),b=1000f,h=900f);val child=g("c",setOf(a.id),eltern=parent.id)
        val karte=KartenDaten(name="Test",knoten=listOf(a),visuelleGruppen=listOf(parent,child))
        val neu=karte.wendeAn(KartenAktion.VisuelleGruppeVerschieben(parent.id,GraphPunkt(20f,30f)))
        assertEquals(a.position+GraphPunkt(20f,30f),neu.knoten.single().position)
        assertEquals(child.position+GraphPunkt(20f,30f),neu.visuelleGruppen.last().position)
        val aufgelöst=neu.wendeAn(KartenAktion.VisuelleGruppeLöschen(child.id))
        assertEquals(neu.knoten,aufgelöst.knoten)
        assertEquals(setOf(a.id),aufgelöst.visuelleGruppen.single().knotenIds)
    }

    @Test fun `Faltung ist ein Undo Schritt ohne Auswertungsrevision`() {
        val a=k("a");val gruppe=g("g",setOf(a.id))
        val editor=KartenEditorZustand(KartenDaten(name="Test",knoten=listOf(a),visuelleGruppen=listOf(gruppe)),prüfung)
        val vorher=editor.karte
        editor.führeAus(KartenAktion.VisuelleGruppeFaltungÄndern(gruppe.id,true))
        assertEquals(0,editor.auswertungsRevision)
        editor.rückgängig();assertEquals(vorher,editor.karte)
        editor.wiederholen();assertTrue(editor.karte.visuelleGruppen.single().eingeklappt)
        assertEquals(0,editor.auswertungsRevision)
    }

    @Test fun `projizierte Anschlüsse erstellen echte geprüfte Verbindungen`() {
        val a=k("a",-300f);val b=k("b");val gruppe=g("g",setOf(b.id),true)
        val editor=KartenEditorZustand(KartenDaten(name="Test",knoten=listOf(a,b),visuelleGruppen=listOf(gruppe)),prüfung)
        val ziel=editor.projektion.sichtbar(ein(b))
        editor.anschlussAngeklickt(aus(a));editor.anschlussAngeklickt(ziel)
        val v=editor.karte.verbindungen.single()
        assertEquals(aus(a),v.von);assertEquals(ein(b),v.zu)
        editor.rückgängig();assertTrue(editor.karte.verbindungen.isEmpty())
    }

    @Test fun `Gruppenansicht schreibt denselben Graphen und behält Außenkanten`() {
        val a=k("a",-300f);val b=k("b");val gruppe=g("g",setOf(b.id))
        val außen=VerbindungDaten(von=aus(a),zu=ein(b))
        val editor=KartenEditorZustand(KartenDaten(name="Test",knoten=listOf(a,b),verbindungen=listOf(außen),visuelleGruppen=listOf(gruppe)),prüfung)
        editor.öffneGruppe(gruppe.id)
        assertTrue(a.id !in editor.darstellungsKarte.knoten.map { it.id })
        assertEquals(2,editor.projektion.portale.size)
        editor.führeAus(KartenAktion.KnotenParameterÄndern(b.id,"wert","2"))
        assertEquals("2",editor.karte.knoten.single { it.id==b.id }.parameter["wert"])
        assertEquals(listOf(außen),editor.karte.verbindungen)
        editor.öffneGruppe(null);assertEquals(2,editor.karte.knoten.size)
    }

    @Test fun `Format9 und unabhängige Kopie erhalten Hierarchie und Ansicht`() {
        val a=k("a");val parent=g("p",emptySet(),true,b=1000f,h=900f);val child=g("c",setOf(a.id),true,parent.id).copy(ansicht=AnsichtsFenster(GraphPunkt(12f,13f),.6f))
        val karte=KartenDaten(name="Test",knoten=listOf(a),visuelleGruppen=listOf(parent,child))
        assertEquals(karte,KartenDatenJson.lese(KartenDatenJson.schreibe(karte)))
        val kopie=karte.alsNeueKarte()
        assertEquals(kopie.visuelleGruppen.first().id,kopie.visuelleGruppen.last().elternGruppenId)
        kopie.prüfeGruppenHierarchie()
        assertTrue(kopie.visuelleGruppen.last().knotenIds.single()!=a.id)
    }

    @Test fun `ungültige Hierarchie wird im Codec abgelehnt`() {
        val parent=g("p",emptySet(),eltern=VisuelleGruppenId("c"));val child=g("c",emptySet(),eltern=parent.id)
        val karte=KartenDaten(name="Test",visuelleGruppen=listOf(parent,child))
        assertFailsWith<IllegalArgumentException> { KartenDatenJson.lese(KartenDatenJson.schreibe(karte)) }
        assertFailsWith<IllegalArgumentException> { karte.copy(visuelleGruppen=listOf(parent)).prüfeGruppenHierarchie() }
    }

    @Test fun `Kamera verwendet dieselbe Projektion und getrennte Gruppenansicht`() {
        val a=k("a");val gruppe=g("g",setOf(a.id),true)
        val editor=KartenEditorZustand(KartenDaten(name="Test",knoten=listOf(a),visuelleGruppen=listOf(gruppe)),prüfung)
        val projektion=editor.projektion
        editor.führeAus(KartenAktion.AnsichtÄndern(AnsichtsFenster(GraphPunkt(10f,20f),.8f)))
        assertSame(projektion,editor.projektion)
        editor.öffneGruppe(gruppe.id)
        editor.führeAus(KartenAktion.AnsichtÄndern(AnsichtsFenster(GraphPunkt(50f,60f),.5f)))
        assertEquals(.5f,editor.darstellungsKarte.ansicht.zoom)
        editor.öffneGruppe(null);assertEquals(.8f,editor.darstellungsKarte.ansicht.zoom)
    }

    @Test fun `unsichtbarer Kindrahmen nimmt keine außen verschobene Gruppe auf`() {
        val parent=g("p",emptySet(),true,b=1200f,h=900f)
        val child=g("c",emptySet(),eltern=parent.id,x=100f,y=120f,b=800f,h=600f)
        val außen=g("d",emptySet(),x=200f,y=220f,b=200f,h=150f)
        val karte=KartenDaten(name="Test",visuelleGruppen=listOf(parent,child,außen)).mitAutomatischenVisuellenGruppenMitgliedschaften()
        assertNull(karte.visuelleGruppen.last().elternGruppenId)
    }

    @Test fun `Einfügen erweitert Rahmen ohne leere Kindgruppen auszuhängen`() {
        val a=k("a",180f,240f)
        val parent=g("p",setOf(a.id),b=1300f,h=900f)
        val child=g("c",emptySet(),eltern=parent.id,x=900f,y=200f,b=300f,h=200f)
        val editor=KartenEditorZustand(KartenDaten(name="Test",knoten=listOf(a),visuelleGruppen=listOf(parent,child)),prüfung)
        editor.öffneGruppe(parent.id)
        editor.führeAus(KartenAktion.KnotenEinfügen(k("neu",240f,400f)))
        assertEquals(parent.größe,editor.karte.visuelleGruppen.first().größe)
        assertEquals(parent.id,editor.karte.visuelleGruppen.last().elternGruppenId)
        editor.rückgängig();assertEquals(listOf(parent,child),editor.karte.visuelleGruppen)
    }

    @Test fun `Einfügen in Untergruppe erweitert auch Elternrahmen`() {
        val parent=g("p",emptySet(),b=900f,h=700f)
        val child=g("c",emptySet(),eltern=parent.id,x=120f,y=150f,b=400f,h=300f)
        val editor=KartenEditorZustand(KartenDaten(name="Test",visuelleGruppen=listOf(parent,child)),prüfung)
        editor.öffneGruppe(child.id)
        val neu=k("neu",1500f,1200f)
        editor.führeAus(KartenAktion.KnotenEinfügen(neu))
        assertEquals(parent.id,editor.karte.visuelleGruppen.last().elternGruppenId)
        assertTrue(neu.id in editor.karte.visuelleGruppen.last().knotenIds)
        assertTrue(editor.karte.visuelleGruppen.first().größe.breite>parent.größe.breite)
    }

    @Test fun `Einklappen hält Mitgliedschaft trotz äußerer überlappender Knoten`() {
        val a=k("a");val fremd=k("fremd",200f,200f)
        val gruppe=g("g",setOf(a.id),true)
        val karte=KartenDaten(name="Test",knoten=listOf(a,fremd),visuelleGruppen=listOf(gruppe)).mitAutomatischenVisuellenGruppenMitgliedschaften()
        assertEquals(setOf(a.id),karte.visuelleGruppen.single().knotenIds)
    }

    @Test fun `alte Karten erhalten räumliche Hierarchie und offene Defaults`() {
        val parent=g("p",emptySet(),b=1000f,h=900f)
        val child=g("c",emptySet(),x=120f,y=150f,b=400f,h=300f)
        val json=org.json.JSONObject(KartenDatenJson.schreibe(KartenDaten(name="Alt",visuelleGruppen=listOf(parent,child))))
        json.put("formatVersion",8)
        val array=json.getJSONArray("visuelleGruppen")
        for (i in 0 until array.length()) { array.getJSONObject(i).remove("eingeklappt");array.getJSONObject(i).remove("elternGruppenId") }
        val geladen=KartenDatenJson.lese(json)
        assertEquals(parent.id,geladen.visuelleGruppen.last().elternGruppenId)
        assertTrue(geladen.visuelleGruppen.none { it.eingeklappt })
    }

    @Test fun `Zwischenablage kopiert verschachtelte Gruppen mit neuen Elternreferenzen`() {
        val a=k("a");val b=k("b",280f)
        val parent=g("p",emptySet(),true,b=1000f,h=900f);val child=g("c",setOf(a.id,b.id),true,parent.id)
        val karte=KartenDaten(name="Test",knoten=listOf(a,b),visuelleGruppen=listOf(parent,child))
        val clip=AtlasZwischenablage();assertTrue(clip.kopiere(karte,setOf(a.id,b.id)))
        val kopie=clip.erzeugeEinfügung(GraphPunkt(2000f,100f))!!
        assertEquals(2,kopie.gruppen.size)
        assertEquals(kopie.gruppen.first().id,kopie.gruppen.last().elternGruppenId)
        assertTrue(kopie.gruppen.all { it.eingeklappt })
    }

    @Test fun `große Projektion ist abbrechbar und verwirft überholte Ergebnisse`() = kotlinx.coroutines.runBlocking {
        val knoten=List(1000) { k("n$it",it*140f) }
        val gruppe=g("g",knoten.mapTo(mutableSetOf()){it.id},true,b=150000f,h=500f)
        val editor=KartenEditorZustand(KartenDaten(name="Groß",knoten=knoten,visuelleGruppen=listOf(gruppe)),prüfung)
        editor.projektionImHintergrund=true
        assertTrue(editor.projektionWirdBerechnet)
        editor.berechneProjektionImHintergrund()
        assertFalse(editor.projektionWirdBerechnet)
        assertEquals(1,editor.darstellungsKarte.knoten.size)
        editor.ersetzeKarte(editor.karte.copy(version=2,knoten=editor.karte.knoten.toList()),historieLeeren=false)
        assertFalse(editor.projektionWirdBerechnet)
        val job=kotlinx.coroutines.Job();job.cancel()
        assertFailsWith<kotlinx.coroutines.CancellationException> { editor.karte.gruppenProjektion(abbruch=job) }
        editor.führeAus(KartenAktion.VisuelleGruppeFaltungÄndern(gruppe.id,false))
        assertTrue(editor.projektionWirdBerechnet)
        editor.berechneProjektionImHintergrund()
        assertEquals(1000,editor.darstellungsKarte.knoten.size)
        val p=editor.projektion
        editor.öffneGruppe(gruppe.id);editor.berechneProjektionImHintergrund()
        val innen=editor.projektion
        editor.führeAus(KartenAktion.AnsichtÄndern(AnsichtsFenster(GraphPunkt(40f,50f),.5f)))
        assertSame(innen,editor.projektion)
        assertNotSame(p,innen)
    }

    @Test fun `Ausschneiden entfernt Gruppeninhalt atomar waehrend Aufloesen ihn erhaelt`() {
        val a=k("a");val gruppe=g("g",setOf(a.id),true)
        val editor=KartenEditorZustand(KartenDaten(name="Test",knoten=listOf(a),visuelleGruppen=listOf(gruppe)),prüfung)
        editor.wähleGruppe(gruppe.id)
        editor.schneideAuswahlAus()
        assertTrue(editor.karte.knoten.isEmpty());assertTrue(editor.karte.visuelleGruppen.isEmpty())
        editor.rückgängig()
        assertEquals(listOf(a),editor.karte.knoten)
        assertEquals(listOf(gruppe),editor.karte.visuelleGruppen)
    }

    @Test fun `Undo einer Faltung erhaelt die unabhaengige Gruppen Kamera`() {
        val a=k("a");val gruppe=g("g",setOf(a.id))
        val editor=KartenEditorZustand(KartenDaten(name="Test",knoten=listOf(a),visuelleGruppen=listOf(gruppe)),prüfung)
        editor.führeAus(KartenAktion.VisuelleGruppeFaltungÄndern(gruppe.id,true))
        editor.öffneGruppe(gruppe.id)
        val kamera=AnsichtsFenster(GraphPunkt(120f,240f),.5f)
        editor.führeAus(KartenAktion.AnsichtÄndern(kamera))
        editor.rückgängig()
        assertFalse(editor.karte.visuelleGruppen.single().eingeklappt)
        assertEquals(kamera,editor.darstellungsKarte.ansicht)
    }
}
