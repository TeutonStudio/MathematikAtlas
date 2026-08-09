package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenId
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.SpaltenVektor
import de.TeutonStudio.MathematikRechenSystem.kern.Tupel
import de.TeutonStudio.MathematikRechenSystem.kern.ZahlAusdruck
import de.TeutonStudio.MathematikRechenSystem.kern.ZeilenVektor
import de.TeutonStudio.MathematikRechenSystem.kern.multinomFolge
import java.math.BigInteger

const val VEKTOR_ART = "mathematik.vektor"
const val VEKTOR_ERZEUGUNGS_ART = "erzeugungsArt"
const val VEKTOR_EINZEL_EINGABEN = "einzelEingaben"
const val VEKTOR_METHODE = "methode"
const val VEKTOR_ORIENTIERUNG = "orientierung"
const val VEKTOR_SPALTE = "spalte"
const val VEKTOR_ZEILE = "zeile"

const val MULTINOM_VEKTOR_ART = "mathematik.multinomVektor"
const val MULTINOM_AUSGABE_FORM = "ausgabeForm"
const val MULTINOM_AUSGABE_VEKTOR = "vektor"
const val MULTINOM_AUSGABE_TUPEL = "tupel"

private val vektorErzeugungsArten = setOf(VEKTOR_EINZEL_EINGABEN, VEKTOR_METHODE)
private val vektorOrientierungen = setOf(VEKTOR_SPALTE, VEKTOR_ZEILE)
private val multinomAusgabeFormen = setOf(MULTINOM_AUSGABE_VEKTOR, MULTINOM_AUSGABE_TUPEL)

data class VektorKonfiguration(
    val erzeugungsArt: String,
    val orientierung: String,
)

data class MultinomVektorKonfiguration(
    val ausgabeForm: String,
    val orientierung: String,
)

fun vektorKonfiguration(knoten: KnotenDaten): VektorKonfiguration = VektorKonfiguration(
    erzeugungsArt = knoten.parameter[VEKTOR_ERZEUGUNGS_ART]?.takeIf(vektorErzeugungsArten::contains)
        ?: VEKTOR_EINZEL_EINGABEN,
    orientierung = knoten.parameter[VEKTOR_ORIENTIERUNG]?.takeIf(vektorOrientierungen::contains)
        ?: if (knoten.art == "mathematik.zeilenVektor") VEKTOR_ZEILE else VEKTOR_SPALTE,
)

fun multinomVektorKonfiguration(knoten: KnotenDaten): MultinomVektorKonfiguration = MultinomVektorKonfiguration(
    ausgabeForm = knoten.parameter[MULTINOM_AUSGABE_FORM]?.takeIf(multinomAusgabeFormen::contains)
        ?: MULTINOM_AUSGABE_VEKTOR,
    orientierung = knoten.parameter[VEKTOR_ORIENTIERUNG]?.takeIf(vektorOrientierungen::contains)
        ?: VEKTOR_SPALTE,
)

private fun orientierteVektorArt(orientierung: String) =
    if (orientierung == VEKTOR_ZEILE) MathematikAnschlussArten.ZeilenVektor.id
    else MathematikAnschlussArten.SpaltenVektor.id

private fun vektorEingang(
    vorhanden: AnschlussDaten?,
    name: String,
    art: de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId,
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
) ?: AnschlussDaten(
    name = name,
    richtung = AnschlussRichtung.Eingang,
    kante = AnschlussKante.Links,
    art = art,
    reihenfolge = reihenfolge,
    kannSichErweitern = erweiterbar,
)

