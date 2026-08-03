package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val ZAHLENRECHNER_FORMEL_ID = "zahl.formel"
const val ZAHLENRECHNER_FORMEL_LATEX = "formelLatex"
const val ZAHLENRECHNER_FORMEL_VARIABLEN = "formelVariablen"

/** Ergänzende stabile Operatorzustände des universellen Zahlenrechners. */
enum class ErweiterterZahlenOperator(
    val stabileId: String,
    val titel: String,
    val symbolLatex: String,
) {
    TANGENS("zahl.tan", "Tangens", "\\tan"),
    COTANGENS("zahl.cot", "Cotangens", "\\cot"),
    SEKANS("zahl.sec", "Sekans", "\\sec"),
    KOSEKANS("zahl.csc", "Kosekans", "\\csc"),
    ARCTANGENS("zahl.arctan", "Arcus Tangens", "\\arctan"),
    SINUS_HYPERBOLICUS("zahl.sinh", "Sinus hyperbolicus", "\\sinh"),
    COSINUS_HYPERBOLICUS("zahl.cosh", "Cosinus hyperbolicus", "\\cosh"),
    TANGENS_HYPERBOLICUS("zahl.tanh", "Tangens hyperbolicus", "\\tanh"),
    COTANGENS_HYPERBOLICUS("zahl.coth", "Cotangens hyperbolicus", "\\coth"),
    SEKANS_HYPERBOLICUS("zahl.sech", "Sekans hyperbolicus", "\\operatorname{sech}"),
    KOSEKANS_HYPERBOLICUS("zahl.csch", "Kosekans hyperbolicus", "\\operatorname{csch}"),
    ;

    companion object {
        fun vonId(id: String?): ErweiterterZahlenOperator? = entries.firstOrNull { operator ->
            id == operator.stabileId || id.equals(operator.name, ignoreCase = true)
        }
    }
}

fun istZahlenRechnerFormel(knoten: KnotenDaten): Boolean =
    knoten.art == ZAHLENRECHNER_ART && knoten.parameter[ZAHLENRECHNER_OPERATOR] == ZAHLENRECHNER_FORMEL_ID

fun verwendetGradWinkel(operator: ErweiterterZahlenOperator): Boolean = operator in setOf(
    ErweiterterZahlenOperator.TANGENS,
    ErweiterterZahlenOperator.COTANGENS,
    ErweiterterZahlenOperator.SEKANS,
    ErweiterterZahlenOperator.KOSEKANS,
)

fun konfiguriereStandardZahlenRechner(
    knoten: KnotenDaten,
    operator: UniversellerZahlenOperator,
): KnotenDaten {
    val bereinigt = knoten.copy(
        parameter = knoten.parameter - ZAHLENRECHNER_FORMEL_LATEX - ZAHLENRECHNER_FORMEL_VARIABLEN,
    )
    return konfiguriereZahlenRechner(bereinigt, operator = operator)
}

fun konfiguriereErweitertenZahlenRechner(
    knoten: KnotenDaten,
    operator: ErweiterterZahlenOperator,
): KnotenDaten {
    require(knoten.art == ZAHLENRECHNER_ART)
    val bisherigerEingang = knoten.anschlüsse
        .firstOrNull { it.richtung == AnschlussRichtung.Eingang && it.name == "a" }
        ?: knoten.anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Eingang }
    val bisherigerAusgang = knoten.anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Ausgang }
    val eingang = (bisherigerEingang ?: AnschlussDaten(
        name = "a",
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = MathematikAnschlussArten.Zahl.id,
    )).copy(
        name = "a",
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = MathematikAnschlussArten.Zahl.id,
        reihenfolge = 0,
        kannSichErweitern = false,
        dynamischErzeugt = false,
    )
    val ausgang = (bisherigerAusgang ?: AnschlussDaten(
        name = "wert",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = MathematikAnschlussArten.Zahl.id,
    )).copy(
        name = "wert",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = MathematikAnschlussArten.Zahl.id,
        reihenfolge = 0,
        kannSichErweitern = false,
        dynamischErzeugt = false,
    )
    return knoten.copy(
        name = operator.titel,
        anschlüsse = listOf(eingang, ausgang),
        parameter = (knoten.parameter - ZAHLENRECHNER_FORMEL_LATEX - ZAHLENRECHNER_FORMEL_VARIABLEN) +
            (ZAHLENRECHNER_OPERATOR to operator.stabileId),
    )
}

