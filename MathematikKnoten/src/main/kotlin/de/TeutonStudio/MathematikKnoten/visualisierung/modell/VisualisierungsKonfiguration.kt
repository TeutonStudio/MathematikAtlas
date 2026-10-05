package de.TeutonStudio.MathematikKnoten.visualisierung.modell

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenEigenschaft
import de.TeutonStudio.KnotenKartenVerwalter.daten.dezimalzahl
import de.TeutonStudio.KnotenKartenVerwalter.daten.ganzzahl
import de.TeutonStudio.KnotenKartenVerwalter.daten.objekt
import de.TeutonStudio.KnotenKartenVerwalter.daten.text

enum class RaumDimension { R1, R2, R3, C }
enum class DimensionsModus { Automatisch, Manuell }
data class AchsenZuordnung(val x: String, val y: String, val z: String?)
data class ZahlenBereich(val minimum: Double, val maximum: Double) {
    init {
        require(minimum.isFinite() && maximum.isFinite() && minimum < maximum && (maximum - minimum).isFinite()) {
            "Ein Achsenbereich benötigt zwei endliche Grenzen, eine endliche Spannweite und ein Minimum kleiner als sein Maximum."
        }
    }
}
data class AchsenBereiche(val x: ZahlenBereich, val y: ZahlenBereich, val z: ZahlenBereich?)
enum class FarbModus { Keine, FesteFarbe, Spektrum }
enum class MethodenDarstellungsModus { Automatisch, Bild, Funktionsgraph, Koordinatenausgabe }
data class FarbZuordnung(val modus: FarbModus, val variable: String?, val festeFarbe: Long?, val palette: String, val bereich: ZahlenBereich?)
data class SamplingKonfiguration(
    val auflösung2D: Int,
    val auflösung3D: Int,
    val toleranz: Double,
    val auflösung1D: Int = 160,
    val maximalesRasterBudget: Int = 250_000,
    val fensterBegrenztePrädikatsMengen: Boolean = false,
    val maximaleOrbitSchritte: Int = 256,
)
data class KameraZustand(
    val rotationX: Double,
    val rotationY: Double,
    val rotationZ: Double,
    val translationX: Double,
    val translationY: Double,
    val translationZ: Double,
    val zoom: Double,
) {
    fun istStandard(dimension: RaumDimension, epsilon: Double = 1e-6): Boolean {
        val relevanteWerte = when (dimension) {
            RaumDimension.R1 -> listOf(translationX, zoom - 1.0)
            RaumDimension.R2, RaumDimension.C -> listOf(translationX, translationY, zoom - 1.0)
            RaumDimension.R3 -> listOf(rotationX, rotationY, rotationZ, translationX, translationY, translationZ, zoom - 1.0)
        }
        return relevanteWerte.all { kotlin.math.abs(it) < epsilon }
    }
}

