package de.TeutonStudio.MathematikAtlas.speicher

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class FarbEingabeModus { RGB, HSB, HSL, LAB, CMYK }

data class RgbTextZustand(val rot: String, val gruen: String, val blau: String)
data class HsbTextZustand(val farbton: String, val saettigung: String, val helligkeit: String)
data class HslTextZustand(val farbton: String, val saettigung: String, val helligkeit: String)
data class LabTextZustand(val helligkeit: String, val a: String, val b: String)
data class CmykTextZustand(val cyan: String, val magenta: String, val gelb: String, val schwarz: String)

data class FarbTextZustaende(
    val rgb: RgbTextZustand,
    val hsb: HsbTextZustand,
    val hsl: HslTextZustand,
    val lab: LabTextZustand,
    val cmyk: CmykTextZustand,
)

data class FarbBearbeitungsMetadaten(
    val bevorzugterHsbFarbton: Float,
    val bevorzugterHslFarbton: Double,
    val bevorzugteCmykZerlegung: CmykFarbe? = null,
)

sealed interface FarbEingabeFehler {
    val modus: FarbEingabeModus?
    val nachricht: String

    data class KanalAusserhalbBereich(
        override val modus: FarbEingabeModus,
        val kanal: String,
        val minimum: Double,
        val maximum: Double,
    ) : FarbEingabeFehler {
        override val nachricht: String = "$kanal muss zwischen ${formatiereGrenze(minimum)} und ${formatiereGrenze(maximum)} liegen."
    }

    data class UnvollstaendigeEingabe(override val modus: FarbEingabeModus) : FarbEingabeFehler {
        override val nachricht: String = "Die Eingabe ist noch unvollständig oder keine gültige Zahl."
    }

    data object LabAusserhalbSrgb : FarbEingabeFehler {
        override val modus: FarbEingabeModus = FarbEingabeModus.LAB
        override val nachricht: String = "Diese Lab-Farbe liegt außerhalb des unterstützten sRGB-Farbraums."
    }

    data object HexUngueltig : FarbEingabeFehler {
        override val modus: FarbEingabeModus? = null
        override val nachricht: String = "Erwartet wird #RRGGBB."
    }
}

/**
 * UI-unabhängiger Farbauswahlzustand mit deckendem sRGB als einziger Farbquelle.
 * Textfelder dürfen vorübergehend unvollständig sein, ohne [kanonisch] zu verändern.
 */