fun konfiguriereZahlenRechnerFormel(
    knoten: KnotenDaten,
    latex: String,
): KnotenDaten {
    require(knoten.art == ZAHLENRECHNER_ART)
    val import = FormelLatexCodec.importiere(latex)
    val ausdruck = (import as? FormelLatexImportErgebnis.Erfolg)?.ausdruck
        ?: error((import as FormelLatexImportErgebnis.Fehler).nachricht)
    require(FormelAusdruckPruefer.pruefe(ausdruck) == FormelPruefung.Gueltig) {
        "Die Formel enthält noch Platzhalter oder ungültige Teilstrukturen."
    }
    val kanonisch = FormelLatexCodec.exportiere(ausdruck)
    val variablen = formelVariablen(ausdruck)
    val vorhandene = knoten.anschlüsse
        .filter { it.richtung == AnschlussRichtung.Eingang }
        .associateBy { it.name }
    val eingänge = variablen.mapIndexed { index, name ->
        (vorhandene[name] ?: AnschlussDaten(
            name = name,
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = MathematikAnschlussArten.Zahl.id,
        )).copy(
            name = name,
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = MathematikAnschlussArten.Zahl.id,
            reihenfolge = index,
            kannSichErweitern = false,
            dynamischErzeugt = true,
        )
    }
    val alterAusgang = knoten.anschlüsse.firstOrNull { it.richtung == AnschlussRichtung.Ausgang }
    val ausgang = (alterAusgang ?: AnschlussDaten(
        name = "wert",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = MathematikAnschlussArten.Zahl.id,
    )).copy(
        name = "wert",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = MathematikAnschlussArten.Zahl.id,
        reihenfolge = 0,
        kannSichErweitern = false,
        dynamischErzeugt = false,
    )
    return knoten.copy(
        name = "Formel",
        anschlüsse = eingänge + ausgang,
        parameter = knoten.parameter + mapOf(
            ZAHLENRECHNER_OPERATOR to ZAHLENRECHNER_FORMEL_ID,
            ZAHLENRECHNER_FORMEL_LATEX to kanonisch,
            ZAHLENRECHNER_FORMEL_VARIABLEN to variablen.joinToString(","),
        ),
    )
}

/** Überschreibt nur die ergänzten Zustände; alle bisherigen Operatoren delegieren an den bestehenden Auswerter. */
fun MathematikAuswerterRegister.registriereZahlenRechnerErweiterungen() {
    val basis = requireNotNull(finde(ZAHLENRECHNER_ART))
    registriere(ZAHLENRECHNER_ART) { kontext ->
        val operatorId = kontext.knoten.parameter[ZAHLENRECHNER_OPERATOR]
        when {
            operatorId == ZAHLENRECHNER_FORMEL_ID -> werteFormelAus(kontext)
            ErweiterterZahlenOperator.vonId(operatorId) != null ->
                werteErweitertenOperatorAus(kontext, requireNotNull(ErweiterterZahlenOperator.vonId(operatorId)))
            else -> basis.auswerten(kontext)
        }
    }
}

