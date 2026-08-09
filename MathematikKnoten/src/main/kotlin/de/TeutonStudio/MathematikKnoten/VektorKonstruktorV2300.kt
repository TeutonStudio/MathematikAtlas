package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import java.math.BigInteger

const val VEKTOR_KONSTRUKTOR_ART = "mathematik.vektor"
const val VEKTOR_ORIENTIERUNG_PARAMETER = "orientierung"
const val VEKTOR_ERZEUGUNGSART_PARAMETER = "erzeugungsArt"
const val VEKTOR_ORIENTIERUNG_SPALTE = "spalte"
const val VEKTOR_ORIENTIERUNG_ZEILE = "zeile"
const val VEKTOR_EINZEL_EINGABEN = "einzelEingaben"
const val VEKTOR_METHODE = "methode"

private val VEKTOR_ERZEUGUNGSARTEN = setOf(VEKTOR_EINZEL_EINGABEN, VEKTOR_METHODE)
private val VEKTOR_ORIENTIERUNGEN = setOf(VEKTOR_ORIENTIERUNG_SPALTE, VEKTOR_ORIENTIERUNG_ZEILE)

data class VektorKonstruktorKonfiguration(
    val erzeugungsArt: String,
    val orientierung: String,
)

fun vektorKonstruktorKonfiguration(knoten: KnotenDaten): VektorKonstruktorKonfiguration {
    val historischeZeile = knoten.art == "mathematik.zeilenVektor"
    return VektorKonstruktorKonfiguration(
        erzeugungsArt = knoten.parameter[VEKTOR_ERZEUGUNGSART_PARAMETER]
            ?.takeIf(VEKTOR_ERZEUGUNGSARTEN::contains)
            ?: VEKTOR_EINZEL_EINGABEN,
        orientierung = knoten.parameter[VEKTOR_ORIENTIERUNG_PARAMETER]
            ?.takeIf(VEKTOR_ORIENTIERUNGEN::contains)
            ?: if (historischeZeile) VEKTOR_ORIENTIERUNG_ZEILE else VEKTOR_ORIENTIERUNG_SPALTE,
    )
}

fun konfiguriereVektorKonstruktor(
    knoten: KnotenDaten,
    erzeugungsArt: String = vektorKonstruktorKonfiguration(knoten).erzeugungsArt,
    orientierung: String = vektorKonstruktorKonfiguration(knoten).orientierung,
): KnotenDaten {
    require(knoten.art in setOf(VEKTOR_KONSTRUKTOR_ART, "mathematik.zeilenVektor")) {
        "Nur Vektorkonstruktoren können so konfiguriert werden."
    }
    val art = erzeugungsArt.takeIf(VEKTOR_ERZEUGUNGSARTEN::contains) ?: VEKTOR_EINZEL_EINGABEN
    val orient = orientierung.takeIf(VEKTOR_ORIENTIERUNGEN::contains) ?: VEKTOR_ORIENTIERUNG_SPALTE
    val vorhandene = knoten.anschlüsse.associateBy { it.name }
    val eingänge = when (art) {
        VEKTOR_METHODE -> listOf(
            vektorKonstruktorEingang(vorhandene["dimension"], "dimension", MathematikAnschlussArten.Zahl.id, 0),
            vektorKonstruktorEingang(vorhandene["methode"], "methode", MathematikAnschlussArten.Methode.id, 1),
        )
        else -> {
            val anzahl = knoten.parameter["festeEingänge"]?.toIntOrNull()?.coerceAtLeast(2) ?: 2
            List(anzahl) { index ->
                val name = when (index) {
                    0 -> "a"
                    1 -> "b"
                    else -> "input${index + 1}"
                }
                vektorKonstruktorEingang(
                    vorhanden = vorhandene[name],
                    name = name,
                    art = MathematikAnschlussArten.Zahl.id,
                    reihenfolge = index,
                    erweiterbar = true,
                )
            }
        }
    }
    val zielArt = if (orient == VEKTOR_ORIENTIERUNG_ZEILE) {
        MathematikAnschlussArten.ZeilenVektor.id
    } else {
        MathematikAnschlussArten.SpaltenVektor.id
    }
    val alterAusgang = knoten.anschlüsse.firstOrNull {
        it.richtung == AnschlussRichtung.Ausgang && it.name == "vektor" && it.art == zielArt
    }
    val ausgang = alterAusgang?.copy(
        name = "vektor",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = zielArt,
        reihenfolge = 0,
        kannSichErweitern = false,
        dynamischErzeugt = false,
    ) ?: AnschlussDaten(
        name = "vektor",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = zielArt,
    )
    val parameter = knoten.parameter + mapOf(
        VEKTOR_ERZEUGUNGSART_PARAMETER to art,
        VEKTOR_ORIENTIERUNG_PARAMETER to orient,
    ) + if (art == VEKTOR_EINZEL_EINGABEN) {
        mapOf(
            "festeEingänge" to (knoten.parameter["festeEingänge"]?.toIntOrNull()?.coerceAtLeast(2) ?: 2).toString(),
            "operatorAnzeige" to (knoten.parameter["operatorAnzeige"] ?: "wert"),
        )
    } else {
        emptyMap()
    }
    return knoten.copy(
        art = VEKTOR_KONSTRUKTOR_ART,
        name = "Vektor",
        anschlüsse = eingänge + ausgang,
        parameter = parameter,
    )
}

