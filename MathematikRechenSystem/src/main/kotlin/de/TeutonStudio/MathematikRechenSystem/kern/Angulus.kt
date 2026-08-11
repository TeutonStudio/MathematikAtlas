package de.TeutonStudio.MathematikRechenSystem.kern

import de.TeutonStudio.TypSystem.TypAusdruck
import de.TeutonStudio.TypSystem.TypTragend

/** Kanonische Winkeleinheiten des Atlas. */
enum class AngulusEinheit(val stabileId: String, val kurzname: String) {
    RADIAN("radian", "rad"),
    GRAD("grad", "°"),
    GON("gon", "gon"),
    ;

    companion object {
        fun vonIdOderNull(id: String?): AngulusEinheit? = entries.firstOrNull {
            id.equals(it.stabileId, ignoreCase = true) || id.equals(it.name, ignoreCase = true)
        }
    }
}

/**
 * Ein Winkel ist kein nackter Zahlenwert. Neben der Einheit trägt er den
 * Dimensionsbezug, beispielsweise ["x", "y"] für einen Winkel in der xy-Ebene.
 * Der Dimensionsbezug wird bei Einheitenumwandlungen unverändert weitergereicht.
 */
data class Angulus(
    val wert: ZahlAusdruck,
    val einheit: AngulusEinheit = AngulusEinheit.RADIAN,
    val dimensionen: List<String> = emptyList(),
) : MathematischesObjekt, TypTragend {
    init {
        require(dimensionen.none(String::isBlank)) { "Angulus-Dimensionen dürfen nicht leer sein." }
    }

    override fun zuLatex(): String = when (einheit) {
        AngulusEinheit.RADIAN -> "${wert.zuLatex()}\\,\\mathrm{rad}"
        AngulusEinheit.GRAD -> "${wert.zuLatex()}^{\\circ}"
        AngulusEinheit.GON -> "${wert.zuLatex()}\\,\\mathrm{gon}"
    }

    override val typAusdruck: TypAusdruck
        get() = MathematischeTypen.angulusTyp(einheit, dimensionen)
}

/** Exakte symbolische Einheitenumwandlung; keine Rundung wird in den Typkern eingeschmuggelt. */
fun Angulus.inEinheit(ziel: AngulusEinheit): Angulus {
    if (ziel == einheit) return this
    val radian = when (einheit) {
        AngulusEinheit.RADIAN -> wert
        AngulusEinheit.GRAD -> multiplikation(wert, Division(Pi, RationaleZahl.von(180)))
        AngulusEinheit.GON -> multiplikation(wert, Division(Pi, RationaleZahl.von(200)))
    }
    val zielWert = when (ziel) {
        AngulusEinheit.RADIAN -> radian
        AngulusEinheit.GRAD -> multiplikation(radian, Division(RationaleZahl.von(180), Pi))
        AngulusEinheit.GON -> multiplikation(radian, Division(RationaleZahl.von(200), Pi))
    }
    return copy(wert = zielWert, einheit = ziel)
}

fun angulus(
    wert: ZahlAusdruck,
    einheit: AngulusEinheit = AngulusEinheit.RADIAN,
    dimensionen: List<String> = emptyList(),
): Angulus = Angulus(wert, einheit, dimensionen)

enum class TupelKoordinatenArt { ALLGEMEIN, KARTESISCH, POLAR }

/**
 * Kartesisch: ausschließlich Zahlen. Polar: Radius als Zahl, danach mindestens
 * ein Angulus. Alle anderen Kombinationen bleiben gewöhnliche Tupel.
 */
fun Tupel.koordinatenArt(): TupelKoordinatenArt = when {
    elemente.isNotEmpty() && elemente.all { it is ZahlAusdruck } -> TupelKoordinatenArt.KARTESISCH
    elemente.size >= 2 && elemente.first() is ZahlAusdruck && elemente.drop(1).all { it is Angulus } ->
        TupelKoordinatenArt.POLAR
    else -> TupelKoordinatenArt.ALLGEMEIN
}

fun Tupel.koordinatenTypAusdruck(): TypAusdruck {
    val argumente = elemente.map { element ->
        when (element) {
            is Angulus -> element.typAusdruck
            is ZahlAusdruck -> TypAusdruck.Atom(MathematischeTypen.Zahl)
            else -> TypAusdruck.Unbekannt
        }
    }
    val konstruktor = when (koordinatenArt()) {
        TupelKoordinatenArt.KARTESISCH -> MathematischeTypen.KartesischesTupel
        TupelKoordinatenArt.POLAR -> MathematischeTypen.PolarTupel
        TupelKoordinatenArt.ALLGEMEIN -> MathematischeTypen.Tupel
    }
    return TypAusdruck.Parameterisiert(konstruktor, argumente)
}
