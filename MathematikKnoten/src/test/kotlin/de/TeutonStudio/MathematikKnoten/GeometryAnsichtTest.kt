package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikRechenSystem.kern.*
import kotlin.test.*

class GeometryAnsichtTest {
    private fun ansicht(vararg p: GeometrieVektor3) = GeometryAnsicht(GeometryVorschauDaten(p.toList(),emptyList(),false))
    private fun bild(geometrie: ProzeduraleGeometrie) = GeometryProjektion(GeometryKamera.XY,800.0,400.0).alle(GeometryAnsicht(geometryVorschau(geometrie)))
    private fun span(p: List<GeometrieVektor3>) = p.maxOf { it.x }-p.minOf { it.x }

    @Test fun `weit verschobener Wuerfel behaelt sichtbare Bildausdehnung`() {
        val cube=GeometrieOperationen.wuerfel()
        val fern=GeometrieOperationen.transformieren(cube,GeometrieTransformation.trs(GeometrieVektor3(1e12,-1e12,0.0)))
        val a=bild(cube);val b=bild(fern)
        assertTrue(span(b)>200);assertEquals(span(a),span(b),0.1)
        assertEquals(400.0,(b.maxOf { it.x }+b.minOf { it.x })/2,0.1)
    }
    @Test fun `sehr kleine und sehr grosse Koordinaten bleiben endlich und eingepasst`() {
        for(groesse in listOf(Double.MIN_VALUE,1e-250,1.0,1e250,Double.MAX_VALUE)) {
            val a=ansicht(GeometrieVektor3(-groesse,-groesse,-groesse),GeometrieVektor3(groesse,groesse,groesse))
            val p=GeometryProjektion(GeometryKamera(),600.0,400.0).alle(a)
            assertTrue(p.all { it.x.isFinite() && it.y.isFinite() && it.z.isFinite() })
            assertTrue(p.all { it.x in 0.0..600.0 && it.y in 0.0..400.0 })
        }
    }
    @Test fun `einzelner Punkt und leere Geometrie benoetigen keinen kuenstlichen Radius`() {
        assertTrue(GeometryProjektion(GeometryKamera(),0.0,0.0).alle(ansicht()).isEmpty())
        val p=GeometryProjektion(GeometryKamera(),600.0,400.0).alle(ansicht(GeometrieVektor3(1e200,1e200,1e200))).single()
        assertEquals(300.0,p.x);assertEquals(200.0,p.y)
    }
    @Test fun `riesige konstante Translation verschluckt keine winzige orthogonale Ausdehnung`() {
        val a=ansicht(GeometrieVektor3(1e308,0.0,0.0),GeometrieVektor3(1e308,1e-20,0.0))
        val p=GeometryProjektion(GeometryKamera.XY,800.0,400.0).alle(a)
        assertEquals(208.0,p[0].y-p[1].y,1e-10)
        assertEquals(400.0,p[0].x);assertEquals(400.0,p[1].x)
        assertEquals(200.0,(p[0].y+p[1].y)/2)
    }
    @Test fun `subnormale asymmetrische Ausdehnung wird mittig eingepasst`() {
        val a=ansicht(GeometrieVektor3.Null,GeometrieVektor3(Double.MIN_VALUE,0.0,0.0))
        val p=GeometryProjektion(GeometryKamera.XY,800.0,400.0).alle(a)
        assertEquals(208.0,p[1].x-p[0].x,1e-10)
        assertEquals(400.0,(p[0].x+p[1].x)/2)
    }
    @Test fun `alle Achsen behalten denselben mathematischen Massstab`() {
        val a=ansicht(GeometrieVektor3.Null,GeometrieVektor3(10.0,0.0,0.0),GeometrieVektor3(0.0,1.0,0.0))
        val p=GeometryProjektion(GeometryKamera.XY,800.0,400.0).alle(a)
        assertEquals((p[1].x-p[0].x)/10,p[0].y-p[2].y,1e-10)
    }
    @Test fun `gemeinsame Kantenendpunkte werden nur einmal projiziert`() {
        val daten=geometryVorschau(GeometrieOperationen.wuerfel())
        val a=GeometryAnsicht(daten)
        assertEquals(32,daten.punkte.size+daten.linien.size*2)
        assertEquals(8,a.positionen.size);assertEquals(12,a.kanten.size)
        assertEquals(8,GeometryProjektion(GeometryKamera(),800.0,400.0).alle(a).size)
    }
    @Test fun `orthogonale Ansichten bilden richtige Achsen ohne Verzerrung ab`() {
        val x=GeometrieVektor3(1.0,0.0,0.0);val y=GeometrieVektor3(0.0,1.0,0.0);val z=GeometrieVektor3(0.0,0.0,1.0)
        for((k,h,v) in listOf(Triple(GeometryKamera.XY,x,y),Triple(GeometryKamera.XZ,x,z),Triple(GeometryKamera.YZ,y,z))) {
            val p=GeometryProjektion(k,800.0,400.0);val n=p.punkt(GeometrieVektor3.Null)
            assertEquals(104.0,p.punkt(h).x-n.x,1e-9);assertEquals(104.0,n.y-p.punkt(v).y,1e-9)
            assertEquals(n.y,p.punkt(h).y,1e-9);assertEquals(n.x,p.punkt(v).x,1e-9)
        }
    }
    @Test fun `Zoom haelt den Zeigeranker auch an Zoomgrenzen fest`() {
        val punkt=GeometrieVektor3(0.3,-0.5,0.2)
        for(zoom in listOf(1.0,99.0,0.051)) {
            val k=GeometryKamera(zoom=zoom,panX=0.1,panY=-0.1)
            val vorher=GeometryProjektion(k,800.0,400.0).punkt(punkt)
            val neu=k.zoomen(if(zoom<1) 0.1 else 2.0,(vorher.x-400)/400,(vorher.y-200)/400)
            val nachher=GeometryProjektion(neu,800.0,400.0).punkt(punkt)
            assertEquals(vorher.x,nachher.x,1e-9);assertEquals(vorher.y,nachher.y,1e-9)
        }
    }
    @Test fun `Verschieben und Einpassen aendern die Orientierung nicht`() {
        val k=GeometryKamera().drehen(0.8,-0.2).zoomen(2.0).verschieben(0.1,0.2)
        val p=GeometrieVektor3.Null
        assertEquals(440.0,GeometryProjektion(k,800.0,400.0).punkt(p).x,1e-10)
        assertEquals(280.0,GeometryProjektion(k,800.0,400.0).punkt(p).y,1e-10)
        val fit=k.einpassen();assertEquals(k.yaw,fit.yaw);assertEquals(k.pitch,fit.pitch)
        assertEquals(1.0,fit.zoom);assertEquals(0.0,fit.panX);assertEquals(0.0,fit.panY)
    }
}
