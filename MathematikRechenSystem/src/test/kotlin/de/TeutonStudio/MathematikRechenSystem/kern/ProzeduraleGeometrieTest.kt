package de.TeutonStudio.MathematikRechenSystem.kern

import kotlin.math.PI
import kotlin.test.*

class ProzeduraleGeometrieTest {
    private fun v(x: Double, y: Double = 0.0, z: Double = 0.0) = GeometrieVektor3(x,y,z)
    private val nullFeld=SkalarFeld.Konstant(0.0)
    private fun linie(n: Int) = GeometrieOperationen.punkte(n,VektorFeld.Kombinieren(SkalarFeld.Index,nullFeld,nullFeld))
    private fun nahe(a: GeometrieVektor3,b: GeometrieVektor3) { assertEquals(a.x,b.x,1e-10); assertEquals(a.y,b.y,1e-10); assertEquals(a.z,b.z,1e-10) }

    @Test fun `Wuerfel besitzt vollstaendige orientierte Topologie`() {
        val m=GeometrieOperationen.wuerfel(v(2.0,4.0,6.0)).mesh!!
        assertEquals(8,m.punkte.size); assertEquals(12,m.kanten.size); assertEquals(6,m.flaechen.size)
        assertEquals(v(-1.0,-2.0,-3.0),m.punkte[0])
        assertEquals(24,m.flaechen.sumOf { it.size })
        m.flaechen.forEach { f -> val a=m.punkte[f[0]]; val b=m.punkte[f[1]]; val c=m.punkte[f[2]]; val u=b-a; val w=c-a; val normal=v(u.y*w.z-u.z*w.y,u.z*w.x-u.x*w.z,u.x*w.y-u.y*w.x); assertTrue(normal.x*a.x+normal.y*a.y+normal.z*a.z > 0) }
    }
    @Test fun `Gitter erzeugt geteilte Kanten und konstante Ebene`() {
        val m=GeometrieOperationen.gitter(3,4,2.0,6.0).mesh!!
        assertEquals(12,m.punkte.size); assertEquals(17,m.kanten.size); assertEquals(6,m.flaechen.size)
        assertEquals(v(-1.0,-3.0),m.punkte.first()); assertEquals(v(1.0,3.0),m.punkte.last())
        assertFailsWith<IllegalArgumentException> { GeometrieOperationen.gitter(Int.MAX_VALUE,Int.MAX_VALUE) }
    }
    @Test fun `Modell nimmt echte unveraenderliche Snapshots auf`() {
        val p=mutableListOf(v(0.0),v(1.0),v(0.0,1.0)); val f=mutableListOf(0,1,2)
        val m=PolygonMesh(p,listOf(MeshKante(0,1),MeshKante(1,2),MeshKante(2,0)),listOf(f))
        p.clear(); f.clear(); assertEquals(3,m.punkte.size); assertEquals(listOf(0,1,2),m.flaechen.single())
        assertFailsWith<UnsupportedOperationException> { (m.punkte as MutableList).clear() }
        assertFailsWith<UnsupportedOperationException> { (m.flaechen.single() as MutableList).clear() }
        assertEquals(MeshKante(1,0),MeshKante(0,1))
        assertEquals(GeometrieOperationen.wuerfel(),GeometrieOperationen.wuerfel())
        val refs=mutableListOf(GeometrieInstanz(linie(1))); val g=ProzeduraleGeometrie(instanzen=refs); refs.clear(); assertEquals(1,g.instanzen.size)
    }
    @Test fun `Ungueltige Indizes Randkanten und nichtendliche Koordinaten werden abgelehnt`() {
        val p=listOf(v(0.0),v(1.0),v(0.0,1.0))
        assertFailsWith<IllegalArgumentException> { PolygonMesh(p,listOf(MeshKante(0,1)),listOf(listOf(0,1,2))) }
        assertFailsWith<IllegalArgumentException> { PolygonMesh(p,listOf(MeshKante(0,3)),emptyList()) }
        assertFailsWith<IllegalArgumentException> { PolygonMesh(p,listOf(MeshKante(0,1),MeshKante(1,0)),emptyList()) }
        assertFailsWith<IllegalArgumentException> { GeometrieVektor3(Double.NaN,0.0,0.0) }
        assertFailsWith<IllegalArgumentException> { GeometrieOperationen.wuerfel(v(-1.0)) }
    }
    @Test fun `Index und skalare Vektorkomponenten erzeugen Punktlinie`() {
        assertEquals(listOf(v(0.0),v(1.0),v(2.0)),linie(3).punktwolke!!.punkte)
        assertEquals(0,linie(0).punktwolke!!.punkte.size)
    }
    @Test fun `Felder rechnen typisiert vergleichen und pruefen Division und Ueberlauf`() {
        val k=GeometrieFeldKontext(v(2.0,3.0),4)
        val a=SkalarFeld.Rechnung(SkalarOperation.ADDIEREN,SkalarFeld.Index,SkalarFeld.Konstant(2.0))
        assertEquals(6.0,a.auswerten(k))
        assertTrue(AuswahlFeld.Vergleich(VergleichsOperation.GROESSER,a,SkalarFeld.Index).auswerten(k))
        assertEquals(v(4.0,6.0),VektorFeld.Skalieren(VektorFeld.Position,SkalarFeld.Konstant(2.0)).auswerten(k))
        assertFailsWith<IllegalArgumentException> { SkalarFeld.Rechnung(SkalarOperation.DIVIDIEREN,a,nullFeld).auswerten(k) }
        assertFailsWith<IllegalArgumentException> { SkalarFeld.Rechnung(SkalarOperation.MULTIPLIZIEREN,SkalarFeld.Konstant(Double.MAX_VALUE),SkalarFeld.Konstant(2.0)).auswerten(k) }
        assertFailsWith<IllegalArgumentException> { VektorFeld.Skalieren(VektorFeld.Konstant(v(Double.MAX_VALUE)),SkalarFeld.Konstant(2.0)).auswerten(k) }
    }
    @Test fun `Felder begrenzen Tiefe und Arbeit auch bei geteiltem AST`() {
        var feld: SkalarFeld=SkalarFeld.Index
        repeat(11) { feld=SkalarFeld.Rechnung(SkalarOperation.ADDIEREN,feld,feld) }
        assertEquals(feld.hashCode(),feld.hashCode())
        assertFailsWith<IllegalArgumentException> { SkalarFeld.Rechnung(SkalarOperation.ADDIEREN,feld,feld) }
        var teuer: SkalarFeld=SkalarFeld.Index
        repeat(10) { teuer=SkalarFeld.Rechnung(SkalarOperation.ADDIEREN,teuer,teuer) }
        assertFailsWith<IllegalArgumentException> { GeometrieOperationen.punkte(2000,VektorFeld.Kombinieren(teuer,nullFeld,nullFeld)) }
        feld=SkalarFeld.Index
        repeat(31) { feld=SkalarFeld.Rechnung(SkalarOperation.ADDIEREN,feld,nullFeld) }
        assertFailsWith<IllegalArgumentException> { SkalarFeld.Rechnung(SkalarOperation.ADDIEREN,feld,nullFeld) }
    }
    @Test fun `Join verschiebt Meshindizes und verbindet Clouds getrennt`() {
        val a=GeometrieOperationen.wuerfel(); val b=linie(2)
        val g=GeometrieOperationen.verbinden(listOf(a,b,a,b))
        assertEquals(16,g.mesh!!.punkte.size); assertEquals(24,g.mesh!!.kanten.size)
        assertTrue(g.mesh!!.flaechen.drop(6).flatten().all { it in 8..15 }); assertEquals(4,g.punktwolke!!.punkte.size)
        val punkte=GeometrieOperationen.meshZuPunkten(g)
        assertNull(punkte.mesh); assertEquals(20,punkte.punktwolke!!.punkte.size)
        assertNull(GeometrieOperationen.verbinden(emptyList()).mesh)
    }
    @Test fun `TRS Reihenfolge ist Translation Rz Ry Rx Skalierung`() {
        val t=GeometrieTransformation.trs(v(10.0,20.0,30.0),v(PI/2,PI/2,PI/2),v(2.0,3.0,4.0))
        nahe(v(14.0,23.0,28.0),t.anwenden(v(1.0,1.0,1.0)))
        val neg=GeometrieTransformation.trs(skalierung=v(-1.0,0.0,1.0))
        val g=GeometrieOperationen.wuerfel(); val n=GeometrieOperationen.transformieren(g,neg)
        assertEquals(g.mesh!!.flaechen,n.mesh!!.flaechen); assertEquals(0.0,n.mesh!!.punkte.first().y)
    }
    @Test fun `Set Position benutzt fuer Position und Offset denselben Originalkontext`() {
        val g=GeometrieOperationen.positionSetzen(linie(3),position=VektorFeld.Skalieren(VektorFeld.Position,SkalarFeld.Konstant(2.0)),versatz=VektorFeld.Position)
        assertEquals(listOf(v(0.0),v(3.0),v(6.0)),g.punktwolke!!.punkte)
        val auswahl=AuswahlFeld.Vergleich(VergleichsOperation.KLEINER,SkalarFeld.Index,SkalarFeld.Konstant(1.0))
        val gemischt=GeometrieOperationen.verbinden(listOf(GeometrieOperationen.wuerfel(),linie(2)))
        val neu=GeometrieOperationen.positionSetzen(gemischt,auswahl,versatz=VektorFeld.Konstant(v(9.0)))
        assertEquals(gemischt.mesh!!.punkte[0]+v(9.0),neu.mesh!!.punkte[0]); assertEquals(gemischt.mesh!!.punkte[1],neu.mesh!!.punkte[1])
        assertEquals(v(9.0),neu.punktwolke!!.punkte[0]); assertEquals(v(1.0),neu.punktwolke!!.punkte[1])
    }
    @Test fun `Instanzen teilen Referenz und evaluieren Auswahl Rotation Skalierung pro Punkt`() {
        val referenz=GeometrieOperationen.wuerfel()
        val g=GeometrieOperationen.aufPunktenInstanzieren(linie(3),referenz,AuswahlFeld.Vergleich(VergleichsOperation.GROESSER,SkalarFeld.Index,nullFeld),VektorFeld.Konstant(v(0.0,0.0,PI/2)),VektorFeld.Konstant(v(2.0,2.0,2.0)))
        assertEquals(2,g.instanzen.size); assertSame(referenz,g.instanzen[0].geometrie)
        nahe(v(2.0,-1.0,-1.0),g.instanzen[0].transformation.anwenden(v(-0.5,-0.5,-0.5)))
        assertEquals(16,GeometrieOperationen.realisieren(g).mesh!!.punkte.size)
    }
    @Test fun `Transformierte verschachtelte Instanzen komponieren parent mal child genau einmal`() {
        val basis=linie(1)
        val innen=ProzeduraleGeometrie(instanzen=listOf(GeometrieInstanz(basis,GeometrieTransformation.trs(v(2.0)))))
        val aussen=ProzeduraleGeometrie(instanzen=listOf(GeometrieInstanz(innen,GeometrieTransformation.trs(v(10.0),skalierung=v(3.0,3.0,3.0)))))
        val transformiert=GeometrieOperationen.transformieren(aussen,GeometrieTransformation.trs(v(5.0)))
        assertSame(innen,transformiert.instanzen.single().geometrie)
        assertEquals(v(21.0),GeometrieOperationen.realisieren(transformiert).punktwolke!!.punkte.single())
        assertTrue(GeometrieOperationen.realisieren(transformiert).instanzen.isEmpty())
    }
    @Test fun `Nichtunterstuetzte implizite Instanzbehandlung meldet einen Fehler`() {
        val instanzen=ProzeduraleGeometrie(instanzen=listOf(GeometrieInstanz(linie(1))))
        assertFailsWith<IllegalArgumentException> { GeometrieOperationen.meshZuPunkten(instanzen) }
        assertFailsWith<IllegalArgumentException> { GeometrieOperationen.positionSetzen(instanzen) }
        assertFailsWith<IllegalArgumentException> { GeometrieOperationen.aufPunktenInstanzieren(instanzen,linie(1)) }
    }
    @Test fun `Realize prueft globales Vertexbudget bei mehrfach geteilten Referenzen`() {
        val g=ProzeduraleGeometrie(mesh=PolygonMesh(List(60_000) { v(0.0) },emptyList(),emptyList()))
        val refs=ProzeduraleGeometrie(instanzen=listOf(GeometrieInstanz(g),GeometrieInstanz(g)))
        assertFailsWith<IllegalArgumentException> { GeometrieOperationen.realisieren(refs) }
        assertFailsWith<IllegalArgumentException> { GeometrieOperationen.verbinden(listOf(g,g)) }
    }
    @Test fun `Realize prueft globales Cornerbudget getrennt vom Facebudget`() {
        val ring=List(10) { v(it.toDouble()) }; val kanten=List(10) { MeshKante(it,(it+1)%10) }
        val mesh=ProzeduraleGeometrie(mesh=PolygonMesh(ring,kanten,List(30_000) { (0..9).toList() }))
        val refs=ProzeduraleGeometrie(instanzen=listOf(GeometrieInstanz(mesh),GeometrieInstanz(mesh)))
        assertFailsWith<IllegalArgumentException> { GeometrieOperationen.realisieren(refs) }
    }
    @Test fun `Unabhaengig gebaute gleichwertige Instanz DAGs vergleichen gemeinsame Referenzpaare einmal`() {
        fun baum(): ProzeduraleGeometrie {
            var g=ProzeduraleGeometrie(mesh=PolygonMesh(List(10_000) { v(it.toDouble()) },emptyList(),emptyList()))
            repeat(15) { g=ProzeduraleGeometrie(instanzen=List(2) { GeometrieInstanz(g) }) }
            return g
        }
        val a=baum(); val b=baum()
        assertEquals(a.hashCode(),b.hashCode()); assertEquals(a,b)
    }
    @Test fun `Instanztiefe und exponentielle leere Traversierung bleiben begrenzt`() {
        var tief=ProzeduraleGeometrie()
        repeat(31) { tief=ProzeduraleGeometrie(instanzen=listOf(GeometrieInstanz(tief))) }
        assertFailsWith<IllegalArgumentException> { ProzeduraleGeometrie(instanzen=listOf(GeometrieInstanz(tief))) }
        var verzweigt=ProzeduraleGeometrie()
        repeat(15) { verzweigt=ProzeduraleGeometrie(instanzen=List(2) { GeometrieInstanz(verzweigt) }) }
        assertEquals(verzweigt.hashCode(),verzweigt.hashCode()); assertEquals(verzweigt,verzweigt)
        assertFailsWith<IllegalArgumentException> { GeometrieOperationen.realisieren(verzweigt) }
        assertFailsWith<IllegalArgumentException> { ProzeduraleGeometrie(instanzen=List(2) { GeometrieInstanz(verzweigt) }) }
    }
}
