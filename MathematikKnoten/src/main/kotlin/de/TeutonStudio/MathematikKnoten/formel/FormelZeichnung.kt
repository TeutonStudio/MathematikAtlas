package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import java.util.UUID
import kotlin.math.*

const val FORMEL_ZEICHNUNG_EIGENSCHAFT = "formel.zeichnung"
const val FORMEL_AST_EIGENSCHAFT = "formel.ast"
const val FORMEL_INTERPRETATION_EIGENSCHAFT = "formel.interpretation"
const val MAX_FORMEL_STRICHE = 2048
const val MAX_FORMEL_PUNKTE = 131072

data class FormelStiftPunkt(val x: Double, val y: Double) {
    init { require(x.isFinite() && y.isFinite() && x in 0.0..1024.0 && y in 0.0..512.0) { "Stiftpunkt außerhalb des logischen Zeichenbereichs." } }
}
data class FormelVektorStrich(
    val id: String = UUID.randomUUID().toString(),
    val punkte: List<FormelStiftPunkt>,
    val farbe: Long = 0xff1565c0,
    val breite: Double = 3.0,
    /** Radierpfade bleiben als geordnete Vektorgesten erhalten, nicht als Foto/Raster. */
    val radierung: Boolean = false,
) {
    init {
        require(id.isNotBlank() && id.length <= 128)
        require(punkte.isNotEmpty() && punkte.size <= 16384)
        require(breite.isFinite() && breite in 0.1..128.0)
        require(farbe in 0..0xffffffffL)
    }
}
data class FormelZeichnung(val striche: List<FormelVektorStrich> = emptyList()) {
    init {
        require(striche.size <= MAX_FORMEL_STRICHE && striche.sumOf { it.punkte.size } <= MAX_FORMEL_PUNKTE)
        require(striche.map { it.id }.distinct().size == striche.size)
    }
    fun eigenschaft(): KnotenEigenschaft = KnotenEigenschaft.Objekt(mapOf(
        "version" to KnotenEigenschaft.Ganzzahl(1),
        "breite" to KnotenEigenschaft.Ganzzahl(1024), "höhe" to KnotenEigenschaft.Ganzzahl(512),
        "striche" to KnotenEigenschaft.Liste(striche.map { s -> KnotenEigenschaft.Objekt(mapOf(
            "id" to KnotenEigenschaft.Text(s.id), "farbe" to KnotenEigenschaft.Farbe(s.farbe),
            "breite" to KnotenEigenschaft.Dezimalzahl(s.breite), "radierung" to KnotenEigenschaft.Wahrheitswert(s.radierung),
            "punkte" to KnotenEigenschaft.Liste(s.punkte.map { p -> KnotenEigenschaft.Objekt(mapOf(
                "x" to KnotenEigenschaft.Dezimalzahl(p.x), "y" to KnotenEigenschaft.Dezimalzahl(p.y))) }),
        )) }),
    ))
    companion object {
        fun lese(eigenschaft: KnotenEigenschaft?): FormelZeichnung {
            if (eigenschaft == null) return FormelZeichnung()
            val o = (eigenschaft as? KnotenEigenschaft.Objekt)?.felder ?: error("Zeichnungsobjekt fehlt.")
            require((o["version"] as? KnotenEigenschaft.Ganzzahl)?.wert == 1) { "Unbekannte Zeichnungsversion; Original bleibt erhalten." }
            require((o["breite"] as? KnotenEigenschaft.Ganzzahl)?.wert == 1024 && (o["höhe"] as? KnotenEigenschaft.Ganzzahl)?.wert == 512)
            val liste = (o["striche"] as? KnotenEigenschaft.Liste)?.werte ?: error("Strichliste fehlt.")
            require(liste.size <= MAX_FORMEL_STRICHE)
            require(liste.sumOf { ((it as? KnotenEigenschaft.Objekt)?.felder?.get("punkte") as? KnotenEigenschaft.Liste)?.werte?.size?.toLong() ?: 0L } <= MAX_FORMEL_PUNKTE)
            return FormelZeichnung(liste.map { wert ->
                val s = (wert as? KnotenEigenschaft.Objekt)?.felder ?: error("Strich ungültig.")
                val punkte = (s["punkte"] as? KnotenEigenschaft.Liste)?.werte ?: error("Punkte fehlen.")
                require(punkte.size in 1..16384)
                FormelVektorStrich(id = (s["id"] as? KnotenEigenschaft.Text)?.wert ?: error("Strich-ID fehlt."),
                    punkte = punkte.map { p ->
                        val f = (p as? KnotenEigenschaft.Objekt)?.felder ?: error("Punkt ungültig.")
                        FormelStiftPunkt((f["x"] as? KnotenEigenschaft.Dezimalzahl)?.wert ?: error("x fehlt."),
                            (f["y"] as? KnotenEigenschaft.Dezimalzahl)?.wert ?: error("y fehlt."))
                    }, farbe = (s["farbe"] as? KnotenEigenschaft.Farbe)?.argb ?: error("Farbe fehlt."),
                    breite = (s["breite"] as? KnotenEigenschaft.Dezimalzahl)?.wert ?: error("Breite fehlt."),
                    radierung = (s["radierung"] as? KnotenEigenschaft.Wahrheitswert)?.wert ?: false)
            })
        }
    }
}

