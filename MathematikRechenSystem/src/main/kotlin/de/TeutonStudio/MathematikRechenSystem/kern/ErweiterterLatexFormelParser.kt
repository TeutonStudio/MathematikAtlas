package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Gemeinsamer Vorverarbeitungspfad für semantische LaTeX-Operatoren, die der
 * kompakte Grundparser nicht als gewöhnliche Funktionsnamen behandeln darf.
 */
internal object ErweiterterLatexFormelParser {
    private const val PLUS_MINUS_MARKER = "__ATLAS_PM__"
    private const val MINUS_PLUS_MARKER = "__ATLAS_MP__"
    private const val IDENTITAETS_MARKER = "__ATLAS_ID__"

    fun parse(quelle: String): FormelAusdruck {
        val vorbereitet = quelle
            .replace("\\operatorname{id}", IDENTITAETS_MARKER)
            .replace("\\pm", PLUS_MINUS_MARKER)
            .replace("\\mp", MINUS_PLUS_MARKER)
        return transformiere(LatexFormelParser(vorbereitet).parse())
    }

    private fun transformiere(ausdruck: FormelAusdruck): FormelAusdruck = when (ausdruck) {
        is FormelAusdruck.Literal,
        is FormelAusdruck.Platzhalter,
        -> ausdruck
        is FormelAusdruck.Variable -> when (ausdruck.name) {
            IDENTITAETS_MARKER -> ausdruck.copy(
                name = "id",
                latex = "\\operatorname{id}",
                typ = FormelTyp.METHODE,
            )
            else -> ausdruck
        }
        is FormelAusdruck.Operation -> {
            val argumente = ausdruck.argumente.map { argument ->
                argument.copy(ausdruck = transformiere(argument.ausdruck))
            }
            transformiereVorzeichenMarker(ausdruck.copy(argumente = argumente))
        }
    }

    private fun transformiereVorzeichenMarker(operation: FormelAusdruck.Operation): FormelAusdruck {
        if (operation.operatorId != "zahl.multiplikation" || operation.argumente.size != 2) return operation
        val sortiert = operation.argumente.sortedBy { it.position }
        val marker = sortiert[0].ausdruck as? FormelAusdruck.Variable ?: return operation
        val operatorId = when (marker.name) {
            PLUS_MINUS_MARKER -> "algebra.vorzeichen.plusMinus"
            MINUS_PLUS_MARKER -> "algebra.vorzeichen.minusPlus"
            else -> return operation
        }
        return FormelAusdruck.Operation(
            id = operation.id,
            operatorId = operatorId,
            argumente = listOf(FormelArgument("operand", 0, sortiert[1].ausdruck)),
            typ = FormelTyp.TUPEL,
            bedingungen = operation.bedingungen,
            explizitGruppiert = operation.explizitGruppiert,
        )
    }
}
