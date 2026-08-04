package de.TeutonStudio.MathematikAtlas.speicher

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class FarbEingabeModus { RGB, HSB }

/**
 * Deckende HSB-Farbe. HSB entspricht hier HSV: Helligkeit ist der Value-Kanal.
 */
data class HsbFarbe(
    val farbton: Float,
    val saettigung: Float,
    val helligkeit: Float,
) {
    init {
        require(farbton in 0f..<360f)
        require(saettigung in 0f..1f)
        require(helligkeit in 0f..1f)
    }
}

/** Quellkompatibler Name der bisherigen Profilfarbauswahl. */
typealias HsvProfilFarbe = HsbFarbe

/**
 * Wiederverwendbarer, UI-unabhängiger Farbauswahlzustand.
 *
 * [kanonisch] ist die einzige gültige Farbquelle. Textfelder dürfen vorübergehend
 * ungültig sein, ohne diese Farbe zu überschreiben. Ein Moduswechsel ändert nur
 * die sichtbaren Bedienelemente und konvertiert die Farbe deshalb nicht erneut.
 */
data class FarbEntwurf(
    val kanonisch: RgbFarbe,
    val modus: FarbEingabeModus,
    val hsb: HsbFarbe,
    val hexText: String,
    val rotText: String,
    val gruenText: String,
    val blauText: String,
    val farbtonText: String,
    val saettigungText: String,
    val helligkeitText: String,
    val fehler: String? = null,
) {
    val istGueltig: Boolean get() = fehler == null

    /** Übergangshilfen für den bisherigen profilbezogenen Aufrufer. */
    val letzteGueltigeFarbe: ProfilFarbe get() = kanonisch.zuProfilFarbe()
    val hsv: HsbFarbe get() = hsb

    fun mitModus(neuerModus: FarbEingabeModus): FarbEntwurf = copy(modus = neuerModus)

    fun mitHex(text: String): FarbEntwurf {
        val begrenzt = text.take(7)
        val farbe = ProfilFarbe.parse(begrenzt)
            ?: return copy(hexText = begrenzt, fehler = "Erwartet wird #RRGGBB.")
        return von(RgbFarbe.aus(farbe), modus, bevorzugterFarbton = hsb.farbton)
    }

    fun mitRgb(
        rot: String = rotText,
        gruen: String = gruenText,
        blau: String = blauText,
    ): FarbEntwurf {
        val r = rot.toIntOrNull()
        val g = gruen.toIntOrNull()
        val b = blau.toIntOrNull()
        if (r == null || g == null || b == null || r !in 0..255 || g !in 0..255 || b !in 0..255) {
            return copy(
                rotText = rot.take(3),
                gruenText = gruen.take(3),
                blauText = blau.take(3),
                fehler = "RGB-Kanäle müssen ganze Zahlen von 0 bis 255 sein.",
            )
        }
        return von(
            RgbFarbe(r / 255.0, g / 255.0, b / 255.0),
            modus,
            bevorzugterFarbton = hsb.farbton,
        )
    }

    fun mitHsbText(
        farbton: String = farbtonText,
        saettigung: String = saettigungText,
        helligkeit: String = helligkeitText,
    ): FarbEntwurf {
        val h = farbton.toFloatOrNull()
        val s = saettigung.toFloatOrNull()
        val b = helligkeit.toFloatOrNull()
        if (h == null || s == null || b == null || h !in 0f..360f || s !in 0f..100f || b !in 0f..100f) {
            return copy(
                farbtonText = farbton.take(6),
                saettigungText = saettigung.take(6),
                helligkeitText = helligkeit.take(6),
                fehler = "HSB erwartet Farbton 0–360° sowie Sättigung und Helligkeit 0–100 %.",
            )
        }
        return mitHsb(
            farbton = h,
            saettigung = s / 100f,
            helligkeit = b / 100f,
        )
    }

    fun mitHsb(
        farbton: Float = hsb.farbton,
        saettigung: Float = hsb.saettigung,
        helligkeit: Float = hsb.helligkeit,
    ): FarbEntwurf {
        val normalisiert = HsbFarbe(
            farbton = normalisiereFarbton(farbton),
            saettigung = saettigung.coerceIn(0f, 1f),
            helligkeit = helligkeit.coerceIn(0f, 1f),
        )
        return vonHsb(normalisiert, modus)
    }

    /** Quellkompatibler Name der bisherigen HSV-API. */
    fun mitHsv(
        farbton: Float = hsb.farbton,
        saettigung: Float = hsb.saettigung,
        helligkeit: Float = hsb.helligkeit,
    ): FarbEntwurf = mitHsb(farbton, saettigung, helligkeit)

    fun zuruecksetzen(standardFarbe: RgbFarbe): FarbEntwurf =
        von(standardFarbe, modus, bevorzugterFarbton = standardFarbe.zuHsb().farbton)

    /** Quellkompatibler Profilstandard. Wiederverwendbare Dialoge übergeben ihren Standard explizit. */
    fun zuruecksetzen(): FarbEntwurf = zuruecksetzen(RgbFarbe.aus(ProfilFarbe.Standard))

    companion object {
        fun von(farbe: ProfilFarbe): FarbEntwurf = von(RgbFarbe.aus(farbe))

        fun von(
            farbe: RgbFarbe,
            modus: FarbEingabeModus = FarbEingabeModus.RGB,
            bevorzugterFarbton: Float? = null,
        ): FarbEntwurf {
            val hsb = farbe.zuHsb(bevorzugterFarbton)
            val (r, g, b) = farbe.rgbKanaele()
            return FarbEntwurf(
                kanonisch = farbe,
                modus = modus,
                hsb = hsb,
                hexText = farbe.rgbHex,
                rotText = r.toString(),
                gruenText = g.toString(),
                blauText = b.toString(),
                farbtonText = formatiereKanal(hsb.farbton),
                saettigungText = formatiereKanal(hsb.saettigung * 100f),
                helligkeitText = formatiereKanal(hsb.helligkeit * 100f),
            )
        }

        private fun vonHsb(hsb: HsbFarbe, modus: FarbEingabeModus): FarbEntwurf {
            val farbe = hsb.zuRgb()
            val (r, g, b) = farbe.rgbKanaele()
            return FarbEntwurf(
                kanonisch = farbe,
                modus = modus,
                hsb = hsb,
                hexText = farbe.rgbHex,
                rotText = r.toString(),
                gruenText = g.toString(),
                blauText = b.toString(),
                farbtonText = formatiereKanal(hsb.farbton),
                saettigungText = formatiereKanal(hsb.saettigung * 100f),
                helligkeitText = formatiereKanal(hsb.helligkeit * 100f),
            )
        }
    }
}

