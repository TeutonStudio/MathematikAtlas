package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

/**
 * Historisch stabiler Art-Schlüssel der Methodenbereich-Operatorfamilie.
 *
 * Restriktion und Bereichsanpassung sind fachlich getrennte Vorlagen und werden über
 * [METHODEN_BEREICHS_OPERATOR_PARAMETER] eindeutig persistiert. Der Art-Schlüssel
 * bleibt absichtlich stabil, damit bestehende Karten ohne Knoten-ID-Wechsel migrieren.
 */
const val RESTRIKTIONS_KNOTEN_ART = "mathematik.restriktion"
const val METHODEN_BEREICHS_OPERATOR_PARAMETER = "methodenBereich.operator"
const val METHODEN_BEREICHS_OPERATOR_RESTRIKTION = "restriktion"
const val METHODEN_BEREICHS_OPERATOR_ANPASSUNG = "bereichsanpassung"
const val RESTRIKTIONS_ERGÄNZUNG_PREFIX = "ergänzung."

fun KnotenDaten.methodenBereichsOperator(): String =
    parameter[METHODEN_BEREICHS_OPERATOR_PARAMETER] ?: METHODEN_BEREICHS_OPERATOR_RESTRIKTION

fun KnotenDaten.istMethodenBereichsanpassung(): Boolean =
    methodenBereichsOperator() == METHODEN_BEREICHS_OPERATOR_ANPASSUNG

object RestriktionsKnotenVorlagen {
    private val festeAnschlüsse = listOf(
        AnschlussDaten(
            name = "methode",
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = MathematikAnschlussArten.Methode.id,
            reihenfolge = 0,
        ),
        AnschlussDaten(
            name = "menge",
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = MathematikAnschlussArten.Menge.id,
            reihenfolge = 1,
        ),
        AnschlussDaten(
            name = "methode",
            richtung = AnschlussRichtung.Ausgang,
            kante = AnschlussKante.Rechts,
            art = MathematikAnschlussArten.Methode.id,
        ),
    )

    val Restriktion = KnotenVorlage(
        art = RESTRIKTIONS_KNOTEN_ART,
        name = "Methodenrestriktion",
        kategorie = "Abbildungen",
        beschreibung = "Schränkt eine Methode mathematisch korrekt auf eine Teilmenge ihrer Definitionsmenge ein.",
        standardGröße = GraphGröße(300f, 150f),
        anschlüsse = festeAnschlüsse,
        standardParameter = mapOf(METHODEN_BEREICHS_OPERATOR_PARAMETER to METHODEN_BEREICHS_OPERATOR_RESTRIKTION),
    )

    val Bereichsanpassung = KnotenVorlage(
        art = RESTRIKTIONS_KNOTEN_ART,
        name = "Methoden-Bereichsanpassung",
        kategorie = "Abbildungen",
        beschreibung = "Passt die gewünschte Definitionsmenge mit priorisierten, geordneten Ergänzungsmethoden an.",
        standardGröße = GraphGröße(320f, 165f),
        anschlüsse = festeAnschlüsse,
        standardParameter = mapOf(METHODEN_BEREICHS_OPERATOR_PARAMETER to METHODEN_BEREICHS_OPERATOR_ANPASSUNG),
    )

    val alle = listOf(Restriktion, Bereichsanpassung)
}

internal fun MathematikAuswerterRegister.registriereRestriktionsKnoten() {
    registriere(RESTRIKTIONS_KNOTEN_ART) { kontext ->
        val basis = kontext.eingänge["methode"]?.objekt as? Methode
            ?: error("Die Methode fehlt.")
        val menge = kontext.eingänge["menge"]?.objekt as? MengenAusdruck
            ?: error("Die gewünschte Definitionsmenge fehlt.")

        when (kontext.knoten.methodenBereichsOperator()) {
            METHODEN_BEREICHS_OPERATOR_ANPASSUNG -> werteBereichsanpassungAus(kontext, basis, menge)
            else -> werteRestriktionAus(kontext, basis, menge)
        }
    }
}

