package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*
import java.math.BigInteger

const val ZAHLENRECHNER_DIFFERENTIAL_ERGEBNIS = "differential.ergebnisArt"

enum class ZahlenRechnerDifferentialErgebnisArt(
    val persistenzWert: String,
    val titel: String,
) {
    ABLEITUNGSFUNKTION("ableitungsfunktion", "Ableitungsfunktion"),
    DIFFERENTIAL("differential", "Differential"),
    ;

    companion object {
        fun ausPersistenzOderNull(wert: String?): ZahlenRechnerDifferentialErgebnisArt? =
            entries.firstOrNull { it.persistenzWert == wert || it.name.equals(wert, ignoreCase = true) }
    }
}

fun aktuelleZahlenRechnerDifferentialErgebnisArt(
    knoten: KnotenDaten,
): ZahlenRechnerDifferentialErgebnisArt? =
    ZahlenRechnerDifferentialErgebnisArt.ausPersistenzOderNull(
        knoten.parameter[ZAHLENRECHNER_DIFFERENTIAL_ERGEBNIS],
    )

fun aktuelleZahlenRechnerDifferentialOperator(knoten: KnotenDaten): DifferentialOperator {
    val operatorId = knoten.parameter[DIFFERENTIAL_OPERATOR_PARAMETER]
    if (operatorId != DifferentialOperator.Partiell(1).operatorId) return DifferentialOperator.Total
    val index = knoten.parameter[DIFFERENTIAL_ARGUMENT_INDEX_PARAMETER]
        ?.toIntOrNull()
        ?.takeIf { it >= 1 }
        ?: 1
    return DifferentialOperator.Partiell(index)
}

fun aktuellerZahlenRechnerDifferentialBegriff(knoten: KnotenDaten): DifferentialBegriff =
    DifferentialBegriff.entries.firstOrNull { it.name == knoten.parameter[DIFFERENTIAL_BEGRIFF_PARAMETER] }
        ?: DifferentialBegriff.REELL_FRECHET

/**
 * Schaltet den vorhandenen Zahlenrechner in einen strukturierten Analysiszustand.
 * Beide Ergebnisarten behalten dieselben Eingangsrollen, damit beim Wechsel keine
 * bestehende Methoden- oder Ordnungsverbindung still verloren geht.
 */