/** Eine abgeschlossene Pointergeste ist genau ein Entwurfsschritt. Original bleibt bei Abbruch unverändert. */
class FormelZeichnungsEntwurf(val original: KnotenEigenschaft?) {
    private val lesen = runCatching { FormelZeichnung.lese(original) }
    val diagnose: String? = lesen.exceptionOrNull()?.message
    var zeichnung = lesen.getOrDefault(FormelZeichnung()); private set
    private val zurück = mutableListOf<FormelZeichnung>()
    private val vor = mutableListOf<FormelZeichnung>()
    val kannRückgängig get() = zurück.isNotEmpty()
    val kannWiederholen get() = vor.isNotEmpty()
    fun geste(strich: FormelVektorStrich) {
        check(diagnose == null) { diagnose.orEmpty() }
        val neu = FormelZeichnung(zeichnung.striche + strich)
        zurück += zeichnung; vor.clear(); zeichnung = neu
    }
    fun rückgängig() { if (zurück.isNotEmpty()) { vor += zeichnung; zeichnung = zurück.removeAt(zurück.lastIndex) } }
    fun wiederholen() { if (vor.isNotEmpty()) { zurück += zeichnung; zeichnung = vor.removeAt(vor.lastIndex) } }
    fun übernehmen(): KnotenEigenschaft = if (diagnose != null) requireNotNull(original) else zeichnung.eigenschaft()
}