data class FarbEntwurf(
    val kanonisch: RgbFarbe,
    val modus: FarbEingabeModus,
    val texte: FarbTextZustaende,
    val metadaten: FarbBearbeitungsMetadaten,
    val hexText: String,
    val fehler: FarbEingabeFehler? = null,
) {
    val istGueltig: Boolean get() = fehler == null
    val fehlerText: String? get() = fehler?.nachricht

    val hsb: HsbFarbe get() = kanonisch.zuHsb(metadaten.bevorzugterHsbFarbton)
    val hsl: HslFarbe get() = kanonisch.zuHsl(metadaten.bevorzugterHslFarbton)
    val lab: LabFarbe get() = kanonisch.zuLabD50()
    val cmyk: CmykFarbe get() = metadaten.bevorzugteCmykZerlegung ?: kanonisch.zuCmyk()

    val rotText: String get() = texte.rgb.rot
    val gruenText: String get() = texte.rgb.gruen
    val blauText: String get() = texte.rgb.blau
    val farbtonText: String get() = texte.hsb.farbton
    val saettigungText: String get() = texte.hsb.saettigung
    val helligkeitText: String get() = texte.hsb.helligkeit

    /** Übergangshilfen für bestehende profilbezogene Aufrufer. */
    val letzteGueltigeFarbe: ProfilFarbe get() = kanonisch.zuProfilFarbe()
    val hsv: HsbFarbe get() = hsb

    fun mitModus(neuerModus: FarbEingabeModus): FarbEntwurf {
        if (neuerModus == modus) return this
        if (fehler == FarbEingabeFehler.HexUngueltig) return copy(modus = neuerModus)
        return copy(modus = neuerModus, fehler = validiere(neuerModus))
    }

    fun mitHex(text: String): FarbEntwurf {
        val begrenzt = text.take(7)
        val farbe = ProfilFarbe.parse(begrenzt)
            ?: return copy(hexText = begrenzt, fehler = FarbEingabeFehler.HexUngueltig)
        return neuAusRgb(
            farbe = RgbFarbe.aus(farbe),
            quelle = null,
            neueMetadaten = metadaten.copy(bevorzugteCmykZerlegung = null),
        )
    }

    fun mitRgb(
        rot: String = texte.rgb.rot,
        gruen: String = texte.rgb.gruen,
        blau: String = texte.rgb.blau,
    ): FarbEntwurf {
        val neueTexte = texte.copy(rgb = RgbTextZustand(rot.take(3), gruen.take(3), blau.take(3)))
        val werte = listOf(rot.toIntOrNull(), gruen.toIntOrNull(), blau.toIntOrNull())
        if (werte.any { it == null }) return copy(texte = neueTexte, fehler = FarbEingabeFehler.UnvollstaendigeEingabe(FarbEingabeModus.RGB))
        val namen = listOf("Rot", "Grün", "Blau")
        werte.forEachIndexed { index, wert ->
            if (wert !in 0..255) return copy(
                texte = neueTexte,
                fehler = FarbEingabeFehler.KanalAusserhalbBereich(FarbEingabeModus.RGB, namen[index], 0.0, 255.0),
            )
        }
        val rgb = RgbFarbe(werte[0]!! / 255.0, werte[1]!! / 255.0, werte[2]!! / 255.0)
        return neuAusRgb(
            farbe = rgb,
            quelle = FarbEingabeModus.RGB,
            aktiveTexte = neueTexte,
            neueMetadaten = metadaten.copy(bevorzugteCmykZerlegung = null),
        )
    }

    fun mitHsbText(
        farbton: String = texte.hsb.farbton,
        saettigung: String = texte.hsb.saettigung,
        helligkeit: String = texte.hsb.helligkeit,
    ): FarbEntwurf {
        val neueTexte = texte.copy(hsb = HsbTextZustand(farbton.take(8), saettigung.take(8), helligkeit.take(8)))
        val h = parseDezimal(farbton)
        val s = parseDezimal(saettigung)
        val v = parseDezimal(helligkeit)
        if (h == null || s == null || v == null) return copy(texte = neueTexte, fehler = FarbEingabeFehler.UnvollstaendigeEingabe(FarbEingabeModus.HSB))
        pruefeBereich(FarbEingabeModus.HSB, "Farbton", h, 0.0, 360.0)?.let { return copy(texte = neueTexte, fehler = it) }
        pruefeBereich(FarbEingabeModus.HSB, "Sättigung", s, 0.0, 100.0)?.let { return copy(texte = neueTexte, fehler = it) }
        pruefeBereich(FarbEingabeModus.HSB, "Helligkeit", v, 0.0, 100.0)?.let { return copy(texte = neueTexte, fehler = it) }
        val hue = normalisiereFarbton(h).toFloat()
        val farbe = HsbFarbe(hue, (s / 100.0).toFloat(), (v / 100.0).toFloat()).zuRgb()
        return neuAusRgb(
            farbe = farbe,
            quelle = FarbEingabeModus.HSB,
            aktiveTexte = neueTexte,
            neueMetadaten = metadaten.copy(
                bevorzugterHsbFarbton = hue,
                bevorzugterHslFarbton = hue.toDouble(),
                bevorzugteCmykZerlegung = null,
            ),
        )
    }

    fun mitHsb(
        farbton: Float = hsb.farbton,
        saettigung: Float = hsb.saettigung,
        helligkeit: Float = hsb.helligkeit,
    ): FarbEntwurf {
        val hue = normalisiereFarbton(farbton.toDouble()).toFloat()
        val farbe = HsbFarbe(hue, saettigung.coerceIn(0f, 1f), helligkeit.coerceIn(0f, 1f)).zuRgb()
        return neuAusRgb(
            farbe = farbe,
            quelle = FarbEingabeModus.HSB,
            neueMetadaten = metadaten.copy(
                bevorzugterHsbFarbton = hue,
                bevorzugterHslFarbton = hue.toDouble(),
                bevorzugteCmykZerlegung = null,
            ),
        )
    }

    /** Quellkompatibler Name der bisherigen HSV-API. */
    fun mitHsv(
        farbton: Float = hsb.farbton,
        saettigung: Float = hsb.saettigung,
        helligkeit: Float = hsb.helligkeit,
    ): FarbEntwurf = mitHsb(farbton, saettigung, helligkeit)

    fun mitHslText(
        farbton: String = texte.hsl.farbton,
        saettigung: String = texte.hsl.saettigung,
        helligkeit: String = texte.hsl.helligkeit,
    ): FarbEntwurf {
        val neueTexte = texte.copy(hsl = HslTextZustand(farbton.take(8), saettigung.take(8), helligkeit.take(8)))
        val h = parseDezimal(farbton)
        val s = parseDezimal(saettigung)
        val l = parseDezimal(helligkeit)
        if (h == null || s == null || l == null) return copy(texte = neueTexte, fehler = FarbEingabeFehler.UnvollstaendigeEingabe(FarbEingabeModus.HSL))
        pruefeBereich(FarbEingabeModus.HSL, "Farbton", h, 0.0, 360.0)?.let { return copy(texte = neueTexte, fehler = it) }
        pruefeBereich(FarbEingabeModus.HSL, "Sättigung", s, 0.0, 100.0)?.let { return copy(texte = neueTexte, fehler = it) }
        pruefeBereich(FarbEingabeModus.HSL, "Helligkeit", l, 0.0, 100.0)?.let { return copy(texte = neueTexte, fehler = it) }
        val hue = normalisiereFarbton(h)
        val farbe = HslFarbe(hue, s / 100.0, l / 100.0).zuRgb()
        return neuAusRgb(
            farbe = farbe,
            quelle = FarbEingabeModus.HSL,
            aktiveTexte = neueTexte,
            neueMetadaten = metadaten.copy(
                bevorzugterHsbFarbton = hue.toFloat(),
                bevorzugterHslFarbton = hue,
                bevorzugteCmykZerlegung = null,
            ),
        )
    }

    fun mitLabText(
        helligkeit: String = texte.lab.helligkeit,
        a: String = texte.lab.a,
        b: String = texte.lab.b,
    ): FarbEntwurf {
        val neueTexte = texte.copy(lab = LabTextZustand(helligkeit.take(9), a.take(9), b.take(9)))
        val l = parseDezimal(helligkeit)
        val aWert = parseDezimal(a)
        val bWert = parseDezimal(b)
        if (l == null || aWert == null || bWert == null) return copy(texte = neueTexte, fehler = FarbEingabeFehler.UnvollstaendigeEingabe(FarbEingabeModus.LAB))
        pruefeBereich(FarbEingabeModus.LAB, "L*", l, 0.0, 100.0)?.let { return copy(texte = neueTexte, fehler = it) }
        pruefeBereich(FarbEingabeModus.LAB, "a*", aWert, -128.0, 127.0)?.let { return copy(texte = neueTexte, fehler = it) }
        pruefeBereich(FarbEingabeModus.LAB, "b*", bWert, -128.0, 127.0)?.let { return copy(texte = neueTexte, fehler = it) }
        val rgb = LabFarbe(l, aWert, bWert).zuRgbOderNull()
            ?: return copy(texte = neueTexte, fehler = FarbEingabeFehler.LabAusserhalbSrgb)
        return neuAusRgb(
            farbe = rgb,
            quelle = FarbEingabeModus.LAB,
            aktiveTexte = neueTexte,
            neueMetadaten = metadaten.copy(bevorzugteCmykZerlegung = null),
        )
    }

    fun mitCmykText(
        cyan: String = texte.cmyk.cyan,
        magenta: String = texte.cmyk.magenta,
        gelb: String = texte.cmyk.gelb,
        schwarz: String = texte.cmyk.schwarz,
    ): FarbEntwurf {
        val neueTexte = texte.copy(cmyk = CmykTextZustand(cyan.take(8), magenta.take(8), gelb.take(8), schwarz.take(8)))
        val werte = listOf(parseDezimal(cyan), parseDezimal(magenta), parseDezimal(gelb), parseDezimal(schwarz))
        if (werte.any { it == null }) return copy(texte = neueTexte, fehler = FarbEingabeFehler.UnvollstaendigeEingabe(FarbEingabeModus.CMYK))
        val namen = listOf("Cyan", "Magenta", "Gelb", "Schwarz")
        werte.forEachIndexed { index, wert ->
            pruefeBereich(FarbEingabeModus.CMYK, namen[index], wert!!, 0.0, 100.0)?.let {
                return copy(texte = neueTexte, fehler = it)
            }
        }
        val cmyk = CmykFarbe(werte[0]!! / 100.0, werte[1]!! / 100.0, werte[2]!! / 100.0, werte[3]!! / 100.0)
        return neuAusRgb(
            farbe = cmyk.zuRgb(),
            quelle = FarbEingabeModus.CMYK,
            aktiveTexte = neueTexte,
            neueMetadaten = metadaten.copy(bevorzugteCmykZerlegung = cmyk),
        )
    }

    fun zuruecksetzen(standardFarbe: RgbFarbe): FarbEntwurf = von(standardFarbe, modus)

    /** Quellkompatibler Profilstandard. */
    fun zuruecksetzen(): FarbEntwurf = zuruecksetzen(RgbFarbe.aus(ProfilFarbe.Standard))

    private fun neuAusRgb(
        farbe: RgbFarbe,
        quelle: FarbEingabeModus?,
        aktiveTexte: FarbTextZustaende? = null,
        neueMetadaten: FarbBearbeitungsMetadaten,
    ): FarbEntwurf {
        val abgeleitet = von(
            farbe = farbe,
            modus = modus,
            bevorzugterHsbFarbton = neueMetadaten.bevorzugterHsbFarbton,
            bevorzugterHslFarbton = neueMetadaten.bevorzugterHslFarbton,
            bevorzugteCmykZerlegung = neueMetadaten.bevorzugteCmykZerlegung,
        )
        val synchron = when (quelle) {
            FarbEingabeModus.RGB -> abgeleitet.texte.copy(rgb = requireNotNull(aktiveTexte).rgb)
            FarbEingabeModus.HSB -> aktiveTexte?.let { abgeleitet.texte.copy(hsb = it.hsb) } ?: abgeleitet.texte
            FarbEingabeModus.HSL -> abgeleitet.texte.copy(hsl = requireNotNull(aktiveTexte).hsl)
            FarbEingabeModus.LAB -> abgeleitet.texte.copy(lab = requireNotNull(aktiveTexte).lab)
            FarbEingabeModus.CMYK -> abgeleitet.texte.copy(cmyk = requireNotNull(aktiveTexte).cmyk)
            null -> abgeleitet.texte
        }
        return abgeleitet.copy(texte = synchron, metadaten = neueMetadaten, fehler = null)
    }

    private fun validiere(zuPruefen: FarbEingabeModus): FarbEingabeFehler? = when (zuPruefen) {
        FarbEingabeModus.RGB -> validiereRgb(texte.rgb)
        FarbEingabeModus.HSB -> validiereHsb(texte.hsb)
        FarbEingabeModus.HSL -> validiereHsl(texte.hsl)
        FarbEingabeModus.LAB -> validiereLab(texte.lab)
        FarbEingabeModus.CMYK -> validiereCmyk(texte.cmyk)
    }

    companion object {
        fun von(farbe: ProfilFarbe): FarbEntwurf = von(RgbFarbe.aus(farbe))

        fun von(
            farbe: RgbFarbe,
            modus: FarbEingabeModus = FarbEingabeModus.RGB,
            bevorzugterHsbFarbton: Float? = null,
            bevorzugterHslFarbton: Double? = null,
            bevorzugteCmykZerlegung: CmykFarbe? = null,
        ): FarbEntwurf {
            val hsb = farbe.zuHsb(bevorzugterHsbFarbton)
            val hsl = farbe.zuHsl(bevorzugterHslFarbton)
            val lab = farbe.zuLabD50()
            val cmyk = bevorzugteCmykZerlegung ?: farbe.zuCmyk()
            val (r, g, b) = farbe.rgbKanaele()
            return FarbEntwurf(
                kanonisch = farbe,
                modus = modus,
                texte = FarbTextZustaende(
                    rgb = RgbTextZustand(r.toString(), g.toString(), b.toString()),
                    hsb = HsbTextZustand(
                        formatiereKanal(hsb.farbton.toDouble()),
                        formatiereKanal(hsb.saettigung * 100.0),
                        formatiereKanal(hsb.helligkeit * 100.0),
                    ),
                    hsl = HslTextZustand(
                        formatiereKanal(hsl.farbton),
                        formatiereKanal(hsl.saettigung * 100.0),
                        formatiereKanal(hsl.helligkeit * 100.0),
                    ),
                    lab = LabTextZustand(
                        formatiereKanal(lab.helligkeit),
                        formatiereKanal(lab.a),
                        formatiereKanal(lab.b),
                    ),
                    cmyk = CmykTextZustand(
                        formatiereKanal(cmyk.cyan * 100.0),
                        formatiereKanal(cmyk.magenta * 100.0),
                        formatiereKanal(cmyk.gelb * 100.0),
                        formatiereKanal(cmyk.schwarz * 100.0),
                    ),
                ),
                metadaten = FarbBearbeitungsMetadaten(
                    bevorzugterHsbFarbton = hsb.farbton,
                    bevorzugterHslFarbton = hsl.farbton,
                    bevorzugteCmykZerlegung = bevorzugteCmykZerlegung,
                ),
                hexText = farbe.rgbHex,
            )
        }
    }
}

