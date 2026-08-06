package de.TeutonStudio.MathematikRechenSystem.kern

sealed interface DifferentialOperator {
    val operatorId: String
    fun zuLatexPraefix(): String

    data object Total : DifferentialOperator {
        override val operatorId: String = "analysis.differential.total"
        override fun zuLatexPraefix(): String = "d"
    }

    data class Partiell(val argumentIndex: Int) : DifferentialOperator {
        init { require(argumentIndex >= 1) { "Argumentindizes beginnen sichtbar bei 1." } }
        override val operatorId: String = "analysis.differential.partiell"
        override fun zuLatexPraefix(): String = "\\partial_{${argumentIndex}}"
    }
}

fun DifferentialOperator.pruefeFuer(methode: Methode) {
    if (this is DifferentialOperator.Partiell) {
        require(argumentIndex <= methode.parameter.size) {
            "Die Methode '${methode.name}' besitzt kein Argument $argumentIndex."
        }
    }
}

data class DifferentialDerIdentitaet(
    val werteVorrat: MengenAusdruck,
    val operator: DifferentialOperator = DifferentialOperator.Total,
) : MathematischesObjekt {
    override fun zuLatex(): String =
        "${operator.zuLatexPraefix()}\\left(\\operatorname{id}\\vert_{${werteVorrat.zuLatex()}}\\right)"
}

data class AbleitungsMethodenAusdruck(
    val methode: Methode,
    val operator: DifferentialOperator = DifferentialOperator.Total,
) : MathematischesObjekt {
    init { operator.pruefeFuer(methode) }

    override fun zuLatex(): String = when (operator) {
        DifferentialOperator.Total -> "${methode.name}^{\\mathrm I}"
        is DifferentialOperator.Partiell -> "\\partial_{${operator.argumentIndex}}${methode.name}"
    }
}

data class MethodenDifferentialGleichung(
    val methode: Methode,
    val werteVorrat: MengenAusdruck,
    val operator: DifferentialOperator = DifferentialOperator.Total,
) : Aussage {
    init { operator.pruefeFuer(methode) }

    val ableitung = AbleitungsMethodenAusdruck(methode, operator)
    val identitaetsDifferential = DifferentialDerIdentitaet(werteVorrat, operator)

    override fun entscheide(kontext: RechenKontext): AussageErgebnis = AussageErgebnis(
        wahrheitswert = null,
        status = EntscheidungsStatus.Unbekannt,
        begründung = "Die Differentialgleichung ist eine strukturierte Definitionsaussage.",
    )

    override fun zuLatex(): String =
        "${operator.zuLatexPraefix()}${methode.name}=" +
            "${ableitung.zuLatex()}\\cdot${identitaetsDifferential.zuLatex()}"
}

data class DifferentialVariable(
    val variable: Variable,
    val quellenId: String = variable.name,
) : ZahlAusdruck {
    init { require(quellenId.isNotBlank()) }
    override fun zuLatex(): String = "d${variable.zuLatex()}"
}

data class DifferentialTerm(
    val ursprung: ZahlAusdruck,
    val variable: Variable,
    val ableitung: ZahlAusdruck,
    val operator: DifferentialOperator = DifferentialOperator.Total,
    val quellenId: String = variable.name,
) : ZahlAusdruck {
    init {
        require(quellenId.isNotBlank())
        if (operator is DifferentialOperator.Partiell) {
            require(operator.argumentIndex >= 1)
        }
    }

    val differentialVariable = DifferentialVariable(variable, quellenId)

    override fun zuLatex(): String =
        "${operator.zuLatexPraefix()}\\left(${ursprung.zuLatex()}\\right)=" +
            "${ableitung.zuLatex()}\\cdot${differentialVariable.zuLatex()}"
}

fun bildeDifferentialTerm(
    term: ZahlAusdruck,
    variable: Variable,
    operator: DifferentialOperator = DifferentialOperator.Total,
    quellenId: String = variable.name,
): DifferentialTerm = DifferentialTerm(
    ursprung = term,
    variable = variable,
    ableitung = ableiten(term, variable).ergebnis,
    operator = operator,
    quellenId = quellenId,
)

fun partielleAbleitung(
    methode: Methode,
    argumentIndex: Int,
): AbleitungsMethodenAusdruck {
    val operator = DifferentialOperator.Partiell(argumentIndex)
    operator.pruefeFuer(methode)
    return AbleitungsMethodenAusdruck(methode, operator)
}

fun totaleAbleitung(methode: Methode): AbleitungsMethodenAusdruck =
    AbleitungsMethodenAusdruck(methode, DifferentialOperator.Total)

fun eindimensionaleAbleitungenStimmenUeberein(methode: Methode): Boolean =
    methode.parameter.size == 1
