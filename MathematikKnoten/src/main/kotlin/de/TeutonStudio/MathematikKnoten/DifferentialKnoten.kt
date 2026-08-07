package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.*
import java.math.BigInteger

const val DIFFERENTIAL_KNOTEN_ART = "mathematik.differential"
const val DIFFERENTIAL_AUSGABEFORM_PARAMETER = "differential.ausgabeForm"
const val DIFFERENTIAL_ORDNUNG_PARAMETER = "differential.ordnung"
const val DIFFERENTIAL_OPERATOR_PARAMETER = "differential.operator"
const val DIFFERENTIAL_ARGUMENT_INDEX_PARAMETER = "differential.argumentIndex"
const val DIFFERENTIAL_BEGRIFF_PARAMETER = "differential.begriff"
const val DIFFERENTIAL_QUELLEN_ID_PARAMETER = "differential.quellenId"
const val DIFFERENTIAL_MIGRATIONSFEHLER_PARAMETER = "differential.migrationsFehler"

private const val HISTORISCHE_ABLEITUNG_ART = "mathematik.ableitung"
private const val HISTORISCHES_DIFFERENTIAL_TERM_ART = "mathematik.differentialTerm"

object DifferentialKnotenVorlagen {
    val Differential = KnotenVorlage(
        art = DIFFERENTIAL_KNOTEN_ART,
        name = "Differential",
        kategorie = "Analysis: Differentialrechnung",
        beschreibung = "Ein gemeinsamer Knoten für Methodenableitung und Differentialterm. Ordnung, Differentialbegriff und partieller Argumentindex bleiben strukturierte Parameter.",
        standardGröße = GraphGröße(285f, 135f),
        anschlüsse = differentialAnschluesse(DifferentialAusgabeForm.METHODE),
        standardParameter = mapOf(
            DIFFERENTIAL_AUSGABEFORM_PARAMETER to DifferentialAusgabeForm.METHODE.name,
            DIFFERENTIAL_ORDNUNG_PARAMETER to "1",
            DIFFERENTIAL_OPERATOR_PARAMETER to DifferentialOperator.Total.operatorId,
            DIFFERENTIAL_ARGUMENT_INDEX_PARAMETER to "1",
            DIFFERENTIAL_BEGRIFF_PARAMETER to DifferentialBegriff.REELL_FRECHET.name,
            DIFFERENTIAL_QUELLEN_ID_PARAMETER to "differential.variable",
        ),
    )
    val alle = listOf(Differential)
}

data class DifferentialModusWechselDiagnose(
    val ausgabeForm: DifferentialAusgabeForm,
    val erhalteneAnschlussIds: Set<AnschlussId>,
    val entfernteAnschlussIds: Set<AnschlussId>,
    val verbundeneEntfernteAnschlussIds: Set<AnschlussId>,
) { val trenntVerbindungen: Boolean get() = verbundeneEntfernteAnschlussIds.isNotEmpty() }

fun aktuelleDifferentialAusgabeForm(knoten: KnotenDaten): DifferentialAusgabeForm =
    DifferentialAusgabeForm.entries.firstOrNull { it.name == knoten.parameter[DIFFERENTIAL_AUSGABEFORM_PARAMETER] }
        ?: DifferentialAusgabeForm.METHODE

fun konfiguriereDifferentialKnoten(knoten: KnotenDaten, ausgabeForm: DifferentialAusgabeForm): KnotenDaten {
    require(knoten.art == DIFFERENTIAL_KNOTEN_ART)
    val gewuenscht = differentialAnschluesse(ausgabeForm)
    val bestehend = knoten.anschlüsse.associateBy { it.richtung to it.name }
    val anschluesse = gewuenscht.map { neu -> bestehend[neu.richtung to neu.name]?.let { alt -> neu.copy(id = alt.id) } ?: neu }
    return knoten.copy(
        name = when (ausgabeForm) { DifferentialAusgabeForm.METHODE -> "Ableitung"; DifferentialAusgabeForm.TERM -> "Differentialterm" },
        anschlüsse = anschluesse,
        parameter = knoten.parameter + (DIFFERENTIAL_AUSGABEFORM_PARAMETER to ausgabeForm.name),
    )
}

fun diagnostiziereDifferentialModusWechsel(knoten: KnotenDaten, ausgabeForm: DifferentialAusgabeForm, verbundeneAnschlussIds: Set<AnschlussId>): DifferentialModusWechselDiagnose {
    val probe = konfiguriereDifferentialKnoten(knoten, ausgabeForm)
    val alt = knoten.anschlüsse.mapTo(linkedSetOf(), AnschlussDaten::id)
    val neu = probe.anschlüsse.mapTo(linkedSetOf(), AnschlussDaten::id)
    val entfernt = alt - neu
    return DifferentialModusWechselDiagnose(ausgabeForm, alt.intersect(neu), entfernt, entfernt.intersect(verbundeneAnschlussIds))
}

