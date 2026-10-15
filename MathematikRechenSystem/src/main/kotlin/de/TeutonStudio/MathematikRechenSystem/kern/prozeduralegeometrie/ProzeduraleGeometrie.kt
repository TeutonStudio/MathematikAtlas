package de.TeutonStudio.MathematikRechenSystem.kern

import java.util.Collections
import java.util.IdentityHashMap
import kotlin.math.cos
import kotlin.math.sin

internal fun <T> geometrieSnapshot(werte: List<T>): List<T> = Collections.unmodifiableList(ArrayList(werte))
internal fun <T> begrenzterGeometrieSnapshot(werte: List<T>, maximum: Int): List<T> {
    require(werte.size <= maximum) { "Geometriebudget überschritten." }
    return geometrieSnapshot(werte)
}

/** Endliche, numerische Modellkoordinaten; keine intrinsischen geometrischen Objekte. */
data class GeometrieVektor3(val x: Double, val y: Double, val z: Double) {
    init { require(x.isFinite() && y.isFinite() && z.isFinite()) { "Geometriekoordinaten müssen endlich sein." } }
    operator fun plus(b: GeometrieVektor3) = GeometrieVektor3(x+b.x,y+b.y,z+b.z)
    operator fun minus(b: GeometrieVektor3) = GeometrieVektor3(x-b.x,y-b.y,z-b.z)
    operator fun times(s: Double) = GeometrieVektor3(x*s,y*s,z*s)
    companion object { val Null = GeometrieVektor3(0.0,0.0,0.0); val Eins = GeometrieVektor3(1.0,1.0,1.0) }
}

/** Ungerichtete Kante; kanonische Endpunktreihenfolge für strukturellen Vergleich. */
class MeshKante(a: Int, b: Int) {
    val a = minOf(a,b); val b = maxOf(a,b)
    init { require(a >= 0 && b >= 0 && a != b) { "Ungültige Meshkante." } }
    override fun equals(other: Any?) = other is MeshKante && a == other.a && b == other.b
    override fun hashCode() = 31*a+b
}

object GeometrieGrenzen {
    const val PUNKTE = 100_000
    const val KANTEN = 200_000
    const val FLAECHEN = 100_000
    const val ECKEN = 500_000
    const val INSTANZEN = 10_000
    const val TIEFE = 32
    const val FELD_OPERATIONEN = 2_000_000
}

/** Polygonränder sind geordnet; alle Randkanten müssen in [kanten] vorkommen. */
class PolygonMesh(punkte: List<GeometrieVektor3>, kanten: List<MeshKante>, flaechen: List<List<Int>>) {
    val punkte = begrenzterGeometrieSnapshot(punkte,GeometrieGrenzen.PUNKTE)
    val kanten = begrenzterGeometrieSnapshot(kanten,GeometrieGrenzen.KANTEN)
    val flaechen: List<List<Int>> = run {
        require(flaechen.size <= GeometrieGrenzen.FLAECHEN && flaechen.sumOf { it.size.toLong() } <= GeometrieGrenzen.ECKEN)
        geometrieSnapshot(flaechen.map(::geometrieSnapshot))
    }
    init {
        require(this.punkte.size <= GeometrieGrenzen.PUNKTE && this.kanten.size <= GeometrieGrenzen.KANTEN && this.flaechen.size <= GeometrieGrenzen.FLAECHEN)
        require(this.flaechen.sumOf { it.size.toLong() } <= GeometrieGrenzen.ECKEN)
        require(this.kanten.distinct().size == this.kanten.size) { "Doppelte Meshkante." }
        require(this.kanten.all { it.b < this.punkte.size }) { "Kantenindex außerhalb des Meshs." }
        val rand = this.kanten.toHashSet()
        require(this.flaechen.all { f -> f.size >= 3 && f.distinct().size == f.size && f.all { it in this.punkte.indices } && f.indices.all { MeshKante(f[it],f[(it+1)%f.size]) in rand } }) { "Ungültiger Polygonrand." }
    }
    override fun equals(other: Any?) = other is PolygonMesh && punkte == other.punkte && kanten == other.kanten && flaechen == other.flaechen
    override fun hashCode() = 31*(31*punkte.hashCode()+kanten.hashCode())+flaechen.hashCode()
}

class Punktwolke(punkte: List<GeometrieVektor3>) {
    val punkte = begrenzterGeometrieSnapshot(punkte,GeometrieGrenzen.PUNKTE)
    init { require(this.punkte.size <= GeometrieGrenzen.PUNKTE) }
    override fun equals(other: Any?) = other is Punktwolke && punkte == other.punkte
    override fun hashCode() = punkte.hashCode()
}