typealias ProfilFarbEntwurf = FarbEntwurf

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

private fun validiereRgb(text: RgbTextZustand): FarbEingabeFehler? {
    val werte = listOf(text.rot.toIntOrNull(), text.gruen.toIntOrNull(), text.blau.toIntOrNull())
    if (werte.any { it == null }) return FarbEingabeFehler.UnvollstaendigeEingabe(FarbEingabeModus.RGB)
    val namen = listOf("Rot", "Grün", "Blau")
    werte.forEachIndexed { index, wert ->
        if (wert !in 0..255) return FarbEingabeFehler.KanalAusserhalbBereich(FarbEingabeModus.RGB, namen[index], 0.0, 255.0)
    }
    return null
}

private fun validiereHsb(text: HsbTextZustand): FarbEingabeFehler? = validiereDreiKanaele(
    FarbEingabeModus.HSB,
    listOf("Farbton" to Triple(parseDezimal(text.farbton), 0.0, 360.0), "Sättigung" to Triple(parseDezimal(text.saettigung), 0.0, 100.0), "Helligkeit" to Triple(parseDezimal(text.helligkeit), 0.0, 100.0)),
)

private fun validiereHsl(text: HslTextZustand): FarbEingabeFehler? = validiereDreiKanaele(
    FarbEingabeModus.HSL,
    listOf("Farbton" to Triple(parseDezimal(text.farbton), 0.0, 360.0), "Sättigung" to Triple(parseDezimal(text.saettigung), 0.0, 100.0), "Helligkeit" to Triple(parseDezimal(text.helligkeit), 0.0, 100.0)),
)