/** Persistierbare Konfiguration; Sampling-relevante Werte sind von der Kamera getrennt. */
data class VisualisierungsKonfiguration(
    val dimension: RaumDimension = RaumDimension.R2,
    val achsen: AchsenZuordnung = AchsenZuordnung("x", "y", "z"),
    val bereiche: AchsenBereiche = AchsenBereiche(ZahlenBereich(-10.0, 10.0), ZahlenBereich(-10.0, 10.0), ZahlenBereich(-10.0, 10.0)),
    val farbe: FarbZuordnung = FarbZuordnung(FarbModus.FesteFarbe, null, 0xFF2563EB, "Ozean", null),
    val sampling: SamplingKonfiguration = SamplingKonfiguration(72, 22, 0.08, auflösung1D = 160),
    val kamera: KameraZustand = KameraZustand(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0),
    val methodenModus: MethodenDarstellungsModus = MethodenDarstellungsModus.Automatisch,
    val festeSchnitte: Map<String, Double> = emptyMap(),
    val dimensionsModus: DimensionsModus = DimensionsModus.Automatisch,
) {
    fun samplingSignatur(): List<Any?> =
        listOf(dimension, achsen, bereiche, farbe.copy(festeFarbe = null), sampling, methodenModus, festeSchnitte)
    fun zuEigenschaften(): Map<String, KnotenEigenschaft> = mapOf(
        "dimension" to KnotenEigenschaft.Text(dimension.name),
        "dimensionsModus" to KnotenEigenschaft.Text(dimensionsModus.name),
        "achsen" to achsen.zuEigenschaft(),
        "bereiche" to bereiche.zuEigenschaft(),
        "farbe" to farbe.zuEigenschaft(),
        "methodenModus" to KnotenEigenschaft.Text(methodenModus.name),
        "sampling" to KnotenEigenschaft.Objekt(mapOf(
            "auflösung1D" to KnotenEigenschaft.Ganzzahl(sampling.auflösung1D),
            "auflösung2D" to KnotenEigenschaft.Ganzzahl(sampling.auflösung2D),
            "auflösung3D" to KnotenEigenschaft.Ganzzahl(sampling.auflösung3D),
            "toleranz" to KnotenEigenschaft.Dezimalzahl(sampling.toleranz),
            "maximalesRasterBudget" to KnotenEigenschaft.Ganzzahl(sampling.maximalesRasterBudget),
            "fensterBegrenztePrädikatsMengen" to KnotenEigenschaft.Text(sampling.fensterBegrenztePrädikatsMengen.toString()),
            "maximaleOrbitSchritte" to KnotenEigenschaft.Ganzzahl(sampling.maximaleOrbitSchritte),
        )),
        "festeSchnitte" to KnotenEigenschaft.Objekt(festeSchnitte.mapValues { KnotenEigenschaft.Dezimalzahl(it.value) }),
        "kamera" to kamera.zuEigenschaft(),
    )

    companion object {
        fun aus(eigenschaften: Map<String, KnotenEigenschaft>): VisualisierungsKonfiguration {
            val standard = VisualisierungsKonfiguration()
            val dimension = runCatching { RaumDimension.valueOf(eigenschaften.text("dimension", standard.dimension.name)) }.getOrDefault(standard.dimension)
            val dimensionsModus = runCatching {
                DimensionsModus.valueOf(
                    eigenschaften.text(
                        "dimensionsModus",
                        if ("dimension" in eigenschaften) DimensionsModus.Manuell.name else standard.dimensionsModus.name,
                    ),
                )
            }.getOrDefault(if ("dimension" in eigenschaften) DimensionsModus.Manuell else standard.dimensionsModus)
            val achsen = eigenschaften.objekt("achsen").zuAchsen(standard.achsen)
            val bereiche = eigenschaften.objekt("bereiche").zuBereiche(standard.bereiche)
            val farbe = eigenschaften.objekt("farbe").zuFarbe(standard.farbe)
            val methodenModus = runCatching {
                MethodenDarstellungsModus.valueOf(eigenschaften.text("methodenModus", standard.methodenModus.name))
            }.getOrDefault(standard.methodenModus)
            val samplingObjekt = eigenschaften.objekt("sampling")?.felder.orEmpty()
            val sampling = SamplingKonfiguration(
                samplingObjekt.ganzzahl("auflösung2D", standard.sampling.auflösung2D).coerceIn(16, 240),
                samplingObjekt.ganzzahl("auflösung3D", standard.sampling.auflösung3D).coerceIn(8, 64),
                samplingObjekt.dezimalzahl("toleranz", standard.sampling.toleranz).coerceIn(1e-5, 2.0),
                samplingObjekt.ganzzahl("auflösung1D", standard.sampling.auflösung1D).coerceIn(16, 2_000),
                samplingObjekt.ganzzahl("maximalesRasterBudget", standard.sampling.maximalesRasterBudget).coerceIn(1_000, 2_000_000),
                samplingObjekt.text("fensterBegrenztePrädikatsMengen", standard.sampling.fensterBegrenztePrädikatsMengen.toString()).toBooleanStrictOrNull()
                    ?: standard.sampling.fensterBegrenztePrädikatsMengen,
                samplingObjekt.ganzzahl("maximaleOrbitSchritte", standard.sampling.maximaleOrbitSchritte).coerceIn(1, 100_000),
            )
            val kamera = eigenschaften.objekt("kamera").zuKamera(standard.kamera)
            val festeSchnitte = eigenschaften.objekt("festeSchnitte")?.felder.orEmpty().mapNotNull { (name, wert) ->
                val zahl = (wert as? KnotenEigenschaft.Dezimalzahl)?.wert
                if (name.isBlank() || zahl == null || !zahl.isFinite()) null else name to zahl
            }.toMap()
            return VisualisierungsKonfiguration(dimension, achsen, bereiche, farbe, sampling, kamera, methodenModus, festeSchnitte, dimensionsModus)
        }
    }
}