fun konfiguriereZahlenRechnerDifferential(
    knoten: KnotenDaten,
    ergebnisArt: ZahlenRechnerDifferentialErgebnisArt,
): KnotenDaten {
    require(knoten.art == ZAHLENRECHNER_ART)

    val vorhandeneEingänge = knoten.anschlüsse
        .filter { it.richtung == AnschlussRichtung.Eingang }
        .sortedBy { it.reihenfolge }
    val vorhandeneNachName = vorhandeneEingänge.associateBy { it.name }
    val bisherigerAusgang = knoten.anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Ausgang }

    fun eingang(
        name: String,
        art: AnschlussArtId,
        reihenfolge: Int,
    ): AnschlussDaten {
        val vorhanden = vorhandeneNachName[name] ?: vorhandeneEingänge.getOrNull(reihenfolge)
        return (vorhanden ?: AnschlussDaten(
            name = name,
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = art,
        )).copy(
            name = name,
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = art,
            reihenfolge = reihenfolge,
            kannSichErweitern = false,
            dynamischErzeugt = false,
            zulässigeArten = emptySet(),
        )
    }

    val methodenEingang = eingang("methode", MathematikAnschlussArten.Methode.id, 0)
    val ordnungsEingang = eingang("ordnung", MathematikAnschlussArten.Zahl.id, 1)
    val ausgangArt = when (ergebnisArt) {
        ZahlenRechnerDifferentialErgebnisArt.ABLEITUNGSFUNKTION -> MathematikAnschlussArten.Methode.id
        ZahlenRechnerDifferentialErgebnisArt.DIFFERENTIAL -> MathematikAnschlussArten.Objekt.id
    }
    val ausgang = (bisherigerAusgang ?: AnschlussDaten(
        name = "wert",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = ausgangArt,
    )).copy(
        name = "wert",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = ausgangArt,
        reihenfolge = 0,
        kannSichErweitern = false,
        dynamischErzeugt = false,
        artFolgtEingang = null,
        artVereinigtEingänge = emptyList(),
        zulässigeArten = emptySet(),
        artAbbildungVonEingang = null,
        artPriorisiertEingänge = null,
    )

    val bisherStandardName = knoten.name == UniversellerZahlenOperator.DIFFERENTIAL.titel ||
        knoten.name in ZahlenRechnerDifferentialErgebnisArt.entries.map { it.titel } ||
        UniversellerZahlenOperator.entries.any { it.titel == knoten.name }
    val parameter = knoten.parameter + mapOf(
        ZAHLENRECHNER_OPERATOR to UniversellerZahlenOperator.DIFFERENTIAL.stabileId,
        ZAHLENRECHNER_DIFFERENTIAL_ERGEBNIS to ergebnisArt.persistenzWert,
        DIFFERENTIAL_OPERATOR_PARAMETER to (
            knoten.parameter[DIFFERENTIAL_OPERATOR_PARAMETER] ?: DifferentialOperator.Total.operatorId
        ),
        DIFFERENTIAL_ARGUMENT_INDEX_PARAMETER to (
            knoten.parameter[DIFFERENTIAL_ARGUMENT_INDEX_PARAMETER] ?: "1"
        ),
        DIFFERENTIAL_BEGRIFF_PARAMETER to (
            knoten.parameter[DIFFERENTIAL_BEGRIFF_PARAMETER] ?: DifferentialBegriff.REELL_FRECHET.name
        ),
        DIFFERENTIAL_ORDNUNG_PARAMETER to (
            knoten.parameter[DIFFERENTIAL_ORDNUNG_PARAMETER] ?: "1"
        ),
    )

    return knoten.copy(
        name = if (bisherStandardName) ergebnisArt.titel else knoten.name,
        anschlüsse = listOf(methodenEingang, ordnungsEingang, ausgang),
        parameter = parameter,
    )
}

/**
 * Finaler Wrapper um den Zahlenrechner. Historische `zahl.differential`-Zustände
 * ohne Ergebnisart werden bewusst an den bisherigen skalaren Auswerter delegiert.
 */
internal fun MathematikAuswerterRegister.registriereZahlenRechnerDifferential() {
    val basis = requireNotNull(finde(ZAHLENRECHNER_ART)) {
        "Der universelle Zahlenrechner muss vor dem Differentialadapter registriert sein."
    }
    registriere(
        ZAHLENRECHNER_ART,
        MathematikKnotenAuswerter { kontext ->
            val operator = UniversellerZahlenOperator.vonIdOderNull(
                kontext.knoten.parameter[ZAHLENRECHNER_OPERATOR],
            )
            val ergebnisArt = aktuelleZahlenRechnerDifferentialErgebnisArt(kontext.knoten)
            if (operator == UniversellerZahlenOperator.DIFFERENTIAL && ergebnisArt != null) {
                werteZahlenRechnerDifferentialAus(kontext, ergebnisArt)
            } else {
                basis.auswerten(kontext)
            }
        },
    )
}