fun konfiguriereVektor(
    knoten: KnotenDaten,
    erzeugungsArt: String = vektorKonfiguration(knoten).erzeugungsArt,
    orientierung: String = vektorKonfiguration(knoten).orientierung,
): KnotenDaten {
    require(knoten.art == VEKTOR_ART) { "Nur der kanonische Vektorknoten kann konfiguriert werden." }
    val art = erzeugungsArt.takeIf(vektorErzeugungsArten::contains) ?: VEKTOR_EINZEL_EINGABEN
    val orientierungNeu = orientierung.takeIf(vektorOrientierungen::contains) ?: VEKTOR_SPALTE
    val vorhandene = knoten.anschlüsse.associateBy { it.name }
    val eingänge = when (art) {
        VEKTOR_METHODE -> listOf(
            vektorEingang(vorhandene["dimension"], "dimension", MathematikAnschlussArten.Zahl.id, 0),
            vektorEingang(vorhandene["methode"], "methode", MathematikAnschlussArten.Methode.id, 1),
        )
        else -> {
            val anzahl = knoten.parameter["festeEingänge"]?.toIntOrNull()?.coerceAtLeast(2) ?: 2
            List(anzahl) { index ->
                val name = when (index) {
                    0 -> "a"
                    1 -> "b"
                    else -> "input${index + 1}"
                }
                vektorEingang(vorhandene[name], name, MathematikAnschlussArten.Zahl.id, index, erweiterbar = true)
            }
        }
    }
    val ausgang = vorhandene["vektor"]?.copy(
        name = "vektor",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = orientierteVektorArt(orientierungNeu),
        reihenfolge = 0,
        kannSichErweitern = false,
        dynamischErzeugt = false,
    ) ?: AnschlussDaten(
        name = "vektor",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = orientierteVektorArt(orientierungNeu),
    )
    val parameter = knoten.parameter + mapOf(
        VEKTOR_ERZEUGUNGS_ART to art,
        VEKTOR_ORIENTIERUNG to orientierungNeu,
    ) + if (art == VEKTOR_EINZEL_EINGABEN) {
        mapOf(
            "festeEingänge" to (knoten.parameter["festeEingänge"]?.toIntOrNull()?.coerceAtLeast(2) ?: 2).toString(),
            "operatorAnzeige" to (knoten.parameter["operatorAnzeige"] ?: "wert"),
        )
    } else emptyMap()
    return knoten.copy(anschlüsse = eingänge + ausgang, parameter = parameter)
}

fun konfiguriereMultinomVektor(
    knoten: KnotenDaten,
    ausgabeForm: String = multinomVektorKonfiguration(knoten).ausgabeForm,
    orientierung: String = multinomVektorKonfiguration(knoten).orientierung,
): KnotenDaten {
    require(knoten.art == MULTINOM_VEKTOR_ART) { "Nur Multinomvektoren können so konfiguriert werden." }
    val form = ausgabeForm.takeIf(multinomAusgabeFormen::contains) ?: MULTINOM_AUSGABE_VEKTOR
    val orientierungNeu = orientierung.takeIf(vektorOrientierungen::contains) ?: VEKTOR_SPALTE
    val vorhandene = knoten.anschlüsse.associateBy { it.name }
    val eingänge = listOf(
        vektorEingang(vorhandene["x"], "x", MathematikAnschlussArten.Zahl.id, 0),
        vektorEingang(vorhandene["dim"], "dim", MathematikAnschlussArten.Zahl.id, 1),
    )
    val ausgangsArt = if (form == MULTINOM_AUSGABE_TUPEL) {
        MathematikAnschlussArten.Tupel.id
    } else orientierteVektorArt(orientierungNeu)
    val ausgang = vorhandene["wert"]?.copy(
        name = "wert",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = ausgangsArt,
        reihenfolge = 0,
        kannSichErweitern = false,
        dynamischErzeugt = false,
    ) ?: AnschlussDaten(
        name = "wert",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = ausgangsArt,
    )
    return knoten.copy(
        anschlüsse = eingänge + ausgang,
        parameter = knoten.parameter + mapOf(
            MULTINOM_AUSGABE_FORM to form,
            VEKTOR_ORIENTIERUNG to orientierungNeu,
        ),
    )
}

fun KartenEditorZustand.setzeVektorKonfiguration(
    knotenId: KnotenId,
    erzeugungsArt: String,
    orientierung: String,
) {
    val knoten = karte.knoten.firstOrNull { it.id == knotenId } ?: return
    val neu = konfiguriereVektor(knoten, erzeugungsArt, orientierung)
    führeAus(KartenAktion.KnotenKonfigurationErsetzen(knotenId, neu.parameter, neu.anschlüsse))
}

fun KartenEditorZustand.setzeMultinomVektorKonfiguration(
    knotenId: KnotenId,
    ausgabeForm: String,
    orientierung: String,
) {
    val knoten = karte.knoten.firstOrNull { it.id == knotenId } ?: return
    val neu = konfiguriereMultinomVektor(knoten, ausgabeForm, orientierung)
    führeAus(KartenAktion.KnotenKonfigurationErsetzen(knotenId, neu.parameter, neu.anschlüsse))
}