private fun AchsenZuordnung.zuEigenschaft() = KnotenEigenschaft.Objekt(mapOf("x" to KnotenEigenschaft.Text(x), "y" to KnotenEigenschaft.Text(y), "z" to KnotenEigenschaft.Text(z ?: "")))
private fun AchsenBereiche.zuEigenschaft() = KnotenEigenschaft.Objekt(mapOf("x" to x.zuEigenschaft(), "y" to y.zuEigenschaft(), "z" to (z ?: ZahlenBereich(-10.0, 10.0)).zuEigenschaft()))
private fun ZahlenBereich.zuEigenschaft() = KnotenEigenschaft.Objekt(mapOf("minimum" to KnotenEigenschaft.Dezimalzahl(minimum), "maximum" to KnotenEigenschaft.Dezimalzahl(maximum)))
private fun FarbZuordnung.zuEigenschaft() = KnotenEigenschaft.Objekt(
    mapOf(
        "modus" to KnotenEigenschaft.Text(modus.name),
        "variable" to KnotenEigenschaft.Text(variable ?: ""),
        "farbe" to KnotenEigenschaft.Farbe(festeFarbe ?: 0xFF2563EB),
        "palette" to KnotenEigenschaft.Text(palette),
        "bereichAktiv" to KnotenEigenschaft.Text((bereich != null).toString()),
        "bereich" to (bereich ?: ZahlenBereich(-1.0, 1.0)).zuEigenschaft(),
    ),
)
private fun KameraZustand.zuEigenschaft() = KnotenEigenschaft.Objekt(mapOf("rotationX" to KnotenEigenschaft.Dezimalzahl(rotationX), "rotationY" to KnotenEigenschaft.Dezimalzahl(rotationY), "rotationZ" to KnotenEigenschaft.Dezimalzahl(rotationZ), "translationX" to KnotenEigenschaft.Dezimalzahl(translationX), "translationY" to KnotenEigenschaft.Dezimalzahl(translationY), "translationZ" to KnotenEigenschaft.Dezimalzahl(translationZ), "zoom" to KnotenEigenschaft.Dezimalzahl(zoom)))
private fun KnotenEigenschaft.Objekt?.zuAchsen(standard: AchsenZuordnung): AchsenZuordnung { val f = this?.felder.orEmpty(); return AchsenZuordnung(f.text("x", standard.x), f.text("y", standard.y), f.text("z", standard.z ?: "").ifBlank { null }) }
private fun KnotenEigenschaft.Objekt?.zuBereiche(standard: AchsenBereiche): AchsenBereiche { val f = this?.felder.orEmpty(); return AchsenBereiche(f.objekt("x").zuBereich(standard.x), f.objekt("y").zuBereich(standard.y), f.objekt("z").zuBereich(standard.z ?: ZahlenBereich(-10.0, 10.0))) }
private fun KnotenEigenschaft.Objekt?.zuBereich(standard: ZahlenBereich): ZahlenBereich { val f = this?.felder.orEmpty(); val min = f.dezimalzahl("minimum", standard.minimum); val max = f.dezimalzahl("maximum", standard.maximum); return if (min.isFinite() && max.isFinite() && min < max && (max - min).isFinite()) ZahlenBereich(min, max) else standard }
private fun KnotenEigenschaft.Objekt?.zuFarbe(standard: FarbZuordnung): FarbZuordnung {
    val f = this?.felder.orEmpty()
    val modus = runCatching { FarbModus.valueOf(f.text("modus", standard.modus.name)) }.getOrDefault(standard.modus)
    val bereich = when ((f["bereichAktiv"] as? KnotenEigenschaft.Text)?.wert?.toBooleanStrictOrNull()) {
        false -> null
        true -> f.objekt("bereich").zuBereich(standard.bereich ?: ZahlenBereich(-1.0, 1.0))
        null -> if (f.isEmpty()) standard.bereich else f.objekt("bereich").zuBereich(standard.bereich ?: ZahlenBereich(-1.0, 1.0))
    }
    return FarbZuordnung(
        modus,
        f.text("variable", standard.variable ?: "").ifBlank { null },
        (f["farbe"] as? KnotenEigenschaft.Farbe)?.argb ?: standard.festeFarbe,
        f.text("palette", standard.palette),
        bereich,
    )
}
private fun KnotenEigenschaft.Objekt?.zuKamera(standard: KameraZustand): KameraZustand { val f = this?.felder.orEmpty(); return KameraZustand(f.dezimalzahl("rotationX", standard.rotationX), f.dezimalzahl("rotationY", standard.rotationY), f.dezimalzahl("rotationZ", standard.rotationZ), f.dezimalzahl("translationX", standard.translationX), f.dezimalzahl("translationY", standard.translationY), f.dezimalzahl("translationZ", standard.translationZ), f.dezimalzahl("zoom", standard.zoom).coerceIn(0.1, 20.0)) }