private fun werteZahlenRechnerDifferentialAus(
    kontext: KnotenAuswertungsKontext,
    ergebnisArt: ZahlenRechnerDifferentialErgebnisArt,
): KnotenAuswertungsErgebnis {
    val methodeWert = kontext.eingänge["methode"]
        ?: return differentialFehler(kontext, "Der Differentialzustand benötigt eine Methode.")
    val methode = methodeWert.objekt as? Methode
        ?: return differentialFehler(kontext, "Der Eingang 'methode' enthält keine Methode.")
    val ordnung = bestimmeZahlenRechnerDifferentialOrdnung(kontext)
        ?: return differentialFehler(kontext, "Die Differentiationsordnung muss konkret oder symbolisch in ℕ₀ liegen.")
    val operator = aktuelleZahlenRechnerDifferentialOperator(kontext.knoten)
    val begriff = aktuellerZahlenRechnerDifferentialBegriff(kontext.knoten)
    val ergebnis = runCatching {
        differenziereMethodeStrukturiert(
            methode = methode,
            ordnung = ordnung,
            operator = operator,
            begriff = begriff,
        )
    }.getOrElse { fehler ->
        return differentialFehler(kontext, fehler.message ?: "Die Methode kann nicht differenziert werden.")
    }
    if (ergebnis.status == DifferentialUnterstuetzungsStatus.MATHEMATISCH_NICHT_MOEGLICH) {
        return differentialFehler(kontext, ergebnis.verwendeteRegel)
    }

    val annahmen = kontext.eingänge.values.flatMap { it.annahmen }.toSet() + ergebnis.voraussetzungen
    val objekt: MathematischesObjekt
    val latex: String?
    val definition: String
    when (ergebnisArt) {
        ZahlenRechnerDifferentialErgebnisArt.ABLEITUNGSFUNKTION -> {
            objekt = ergebnis.methode
            latex = null
            definition = "Ableitungsfunktion: ${ergebnis.methode.name}"
        }
        ZahlenRechnerDifferentialErgebnisArt.DIFFERENTIAL -> {
            val differential = MethodenDifferential(
                methode = methode,
                ordnung = ordnung,
                operator = operator,
                begriff = begriff,
            )
            objekt = differential
            latex = differential.zuLatex()
            definition = differential.definitionsLatex()
        }
    }

    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf(
            "wert" to BedingterWert(
                objekt = objekt,
                annahmen = annahmen,
                zielMenge = ergebnis.zielRaum,
                werteVorrat = ergebnis.werteVorrat,
                reelleVariablen = methodeWert.reelleVariablen,
                variablenQuellen = methodeWert.variablenQuellen,
                latexDarstellung = latex,
            ),
        ),
        warnungen = listOf(
            "Definition: $definition",
            "Status: ${ergebnis.status.name}",
            "Wertevorrat: ${ergebnis.werteVorrat.zuLatex()}",
            "Zielraum: ${ergebnis.zielRaum.zuLatex()}",
            "Regel: ${ergebnis.verwendeteRegel}",
        ),
        eingänge = kontext.eingänge,
    )
}

private fun bestimmeZahlenRechnerDifferentialOrdnung(
    kontext: KnotenAuswertungsKontext,
): DifferentialOrdnung? {
    val verbunden = kontext.eingänge["ordnung"]
    val ausdruck = verbunden?.objekt as? ZahlAusdruck
    if (ausdruck != null) {
        val rational = ausdruck as? RationaleZahl
        if (rational != null) {
            if (rational.nenner != BigInteger.ONE || rational.zähler.signum() < 0) return null
            return DifferentialOrdnung.Konkret(rational.zähler)
        }
        return DifferentialOrdnung.Symbolisch(
            ausdruck = ausdruck,
            annahmen = verbunden.annahmen + UnentscheidbareAussage(
                "${ausdruck.zuLatex()}\\in\\mathbb N_0",
                "Differentialordnung",
            ),
        )
    }
    val fallback = kontext.knoten.parameter[DIFFERENTIAL_ORDNUNG_PARAMETER]
        ?.trim()
        ?.toBigIntegerOrNull()
        ?: BigInteger.ONE
    return runCatching { DifferentialOrdnung.Konkret(fallback) }.getOrNull()
}

private fun differentialFehler(
    kontext: KnotenAuswertungsKontext,
    nachricht: String,
): KnotenAuswertungsErgebnis = KnotenAuswertungsErgebnis(
    ausgaben = emptyMap(),
    fehler = nachricht,
    eingänge = kontext.eingänge,
)