/** Radierpfade zerlegen Originalsegmente in sichtbare Vektorfragmente; die Historie bleibt unberührt. */
fun FormelZeichnung.sichtbareFragmente(maxPrüfungen: Int = 2_000_000): List<FormelVektorStrich> {
    var prüfungen = 0
    fun prüfeBudget() { check(++prüfungen <= maxPrüfungen) { "Darstellungsbudget für Radierfragmente erschöpft; Originalzeichnung bleibt erhalten." } }
    var sichtbar = emptyList<FormelVektorStrich>()
    striche.forEach { geste ->
        if (!geste.radierung) sichtbar = sichtbar + geste
        else (if (geste.punkte.size == 1) listOf(geste.punkte.single() to geste.punkte.single()) else geste.punkte.zipWithNext()).forEach { (radierer, radiererEnde) ->
            sichtbar = sichtbar.flatMap { strich ->
                val radius = (geste.breite + strich.breite) / 2
                fun abstand(p: FormelStiftPunkt): Double {
                    val ex=radiererEnde.x-radierer.x; val ey=radiererEnde.y-radierer.y
                    val länge2=ex*ex+ey*ey
                    val t=if (länge2 == 0.0) 0.0 else (((p.x-radierer.x)*ex+(p.y-radierer.y)*ey)/länge2).coerceIn(0.0,1.0)
                    return hypot(p.x-radierer.x-t*ex,p.y-radierer.y-t*ey)
                }
                if (strich.punkte.maxOf { it.x } < min(radierer.x,radiererEnde.x)-radius ||
                    strich.punkte.minOf { it.x } > max(radierer.x,radiererEnde.x)+radius ||
                    strich.punkte.maxOf { it.y } < min(radierer.y,radiererEnde.y)-radius ||
                    strich.punkte.minOf { it.y } > max(radierer.y,radiererEnde.y)+radius) return@flatMap listOf(strich)
                val fragmente = mutableListOf<MutableList<FormelStiftPunkt>>()
                var aktuell = mutableListOf<FormelStiftPunkt>()
                fun ende() { if (aktuell.isNotEmpty()) fragmente += aktuell; aktuell = mutableListOf() }
                if (strich.punkte.size == 1) {
                    val p = strich.punkte.single()
                    if (abstand(p) > radius) aktuell += p
                }
                strich.punkte.zipWithNext().forEach { (a,b) ->
                    prüfeBudget()
                    val dx = b.x-a.x; val dy=b.y-a.y; val aa=dx*dx+dy*dy
                    if (aa == 0.0) return@forEach
                    fun kreisSchnitte(mitte: FormelStiftPunkt): List<Double> {
                        val fx=a.x-mitte.x; val fy=a.y-mitte.y
                        val bb=2*(fx*dx+fy*dy); val cc=fx*fx+fy*fy-radius*radius
                        val diskr=bb*bb-4*aa*cc
                        return if (diskr > 0) listOf((-bb-sqrt(diskr))/(2*aa),(-bb+sqrt(diskr))/(2*aa)) else emptyList()
                    }
                    val ex=radiererEnde.x-radierer.x; val ey=radiererEnde.y-radierer.y; val länge=hypot(ex,ey)
                    val streifen = if (länge > 0) {
                        val ux=ex/länge; val uy=ey/länge
                        val x=(a.x-radierer.x)*ux+(a.y-radierer.y)*uy; val vx=dx*ux+dy*uy
                        val y=-(a.x-radierer.x)*uy+(a.y-radierer.y)*ux; val vy=-dx*uy+dy*ux
                        (if (vx != 0.0) listOf(-x/vx,(länge-x)/vx) else emptyList()) +
                            (if (vy != 0.0) listOf((-radius-y)/vy,(radius-y)/vy) else emptyList())
                    } else emptyList()
                    val schnitte = (kreisSchnitte(radierer)+kreisSchnitte(radiererEnde)+streifen).filter { it > 0 && it < 1 }.distinct()
                    val grenzen = listOf(0.0) + schnitte.sorted() + 1.0
                    grenzen.zipWithNext().forEach { (von,bis) ->
                        val mitte=(von+bis)/2
                        if (abstand(FormelStiftPunkt((a.x+mitte*dx).coerceIn(0.0,1024.0),(a.y+mitte*dy).coerceIn(0.0,512.0))) > radius) {
                            val start=FormelStiftPunkt((a.x+von*dx).coerceIn(0.0,1024.0),(a.y+von*dy).coerceIn(0.0,512.0)); val stop=FormelStiftPunkt((a.x+bis*dx).coerceIn(0.0,1024.0),(a.y+bis*dy).coerceIn(0.0,512.0))
                            if (aktuell.isNotEmpty() && aktuell.last()!=start) ende()
                            if (aktuell.lastOrNull()!=start) aktuell += start
                            aktuell += stop
                        } else ende()
                    }
                }
                ende()
                fragmente.mapIndexed { index, punkte -> strich.copy(id = strich.id.take(100) + ".f$index", punkte = punkte) }
            }
        }
    }
    return sichtbar
}

fun ladeFormelAst(knoten: KnotenDaten): de.TeutonStudio.MathematikRechenSystem.kern.FormelAusdruck? =
    (knoten.eigenschaften[FORMEL_AST_EIGENSCHAFT] as? KnotenEigenschaft.Text)?.wert?.let {
        runCatching { de.TeutonStudio.MathematikRechenSystem.kern.StrukturFormelCodec.dekodieren(it) }.getOrNull()
    }

/** Nur unmittelbar wiederholte Samples sind redundant; geschlossene Konturen bleiben geschlossen. */
fun verdichteFormelStiftPunkte(punkte: List<FormelStiftPunkt>): List<FormelStiftPunkt> = buildList {
    punkte.forEach { if (lastOrNull() != it) add(it) }
}