/** Affine Matrix; Komposition a * b wendet zuerst b an. TRS benutzt Bogenmaß. */
class GeometrieTransformation private constructor(private val m: List<Double>) {
    init { require(m.all(Double::isFinite)) { "Transformation muss endlich sein." } }
    fun anwenden(p: GeometrieVektor3) = GeometrieVektor3(m[0]*p.x+m[1]*p.y+m[2]*p.z+m[3],m[4]*p.x+m[5]*p.y+m[6]*p.z+m[7],m[8]*p.x+m[9]*p.y+m[10]*p.z+m[11])
    operator fun times(b: GeometrieTransformation) = GeometrieTransformation(List(16) { i -> (0..3).sumOf { k -> m[(i/4)*4+k]*b.m[k*4+i%4] } })
    override fun equals(other: Any?) = other is GeometrieTransformation && m == other.m
    override fun hashCode() = m.hashCode()
    companion object {
        val Identitaet = GeometrieTransformation(List(16) { if(it/4==it%4) 1.0 else 0.0 })
        fun trs(translation: GeometrieVektor3 = GeometrieVektor3.Null, rotation: GeometrieVektor3 = GeometrieVektor3.Null, skalierung: GeometrieVektor3 = GeometrieVektor3.Eins): GeometrieTransformation {
            val cx=cos(rotation.x); val sx=sin(rotation.x); val cy=cos(rotation.y); val sy=sin(rotation.y); val cz=cos(rotation.z); val sz=sin(rotation.z)
            return GeometrieTransformation(listOf(cz*cy*skalierung.x,(cz*sy*sx-sz*cx)*skalierung.y,(cz*sy*cx+sz*sx)*skalierung.z,translation.x,sz*cy*skalierung.x,(sz*sy*sx+cz*cx)*skalierung.y,(sz*sy*cx-cz*sx)*skalierung.z,translation.y,-sy*skalierung.x,cy*sx*skalierung.y,cy*cx*skalierung.z,translation.z,0.0,0.0,0.0,1.0))
        }
    }
}

data class GeometrieInstanz(val geometrie: ProzeduraleGeometrie, val transformation: GeometrieTransformation = GeometrieTransformation.Identitaet)

/** Immutable Geometry Set: Mesh, Punktwolke und geteilte Instanzreferenzen. Keine Attribute in dieser Ausbaustufe. */
class ProzeduraleGeometrie(val mesh: PolygonMesh? = null, val punktwolke: Punktwolke? = null, instanzen: List<GeometrieInstanz> = emptyList()) : DarstellungsWert {
    val instanzen = begrenzterGeometrieSnapshot(instanzen,GeometrieGrenzen.INSTANZEN)
    internal val tiefe: Int = 1 + (this.instanzen.maxOfOrNull { it.geometrie.tiefe } ?: 0)
    internal val expandierteGroesse: Long = 1L + this.instanzen.sumOf { it.geometrie.expandierteGroesse }
    private val strukturellerHash = 31*(31*(mesh?.hashCode() ?: 0)+(punktwolke?.hashCode() ?: 0))+this.instanzen.hashCode()
    init { require(this.instanzen.size <= GeometrieGrenzen.INSTANZEN && tiefe <= GeometrieGrenzen.TIEFE && expandierteGroesse <= 100_000) { "Instanzgrenze überschritten." } }
    override fun equals(other: Any?) = other is ProzeduraleGeometrie && strukturellGleich(other,IdentityHashMap())
    private fun strukturellGleich(other: ProzeduraleGeometrie, paare: IdentityHashMap<ProzeduraleGeometrie,IdentityHashMap<ProzeduraleGeometrie,Boolean>>): Boolean {
        if(this === other) return true
        if(strukturellerHash != other.strukturellerHash) return false
        if(paare[this]?.containsKey(other) == true) return true
        if(mesh != other.mesh || punktwolke != other.punktwolke || instanzen.size != other.instanzen.size) return false
        paare.getOrPut(this) { IdentityHashMap() }[other]=true
        return instanzen.indices.all { i -> val a=instanzen[i]; val b=other.instanzen[i]; a.transformation == b.transformation && a.geometrie.strukturellGleich(b.geometrie,paare) }
    }
    override fun hashCode() = strukturellerHash
    override fun zuLatex() = "\\text{Geometrie: ${mesh?.punkte?.size ?: 0} Vertices, ${punktwolke?.punkte?.size ?: 0} Punkte, ${instanzen.size} Instanzen}"
}
