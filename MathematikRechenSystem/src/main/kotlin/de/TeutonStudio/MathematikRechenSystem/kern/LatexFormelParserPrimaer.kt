package de.TeutonStudio.MathematikRechenSystem.kern

internal fun LatexFormelParser.parsePrimaer(): FormelAusdruck {
    leerraum()
    if (position >= text.length) fehler("Ausdruck erwartet.")
    return when (text[position]) {
        '(' -> {
            position++
            val innen = parseSumme()
            erwarte(')')
            gruppiere(innen)
        }
        '{' -> parseGruppe()
        '\\' -> parseBefehl()
        '|' -> parseBetrag()
        in '0'..'9', '.' -> parseZahl()
        else -> parseNameOderKonstante()
    }
}

internal fun LatexFormelParser.parseBefehl(): FormelAusdruck {
    val start = position
    position++
    val name = liesBefehlsName()
    return when (name) {
        "frac" -> operation("zahl.division", parseGruppe(), parseGruppe(), "zaehler", "nenner")
        "sqrt" -> {
            val grad = if (verbrauche('[')) {
                val ausdruck = parseSumme()
                erwarte(']')
                ausdruck
            } else null
            val radikand = parseGruppe()
            if (grad == null) operation("zahl.wurzel", listOf("radikand" to radikand))
            else operation("zahl.wurzel", listOf("radikand" to radikand, "grad" to grad))
        }
        "pi" -> literal(Pi)
        "log" -> parseLogarithmus()
        "mathrm" -> parseGruppeAlsName()
        "operatorname" -> parseOperatorname()
        in funktionsBefehle -> parseFunktion(funktionsBefehle.getValue(name))
        in griechischeBefehle -> variable(name, "\\$name")
        else -> throw LatexFormelParseFehler(start, "Nicht unterstützter LaTeX-Befehl \\$name.")
    }
}


internal fun LatexFormelParser.parseBetrag(): FormelAusdruck {
    erwarte('|')
    val innen = parseSumme()
    erwarte('|')
    return operation("zahl.betrag", listOf("argument" to innen))
}

internal fun LatexFormelParser.parseLogarithmus(): FormelAusdruck {
    leerraum()
    val basis = if (verbrauche('_')) parseGruppenOderPrimaer() else null
    leerraum()
    val argument = when {
        position < text.length && text[position] == '(' -> {
            position++
            val wert = parseSumme()
            erwarte(')')
            wert
        }
        position < text.length && text[position] == '{' -> parseGruppe()
        else -> parsePrimaer()
    }
    return if (basis == null) {
        operation("zahl.log10", listOf("argument" to argument))
    } else {
        operation("zahl.logarithmus", listOf("basis" to basis, "argument" to argument))
    }
}

internal fun LatexFormelParser.parseOperatorname(): FormelAusdruck {
    val name = liesGruppenText().trim()
    val operatorId = funktionsNamen[name.lowercase()]
        ?: name.takeIf { it.startsWith("zahl.") }
        ?: fehler("Unbekannte Funktion '$name'.")
    return parseFunktion(operatorId)
}

internal fun LatexFormelParser.parseFunktion(operatorId: String): FormelAusdruck {
    leerraum()
    val argumente = if (position < text.length && text[position] == '(') {
        position++
        parseArgumentListe(')')
    } else if (position < text.length && text[position] == '{') {
        listOf(parseGruppe())
    } else {
        listOf(parsePrimaer())
    }
    val rollen = operatorRollen(operatorId, argumente.size)
    return operation(operatorId, argumente.mapIndexed { index, argument -> rollen[index] to argument })
}

internal fun LatexFormelParser.parseArgumentListe(ende: Char): List<FormelAusdruck> {
    leerraum()
    if (verbrauche(ende)) return emptyList()
    val argumente = mutableListOf<FormelAusdruck>()
    while (true) {
        argumente += parseSumme()
        leerraum()
        if (verbrauche(ende)) return argumente
        erwarte(',')
    }
}

internal fun LatexFormelParser.parseGruppenOderPrimaer(): FormelAusdruck {
    leerraum()
    return if (position < text.length && text[position] == '{') parseGruppe() else parsePrimaer()
}

internal fun LatexFormelParser.parseGruppe(): FormelAusdruck {
    erwarte('{')
    val innen = parseSumme()
    erwarte('}')
    return innen
}

internal fun LatexFormelParser.parseGruppeAlsName(): FormelAusdruck {
    val name = liesGruppenText().trim()
    if (name.isBlank()) fehler("Leerer Variablenname.")
    return variable(name, "\\mathrm{$name}")
}

internal fun LatexFormelParser.liesGruppenText(): String {
    leerraum()
    erwarte('{')
    val start = position
    var tiefe = 1
    while (position < text.length && tiefe > 0) {
        when (text[position++]) {
            '{' -> tiefe++
            '}' -> tiefe--
        }
    }
    if (tiefe != 0) fehler("Nicht geschlossene Gruppe.")
    return text.substring(start, position - 1)
}

internal fun LatexFormelParser.parseZahl(): FormelAusdruck {
    val start = position
    while (position < text.length && (text[position].isDigit() || text[position] == '.')) position++
    val wert = text.substring(start, position)
    return literal(parseRationaleEingabe(wert))
}

internal fun LatexFormelParser.parseNameOderKonstante(): FormelAusdruck {
    val start = position
    if (!text[position].isLetter() && text[position] != '_') {
        fehler("Variable, Zahl oder Funktion erwartet.")
    }
    position++
    while (position < text.length && (text[position].isLetterOrDigit() || text[position] == '_')) position++
    val name = text.substring(start, position)
    val funktionsId = funktionsNamen[name.lowercase()]
    if (funktionsId != null) {
        leerraum()
        if (position < text.length && (text[position] == '(' || text[position] == '{')) {
            return parseFunktion(funktionsId)
        }
    }
    return when (name) {
        "e" -> literal(EulerscheZahl)
        else -> variable(name, name)
    }
}

internal val funktionsBefehle = mapOf(
    "sin" to "zahl.sin",
    "cos" to "zahl.cos",
    "tan" to "zahl.tan",
    "cot" to "zahl.cot",
    "sec" to "zahl.sec",
    "csc" to "zahl.csc",
    "sinh" to "zahl.sinh",
    "cosh" to "zahl.cosh",
    "tanh" to "zahl.tanh",
    "coth" to "zahl.coth",
    "sech" to "zahl.sech",
    "csch" to "zahl.csch",
    "arcsin" to "zahl.arcsin",
    "arccos" to "zahl.arccos",
    "arctan" to "zahl.arctan",
    "ln" to "zahl.ln",
    "log" to "zahl.log10",
    "exp" to "zahl.exp",
)

internal val funktionsNamen = funktionsBefehle + mapOf(
    "cosec" to "zahl.csc",
    "abs" to "zahl.betrag",
    "sqrt" to "zahl.wurzel",
)

internal val griechischeBefehle = setOf(
    "alpha", "beta", "gamma", "delta", "epsilon", "zeta", "eta", "theta",
    "iota", "kappa", "lambda", "mu", "nu", "xi", "rho", "sigma", "tau",
    "upsilon", "phi", "chi", "psi", "omega",
)