internal fun MathematikAuswerterRegister.registriereDifferentialKnoten() {
    registriere(DIFFERENTIAL_KNOTEN_ART) { kontext -> kontext.werteDifferentialAus() }
}

private fun KnotenAuswertungsKontext.werteDifferentialAus(): KnotenAuswertungsErgebnis {
    knoten.parameter[DIFFERENTIAL_MIGRATIONSFEHLER_PARAMETER]?.let { return fehlerErgebnis(it) }
    return when (aktuelleDifferentialAusgabeForm(knoten)) {
        DifferentialAusgabeForm.METHODE -> werteMethodenAbleitungAus()
        DifferentialAusgabeForm.TERM -> werteDifferentialTermAus()
    }
}

private fun KnotenAuswertungsKontext.werteMethodenAbleitungAus(): KnotenAuswertungsErgebnis {
    val methode = eingänge["methode"]?.objekt as? Methode ?: return fehlerErgebnis("Der Methodenmodus benötigt eine Methode.")
    val ordnung = bestimmeDifferentialOrdnung() ?: return fehlerErgebnis("Die Differentiationsordnung muss konkret oder symbolisch in ℕ₀ liegen.")
    val operator = bestimmeDifferentialOperator()
    val begriff = DifferentialBegriff.entries.firstOrNull { it.name == knoten.parameter[DIFFERENTIAL_BEGRIFF_PARAMETER] }
        ?: DifferentialBegriff.REELL_FRECHET
    val ergebnis = runCatching { differenziereMethodeStrukturiert(methode, ordnung, operator, begriff) }
        .getOrElse { return fehlerErgebnis(it.message ?: "Die Methode kann nicht differenziert werden.") }
    if (ergebnis.status == DifferentialUnterstuetzungsStatus.MATHEMATISCH_NICHT_MOEGLICH) return fehlerErgebnis(ergebnis.verwendeteRegel)
    val annahmen = gemeinsameAnnahmen() + ergebnis.voraussetzungen
    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf("methode" to BedingterWert(ergebnis.methode, annahmen, ergebnis.methode.zielMenge)),
        warnungen = listOf("Status: ${ergebnis.status.name}", "Wertevorrat: ${ergebnis.werteVorrat.zuLatex()}", "Zielraum: ${ergebnis.zielRaum.zuLatex()}", "Regel: ${ergebnis.verwendeteRegel}"),
        eingänge = eingänge,
    )
}

private fun KnotenAuswertungsKontext.werteDifferentialTermAus(): KnotenAuswertungsErgebnis {
    val term = eingänge["term"]?.objekt as? ZahlAusdruck ?: return fehlerErgebnis("Der Termmodus benötigt einen Zahlterm.")
    val variable = eingänge["nach"]?.objekt as? Variable ?: return fehlerErgebnis("Der Termmodus benötigt eine gebundene Variable am Eingang 'nach'.")
    val quellenId = knoten.parameter[DIFFERENTIAL_QUELLEN_ID_PARAMETER]?.takeIf(String::isNotBlank) ?: variable.name
    val differential = bildeDifferentialTerm(term, variable, DifferentialOperator.Total, quellenId)
    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf("term" to BedingterWert(differential, gemeinsameAnnahmen())),
        warnungen = listOf("Differentialquelle: $quellenId", "Regel: totale Differentiation relativ zur gewählten Variable."),
        eingänge = eingänge,
    )
}

private fun KnotenAuswertungsKontext.bestimmeDifferentialOrdnung(): DifferentialOrdnung? {
    val verbunden = eingänge["ordnung"]
    val ausdruck = verbunden?.objekt as? ZahlAusdruck
    if (ausdruck != null) {
        val rational = ausdruck as? RationaleZahl
        if (rational != null) {
            if (rational.nenner != BigInteger.ONE || rational.zähler.signum() < 0) return null
            return DifferentialOrdnung.Konkret(rational.zähler)
        }
        return DifferentialOrdnung.Symbolisch(ausdruck, verbunden.annahmen + UnentscheidbareAussage("${ausdruck.zuLatex()}\\in\\mathbb N_0", "Differentialordnung"))
    }
    val fallback = knoten.parameter[DIFFERENTIAL_ORDNUNG_PARAMETER]?.trim()?.toBigIntegerOrNull() ?: return null
    return runCatching { DifferentialOrdnung.Konkret(fallback) }.getOrNull()
}

