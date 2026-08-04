package de.TeutonStudio.MathematikAtlas.speicher

import kotlin.math.abs
import kotlin.math.cbrt
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Deckende HSB-Farbe. HSB entspricht HSV; Sättigung und Helligkeit liegen in 0–1. */
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

/** Deckende HSL-Farbe. Sättigung und Helligkeit liegen in 0–1. */
data class HslFarbe(
    val farbton: Double,
    val saettigung: Double,
    val helligkeit: Double,
) {
    init {
        require(farbton in 0.0..<360.0)
        require(saettigung in 0.0..1.0)
        require(helligkeit in 0.0..1.0)
    }
}

/** CIE L*a*b* mit D50-Weißpunkt. */
data class LabFarbe(
    val helligkeit: Double,
    val a: Double,
    val b: Double,
) {
    init {
        require(helligkeit in 0.0..100.0)
        require(a in -128.0..127.0)
        require(b in -128.0..127.0)
    }
}

/** Profilfreies generisches CMYK; alle Kanäle liegen in 0–1. */
data class CmykFarbe(
    val cyan: Double,
    val magenta: Double,
    val gelb: Double,
    val schwarz: Double,
) {
    init {
        require(cyan in 0.0..1.0)
        require(magenta in 0.0..1.0)
        require(gelb in 0.0..1.0)
        require(schwarz in 0.0..1.0)
    }
}

internal data class XyzFarbe(val x: Double, val y: Double, val z: Double)

fun RgbFarbe.zuHsb(bevorzugterFarbton: Float? = null): HsbFarbe {
    val maximum = max(rot, max(gruen, blau))
    val minimum = min(rot, min(gruen, blau))
    val delta = maximum - minimum
    val farbton = when {
        delta == 0.0 -> normalisiereFarbton((bevorzugterFarbton ?: 0f).toDouble())
        maximum == rot -> 60.0 * (((gruen - blau) / delta) % 6.0)
        maximum == gruen -> 60.0 * (((blau - rot) / delta) + 2.0)
        else -> 60.0 * (((rot - gruen) / delta) + 4.0)
    }.let(::normalisiereFarbton)
    return HsbFarbe(
        farbton = farbton.toFloat(),
        saettigung = (if (maximum == 0.0) 0.0 else delta / maximum).toFloat(),
        helligkeit = maximum.toFloat(),
    )
}

fun HsbFarbe.zuRgb(): RgbFarbe {
    val h = farbton.toDouble()
    val s = saettigung.toDouble()
    val v = helligkeit.toDouble()
    val chroma = v * s
    val x = chroma * (1.0 - abs((h / 60.0) % 2.0 - 1.0))
    val m = v - chroma
    val (r1, g1, b1) = when (h) {
        in 0.0..<60.0 -> Triple(chroma, x, 0.0)
        in 60.0..<120.0 -> Triple(x, chroma, 0.0)
        in 120.0..<180.0 -> Triple(0.0, chroma, x)
        in 180.0..<240.0 -> Triple(0.0, x, chroma)
        in 240.0..<300.0 -> Triple(x, 0.0, chroma)
        else -> Triple(chroma, 0.0, x)
    }
    return RgbFarbe(
        (r1 + m).coerceIn(0.0, 1.0),
        (g1 + m).coerceIn(0.0, 1.0),
        (b1 + m).coerceIn(0.0, 1.0),
    )
}

fun RgbFarbe.zuHsl(bevorzugterFarbton: Double? = null): HslFarbe {
    val maximum = max(rot, max(gruen, blau))
    val minimum = min(rot, min(gruen, blau))
    val delta = maximum - minimum
    val helligkeit = (maximum + minimum) / 2.0
    val farbton = when {
        delta == 0.0 -> normalisiereFarbton(bevorzugterFarbton ?: 0.0)
        maximum == rot -> 60.0 * (((gruen - blau) / delta) % 6.0)
        maximum == gruen -> 60.0 * (((blau - rot) / delta) + 2.0)
        else -> 60.0 * (((rot - gruen) / delta) + 4.0)
    }.let(::normalisiereFarbton)
    val nenner = 1.0 - abs(2.0 * helligkeit - 1.0)
    val saettigung = if (delta == 0.0 || nenner == 0.0) 0.0 else delta / nenner
    return HslFarbe(farbton, saettigung.coerceIn(0.0, 1.0), helligkeit)
}

fun HslFarbe.zuRgb(): RgbFarbe {
    val chroma = (1.0 - abs(2.0 * helligkeit - 1.0)) * saettigung
    val x = chroma * (1.0 - abs((farbton / 60.0) % 2.0 - 1.0))
    val m = helligkeit - chroma / 2.0
    val (r1, g1, b1) = when (farbton) {
        in 0.0..<60.0 -> Triple(chroma, x, 0.0)
        in 60.0..<120.0 -> Triple(x, chroma, 0.0)
        in 120.0..<180.0 -> Triple(0.0, chroma, x)
        in 180.0..<240.0 -> Triple(0.0, x, chroma)
        in 240.0..<300.0 -> Triple(x, 0.0, chroma)
        else -> Triple(chroma, 0.0, x)
    }
    return RgbFarbe(
        (r1 + m).coerceIn(0.0, 1.0),
        (g1 + m).coerceIn(0.0, 1.0),
        (b1 + m).coerceIn(0.0, 1.0),
    )
}