typealias ProfilFarbEntwurf = FarbEntwurf

fun RgbFarbe.zuHsb(bevorzugterFarbton: Float? = null): HsbFarbe {
    val r = rot.toFloat()
    val g = gruen.toFloat()
    val b = blau.toFloat()
    val maximum = max(r, max(g, b))
    val minimum = min(r, min(g, b))
    val delta = maximum - minimum
    val farbton = when {
        delta == 0f -> normalisiereFarbton(bevorzugterFarbton ?: 0f)
        maximum == r -> 60f * (((g - b) / delta) % 6f)
        maximum == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }.let(::normalisiereFarbton)
    return HsbFarbe(
        farbton = farbton,
        saettigung = if (maximum == 0f) 0f else delta / maximum,
        helligkeit = maximum,
    )
}

fun HsbFarbe.zuRgb(): RgbFarbe {
    val chroma = helligkeit * saettigung
    val x = chroma * (1f - abs((farbton / 60f) % 2f - 1f))
    val m = helligkeit - chroma
    val (r1, g1, b1) = when (farbton) {
        in 0f..<60f -> Triple(chroma, x, 0f)
        in 60f..<120f -> Triple(x, chroma, 0f)
        in 120f..<180f -> Triple(0f, chroma, x)
        in 180f..<240f -> Triple(0f, x, chroma)
        in 240f..<300f -> Triple(x, 0f, chroma)
        else -> Triple(chroma, 0f, x)
    }
    return RgbFarbe(
        (r1 + m).toDouble().coerceIn(0.0, 1.0),
        (g1 + m).toDouble().coerceIn(0.0, 1.0),
        (b1 + m).toDouble().coerceIn(0.0, 1.0),
    )
}

/** Quellkompatible Profilkonvertierungen auf Basis des gemeinsamen Farbmodells. */
fun ProfilFarbe.zuHsv(): HsbFarbe = RgbFarbe.aus(this).zuHsb()
fun HsbFarbe.zuProfilFarbe(): ProfilFarbe = zuRgb().zuProfilFarbe()

fun RgbFarbe.zuProfilFarbe(): ProfilFarbe = requireNotNull(ProfilFarbe.parse(rgbHex))

val RgbFarbe.rgbHex: String
    get() {
        val (r, g, b) = rgbKanaele()
        return "#%02X%02X%02X".format(r, g, b)
    }

internal fun RgbFarbe.rgbKanaele(): Triple<Int, Int, Int> = Triple(
    (rot * 255.0).roundToInt().coerceIn(0, 255),
    (gruen * 255.0).roundToInt().coerceIn(0, 255),
    (blau * 255.0).roundToInt().coerceIn(0, 255),
)

private fun normalisiereFarbton(farbton: Float): Float = ((farbton % 360f) + 360f) % 360f

private fun formatiereKanal(wert: Float): String {
    val gerundet = wert.roundToInt()
    return if (abs(wert - gerundet) < 0.005f) gerundet.toString() else "%.1f".format(wert)
}