private fun KnotenAuswertungsKontext.bestimmeDifferentialOperator(): DifferentialOperator {
    val operatorId = knoten.parameter[DIFFERENTIAL_OPERATOR_PARAMETER]
    if (operatorId != DifferentialOperator.Partiell(1).operatorId) return DifferentialOperator.Total
    val index = knoten.parameter[DIFFERENTIAL_ARGUMENT_INDEX_PARAMETER]?.toIntOrNull()?.takeIf { it >= 1 } ?: 1
    return DifferentialOperator.Partiell(index)
}

private fun KnotenAuswertungsKontext.gemeinsameAnnahmen(): Set<Aussage> = eingänge.values.flatMap { it.annahmen }.toSet()
private fun KnotenAuswertungsKontext.fehlerErgebnis(nachricht: String) = KnotenAuswertungsErgebnis(ausgaben = emptyMap(), fehler = nachricht, eingänge = eingänge)

fun KartenDaten.migriereDifferentialKnoten(): KartenDaten = copy(knoten = knoten.map { knoten ->
    when (knoten.art) {
        DIFFERENTIAL_KNOTEN_ART -> knoten.normalisiereDifferentialParameter()
        HISTORISCHE_ABLEITUNG_ART -> knoten.copy(
            art = DIFFERENTIAL_KNOTEN_ART, name = "Ableitung",
            parameter = knoten.parameter + mapOf(
                DIFFERENTIAL_AUSGABEFORM_PARAMETER to DifferentialAusgabeForm.METHODE.name,
                DIFFERENTIAL_ORDNUNG_PARAMETER to (knoten.parameter["ordnung"] ?: "1"),
                DIFFERENTIAL_OPERATOR_PARAMETER to DifferentialOperator.Total.operatorId,
                DIFFERENTIAL_ARGUMENT_INDEX_PARAMETER to "1", DIFFERENTIAL_BEGRIFF_PARAMETER to DifferentialBegriff.REELL_FRECHET.name,
                DIFFERENTIAL_QUELLEN_ID_PARAMETER to "differential.variable",
            ),
        ).normalisiereDifferentialParameter()
        HISTORISCHES_DIFFERENTIAL_TERM_ART -> knoten.copy(
            art = DIFFERENTIAL_KNOTEN_ART, name = "Differentialterm",
            parameter = knoten.parameter + mapOf(
                DIFFERENTIAL_AUSGABEFORM_PARAMETER to DifferentialAusgabeForm.TERM.name,
                DIFFERENTIAL_ORDNUNG_PARAMETER to "1", DIFFERENTIAL_OPERATOR_PARAMETER to DifferentialOperator.Total.operatorId,
                DIFFERENTIAL_ARGUMENT_INDEX_PARAMETER to "1", DIFFERENTIAL_BEGRIFF_PARAMETER to DifferentialBegriff.REELL_FRECHET.name,
                DIFFERENTIAL_QUELLEN_ID_PARAMETER to (knoten.parameter["quellenId"] ?: "differential.variable"),
            ),
        ).normalisiereDifferentialParameter()
        else -> knoten
    }
})

private fun KnotenDaten.normalisiereDifferentialParameter(): KnotenDaten {
    val form = aktuelleDifferentialAusgabeForm(this)
    return konfiguriereDifferentialKnoten(copy(parameter = DifferentialKnotenVorlagen.Differential.standardParameter + parameter), form)
}

private fun differentialAnschluesse(form: DifferentialAusgabeForm): List<AnschlussDaten> = when (form) {
    DifferentialAusgabeForm.METHODE -> listOf(
        AnschlussDaten(name="methode", richtung=AnschlussRichtung.Eingang, kante=AnschlussKante.Links, art=MathematikAnschlussArten.Methode.id, reihenfolge=0),
        AnschlussDaten(name="ordnung", richtung=AnschlussRichtung.Eingang, kante=AnschlussKante.Links, art=MathematikAnschlussArten.Zahl.id, reihenfolge=1),
        AnschlussDaten(name="methode", richtung=AnschlussRichtung.Ausgang, kante=AnschlussKante.Rechts, art=MathematikAnschlussArten.Methode.id),
    )
    DifferentialAusgabeForm.TERM -> listOf(
        AnschlussDaten(name="term", richtung=AnschlussRichtung.Eingang, kante=AnschlussKante.Links, art=MathematikAnschlussArten.Zahl.id, reihenfolge=0),
        AnschlussDaten(name="nach", richtung=AnschlussRichtung.Eingang, kante=AnschlussKante.Links, art=MathematikAnschlussArten.Zahl.id, reihenfolge=1),
        AnschlussDaten(name="term", richtung=AnschlussRichtung.Ausgang, kante=AnschlussKante.Rechts, art=MathematikAnschlussArten.Zahl.id),
    )
}
