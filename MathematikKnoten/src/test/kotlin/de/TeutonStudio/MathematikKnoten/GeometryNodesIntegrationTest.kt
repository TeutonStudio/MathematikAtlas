package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.logik.VerbindungsPrüfung
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikKnoten.katalog.KanonischerMathematikKnotenKatalog
import de.TeutonStudio.MathematikRechenSystem.kern.*
import java.io.File
import kotlin.test.*

class GeometryNodesIntegrationTest {
    private val laufzeit=MathematikKartenLaufzeit()
    private fun node(art: String,vararg p: Pair<String,String>) = GeometryNodesVorlagen.alle.single { it.art==GEOMETRY_NODES_PREFIX+art }.erzeuge(GraphPunkt.Zero).let { it.copy(parameter=it.parameter+p) }
    private fun edge(a: KnotenDaten,aus: String,b: KnotenDaten,ein: String) = VerbindungDaten(
        von=AnschlussVerweis(a.id,a.anschlüsse.single { it.name==aus && it.richtung==AnschlussRichtung.Ausgang }.id),
        zu=AnschlussVerweis(b.id,b.anschlüsse.single { it.name==ein && it.richtung==AnschlussRichtung.Eingang }.id))
    private fun auswerten(k: KnotenDaten,vararg eingang: Pair<String,AtlasWert>) = GesamterMathematikAuswerter.erzeugeRegister().finde(k.art)!!.auswerten(
        KnotenAuswertungsKontext(k,eingang.associate { it.first to BedingterWert(it.second) },RechenKontext()))
    private fun geom(k: KnotenDaten,vararg eingang: Pair<String,AtlasWert>) = assertIs<ProzeduraleGeometrie>(auswerten(k,*eingang).ausgaben.getValue("geometrie").objekt)