private fun werteRestriktionAus(
    kontext: KnotenAuswertungsKontext,
    basis: Methode,
    menge: MengenAusdruck,
): KnotenAuswertungsErgebnis {
    val ergebnis = restriktiereMethode(basis, menge, kontext.rechenKontext)
    val annahmen = kontext.eingänge.values.flatMap { it.annahmen }.toSet() + ergebnis.bedingungen
    val methode = ergebnis.methode

    return if (methode == null) {
        KnotenAuswertungsErgebnis(
            ausgaben = emptyMap(),
            fehler = "Ungültige Restriktion: ${menge.zuLatex()} ist keine Teilmenge von ${ergebnis.basisWerteVorrat.zuLatex()}.",
        )
    } else {
        KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "methode" to BedingterWert(
                    objekt = methode,
                    annahmen = annahmen,
                    latexDarstellung = methode.zuLatex(),
                ),
            ),
            warnungen = if (ergebnis.teilmengenPrüfung.wahrheitswert == null) {
                listOf("Die Teilmengenbeziehung ist symbolisch noch nicht bewiesen und wird als Voraussetzung weitergereicht.")
            } else emptyList(),
        )
    }
}

private fun werteBereichsanpassungAus(
    kontext: KnotenAuswertungsKontext,
    basis: Methode,
    menge: MengenAusdruck,
): KnotenAuswertungsErgebnis {
    val ergänzungen = kontext.knoten.ergänzungsAnschlüsse()
        .mapNotNull { anschluss -> kontext.eingänge[anschluss.name]?.objekt as? Methode }
    val ergebnis = passeMethodenBereichAn(basis, menge, ergänzungen, kontext.rechenKontext)
    val resultierendeMethode = ergebnis.methode
    val annahmen = kontext.eingänge.values.flatMap { it.annahmen }.toSet() + ergebnis.bedingungen

    val zielFehler = ergebnis.ergänzungen.withIndex().firstOrNull {
        it.value.zielPrüfung.wahrheitswert == Wahrheitswert.Lüge
    }
    return when {
        zielFehler != null -> KnotenAuswertungsErgebnis(
            ausgaben = emptyMap(),
            fehler = "Ergänzung ${zielFehler.index + 1} bildet ihren effektiven Bereich nicht vollständig in ${ergebnis.zielMenge.zuLatex()} ab.",
            warnungen = ergebnis.warnungen,
        )
        resultierendeMethode == null -> KnotenAuswertungsErgebnis(
            ausgaben = emptyMap(),
            fehler = "Die Bereichsanpassung ist auf ${ergebnis.gewünschterWerteVorrat.zuLatex()} noch nicht vollständig definiert. Offen: ${ergebnis.restMenge.zuLatex()}.",
            warnungen = ergebnis.warnungen,
        )
        else -> KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "methode" to BedingterWert(
                    objekt = resultierendeMethode,
                    annahmen = annahmen,
                    latexDarstellung = resultierendeMethode.zuLatex(),
                ),
            ),
            warnungen = ergebnis.warnungen + when (ergebnis.abdeckungsStatus) {
                AbdeckungsStatus.Unbekannt -> listOf(
                    "Die vollständige Abdeckung ist symbolisch noch nicht bewiesen; die notwendige Teilmengenbedingung wird als Annahme weitergereicht.",
                )
                else -> emptyList()
            },
        )
    }
}

/**
 * Synchronisiert ausschließlich die dynamischen Ergänzungseingänge einer
 * Bereichsanpassung. Reine Restriktionsknoten besitzen prinzipiell keine solchen
 * Eingänge. Verbundene Ergänzungen und der freie Folgeanschluss behalten ihre IDs.
 */
fun synchronisiereRestriktionsAnschlüsse(
    karte: KartenDaten,
    auswertung: KartenAuswertungsErgebnis,
): KartenDaten {
    val knoten = karte.knoten.map { aktuell ->
        if (aktuell.art != RESTRIKTIONS_KNOTEN_ART || !aktuell.istMethodenBereichsanpassung()) return@map aktuell
        synchronisiereBereichsanpassungsKnoten(karte, aktuell, auswertung.knoten[aktuell.id])
    }
    return if (knoten == karte.knoten) karte else karte.copy(knoten = knoten)
}

