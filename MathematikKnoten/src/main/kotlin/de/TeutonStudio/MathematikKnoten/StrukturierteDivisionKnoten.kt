package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikKartenAdapter.MathematikKnotenAuswerter
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val ZAHLENRECHNER_DIVISIONSSEITE = "divisionsSeite"
const val ZAHLENRECHNER_DIVISIONSSEITE_FEHLT = "divisionsSeiteFehlt"

fun divisionsSeiteOderStandard(knoten: KnotenDaten): DivisionsSeite =
    DivisionsSeite.ausPersistenzOderNull(knoten.parameter[ZAHLENRECHNER_DIVISIONSSEITE])
        ?: DivisionsSeite.RECHTS

fun divisionsSeiteIstHistorischOffen(knoten: KnotenDaten): Boolean =
    knoten.parameter[ZAHLENRECHNER_DIVISIONSSEITE_FEHLT] == "true" &&
        DivisionsSeite.ausPersistenzOderNull(knoten.parameter[ZAHLENRECHNER_DIVISIONSSEITE]) == null

fun konfiguriereDivisionsSeite(
    knoten: KnotenDaten,
    seite: DivisionsSeite,
): KnotenDaten {
    require(knoten.art == ZAHLENRECHNER_ART)
    require(
        UniversellerZahlenOperator.vonId(knoten.parameter[ZAHLENRECHNER_OPERATOR]) ==
            UniversellerZahlenOperator.DIVISION,
    ) { "Die Divisionsseite ist nur für den Divisionszustand des Zahlenrechners definiert." }

    val standardName = knoten.name == UniversellerZahlenOperator.DIVISION.titel ||
        knoten.name.startsWith("${UniversellerZahlenOperator.DIVISION.titel} (")
    val sichtbarerName = when (seite) {
        DivisionsSeite.RECHTS -> "Division (rechts)"
        DivisionsSeite.LINKS -> "Division (links)"
    }
    return knoten.copy(
        name = if (standardName) sichtbarerName else knoten.name,
        parameter = knoten.parameter + mapOf(
            ZAHLENRECHNER_DIVISIONSSEITE to seite.persistenzWert,
            ZAHLENRECHNER_DIVISIONSSEITE_FEHLT to "false",
        ),
    )
}

/**
 * Wird ausschließlich beim Laden aufgerufen. Jede geladene Division ohne Seite
 * ist daher historischer Bestand; neu erzeugte Knoten verwenden rechts als Standard.
 */
fun KartenDaten.migriereStrukturierteDivision(): KartenDaten = copy(
    knoten = knoten.map { knoten ->
        val istHistorischeSpezialDivision = knoten.art == "mathematik.division"
        val istUniverselleDivision = knoten.art == ZAHLENRECHNER_ART &&
            UniversellerZahlenOperator.vonIdOderNull(
                knoten.parameter[ZAHLENRECHNER_OPERATOR],
            ) == UniversellerZahlenOperator.DIVISION
        if (!istHistorischeSpezialDivision && !istUniverselleDivision) return@map knoten
        if (DivisionsSeite.ausPersistenzOderNull(knoten.parameter[ZAHLENRECHNER_DIVISIONSSEITE]) != null) {
            return@map knoten.copy(
                parameter = knoten.parameter + (ZAHLENRECHNER_DIVISIONSSEITE_FEHLT to "false"),
            )
        }
        knoten.copy(
            parameter = knoten.parameter + (ZAHLENRECHNER_DIVISIONSSEITE_FEHLT to "true"),
        )
    },
)

/**
 * Umschließt den final registrierten Zahlenrechner und ersetzt ausschließlich
 * dessen Divisionszweig. Alle anderen Operatoren, einschließlich später ergänzter
 * Zustände, bleiben beim vorhandenen Auswerter.
 */
internal fun MathematikAuswerterRegister.registriereStrukturierteDivision() {
    val basis = requireNotNull(finde(ZAHLENRECHNER_ART)) {
        "Der universelle Zahlenrechner muss vor der strukturierten Division registriert sein."
    }
    registriere(
        ZAHLENRECHNER_ART,
        MathematikKnotenAuswerter { kontext ->
            val operator = UniversellerZahlenOperator.vonIdOderNull(
                kontext.knoten.parameter[ZAHLENRECHNER_OPERATOR],
            )
            if (operator == UniversellerZahlenOperator.DIVISION) {
                werteStrukturierteDivisionAus(basis, kontext)
            } else {
                basis.auswerten(kontext)
            }
        },
    )
}