    @Test fun `exakt fuenfzehn neue Typen sind zentral erzeugbar und auswertbar`() {
        val typen=GeometryNodesVorlagen.alle.map { it.art }
        assertEquals(15,typen.toSet().size)
        assertTrue(KanonischerMathematikKnotenKatalog.alle().map { it.art }.containsAll(typen))
        val register=GesamterMathematikAuswerter.erzeugeRegister()
        for(v in GeometryNodesVorlagen.alle) {
            assertNotNull(register.finde(v.art)); val a=v.erzeuge(GraphPunkt.Zero);val b=v.erzeuge(GraphPunkt.Zero)
            assertNull(pruefeGeometryNodesKonfiguration(a)); assertTrue(a.anschlüsse.map { it.id }.intersect(b.anschlüsse.map { it.id }.toSet()).isEmpty())
        }
    }
    @Test fun `Feldanschluesse verlangen ihren Feldtyp und sind keine mathematischen Zahlen`() {
        val index=node("index"); val vector=node("vectorMath");val pos=node("position");val cmp=node("compare")
        fun kompatibel(a: KnotenDaten,aus: String,b: KnotenDaten,ein: String): Boolean {
            val e=edge(a,aus,b,ein)
            return laufzeit.graphPrüfung.prüfe(KartenDaten(name="Test",knoten=listOf(a,b)),e.von,e.zu) is VerbindungsPrüfung.Erlaubt
        }
        assertTrue(kompatibel(index,"feld",vector,"faktor"));assertTrue(kompatibel(pos,"feld",vector,"a"))
        assertFalse(kompatibel(index,"feld",vector,"a"));assertFalse(kompatibel(cmp,"auswahl",vector,"x"))
        assertFalse(laufzeit.anschlussArten.istUnterart(GeometryNodesAnschlussArten.Skalar.id,MathematikAnschlussArten.Zahl.id))
        assertFalse(laufzeit.anschlussArten.istUnterart(GeometryNodesAnschlussArten.Geometrie.id,GeometrieAnschlussArten.Objekt.id))
    }
    @Test fun `Gitter Feldverschiebung Instanzierung Realisierung und Vorschau bilden einen Graphen`() {
        val grid=node("meshGrid","anzahlX" to "3","anzahlY" to "2")
        val position=node("position");val set=node("setPosition","offsetZ" to "2")
        val cube=node("meshCube","groesseX" to "0.5","groesseY" to "0.5","groesseZ" to "0.5")
        val instance=node("instanceOnPoints"); val realize=node("realizeInstances");val preview=node("preview")
        val knoten=listOf(grid,position,set,cube,instance,realize,preview).mapIndexed { i,k -> k.copy(position=GraphPunkt((i%4)*400f,(i/4)*450f)) }
        val karte=KartenDaten(name="Geometry Nodes – Würfel auf einem Gitter",knoten=knoten,verbindungen=listOf(
            edge(grid,"geometrie",set,"geometrie"),edge(position,"feld",set,"position"),edge(set,"geometrie",instance,"punkte"),edge(cube,"geometrie",instance,"instanz"),edge(instance,"geometrie",realize,"geometrie"),edge(realize,"geometrie",preview,"geometrie")))
        val resultat=laufzeit.auswerten(karte);assertEquals(emptyList(),resultat.fehler)
        val inst=assertIs<ProzeduraleGeometrie>(resultat.knoten.getValue(instance.id).ausgaben.getValue("geometrie").objekt)
        assertEquals(6,inst.instanzen.size);assertSame(inst.instanzen.first().geometrie,inst.instanzen.last().geometrie)
        val g=assertIs<ProzeduraleGeometrie>(resultat.knoten.getValue(realize.id).ausgaben.getValue("geometrie").objekt)
        assertEquals(48,g.mesh!!.punkte.size);assertEquals(36,g.mesh!!.flaechen.size);assertTrue(g.mesh!!.punkte.all { it.z in 1.75..2.25 })
        assertSame(g,resultat.knoten.getValue(preview.id).ausgaben.getValue("geometrie").objekt)
        val roundtrip=MathematikKartenCodec.lade(MathematikKartenCodec.schreibe(karte))
        assertEquals(karte.verbindungen,roundtrip.verbindungen);assertEquals(g,laufzeit.auswerten(roundtrip).knoten.getValue(preview.id).ausgaben.getValue("geometrie").objekt)
        System.getenv("GEOMETRY_BEISPIEL_EXPORT")?.let { File(it).writeText(MathematikKartenCodec.schreibe(karte)) }
    }
    @Test fun `ein Indexfeld erzeugt unterschiedliche Punktmengen und Auswahl bleibt lazy`() {
        val index=node("index");val xyz=node("vectorMath","operator" to "combine");val points=node("points","anzahl" to "5")
        val other=node("points","anzahl" to "2");val cmp=node("compare","b" to "2");val set=node("setPosition","offsetZ" to "7")
        val karte=KartenDaten(name="Test",knoten=listOf(index,xyz,points,other,cmp,set),verbindungen=listOf(
            edge(index,"feld",xyz,"x"),edge(xyz,"feld",points,"position"),edge(xyz,"feld",other,"position"),edge(index,"feld",cmp,"a"),edge(points,"geometrie",set,"geometrie"),edge(cmp,"auswahl",set,"auswahl")))
        val r=laufzeit.auswerten(karte);assertEquals(emptyList(),r.fehler)
        fun cloud(k: KnotenDaten)=assertIs<ProzeduraleGeometrie>(r.knoten.getValue(k.id).ausgaben.getValue("geometrie").objekt).punktwolke!!.punkte
        assertEquals(listOf(0.0,1.0),cloud(other).map { it.x });assertEquals(listOf(7.0,7.0,0.0,0.0,0.0),cloud(set).map { it.z })
    }
    @Test fun `ungueltige verbundene Quellen werden niemals zu Standardfeldern`() {
        val bad=node("scalarMath","operator" to "ungueltig");val vector=node("vectorMath","operator" to "combine");val points=node("points")
        val karte=KartenDaten(name="Test",knoten=listOf(bad,vector,points),verbindungen=listOf(edge(bad,"feld",vector,"x"),edge(vector,"feld",points,"position")))
        val r=laufzeit.auswerten(karte)
        assertNotNull(r.knoten.getValue(bad.id).fehler);assertNotNull(r.knoten.getValue(points.id).fehler);assertTrue(r.knoten.getValue(points.id).ausgaben.isEmpty())
    }
    @Test fun `Defaults fehlende Parameter falsche Typen und ungueltige Zahlen bleiben explizit`() {
        assertEquals(8,geom(node("meshCube").copy(parameter=emptyMap())).mesh!!.punkte.size)
        for(p in listOf("NaN","Infinity","-1","1e400")) assertNotNull(auswerten(node("meshCube","groesseX" to p)).fehler)
        assertNotNull(auswerten(node("points","anzahl" to "2.5")).fehler)
        assertNotNull(auswerten(node("meshGrid","anzahlX" to "100000","anzahlY" to "100000")).fehler)
        assertNotNull(auswerten(node("setPosition")).fehler)
        assertNotNull(auswerten(node("points"),"position" to SkalarFeld.Index).fehler)
        assertNull(geom(node("joinGeometry")).mesh)
    }
    @Test fun `alle neuen Typen speichern Parameter und stabile Anschlussreferenzen ohne Laufzeitdaten`() {
        val knoten=GeometryNodesVorlagen.alle.map { it.erzeuge(GraphPunkt.Zero).let { k -> k.copy(parameter=k.parameter+("unbekannt" to "erhalten")) } }
        val karte=KartenDaten(name="Codec",knoten=knoten)
        val json=MathematikKartenCodec.schreibe(karte);val neu=MathematikKartenCodec.lade(json)
        assertEquals(karte,neu);assertFalse(json.contains("GeometrieFeldKontext"))
    }
    @Test fun `Geometriewerte funktionieren durch eine wiederverwendbare Gruppenkarte`() {
        val cube=node("meshCube")
        val ausgang=KnotenDaten(art="mathematik.kartenAusgang",name="Ausgang",parameter=mapOf("name" to "geometrie"),anschlüsse=listOf(
            AnschlussDaten(name="wert",richtung=AnschlussRichtung.Eingang,kante=AnschlussKante.Links,art=GeometryNodesAnschlussArten.Geometrie.id)))
        val innen=KartenDaten(name="Test",knoten=listOf(cube,ausgang),verbindungen=listOf(edge(cube,"geometrie",ausgang,"wert")))
        val gruppe=KnotenDaten(art="mathematik.gruppe",name="Geometriegruppe",kartenVerweis=KartenVerweis(innen.id,innen.version),anschlüsse=listOf(
            AnschlussDaten(name="geometrie",richtung=AnschlussRichtung.Ausgang,kante=AnschlussKante.Rechts,art=GeometryNodesAnschlussArten.Geometrie.id)))
        val r=MathematikKartenLaufzeit(KartenQuelle { innen }).auswerten(KartenDaten(name="Test",knoten=listOf(gruppe)))
        assertEquals(emptyList(),r.fehler);assertEquals(8,assertIs<ProzeduraleGeometrie>(r.knoten.getValue(gruppe.id).ausgaben.getValue("geometrie").objekt).mesh!!.punkte.size)
    }
    @Test fun `Inspector Konfiguration laesst Anschluesse bestehen und ist atomar rueckgaengig`() {
        val cube=node("meshCube");val editor=KartenEditorZustand(KartenDaten(name="Test",knoten=listOf(cube)),laufzeit.graphPrüfung)
        val kandidat=cube.copy(parameter=cube.parameter+("groesseX" to "5"));assertNull(pruefeGeometryNodesKonfiguration(kandidat))
        editor.führeAus(KartenAktion.KnotenErsetzen(kandidat));assertEquals(cube.anschlüsse,editor.karte.knoten.single().anschlüsse)
        assertEquals(5.0,geom(editor.karte.knoten.single()).mesh!!.punkte.let { ps -> ps.maxOf { it.x }-ps.minOf { it.x } })
        editor.rückgängig();assertEquals(cube,editor.karte.knoten.single());editor.wiederholen();assertEquals(kandidat,editor.karte.knoten.single())
    }
    @Test fun `Vorschau traversiert Instanzen ohne das Modell zu realisieren und begrenzt Ausgabe`() {
        val cube=GeometrieOperationen.wuerfel();val inst=ProzeduraleGeometrie(instanzen=listOf(GeometrieInstanz(cube,GeometrieTransformation.trs(GeometrieVektor3(10.0,0.0,0.0)))))
        val r=geometryVorschau(inst);assertEquals(12,r.linien.size);assertTrue(r.punkte.all { it.x in 9.5..10.5 });assertSame(cube,inst.instanzen.single().geometrie)
        val viele=ProzeduraleGeometrie(instanzen=List(3000) { GeometrieInstanz(cube) });val begrenzt=geometryVorschau(viele)
        assertTrue(begrenzt.begrenzt);assertTrue(begrenzt.punkte.size<=3000);assertTrue(begrenzt.linien.size<=5000);assertEquals(3000,viele.instanzen.size)
    }
}
