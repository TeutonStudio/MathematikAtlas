package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val RESTRIKTIONS_KNOTEN_ART = "mathematik.restriktion"
const val RESTRIKTIONS_ERGÄNZUNG_PREFIX = "ergänzung."

object RestriktionsKnotenVorlagen {
    val Restriktion = KnotenVorlage(
        art = RESTRIKTIONS_KNOTEN_ART,
        name = "Restriktion",
        kategorie = "Abbildungen",
        beschreibung = "Schränkt den Wertevorrat einer Methode auf eine Menge ein und fordert bei Bedarf priorisierte Ergänzungsmethoden an.",
        standardGröße = GraphGröße(300f, 150f),
        anschlüsse = listOf(
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
        ),
    )

    val alle = listOf(Restriktion)
}

internal fun MathematikAuswerterRegister.registriereRestriktionsKnoten() {
    registriere(RESTRIKTIONS_KNOTEN_ART) { kontext ->
        val basis = kontext.eingänge["methode"]?.objekt as? Methode
            ?: error("Die zu restringierende Methode fehlt.")
        val menge = kontext.eingänge["menge"]?.objekt as? MengenAusdruck
            ?: error("Der gewünschte Wertevorrat fehlt.")
        val ergänzungen = kontext.knoten.ergänzungsAnschlüsse()
            .mapNotNull { anschluss -> kontext.eingänge[anschluss.name]?.objekt as? Methode }
        val ergebnis = restriktiereMethode(basis, menge, ergänzungen, kontext.rechenKontext)
        val resultierendeMethode = ergebnis.methode
        val annahmen = kontext.eingänge.values.flatMap { it.annahmen }.toSet() + ergebnis.bedingungen

        val zielFehler = ergebnis.ergänzungen.withIndex().firstOrNull {
            it.value.zielPrüfung.wahrheitswert == Wahrheitswert.Lüge
        }
        when {
            zielFehler != null -> KnotenAuswertungsErgebnis(
                ausgaben = emptyMap(),
                fehler = "Ergänzung ${zielFehler.index + 1} bildet ihren effektiven Bereich nicht vollständig in ${ergebnis.zielMenge.zuLatex()} ab.",
                warnungen = ergebnis.warnungen,
            )
            resultierendeMethode == null -> KnotenAuswertungsErgebnis(
                ausgaben = emptyMap(),
                fehler = "Die Methode ist auf ${ergebnis.gewünschterWerteVorrat.zuLatex()} noch nicht vollständig definiert. Offen: ${ergebnis.restMenge.zuLatex()}.",
                warnungen = ergebnis.warnungen,
            )
            else -> KnotenAuswertungsErgebnis(
                ausgaben = mapOf(
                    "methode" to BedingterWert(
                        objekt = resultierendeMethode,
                        annahmen = annahmen,
                        latexDarstellung = "${basis.name}\\vert_{${menge.zuLatex()}}",
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
}

/**
 * Synchronisiert ausschließlich die automatisch angebotenen Ergänzungseingänge.
 * Verbundene Ergänzungen und der bereits sichtbare freie Folgeanschluss behalten ihre
 * Anschluss-ID. Dadurch verändert eine reine Neuauswertung den Graphen nicht.
 */
fun synchronisiereRestriktionsAnschlüsse(
    karte: KartenDaten,
    auswertung: KartenAuswertungsErgebnis,
): KartenDaten {
    val knoten = karte.knoten.map { aktuell ->
        if (aktuell.art != RESTRIKTIONS_KNOTEN_ART) return@map aktuell
        synchronisiereRestriktionsKnoten(karte, aktuell, auswertung.knoten[aktuell.id])
    }
    return if (knoten == karte.knoten) karte else karte.copy(knoten = knoten)
}

private fun synchronisiereRestriktionsKnoten(
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
    val ergänzungsMethoden = verbundene.mapNotNull { anschluss ->
        auswertung?.eingänge?.get(anschluss.name)?.objekt as? Methode
    }
    val benötigtWeiteren = if (basis == null || menge == null) {
        false
    } else {
        runCatching { restriktiereMethode(basis, menge, ergänzungsMethoden) }
            .map { it.abdeckungsStatus != AbdeckungsStatus.Vollständig }
            .getOrDefault(true)
    }

    val normalisierteVerbundene = verbundene.mapIndexed { index, anschluss ->
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

private fun KnotenDaten.ergänzungsAnschlüsse(): List<AnschlussDaten> = anschlüsse
    .filter { it.richtung == AnschlussRichtung.Eingang && it.name.startsWith(RESTRIKTIONS_ERGÄNZUNG_PREFIX) }
    .sortedBy { it.reihenfolge }
