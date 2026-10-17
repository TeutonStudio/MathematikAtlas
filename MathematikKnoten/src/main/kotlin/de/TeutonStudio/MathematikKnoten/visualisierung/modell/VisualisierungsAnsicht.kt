package de.TeutonStudio.MathematikKnoten.visualisierung.modell

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenEigenschaft
import kotlin.math.*

/** Reine Zahlengeometrie; dieselbe Transformation für Zeichnung, Gesten und Treffertests. */
data class BildPunkt(val x: Double, val y: Double, val tiefe: Double = 0.0)

data class VisualisierungsTransformation(val konfiguration: VisualisierungsKonfiguration, val breite: Double, val höhe: Double) {
    init { require(breite > 0 && höhe > 0 && breite.isFinite() && höhe.isFinite()) }
    private val c get() = konfiguration
    val maßstab: Double = min(breite / (c.bereiche.x.maximum - c.bereiche.x.minimum),
        höhe / (c.bereiche.y.maximum - c.bereiche.y.minimum)) * c.kamera.zoom
    val xMaßstab get() = if (c.dimension == RaumDimension.R1) breite / (c.bereiche.x.maximum - c.bereiche.x.minimum) * c.kamera.zoom else maßstab
    private val mx get() = c.bereiche.x.minimum + (c.bereiche.x.maximum - c.bereiche.x.minimum) / 2
    private val my get() = c.bereiche.y.minimum + (c.bereiche.y.maximum - c.bereiche.y.minimum) / 2
    private val mz get() = c.bereiche.z?.let { it.minimum + (it.maximum - it.minimum) / 2 } ?: 0.0
    private val rx = Math.toRadians(c.kamera.rotationX)
    private val ry = Math.toRadians(c.kamera.rotationY)
    private val rz = Math.toRadians(c.kamera.rotationZ)
    private val cosX = cos(rx); private val sinX = sin(rx)
    private val cosY = cos(ry); private val sinY = sin(ry)
    private val cosZ = cos(rz); private val sinZ = sin(rz)
    private fun drehe(x0: Double, y0: Double, z0: Double): BildPunkt {
        if (c.dimension != RaumDimension.R3) return BildPunkt(x0, y0, z0)
        val x1 = x0 * cosY + z0 * sinY; val z1 = -x0 * sinY + z0 * cosY
        val y1 = y0 * cosX - z1 * sinX; val z2 = y0 * sinX + z1 * cosX
        return BildPunkt(x1 * cosZ - y1 * sinZ, x1 * sinZ + y1 * cosZ, z2)
    }
    fun bild(x: Double, y: Double, z: Double = 0.0): BildPunkt {
        val p = drehe(x - mx, y - my, z - mz)
        return BildPunkt(breite / 2 + (p.x + c.kamera.translationX) * xMaßstab,
            if (c.dimension == RaumDimension.R1) höhe / 2 else höhe / 2 - (p.y + c.kamera.translationY) * maßstab, p.tiefe)
    }
    /** In R3: Punkt auf der zum Bildschirm parallelen Ebene durch das Szenenzentrum. */
    fun welt(x: Double, y: Double, tiefe: Double = 0.0): BildPunkt {
        val bx = (x - breite / 2) / xMaßstab - c.kamera.translationX
        val by = if (c.dimension == RaumDimension.R1) 0.0 else (höhe / 2 - y) / maßstab - c.kamera.translationY
        if (c.dimension != RaumDimension.R3) return BildPunkt(bx + mx, if (c.dimension == RaumDimension.R1) 0.0 else by + my)
        val x1 = bx * cosZ + by * sinZ; val y1 = -bx * sinZ + by * cosZ
        val y2 = y1 * cosX + tiefe * sinX; val z1 = -y1 * sinX + tiefe * cosX
        return BildPunkt(x1 * cosY - z1 * sinY + mx, y2 + my, x1 * sinY + z1 * cosY + mz)
    }
    fun navigiere(dx: Double, dy: Double, faktor: Double, ankerX: Double, ankerY: Double, rotieren: Boolean = false): KameraZustand {
        val k = c.kamera
        if (rotieren) return k.copy(rotationX = (k.rotationX + dy * .5) % 360, rotationY = (k.rotationY + dx * .5) % 360,
            zoom = (k.zoom * faktor).coerceIn(.001, 10_000.0))
        val zoom = (k.zoom * faktor).coerceIn(.001, 10_000.0)
        val ratio = k.zoom / zoom
        return k.copy(zoom = zoom,
            translationX = k.translationX + dx / xMaßstab + (ankerX - breite / 2) / xMaßstab * (ratio - 1),
            translationY = if (c.dimension == RaumDimension.R1) k.translationY else k.translationY - dy / maßstab + (höhe / 2 - ankerY) / maßstab * (ratio - 1))
    }
    fun sichtBereiche(reserve: Double = 0.2): AchsenBereiche {
        if (c.dimension == RaumDimension.R3) {
            // Umschließende Kugel deckt jede Orientierung ab: Drehen braucht kein neues Sampling.
            // Pan und Zoom ändern diese räumliche Abdeckung, niemals eine Parameterdomäne.
            val hz = c.bereiche.z?.let { (it.maximum - it.minimum) / (2 * c.kamera.zoom) } ?: 0.0
            val radius = sqrt((breite / (2 * maßstab)).pow(2) + (höhe / (2 * maßstab)).pow(2) + hz.pow(2)) * (1 + reserve) +
                hypot(c.kamera.translationX, c.kamera.translationY)
            return AchsenBereiche(ZahlenBereich(mx - radius, mx + radius), ZahlenBereich(my - radius, my + radius), ZahlenBereich(mz - radius, mz + radius))
        }
        val a = welt(-breite * reserve / 2, höhe * (1 + reserve / 2))
        val b = welt(breite * (1 + reserve / 2), -höhe * reserve / 2)
        return AchsenBereiche(ZahlenBereich(a.x, b.x), if (c.dimension == RaumDimension.R1) c.bereiche.y else ZahlenBereich(a.y, b.y), c.bereiche.z)
    }
}

