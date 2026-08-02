package de.TeutonStudio.MathematikAtlas.speicher

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.cbrt
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@JvmInline
value class ProfilFarbe private constructor(val rgbHex: String) {
    val rgbInt: Int get() = rgbHex.removePrefix("#").toInt(16)
    val argbLong: Long get() = 0xFF000000L or rgbInt.toLong()

    companion object {
        val Standard = ProfilFarbe("#1D4ED8")

        fun parse(text: String?): ProfilFarbe? {
            val roh = text?.trim()?.removePrefix("#") ?: return null
            val erweitert = when {
                roh.length == 3 && roh.all(Char::isHexDigit) -> roh.flatMap { listOf(it, it) }.joinToString("")
                roh.length == 6 && roh.all(Char::isHexDigit) -> roh
                else -> return null
            }
            return ProfilFarbe("#${erweitert.uppercase()}")
        }
    }
}

data class RgbFarbe(val rot: Double, val gruen: Double, val blau: Double) {
    init {
        require(rot in 0.0..1.0 && gruen in 0.0..1.0 && blau in 0.0..1.0)
    }

    val argbLong: Long
        get() {
            val r = (rot * 255.0).toInt().coerceIn(0, 255)
            val g = (gruen * 255.0).toInt().coerceIn(0, 255)
            val b = (blau * 255.0).toInt().coerceIn(0, 255)
            return 0xFF000000L or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
        }

    fun relativeLeuchtdichte(): Double {
        fun linear(kanal: Double): Double = if (kanal <= 0.04045) kanal / 12.92
        else ((kanal + 0.055) / 1.055).pow(2.4)
        return 0.2126 * linear(rot) + 0.7152 * linear(gruen) + 0.0722 * linear(blau)
    }

    companion object {
        fun aus(farbe: ProfilFarbe): RgbFarbe {
            val rgb = farbe.rgbInt
            return RgbFarbe(
                ((rgb shr 16) and 0xFF) / 255.0,
                ((rgb shr 8) and 0xFF) / 255.0,
                (rgb and 0xFF) / 255.0,
            )
        }
    }
}

data class OklchFarbe(val helligkeit: Double, val chroma: Double, val farbtonGrad: Double)

fun kontrastVerhaeltnis(vordergrund: RgbFarbe, hintergrund: RgbFarbe): Double {
    val a = vordergrund.relativeLeuchtdichte()
    val b = hintergrund.relativeLeuchtdichte()
    return (max(a, b) + 0.05) / (min(a, b) + 0.05)
}

private fun RgbFarbe.zuOklch(): OklchFarbe {
    fun linear(kanal: Double): Double = if (kanal <= 0.04045) kanal / 12.92
    else ((kanal + 0.055) / 1.055).pow(2.4)
    val r = linear(rot)
    val g = linear(gruen)
    val b = linear(blau)
    val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
    val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
    val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
    val okL = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
    val okA = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
    val okB = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
    val hue = ((atan2(okB, okA) * 180.0 / PI) + 360.0) % 360.0
    return OklchFarbe(okL, sqrt(okA * okA + okB * okB), hue)
}

private fun OklchFarbe.zuRgbOhneBegrenzung(): Triple<Double, Double, Double> {
    val winkel = farbtonGrad * PI / 180.0
    val a = chroma * cos(winkel)
    val b = chroma * sin(winkel)
    val l_ = helligkeit + 0.3963377774 * a + 0.2158037573 * b
    val m_ = helligkeit - 0.1055613458 * a - 0.0638541728 * b
    val s_ = helligkeit - 0.0894841775 * a - 1.2914855480 * b
    val l = l_ * l_ * l_
    val m = m_ * m_ * m_
    val s = s_ * s_ * s_
    val rLin = 4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s
    val gLin = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s
    val bLin = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s
    fun srgb(kanal: Double): Double = if (kanal <= 0.0031308) 12.92 * kanal
    else 1.055 * kanal.pow(1.0 / 2.4) - 0.055
    return Triple(srgb(rLin), srgb(gLin), srgb(bLin))
}

/** Reduziert nur die Chroma, bis der gewünschte Ton in sRGB liegt. */
private fun OklchFarbe.zuBegrenztemRgb(): RgbFarbe {
    var unten = 0.0
    var oben = chroma.coerceAtLeast(0.0)
    var kandidat = copy(chroma = 0.0).zuRgbOhneBegrenzung()
    repeat(18) {
        val mitte = (unten + oben) / 2.0
        val probe = copy(chroma = mitte).zuRgbOhneBegrenzung()
        if (listOf(probe.first, probe.second, probe.third).all { it in 0.0..1.0 }) {
            unten = mitte
            kandidat = probe
        } else {
            oben = mitte
        }
    }
    return RgbFarbe(
        kandidat.first.coerceIn(0.0, 1.0),
        kandidat.second.coerceIn(0.0, 1.0),
        kandidat.third.coerceIn(0.0, 1.0),
    )
}

fun lesbareInhaltsFarbe(
    hintergrund: RgbFarbe,
    farbton: Double,
    mindestKontrast: Double = 4.5,
): RgbFarbe {
    val hell = OklchFarbe(0.97, 0.018, farbton).zuBegrenztemRgb()
    val dunkel = OklchFarbe(0.12, 0.018, farbton).zuBegrenztemRgb()
    val kandidaten = listOf(hell, dunkel).sortedByDescending { kontrastVerhaeltnis(it, hintergrund) }
    kandidaten.firstOrNull { kontrastVerhaeltnis(it, hintergrund) >= mindestKontrast }?.let { return it }
    return kandidaten.first()
}