private fun KnotenAuswertungsKontext.konkreteGanzeZahl(
    name: String,
    mindestens: Int,
): Int {
    val zahl = eingänge[name]?.objekt as? RationaleZahl
        ?: error("Der Eingang '$name' muss eine konkrete ganze Zahl sein.")
    require(zahl.nenner == BigInteger.ONE) { "Der Eingang '$name' muss ganzzahlig sein." }
    val wert = runCatching { zahl.zähler.intValueExact() }.getOrElse {
        error("Der Eingang '$name' überschreitet den unterstützten Ganzzahlbereich.")
    }
    require(wert >= mindestens) { "Der Eingang '$name' muss mindestens $mindestens sein." }
    return wert
}

private fun KnotenAuswertungsKontext.geordneteZahlEingänge(): List<ZahlAusdruck> = knoten.anschlüsse
    .filter { it.richtung == AnschlussRichtung.Eingang }
    .sortedBy { it.reihenfolge }
    .map { anschluss ->
        eingänge[anschluss.name]?.objekt as? ZahlAusdruck
            ?: error("Vektoreingang '${anschluss.name}' fehlt oder ist keine Zahl.")
    }

private fun vektorWert(werte: List<ZahlAusdruck>, orientierung: String) =
    if (orientierung == VEKTOR_ZEILE) ZeilenVektor(werte) else SpaltenVektor(werte)

private fun vektorAusIndexMethode(
    methode: Methode,
    dimension: Int,
    orientierung: String,
): de.TeutonStudio.MathematikRechenSystem.kern.MathematischesObjekt {
    require(methode.parameter.size == 1) { "Die Vektor-Indexmethode muss genau einen Indexparameter besitzen." }
    val werte = List(dimension) { index ->
        methode.wendeAn(listOf(RationaleZahl.von((index + 1).toLong()))) as? ZahlAusdruck
            ?: error("Die Vektor-Indexmethode muss für jeden Index eine Zahl liefern.")
    }
    return vektorWert(werte, orientierung)
}

/**
 * Finaler Wrapper für den konsolidierten Vektorkonstruktor und Multinomvektor.
 * Er wird nach den historischen Standardauswertern registriert.
 */
fun MathematikAuswerterRegister.registriereVektorKonsolidierung() {
    registriere(VEKTOR_ART) { kontext ->
        val config = vektorKonfiguration(kontext.knoten)
        val wert = if (config.erzeugungsArt == VEKTOR_METHODE) {
            val dimension = kontext.konkreteGanzeZahl("dimension", 1)
            val methode = kontext.eingänge["methode"]?.objekt as? Methode
                ?: error("Die Vektor-Indexmethode fehlt.")
            vektorAusIndexMethode(methode, dimension, config.orientierung)
        } else {
            vektorWert(kontext.geordneteZahlEingänge(), config.orientierung)
        }
        KnotenAuswertungsErgebnis(
            ausgaben = mapOf("vektor" to BedingterWert(wert, kontext.eingänge.values.flatMap { it.annahmen }.toSet())),
            eingänge = kontext.eingänge,
        )
    }

    // Historische Karten behalten ihre alte Art und erhalten dieselbe Kernsemantik.
    registriere("mathematik.zeilenVektor") { kontext ->
        KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "vektor" to BedingterWert(
                    ZeilenVektor(kontext.geordneteZahlEingänge()),
                    kontext.eingänge.values.flatMap { it.annahmen }.toSet(),
                ),
            ),
            eingänge = kontext.eingänge,
        )
    }

    registriere(MULTINOM_VEKTOR_ART) { kontext ->
        val x = kontext.eingänge["x"]?.objekt as? ZahlAusdruck
            ?: error("Der Multinomvektor benötigt am Eingang 'x' einen Zahlterm.")
        val dim = kontext.konkreteGanzeZahl("dim", 0)
        val config = multinomVektorKonfiguration(kontext.knoten)
        val monome = multinomFolge(x, dim)
        val wert = if (config.ausgabeForm == MULTINOM_AUSGABE_TUPEL) {
            Tupel(monome)
        } else {
            vektorWert(monome, config.orientierung)
        }
        KnotenAuswertungsErgebnis(
            ausgaben = mapOf("wert" to BedingterWert(wert, kontext.eingänge.values.flatMap { it.annahmen }.toSet())),
            eingänge = kontext.eingänge,
        )
    }
}