fun RgbFarbe.zuCmyk(): CmykFarbe {
    val schwarz = 1.0 - max(rot, max(gruen, blau))
    if (schwarz >= 1.0 - 1e-12) return CmykFarbe(0.0, 0.0, 0.0, 1.0)
    val rest = 1.0 - schwarz
    return CmykFarbe(
        cyan = ((1.0 - rot - schwarz) / rest).coerceIn(0.0, 1.0),
        magenta = ((1.0 - gruen - schwarz) / rest).coerceIn(0.0, 1.0),
        gelb = ((1.0 - blau - schwarz) / rest).coerceIn(0.0, 1.0),
        schwarz = schwarz.coerceIn(0.0, 1.0),
    )
}

fun CmykFarbe.zuRgb(): RgbFarbe = RgbFarbe(
    (1.0 - cyan) * (1.0 - schwarz),
    (1.0 - magenta) * (1.0 - schwarz),
    (1.0 - gelb) * (1.0 - schwarz),
)

fun RgbFarbe.zuLabD50(): LabFarbe {
    val d65 = XyzFarbe(
        x = 0.4124564 * rot.zuLinearSrgb() + 0.3575761 * gruen.zuLinearSrgb() + 0.1804375 * blau.zuLinearSrgb(),
        y = 0.2126729 * rot.zuLinearSrgb() + 0.7151522 * gruen.zuLinearSrgb() + 0.0721750 * blau.zuLinearSrgb(),
        z = 0.0193339 * rot.zuLinearSrgb() + 0.1191920 * gruen.zuLinearSrgb() + 0.9503041 * blau.zuLinearSrgb(),
    )
    val d50 = adaptiereD65NachD50(d65)
    val fx = labHilfsfunktion(d50.x / D50_X)
    val fy = labHilfsfunktion(d50.y / D50_Y)
    val fz = labHilfsfunktion(d50.z / D50_Z)
    return LabFarbe(
        helligkeit = (116.0 * fy - 16.0).coerceIn(0.0, 100.0),
        a = (500.0 * (fx - fy)).coerceIn(-128.0, 127.0),
        b = (200.0 * (fy - fz)).coerceIn(-128.0, 127.0),
    )
}

/** Liefert null, wenn die numerisch gültige Lab-Farbe außerhalb von sRGB liegt. */
fun LabFarbe.zuRgbOderNull(): RgbFarbe? {
    val fy = (helligkeit + 16.0) / 116.0
    val fx = fy + a / 500.0
    val fz = fy - b / 200.0
    val d50 = XyzFarbe(
        x = D50_X * labHilfsfunktionInvers(fx),
        y = D50_Y * labHilfsfunktionInvers(fy),
        z = D50_Z * labHilfsfunktionInvers(fz),
    )
    val d65 = adaptiereD50NachD65(d50)
    val rLinear = 3.2404542 * d65.x - 1.5371385 * d65.y - 0.4985314 * d65.z
    val gLinear = -0.9692660 * d65.x + 1.8760108 * d65.y + 0.0415560 * d65.z
    val bLinear = 0.0556434 * d65.x - 0.2040259 * d65.y + 1.0572252 * d65.z
    val linear = listOf(rLinear, gLinear, bLinear)
    if (linear.any { it < -GAMUT_TOLERANZ || it > 1.0 + GAMUT_TOLERANZ }) return null
    val rgb = linear.map { it.coerceIn(0.0, 1.0).zuSrgb() }
    if (rgb.any { it < -GAMUT_TOLERANZ || it > 1.0 + GAMUT_TOLERANZ }) return null
    return RgbFarbe(rgb[0].coerceIn(0.0, 1.0), rgb[1].coerceIn(0.0, 1.0), rgb[2].coerceIn(0.0, 1.0))
}

internal fun adaptiereD65NachD50(farbe: XyzFarbe): XyzFarbe = XyzFarbe(
    x = 1.0478112 * farbe.x + 0.0228866 * farbe.y - 0.0501270 * farbe.z,
    y = 0.0295424 * farbe.x + 0.9904844 * farbe.y - 0.0170491 * farbe.z,
    z = -0.0092345 * farbe.x + 0.0150436 * farbe.y + 0.7521316 * farbe.z,
)

internal fun adaptiereD50NachD65(farbe: XyzFarbe): XyzFarbe = XyzFarbe(
    x = 0.9555766 * farbe.x - 0.0230393 * farbe.y + 0.0631636 * farbe.z,
    y = -0.0282895 * farbe.x + 1.0099416 * farbe.y + 0.0210077 * farbe.z,
    z = 0.0122982 * farbe.x - 0.0204830 * farbe.y + 1.3299098 * farbe.z,
)

internal fun normalisiereFarbton(farbton: Double): Double {
    val normalisiert = ((farbton % 360.0) + 360.0) % 360.0
    return if (normalisiert == 0.0) 0.0 else normalisiert
}

private fun Double.zuLinearSrgb(): Double =
    if (this <= 0.04045) this / 12.92 else ((this + 0.055) / 1.055).pow(2.4)

private fun Double.zuSrgb(): Double =
    if (this <= 0.0031308) 12.92 * this else 1.055 * this.pow(1.0 / 2.4) - 0.055

private fun labHilfsfunktion(wert: Double): Double =
    if (wert > LAB_EPSILON) cbrt(wert) else (LAB_KAPPA * wert + 16.0) / 116.0

private fun labHilfsfunktionInvers(wert: Double): Double {
    val kubik = wert * wert * wert
    return if (kubik > LAB_EPSILON) kubik else (116.0 * wert - 16.0) / LAB_KAPPA
}

private const val D50_X = 0.96422
private const val D50_Y = 1.0
private const val D50_Z = 0.82521
private const val LAB_EPSILON = 216.0 / 24389.0
private const val LAB_KAPPA = 24389.0 / 27.0
private const val GAMUT_TOLERANZ = 1e-7