private fun synchronisiereBereichsanpassungsKnoten(
    karte: KartenDaten,
    knoten: KnotenDaten,
    auswertung: KnotenAuswertungsErgebnis?,
): KnotenDaten {
    val feste = knoten.anschlüsse.filterNot { it.name.startsWith(RESTRIKTIONS_ERGÄNZUNG_PREFIX) }
    val bisherigeErgänzungen = knoten.ergänzungsAnschlüsse()
    val verbundene = bisherigeErgänzungen.filter { anschluss ->
        val ref = AnschlussVerweis(knoten.id, anschluss.id)
        karte.verbindungen.any { it.von == ref || it.zu == ref }
    }
    val freie = bisherigeErgänzungen.filterNot { it in verbundene }

    val basis = auswertung?.eingänge?.get("methode")?.objekt as? Methode
    val menge = auswertung?.eingänge?.get("menge")?.objekt as? MengenAusdruck
    val ergänzungsMethoden = verbundene
        .sortedBy { it.reihenfolge }
        .mapNotNull { anschluss -> auswertung?.eingänge?.get(anschluss.name)?.objekt as? Methode }
    val benötigtWeiteren = if (basis == null || menge == null) {
        false
    } else {
        runCatching { passeMethodenBereichAn(basis, menge, ergänzungsMethoden) }
            .map { it.abdeckungsStatus != AbdeckungsStatus.Vollständig }
            .getOrDefault(true)
    }

    val normalisierteVerbundene = verbundene.sortedBy { it.reihenfolge }.mapIndexed { index, anschluss ->
        anschluss.copy(reihenfolge = index + 2, dynamischErzeugt = true, kannSichErweitern = false)
    }
    val neueErgänzungen = if (benötigtWeiteren) {
        val reihenfolge = normalisierteVerbundene.size + 2
        val freierFolgeanschluss = freie.firstOrNull()?.copy(
            reihenfolge = reihenfolge,
            dynamischErzeugt = true,
            kannSichErweitern = false,
        ) ?: neuerErgänzungsAnschluss(
            reihenfolge = reihenfolge,
            index = nächsterErgänzungsIndex(bisherigeErgänzungen),
        )
        normalisierteVerbundene + freierFolgeanschluss
    } else normalisierteVerbundene

    val methodenEingang = feste.firstOrNull { it.richtung == AnschlussRichtung.Eingang && it.name == "methode" }
    val mengenEingang = feste.firstOrNull { it.richtung == AnschlussRichtung.Eingang && it.name == "menge" }
    val übrige = feste.filterNot { it == methodenEingang || it == mengenEingang }
    return knoten.copy(
        anschlüsse = listOfNotNull(
            methodenEingang?.copy(reihenfolge = 0),
            mengenEingang?.copy(reihenfolge = 1),
        ) + neueErgänzungen + übrige,
    )
}

private fun neuerErgänzungsAnschluss(reihenfolge: Int, index: Int) = AnschlussDaten(
    name = "$RESTRIKTIONS_ERGÄNZUNG_PREFIX$index",
    richtung = AnschlussRichtung.Eingang,
    kante = AnschlussKante.Links,
    art = MathematikAnschlussArten.Methode.id,
    reihenfolge = reihenfolge,
    dynamischErzeugt = true,
)

private fun nächsterErgänzungsIndex(anschlüsse: List<AnschlussDaten>): Int =
    (anschlüsse.mapNotNull { it.name.removePrefix(RESTRIKTIONS_ERGÄNZUNG_PREFIX).toIntOrNull() }.maxOrNull() ?: -1) + 1

internal fun KnotenDaten.ergänzungsAnschlüsse(): List<AnschlussDaten> = anschlüsse
    .filter { it.richtung == AnschlussRichtung.Eingang && it.name.startsWith(RESTRIKTIONS_ERGÄNZUNG_PREFIX) }
    .sortedBy { it.reihenfolge }
