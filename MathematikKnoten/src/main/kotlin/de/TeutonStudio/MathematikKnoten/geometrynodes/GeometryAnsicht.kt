package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikRechenSystem.kern.GeometrieVektor3
import kotlin.math.*

/** Flüchtiger Ansichtsstand, unabhängig von Geometrie, Feldern und Kartenaktionen. */
data class GeometryKamera(
    val yaw: Double = 0.6,
    val pitch: Double = 0.45,
    val zoom: Double = 1.0,
    val panX: Double = 0.0,
    val panY: Double = 0.0,
) {
    init { require(listOf(yaw,pitch,zoom,panX,panY).all(Double::isFinite) && zoom in 0.05..100.0) }
    fun drehen(dx: Double,dy: Double) = copy(yaw=(yaw+dx)%(2*PI),pitch=(pitch+dy).coerceIn(-PI/2,PI/2))
    fun verschieben(dx: Double,dy: Double) = copy(panX=(panX+dx).coerceIn(-100.0,100.0),panY=(panY+dy).coerceIn(-100.0,100.0))
    /** Anker und Pan in Anteilen der kürzeren Viewportseite, relativ zur Bildmitte. */
    fun zoomen(faktor: Double,ankerX: Double = 0.0,ankerY: Double = 0.0): GeometryKamera {
        require(faktor.isFinite() && faktor > 0.0 && ankerX.isFinite() && ankerY.isFinite())
        val neu=(zoom*faktor).coerceIn(0.05,100.0);val real=neu/zoom
        return copy(zoom=neu,panX=(ankerX-real*(ankerX-panX)).coerceIn(-100.0,100.0),panY=(ankerY-real*(ankerY-panY)).coerceIn(-100.0,100.0))
    }
    fun einpassen() = copy(zoom=1.0,panX=0.0,panY=0.0)
    companion object {
        val XY = GeometryKamera(yaw=0.0,pitch=-PI/2)
        val XZ = GeometryKamera(yaw=0.0,pitch=0.0)
        val YZ = GeometryKamera(yaw=-PI/2,pitch=0.0)
    }
}

data class GeometryBildKante(val a: Int,val b: Int)

/**
 * Ausschließlich abgeleiteter Renderplan. Gemeinsame Kantenendpunkte werden je Frame
 * einmal projiziert, ohne fachliche Vertices zu verschweißen.
 */
class GeometryAnsicht(daten: GeometryVorschauDaten) {
    val positionen: List<GeometrieVektor3>
    val kanten: List<GeometryBildKante>
    val punktIndizes: List<Int>
    init {
        val indizes=LinkedHashMap<GeometrieVektor3,Int>()
        fun index(p: GeometrieVektor3) = indizes.getOrPut(p) { indizes.size }
        punktIndizes=daten.punkte.map(::index)
        kanten=daten.linien.map { GeometryBildKante(index(it.a),index(it.b)) }
        val welt=indizes.keys.toList()
        // Erst achsenweise zentrieren: eine riesige konstante Translation darf
        // kleine Ausdehnungen auf anderen Achsen nicht zum Unterlaufen bringen.
        fun weltMitte(f: (GeometrieVektor3) -> Double): Double {
            if(welt.isEmpty()) return 0.0
            val min=welt.minOf(f);val max=welt.maxOf(f);val spanne=max-min
            return if(spanne.isFinite()) min+spanne/2 else min/2+max/2
        }
        val weltZentrum=GeometrieVektor3(weltMitte { it.x },weltMitte { it.y },weltMitte { it.z })
        val delta=welt.map { it-weltZentrum }
        val norm=delta.maxOfOrNull { maxOf(abs(it.x),abs(it.y),abs(it.z)) }?.takeIf { it > 0 } ?: 1.0
        val vor=delta.map { GeometrieVektor3(it.x/norm,it.y/norm,it.z/norm) }
        // Nachzentrieren erhält auch bei subnormalen Koordinaten die Bildmitte.
        fun mitte(f: (GeometrieVektor3) -> Double) = if(vor.isEmpty()) 0.0 else (vor.minOf(f)+vor.maxOf(f))/2
        val zentrum=GeometrieVektor3(mitte { it.x },mitte { it.y },mitte { it.z })
        val radius=vor.maxOfOrNull { maxOf(abs(it.x-zentrum.x),abs(it.y-zentrum.y),abs(it.z-zentrum.z)) }?.takeIf { it > 0 } ?: 1.0
        positionen=vor.map { GeometrieVektor3((it.x-zentrum.x)/radius,(it.y-zentrum.y)/radius,(it.z-zentrum.z)/radius) }
    }
}

/** Orthografische Kamera mit identischem Maßstab für X/Y/Z. */
class GeometryProjektion(private val kamera: GeometryKamera,private val breite: Double,private val hoehe: Double) {
    private val c=cos(kamera.yaw);private val s=sin(kamera.yaw)
    private val cp=cos(kamera.pitch);private val sp=sin(kamera.pitch)
    private val seite=minOf(breite,hoehe)
    init { require(breite.isFinite() && hoehe.isFinite() && breite>=0 && hoehe>=0) }
    fun richtung(p: GeometrieVektor3): GeometrieVektor3 {
        val x=c*p.x-s*p.y;val y=s*p.x+c*p.y
        return GeometrieVektor3(x,cp*p.z-sp*y,cp*y+sp*p.z)
    }
    fun punkt(p: GeometrieVektor3): GeometrieVektor3 {
        val r=richtung(p);val faktor=seite*0.26*kamera.zoom
        return GeometrieVektor3(breite/2+kamera.panX*seite+r.x*faktor,hoehe/2+kamera.panY*seite-r.y*faktor,r.z)
    }
    fun alle(ansicht: GeometryAnsicht) = ansicht.positionen.map(::punkt)
}