fun achsenSchritt(weltProPixel: Double, zielPixel: Double = 80.0): Double {
    val roh = weltProPixel * zielPixel
    if (!roh.isFinite() || roh <= 0) return 1.0
    val basis = 10.0.pow(floor(log10(roh)))
    return listOf(1.0, 2.0, 5.0, 10.0).first { it * basis >= roh } * basis
}

/** Nur die eigenen Felder ersetzen; andere Erweiterungen bleiben erhalten. */
fun VisualisierungsKonfiguration.inEigenschaften(bisher: Map<String, KnotenEigenschaft>): Map<String, KnotenEigenschaft> {
    fun ergänze(alt: Map<String, KnotenEigenschaft>, neu: Map<String, KnotenEigenschaft>): Map<String, KnotenEigenschaft> =
        alt + neu.mapValues { (key, wert) ->
            val vorher = alt[key]
            if (key != "festeSchnitte" && vorher is KnotenEigenschaft.Objekt && wert is KnotenEigenschaft.Objekt)
                KnotenEigenschaft.Objekt(ergänze(vorher.felder, wert.felder)) else wert
        }
    return ergänze(bisher, zuEigenschaften())
}

fun parseFesteSchnitte(text: String): Map<String, Double> {
    if (text.isBlank()) return emptyMap()
    val paare = text.split(';', ',').map { eintrag ->
        val teile = eintrag.split('=', limit = 2)
        require(teile.size == 2 && teile[0].trim().isNotEmpty()) { "Feste Parameter als Name=Wert angeben." }
        teile[0].trim() to (teile[1].trim().toDoubleOrNull()?.takeIf(Double::isFinite)
            ?: throw IllegalArgumentException("Ein Parameterwert muss eine endliche Zahl sein."))
    }
    require(paare.map { it.first }.distinct().size == paare.size) { "Parameter dürfen nicht mehrfach vorkommen." }
    return paare.toMap()
}

