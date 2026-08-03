package de.TeutonStudio.MathematikAtlas.speicher

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Deckende HSV-Farbe für den UI-lokalen Entwurfszustand. */
data class HsvProfilFarbe(
    val farbton: Float,
    val saettigung: Float,
    val helligkeit: Float,
) {
    init {
        require(farbton in 0f..360f)
        require(saettigung in 0f..1f)
        require(helligkeit in 0f..1f)
    }
}

data class ProfilFarbEntwurf(
    val letzteGueltigeFarbe: ProfilFarbe,
    val hsv: HsvProfilFarbe,
    val hexText: String,
    val rotText: String,
    val gruenText: String,
    val blauText: String,
    val fehler: String? = null,
) {
    val istGueltig: Boolean get() = fehler == null

    fun mitHex(text: String): ProfilFarbEntwurf {
        val begrenzt = text.take(7)
        val farbe = ProfilFarbe.parse(begrenzt)
            ?: return copy(hexText = begrenzt, fehler = "Erwartet wird #RRGGBB.")
        return von(farbe)
    }

    fun mitRgb(rot: String = rotText, gruen: String = gruenText, blau: String = blauText): ProfilFarbEntwurf {
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
        return von(profilFarbeAusRgb(r, g, b))
    }

    fun mitHsv(
        farbton: Float = hsv.farbton,
        saettigung: Float = hsv.saettigung,
        helligkeit: Float = hsv.helligkeit,
    ): ProfilFarbEntwurf = von(
        HsvProfilFarbe(
            farbton = ((farbton % 360f) + 360f) % 360f,
            saettigung = saettigung.coerceIn(0f, 1f),
            helligkeit = helligkeit.coerceIn(0f, 1f),
        ).zuProfilFarbe(),
    )

    fun zuruecksetzen(): ProfilFarbEntwurf = von(ProfilFarbe.Standard)

    companion object {
        fun von(farbe: ProfilFarbe): ProfilFarbEntwurf {
            val rgb = farbe.rgbInt
            val r = (rgb shr 16) and 0xFF
            val g = (rgb shr 8) and 0xFF
            val b = rgb and 0xFF
            return ProfilFarbEntwurf(
                letzteGueltigeFarbe = farbe,
                hsv = farbe.zuHsv(),
                hexText = farbe.rgbHex,
                rotText = r.toString(),
                gruenText = g.toString(),
                blauText = b.toString(),
            )
        }
    }
}

fun ProfilFarbe.zuHsv(): HsvProfilFarbe {
    val rgb = rgbInt
    val r = ((rgb shr 16) and 0xFF) / 255f
    val g = ((rgb shr 8) and 0xFF) / 255f
    val b = (rgb and 0xFF) / 255f
    val maximum = max(r, max(g, b))
    val minimum = min(r, min(g, b))
    val delta = maximum - minimum
    val farbton = when {
        delta == 0f -> 0f
        maximum == r -> 60f * (((g - b) / delta) % 6f)
        maximum == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }.let { if (it < 0f) it + 360f else it }
    return HsvProfilFarbe(
        farbton = farbton,
        saettigung = if (maximum == 0f) 0f else delta / maximum,
        helligkeit = maximum,
    )
}

fun HsvProfilFarbe.zuProfilFarbe(): ProfilFarbe {
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
    return profilFarbeAusRgb(
        ((r1 + m) * 255f).roundToInt().coerceIn(0, 255),
        ((g1 + m) * 255f).roundToInt().coerceIn(0, 255),
        ((b1 + m) * 255f).roundToInt().coerceIn(0, 255),
    )
}

private fun profilFarbeAusRgb(rot: Int, gruen: Int, blau: Int): ProfilFarbe = requireNotNull(
    ProfilFarbe.parse("#%02X%02X%02X".format(rot, gruen, blau)),
)