private fun validiereLab(text: LabTextZustand): FarbEingabeFehler? {
    val fehler = validiereDreiKanaele(
        FarbEingabeModus.LAB,
        listOf("L*" to Triple(parseDezimal(text.helligkeit), 0.0, 100.0), "a*" to Triple(parseDezimal(text.a), -128.0, 127.0), "b*" to Triple(parseDezimal(text.b), -128.0, 127.0)),
    )
    if (fehler != null) return fehler
    val lab = LabFarbe(parseDezimal(text.helligkeit)!!, parseDezimal(text.a)!!, parseDezimal(text.b)!!)
    return if (lab.zuRgbOderNull() == null) FarbEingabeFehler.LabAusserhalbSrgb else null
}

private fun validiereCmyk(text: CmykTextZustand): FarbEingabeFehler? {
    val kanäle = listOf(
        "Cyan" to parseDezimal(text.cyan),
        "Magenta" to parseDezimal(text.magenta),
        "Gelb" to parseDezimal(text.gelb),
        "Schwarz" to parseDezimal(text.schwarz),
    )
    if (kanäle.any { it.second == null }) return FarbEingabeFehler.UnvollstaendigeEingabe(FarbEingabeModus.CMYK)
    kanäle.forEach { (name, wert) ->
        if (wert !in 0.0..100.0) return FarbEingabeFehler.KanalAusserhalbBereich(FarbEingabeModus.CMYK, name, 0.0, 100.0)
    }
    return null
}