fun parseAusgabeKoordinaten(text: String): List<Int> {
    if (text.isBlank()) return emptyList()
    val ids = text.split(',').map { it.trim().toIntOrNull() ?: throw IllegalArgumentException("Ganze Koordinatenindizes erforderlich.") }
    require(ids.size in 1..3 && ids.all { it > 0 } && ids.distinct().size == ids.size) { "Ein bis drei verschiedene positive Koordinatenindizes erforderlich." }
    return ids
}

/** v1 rotierte auch Translation und Ursprung; v2 verwendet ein zentriertes Modell. */
internal fun migriereAlteVisualisierungsKamera(k: KameraZustand, dimension: RaumDimension, b: AchsenBereiche): KameraZustand {
    val ax = b.x.minimum + (b.x.maximum - b.x.minimum) / (2 * k.zoom)
    val ay = b.y.minimum + (b.y.maximum - b.y.minimum) / (2 * k.zoom)
    if (dimension != RaumDimension.R3) return k.copy(
        translationX = k.translationX + (b.x.maximum - b.x.minimum) * (1 - 1 / k.zoom) / 2,
        translationY = k.translationY + (b.y.maximum - b.y.minimum) * (1 - 1 / k.zoom) / 2)
    val mx = b.x.minimum + (b.x.maximum - b.x.minimum) / 2
    val my = b.y.minimum + (b.y.maximum - b.y.minimum) / 2
    val mz = b.z?.let { it.minimum + (it.maximum - it.minimum) / 2 } ?: 0.0
    val c = VisualisierungsKonfiguration(dimension=dimension, bereiche=b, kamera=k.copy(translationX=0.0,translationY=0.0,translationZ=0.0,zoom=1.0))
    val t = VisualisierungsTransformation(c,100.0,100.0)
    val rotiert = t.bild(2*mx+k.translationX,2*my+k.translationY,2*mz+k.translationZ)
    return k.copy(translationX=(rotiert.x-50)/t.maßstab-ax,translationY=(50-rotiert.y)/t.maßstab-ay,translationZ=0.0)
}

/** Dimension changes preserve valid axis names and provide the newly required axes together. */
fun VisualisierungsKonfiguration.mitDarstellungsRaum(raum: RaumDimension): VisualisierungsKonfiguration {
    val x = if (dimension == RaumDimension.C && raum != RaumDimension.C && achsen.x == "re") "x" else achsen.x.ifBlank { "x" }
    val y = if (dimension == RaumDimension.C && raum != RaumDimension.C && achsen.y == "im") "y" else achsen.y.ifBlank { "y" }
    return copy(dimension = raum, dimensionsModus = DimensionsModus.Manuell,
        ausgabeKoordinaten = ausgabeKoordinaten.takeIf { it.size == when (raum) { RaumDimension.R1 -> 1; RaumDimension.R3 -> 3; else -> 2 } }.orEmpty(),
        achsen = when (raum) {
            RaumDimension.R1 -> AchsenZuordnung(x, "", null)
            RaumDimension.C -> AchsenZuordnung("re", "im", null)
            RaumDimension.R2 -> AchsenZuordnung(x, y, null)
            RaumDimension.R3 -> AchsenZuordnung(x, y, achsen.z?.ifBlank { "z" } ?: "z")
        }, bereiche = if (raum == RaumDimension.R3 && bereiche.z == null) bereiche.copy(z = ZahlenBereich(-10.0, 10.0)) else bereiche)
}

/** Projection arity and display dimension change atomically, without an invalid intermediate model. */
fun VisualisierungsKonfiguration.mitAusgabeProjektion(indizes: List<Int>): VisualisierungsKonfiguration {
    require(indizes.isEmpty() || indizes.size in 1..3 && indizes.all { it > 0 } && indizes.distinct().size == indizes.size) {
        "Ein bis drei verschiedene positive Koordinatenindizes erforderlich."
    }
    if (indizes.isEmpty()) return copy(ausgabeKoordinaten = emptyList())
    val raum = when (indizes.size) { 1 -> RaumDimension.R1; 2 -> if (dimension == RaumDimension.C) RaumDimension.C else RaumDimension.R2; else -> RaumDimension.R3 }
    return mitDarstellungsRaum(raum).copy(ausgabeKoordinaten = indizes)
}