data class ProfilFarbRollen(
    val primary: RgbFarbe,
    val onPrimary: RgbFarbe,
    val primaryContainer: RgbFarbe,
    val onPrimaryContainer: RgbFarbe,
    val secondary: RgbFarbe,
    val onSecondary: RgbFarbe,
    val secondaryContainer: RgbFarbe,
    val onSecondaryContainer: RgbFarbe,
    val tertiary: RgbFarbe,
    val onTertiary: RgbFarbe,
    val tertiaryContainer: RgbFarbe,
    val onTertiaryContainer: RgbFarbe,
    val background: RgbFarbe,
    val onBackground: RgbFarbe,
    val surface: RgbFarbe,
    val onSurface: RgbFarbe,
    val surfaceVariant: RgbFarbe,
    val onSurfaceVariant: RgbFarbe,
    val surfaceContainerLowest: RgbFarbe,
    val surfaceContainerLow: RgbFarbe,
    val surfaceContainer: RgbFarbe,
    val surfaceContainerHigh: RgbFarbe,
    val surfaceContainerHighest: RgbFarbe,
    val outline: RgbFarbe,
    val outlineVariant: RgbFarbe,
    val inverseSurface: RgbFarbe,
    val inverseOnSurface: RgbFarbe,
    val inversePrimary: RgbFarbe,
)

object ProfilFarbPalettenGenerator {
    fun erzeuge(quelle: ProfilFarbe, dunkel: Boolean): ProfilFarbRollen {
        val basis = RgbFarbe.aus(quelle).zuOklch()
        val hue = basis.farbtonGrad
        val chroma = basis.chroma.coerceIn(0.055, 0.19)
        fun ton(l: Double, c: Double = chroma, h: Double = hue): RgbFarbe =
            OklchFarbe(l.coerceIn(0.0, 1.0), c.coerceAtLeast(0.0), (h + 360.0) % 360.0).zuBegrenztemRgb()
        fun paar(hintergrund: RgbFarbe, kontrast: Double = 4.5) =
            lesbareInhaltsFarbe(hintergrund, hue, kontrast)

        val primary = ton(if (dunkel) 0.78 else 0.49, chroma)
        val primaryContainer = ton(if (dunkel) 0.31 else 0.91, chroma * 0.68)
        val secondary = ton(if (dunkel) 0.76 else 0.47, chroma * 0.72, hue + 34.0)
        val secondaryContainer = ton(if (dunkel) 0.29 else 0.90, chroma * 0.48, hue + 34.0)
        val tertiary = ton(if (dunkel) 0.76 else 0.47, chroma * 0.70, hue + 92.0)
        val tertiaryContainer = ton(if (dunkel) 0.29 else 0.90, chroma * 0.46, hue + 92.0)

        val background = ton(if (dunkel) 0.085 else 0.985, if (dunkel) 0.020 else 0.008)
        val surface = ton(if (dunkel) 0.105 else 0.995, if (dunkel) 0.018 else 0.006)
        val surfaceVariant = ton(if (dunkel) 0.205 else 0.915, if (dunkel) 0.035 else 0.022)
        val lowest = ton(if (dunkel) 0.065 else 1.0, if (dunkel) 0.013 else 0.004)
        val low = ton(if (dunkel) 0.125 else 0.975, if (dunkel) 0.021 else 0.009)
        val container = ton(if (dunkel) 0.155 else 0.955, if (dunkel) 0.026 else 0.012)
        val high = ton(if (dunkel) 0.195 else 0.925, if (dunkel) 0.031 else 0.016)
        val highest = ton(if (dunkel) 0.235 else 0.895, if (dunkel) 0.036 else 0.020)
        val outline = ton(if (dunkel) 0.64 else 0.48, 0.035)
        val outlineVariant = ton(if (dunkel) 0.34 else 0.78, 0.028)
        val inverseSurface = ton(if (dunkel) 0.92 else 0.20, 0.016)
        val inversePrimary = ton(if (dunkel) 0.47 else 0.80, chroma * 0.72)

        return ProfilFarbRollen(
            primary = primary,
            onPrimary = paar(primary),
            primaryContainer = primaryContainer,
            onPrimaryContainer = paar(primaryContainer),
            secondary = secondary,
            onSecondary = lesbareInhaltsFarbe(secondary, hue + 34.0),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = lesbareInhaltsFarbe(secondaryContainer, hue + 34.0),
            tertiary = tertiary,
            onTertiary = lesbareInhaltsFarbe(tertiary, hue + 92.0),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = lesbareInhaltsFarbe(tertiaryContainer, hue + 92.0),
            background = background,
            onBackground = paar(background),
            surface = surface,
            onSurface = paar(surface),
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = paar(surfaceVariant),
            surfaceContainerLowest = lowest,
            surfaceContainerLow = low,
            surfaceContainer = container,
            surfaceContainerHigh = high,
            surfaceContainerHighest = highest,
            outline = outline,
            outlineVariant = outlineVariant,
            inverseSurface = inverseSurface,
            inverseOnSurface = paar(inverseSurface),
            inversePrimary = inversePrimary,
        )
    }
}