private fun validiereDreiKanaele(
    modus: FarbEingabeModus,
    kanäle: List<Pair<String, Triple<Double?, Double, Double>>>,
): FarbEingabeFehler? {
    if (kanäle.any { it.second.first == null }) return FarbEingabeFehler.UnvollstaendigeEingabe(modus)
    kanäle.forEach { (name, werte) ->
        val wert = requireNotNull(werte.first)
        if (wert !in werte.second..werte.third) return FarbEingabeFehler.KanalAusserhalbBereich(modus, name, werte.second, werte.third)
    }
    return null
}

private fun pruefeBereich(
    modus: FarbEingabeModus,
    kanal: String,
    wert: Double,
    minimum: Double,
    maximum: Double,
): FarbEingabeFehler? = if (wert in minimum..maximum) null
else FarbEingabeFehler.KanalAusserhalbBereich(modus, kanal, minimum, maximum)

private fun parseDezimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

private fun formatiereKanal(wert: Double): String {
    val gerundet = wert.roundToInt()
    if (abs(wert - gerundet) < 0.005) return gerundet.toString()
    return String.format(Locale.ROOT, "%.2f", wert).trimEnd('0').trimEnd('.')
}

private fun formatiereGrenze(wert: Double): String =
    if (wert == wert.toInt().toDouble()) wert.toInt().toString() else formatiereKanal(wert)
