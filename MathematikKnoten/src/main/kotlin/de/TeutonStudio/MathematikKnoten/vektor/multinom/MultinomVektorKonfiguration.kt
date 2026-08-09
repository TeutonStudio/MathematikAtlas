package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

const val MULTINOMVEKTOR_ART = "mathematik.multinomVektor"
const val MULTINOM_AUSGABEFORM_PARAMETER = "ausgabeForm"
const val MULTINOM_AUSGABE_VEKTOR = "vektor"
const val MULTINOM_AUSGABE_TUPEL = "tupel"

fun konfiguriereMultinomVektor(
    knoten: KnotenDaten,
    ausgabeForm: String = knoten.parameter[MULTINOM_AUSGABEFORM_PARAMETER] ?: MULTINOM_AUSGABE_VEKTOR,
    orientierung: String = knoten.parameter[VEKTOR_ORIENTIERUNG_PARAMETER] ?: VEKTOR_ORIENTIERUNG_SPALTE,
): KnotenDaten {
    require(knoten.art == MULTINOMVEKTOR_ART) { "Nur Multinomvektoren können so konfiguriert werden." }
    val form = if (ausgabeForm == MULTINOM_AUSGABE_TUPEL) MULTINOM_AUSGABE_TUPEL else MULTINOM_AUSGABE_VEKTOR
    val orient = if (orientierung == VEKTOR_ORIENTIERUNG_ZEILE) VEKTOR_ORIENTIERUNG_ZEILE else VEKTOR_ORIENTIERUNG_SPALTE
    val vorhandene = knoten.anschlüsse.associateBy { it.name }
    val x = multinomEingang(vorhandene["x"], "x", 0)
    val dim = multinomEingang(vorhandene["dim"], "dim", 1)
    val zielArt = when {
        form == MULTINOM_AUSGABE_TUPEL -> MathematikAnschlussArten.Tupel.id
        orient == VEKTOR_ORIENTIERUNG_ZEILE -> MathematikAnschlussArten.ZeilenVektor.id
        else -> MathematikAnschlussArten.SpaltenVektor.id
    }
    val alterAusgang = knoten.anschlüsse.firstOrNull {
        it.richtung == AnschlussRichtung.Ausgang && it.name == "wert" && it.art == zielArt
    }
    val wert = alterAusgang?.copy(
        name = "wert",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = zielArt,
        reihenfolge = 0,
        kannSichErweitern = false,
        dynamischErzeugt = false,
    ) ?: AnschlussDaten(
        name = "wert",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = zielArt,
    )
    return knoten.copy(
        name = "Multinomvektor",
        anschlüsse = listOf(x, dim, wert),
        parameter = knoten.parameter + mapOf(
            MULTINOM_AUSGABEFORM_PARAMETER to form,
            VEKTOR_ORIENTIERUNG_PARAMETER to orient,
        ),
    )
}

private fun multinomEingang(
    vorhanden: AnschlussDaten?,
    name: String,
    reihenfolge: Int,
): AnschlussDaten = vorhanden?.copy(
    name = name,
    richtung = AnschlussRichtung.Eingang,
    kante = AnschlussKante.Links,
    art = MathematikAnschlussArten.Zahl.id,
    reihenfolge = reihenfolge,
    kannSichErweitern = false,
    dynamischErzeugt = false,
    zulässigeArten = emptySet(),
) ?: AnschlussDaten(
    name = name,
    richtung = AnschlussRichtung.Eingang,
    kante = AnschlussKante.Links,
    art = MathematikAnschlussArten.Zahl.id,
    reihenfolge = reihenfolge,
)
