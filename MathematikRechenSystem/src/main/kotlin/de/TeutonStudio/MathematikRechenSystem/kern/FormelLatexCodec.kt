package de.TeutonStudio.MathematikRechenSystem.kern

import java.math.BigDecimal
import java.math.BigInteger

sealed interface FormelLatexImportErgebnis {
    data class Erfolg(val ausdruck: FormelAusdruck) : FormelLatexImportErgebnis
    data class Fehler(val position: Int, val nachricht: String) : FormelLatexImportErgebnis
}

/**
 * Kontrollierter LaTeX-Roundtrip für den Formelbauer.
 *
 * Der Import interpretiert nur den dokumentierten mathematischen Teilumfang und
 * führt weder TeX-Makros noch beliebige Befehle aus. Der Export wird immer aus
 * der strukturierten Formel erzeugt.
 */
object FormelLatexCodec {
    fun exportiere(ausdruck: FormelAusdruck): String = ErweiterterFormelRenderer.render(ausdruck).latex
        .ersetzeFunktionsOperator("zahl.tan", "\\tan")
        .ersetzeFunktionsOperator("zahl.cot", "\\cot")
        .ersetzeFunktionsOperator("zahl.sec", "\\sec")
        .ersetzeFunktionsOperator("zahl.csc", "\\csc")
        .ersetzeFunktionsOperator("zahl.arctan", "\\arctan")
        .ersetzeFunktionsOperator("zahl.sinh", "\\sinh")
        .ersetzeFunktionsOperator("zahl.cosh", "\\cosh")
        .ersetzeFunktionsOperator("zahl.tanh", "\\tanh")
        .ersetzeFunktionsOperator("zahl.coth", "\\coth")
        .ersetzeFunktionsOperator("zahl.sech", "\\operatorname{sech}")
        .ersetzeFunktionsOperator("zahl.csch", "\\operatorname{csch}")

    fun importiere(latex: String): FormelLatexImportErgebnis = runCatching {
        ErweiterterLatexFormelParser.parse(latex)
    }.fold(
        onSuccess = FormelLatexImportErgebnis::Erfolg,
        onFailure = { fehler ->
            val parserFehler = fehler as? LatexFormelParseFehler
            FormelLatexImportErgebnis.Fehler(
                position = parserFehler?.position ?: 0,
                nachricht = parserFehler?.message ?: fehler.message ?: "Unbekannter LaTeX-Fehler.",
            )
        },
    )
}

internal class LatexFormelParseFehler(
    val position: Int,
    nachricht: String,
) : IllegalArgumentException("$nachricht (Position ${position + 1})")

internal fun operatorRollen(operatorId: String, anzahl: Int): List<String> = when (operatorId) {
    "zahl.division" -> listOf("zaehler", "nenner")
    "algebra.division.rechts", "algebra.division.links" -> listOf("dividend", "divisor")
    "zahl.potenz", "iteration.multiplikation" -> listOf("basis", "ordnung")
    "iteration.differentiation", "iteration.selbstkomposition" -> listOf("methode", "ordnung")
    "methode.einschraenkung" -> listOf("methode", "menge")
    "algebra.vorzeichen.plusMinus", "algebra.vorzeichen.minusPlus" -> listOf("operand")
    "zahl.logarithmus" -> listOf("basis", "argument")
    "zahl.wurzel" -> listOf("radikand", "grad")
    "zahl.subtraktion" -> listOf("a", "b")
    else -> List(anzahl.coerceAtLeast(1)) { index -> if (index == 0) "argument" else "argument${index + 1}" }
}.let { rollen ->
    if (rollen.size >= anzahl) rollen else rollen + List(anzahl - rollen.size) { "argument${rollen.size + it + 1}" }
}

internal fun parseRationaleEingabe(text: String): RationaleZahl {
    val bereinigt = text.trim()
    if ('/' in bereinigt) return RationaleZahl.parse(bereinigt)
    if ('.' !in bereinigt) return RationaleZahl.von(BigInteger(bereinigt))
    val dezimal = BigDecimal(bereinigt)
    val skala = dezimal.scale().coerceAtLeast(0)
    return RationaleZahl.von(
        dezimal.movePointRight(skala).toBigIntegerExact(),
        BigInteger.TEN.pow(skala),
    )
}

private fun String.ersetzeFunktionsOperator(operatorId: String, latexName: String): String =
    replace("\\operatorname{$operatorId}", latexName)