private fun vektorKonstruktorEingang(
    vorhanden: AnschlussDaten?,
    name: String,
    art: AnschlussArtId,
    reihenfolge: Int,
    erweiterbar: Boolean = false,
): AnschlussDaten = vorhanden?.copy(
    name = name,
    richtung = AnschlussRichtung.Eingang,
    kante = AnschlussKante.Links,
    art = art,
    reihenfolge = reihenfolge,
    kannSichErweitern = erweiterbar,
    dynamischErzeugt = false,
    zulässigeArten = emptySet(),
) ?: AnschlussDaten(
    name = name,
    richtung = AnschlussRichtung.Eingang,
    kante = AnschlussKante.Links,
    art = art,
    reihenfolge = reihenfolge,
    kannSichErweitern = erweiterbar,
)

object VektorKonstruktorV2300Vorlagen {
    val standard = KnotenVorlage(
        art = VEKTOR_KONSTRUKTOR_ART,
        name = "Vektor",
        kategorie = "Vektoren",
        beschreibung = "Erzeugt einen Zeilen- oder Spaltenvektor aus Einzelwerten oder aus Dimension und Indexmethode.",
        standardGröße = GraphGröße(235f, 130f),
        anschlüsse = listOf(
            vektorKonstruktorEingang(null, "a", MathematikAnschlussArten.Zahl.id, 0, erweiterbar = true),
            vektorKonstruktorEingang(null, "b", MathematikAnschlussArten.Zahl.id, 1, erweiterbar = true),
            AnschlussDaten(
                name = "vektor",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = MathematikAnschlussArten.SpaltenVektor.id,
            ),
        ),
        standardParameter = mapOf(
            VEKTOR_ERZEUGUNGSART_PARAMETER to VEKTOR_EINZEL_EINGABEN,
            VEKTOR_ORIENTIERUNG_PARAMETER to VEKTOR_ORIENTIERUNG_SPALTE,
            "festeEingänge" to "2",
            "operatorAnzeige" to "wert",
        ),
    )
}

fun MathematikAuswerterRegister.registriereVektorKonstruktorV2300() {
    registriere(VEKTOR_KONSTRUKTOR_ART) { kontext -> werteVektorKonstruktorAus(kontext) }
    // Historische Karten bleiben ohne vorherige Persistenzmigration ausführbar.
    registriere("mathematik.zeilenVektor") { kontext ->
        werteVektorKonstruktorAus(
            kontext.copy(
                knoten = kontext.knoten.copy(
                    parameter = kontext.knoten.parameter +
                        (VEKTOR_ORIENTIERUNG_PARAMETER to VEKTOR_ORIENTIERUNG_ZEILE),
                ),
            ),
        )
    }
}

private fun werteVektorKonstruktorAus(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
    val config = vektorKonstruktorKonfiguration(kontext.knoten)
    val werte = when (config.erzeugungsArt) {
        VEKTOR_METHODE -> {
            val dimension = konkretePositiveDimension(kontext, "dimension")
            val methode = kontext.eingänge["methode"]?.objekt as? Methode
                ?: error("Die Indexmethode des Vektors fehlt.")
            tupelAusMethode(methode, dimension).elemente.mapIndexed { index, element ->
                element as? ZahlAusdruck
                    ?: error("Die Indexmethode liefert an Position ${index + 1} keine Zahl.")
            }
        }
        else -> kontext.knoten.anschlüsse
            .filter { it.richtung == AnschlussRichtung.Eingang }
            .sortedBy { it.reihenfolge }
            .map { anschluss ->
                kontext.eingänge[anschluss.name]?.objekt as? ZahlAusdruck
                    ?: error("Vektorkomponente '${anschluss.name}' fehlt oder ist keine Zahl.")
            }
    }
    require(werte.isNotEmpty()) { "Ein Vektor benötigt mindestens eine Komponente." }
    val vektor: MathematischesObjekt = if (config.orientierung == VEKTOR_ORIENTIERUNG_ZEILE) {
        ZeilenVektor(werte)
    } else {
        SpaltenVektor(werte)
    }
    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf(
            "vektor" to BedingterWert(
                objekt = vektor,
                annahmen = kontext.eingänge.values.flatMap { it.annahmen }.toSet(),
                reelleVariablen = reelleVariablen(kontext.eingänge.values),
                variablenQuellen = kontext.eingänge.values.flatMap { it.variablenQuellen }.geordnetEindeutig(),
            ),
        ),
        eingänge = kontext.eingänge,
    )
}

private fun konkretePositiveDimension(kontext: KnotenAuswertungsKontext, name: String): Int {
    val zahl = kontext.eingänge[name]?.objekt as? RationaleZahl
        ?: error("Der Eingang '$name' muss eine konkrete natürliche Zahl sein.")
    require(zahl.nenner == BigInteger.ONE && zahl.zähler.signum() > 0 && zahl.zähler.bitLength() < 31) {
        "Die Dimension muss eine positive ganze Zahl sein."
    }
    return zahl.zähler.toInt()
}