private fun werteErweitertenOperatorAus(
    kontext: KnotenAuswertungsKontext,
    operator: ErweiterterZahlenOperator,
): KnotenAuswertungsErgebnis {
    val eingang = kontext.eingänge["a"] ?: error("Der Eingang a fehlt.")
    val argument = eingang.objekt as? ZahlAusdruck ?: error("Der Eingang a enthält keine Zahl.")
    val argumentLatex = eingang.anzeigeLatex()
    val grad = kontext.knoten.parameter[ZAHLENRECHNER_GRADWINKEL].toBoolean()
    val gradAuswerten = kontext.knoten.parameter[ZAHLENRECHNER_GRAD_AUSWERTEN] != "false"
    val effektivLatex = if (grad && verwendetGradWinkel(operator)) {
        gradWinkelLatex(argument, gradAuswerten)
    } else {
        argumentLatex
    }
    val latex = "${operator.symbolLatex}\\left($effektivLatex\\right)"
    val objekt = symbolischerZahlterm("${operator.stabileId}:$latex", latex)
    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf(
            "wert" to BedingterWert(
                objekt = objekt,
                annahmen = eingang.annahmen,
                werteVorrat = eingang.werteVorrat,
                reelleVariablen = eingang.reelleVariablen,
                variablenQuellen = eingang.variablenQuellen,
                latexDarstellung = latex,
            ),
        ),
        eingänge = kontext.eingänge,
        warnungen = definitionsHinweise(operator),
    )
}

private fun werteFormelAus(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
    val latex = kontext.knoten.parameter[ZAHLENRECHNER_FORMEL_LATEX]
        ?: error("Die gespeicherte Formel fehlt.")
    val import = FormelLatexCodec.importiere(latex)
    val ausdruck = (import as? FormelLatexImportErgebnis.Erfolg)?.ausdruck
        ?: error((import as FormelLatexImportErgebnis.Fehler).nachricht)
    require(FormelAusdruckPruefer.pruefe(ausdruck) == FormelPruefung.Gueltig) {
        "Die gespeicherte Formel ist unvollständig."
    }
    var eingesetzt = FormelLatexCodec.exportiere(ausdruck)
    formelVariablen(ausdruck).sortedByDescending(String::length).forEach { name ->
        val wert = kontext.eingänge[name] ?: error("Der Formeleingang '$name' fehlt.")
        val muster = Regex("(?<![A-Za-z0-9_])${Regex.escape(name)}(?![A-Za-z0-9_])")
        eingesetzt = eingesetzt.replace(muster, "\\left(${wert.anzeigeLatex()}\\right)")
    }
    val werte = kontext.eingänge.values
    val objekt = symbolischerZahlterm("formel:${eingesetzt.hashCode().toUInt()}", eingesetzt)
    return KnotenAuswertungsErgebnis(
        ausgaben = mapOf(
            "wert" to BedingterWert(
                objekt = objekt,
                annahmen = werte.flatMap { it.annahmen }.toSet(),
                werteVorrat = werte.mapNotNull { it.werteVorrat }.distinct().singleOrNull(),
                reelleVariablen = reelleVariablen(werte),
                variablenQuellen = werte.flatMap { it.variablenQuellen }.geordnetEindeutig(),
                latexDarstellung = eingesetzt,
            ),
        ),
        eingänge = kontext.eingänge,
        warnungen = listOf("Formel: $latex"),
    )
}

private fun definitionsHinweise(operator: ErweiterterZahlenOperator): List<String> = when (operator) {
    ErweiterterZahlenOperator.TANGENS,
    ErweiterterZahlenOperator.SEKANS,
    -> listOf("Definitionslücke: cos(x) darf nicht null sein.")
    ErweiterterZahlenOperator.COTANGENS,
    ErweiterterZahlenOperator.KOSEKANS,
    -> listOf("Definitionslücke: sin(x) darf nicht null sein.")
    ErweiterterZahlenOperator.COTANGENS_HYPERBOLICUS,
    ErweiterterZahlenOperator.KOSEKANS_HYPERBOLICUS,
    -> listOf("Definitionslücke: sinh(x) darf nicht null sein.")
    else -> emptyList()
}

private fun formelVariablen(ausdruck: FormelAusdruck): List<String> {
    val namen = linkedSetOf<String>()
    fun besuche(teil: FormelAusdruck) {
        when (teil) {
            is FormelAusdruck.Variable -> namen += teil.name
            is FormelAusdruck.Operation -> teil.argumente.sortedBy { it.position }.forEach { besuche(it.ausdruck) }
            else -> Unit
        }
    }
    besuche(ausdruck)
    return namen.toList()
}