private fun werteStrukturierteDivisionAus(
    basis: MathematikKnotenAuswerter,
    kontext: de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext,
): KnotenAuswertungsErgebnis {
    val basisErgebnis = basis.auswerten(kontext)
    val dividendWert = kontext.eingänge["a"]
        ?: return basisErgebnis.copy(fehler = "Der Dividendeneingang a fehlt.")
    val divisorWert = kontext.eingänge["b"]
        ?: return basisErgebnis.copy(fehler = "Der Divisoreingang b fehlt.")
    val punktweiseMethode = basisErgebnis.ausgaben["wert"]?.objekt as? Methode
    val punktweiseDivision = punktweiseMethode?.vorschrift as? Division
    val dividend = punktweiseDivision?.dividend ?: (dividendWert.objekt as? ZahlAusdruck)
        ?: return basisErgebnis.copy(fehler = "Der Dividend ist weder eine Zahl noch Teil einer punktweisen Zahlenfunktion.")
    val divisor = punktweiseDivision?.divisor ?: (divisorWert.objekt as? ZahlAusdruck)
        ?: return basisErgebnis.copy(fehler = "Der Divisor ist weder eine Zahl noch Teil einer punktweisen Zahlenfunktion.")

    // Der bestehende dritte Eingang ist ein expliziter Ersatzwert für den exakten Nullfall.
    if (divisor == RationaleZahl.Null) return basisErgebnis

    val gemeinsam = gemeinsamerZahlenRechnerBereich(
        listOf(dividendWert, divisorWert).map { wert ->
            when (val objekt = wert.objekt) {
                is ZahlAusdruck -> inferiereZahlenRechnerBereich(objekt, wert.werteVorrat)
                is Methode -> inferiereMethodenZielBereich(objekt)
                else -> ZahlenRechnerBereich.UNBEKANNT
            }
        },
    )
    val kommutativitaet = when {
        gemeinsam.multiplikativKommutativ -> KommutativitaetsStatus.NACHGEWIESEN
        gemeinsam == ZahlenRechnerBereich.UNBEKANNT -> KommutativitaetsStatus.UNBEKANNT
        else -> KommutativitaetsStatus.NICHT_KOMMUTATIV
    }
    if (kommutativitaet != KommutativitaetsStatus.NACHGEWIESEN &&
        divisionsSeiteIstHistorischOffen(kontext.knoten)
    ) {
        return basisErgebnis.copy(
            ausgaben = emptyMap(),
            fehler = "Divisionsseite fehlt. Wähle im Inspector rechts oder links.",
            eingänge = kontext.eingänge,
        )
    }

    val seite = divisionsSeiteOderStandard(kontext.knoten)
    val ausdruck = strukturierteDivision(
        dividend = dividend,
        divisor = divisor,
        seite = seite,
        kommutativitaet = kommutativitaet,
        struktur = gemeinsam.alsZahlbereichsIdOderNull(),
    )
    val bisher = basisErgebnis.ausgaben["wert"]
        ?: BedingterWert(objekt = ausdruck)
    val ergebnisObjekt: MathematischesObjekt = punktweiseMethode?.copy(
        name = "${punktweiseMethode.name}_{${seite.latexIndex}}",
        vorschrift = ausdruck,
    ) ?: ausdruck
    val hinweise = when (ausdruck) {
        is StrukturierteDivision -> buildList {
            add("Divisionsseite: ${seite.persistenzWert}")
            addAll(ausdruck.effektiveVoraussetzungen.map { it.beschreibung })
        }
        else -> emptyList()
    }
    return basisErgebnis.copy(
        ausgaben = basisErgebnis.ausgaben + (
            "wert" to bisher.copy(
                objekt = ergebnisObjekt,
                latexDarstellung = if (ergebnisObjekt is Methode) null else ausdruck.zuLatex(),
            )
        ),
        warnungen = (basisErgebnis.warnungen + hinweise).distinct(),
        fehler = null,
        eingänge = kontext.eingänge,
    )
}

private fun inferiereMethodenZielBereich(methode: Methode): ZahlenRechnerBereich {
    val fundamental = methode.zielMenge.fundamentalerZahlbereichOderNull()
        ?: (methode.zielMenge as? BeschraenkteZahlmenge)?.traeger
    return when (fundamental) {
        FundamentalerZahlbereich.NATUERLICH_POSITIV -> ZahlenRechnerBereich.NATUERLICH
        FundamentalerZahlbereich.NATUERLICH_MIT_NULL -> ZahlenRechnerBereich.NATUERLICH_MIT_NULL
        FundamentalerZahlbereich.GANZ -> ZahlenRechnerBereich.GANZ
        FundamentalerZahlbereich.RATIONAL -> ZahlenRechnerBereich.RATIONAL
        FundamentalerZahlbereich.REELL -> ZahlenRechnerBereich.REELL
        FundamentalerZahlbereich.KOMPLEX -> ZahlenRechnerBereich.KOMPLEX
        FundamentalerZahlbereich.QUATERNION -> ZahlenRechnerBereich.QUATERNION
        null -> ZahlenRechnerBereich.UNBEKANNT
    }
}

private fun ZahlenRechnerBereich.alsZahlbereichsIdOderNull(): ZahlbereichsId? = when (this) {
    ZahlenRechnerBereich.NATUERLICH -> ZahlbereichsIds.NATUERLICH_POSITIV
    ZahlenRechnerBereich.NATUERLICH_MIT_NULL -> ZahlbereichsIds.NATUERLICH_MIT_NULL
    ZahlenRechnerBereich.GANZ -> ZahlbereichsIds.GANZ
    ZahlenRechnerBereich.RATIONAL -> ZahlbereichsIds.RATIONAL
    ZahlenRechnerBereich.REELL -> ZahlbereichsIds.REELL
    ZahlenRechnerBereich.KOMPLEX -> ZahlbereichsIds.KOMPLEX
    ZahlenRechnerBereich.HYPERREELL -> ZahlbereichsIds.HYPER_REELL
    ZahlenRechnerBereich.QUATERNION -> ZahlbereichsIds.QUATERNION
    ZahlenRechnerBereich.MODULO,
    ZahlenRechnerBereich.UNBEKANNT,
    -> null
}
