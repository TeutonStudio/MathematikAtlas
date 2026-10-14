package de.TeutonStudio.MathematikRechenSystem.kern

internal fun LatexFormelParser.parsePrimaer(): FormelAusdruck {
    leerraum()
    if (position >= text.length) fehler("Ausdruck erwartet.")
    return when (text[position]) {
        '(' -> {
            position++
            parseKlammerInhalt()
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
    if (name in setOf("pi", "sin", "cos", "tan", "cot", "sec", "csc", "sinh", "cosh", "tanh", "coth", "sech", "csch",
            "arcsin", "arccos", "arctan", "ln", "log", "exp", "min", "max") + griechischeBefehle &&
        quellen.any { it.anzeigeName == name }) {
        return parseSymbol(name, "\\$name")
    }
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
        in griechischeBefehle -> parseSymbol(name, "\\$name")
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
    if (quellen.any { it.anzeigeName == name }) return parseSymbol(name, "\\operatorname{$name}")
    val operatorId = funktionsNamen[name.lowercase()]
        ?: name.takeIf { it.startsWith("zahl.") }
        ?: fehler("Unbekannte Funktion '$name'.")
    return parseFunktion(operatorId)
}

internal fun LatexFormelParser.parseFunktion(operatorId: String): FormelAusdruck {
    leerraum()
    val argumente = when {
        position < text.length && text[position] == '(' -> {
            position++
            parseArgumentListe(')')
        }
        position < text.length && text[position] == '{' -> {
            position++
            parseArgumentListe('}')
        }
        else -> listOf(parsePrimaer())
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
    return parseSymbol(name, "\\mathrm{$name}")
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
    return parseSymbol(name, name)
}

/** Deklarationen haben Vorrang vor unqualifizierten Funktions- und Konstantennamen. */
internal fun LatexFormelParser.parseSymbol(name: String, latex: String): FormelAusdruck {
    val deklarationen = quellen.filter { it.anzeigeName == name }
    if (deklarationen.size > 1) fehler("Der Name '$name' ist im Importkontext mehrdeutig.")
    val quelle = deklarationen.singleOrNull()
    if (quelle != null) {
        val ziel = variable(name, latex, quelle.typ)
        return if (quelle.rolle == FormelSymbolRolle.METHODE) {
            parseMethodenFolge(ziel, quelle.methodenSignatur ?: fehler("Methodensignatur für '$name' fehlt."))
        } else ziel
    }
    val funktionsId = funktionsNamen[name.lowercase()]
    leerraum()
    if (funktionsId != null && position < text.length && text[position] in "({") return parseFunktion(funktionsId)
    if (position < text.length && text[position] == '(') {
        fehler("Mehrdeutiger Ausdruck '$name(...)': Methode oder Wert deklarieren oder Multiplikation ausdrücklich schreiben.")
    }
    return if (name == "e") literal(EulerscheZahl) else variable(name, latex)
}

/** Ein einzelner Wert in Klammern gruppiert; Kommas und leere Klammern bilden Tupel. */
internal fun LatexFormelParser.parseKlammerInhalt(): FormelAusdruck {
    if (verbrauche(')')) return operation("tupel.konstruktion", emptyList(), FormelTyp.TUPEL)
    val elemente = mutableListOf(parseSumme())
    if (verbrauche(')')) return gruppiere(elemente.single())
    erwarte(',')
    while (!verbrauche(')')) {
        elemente += parseSumme()
        if (verbrauche(')')) break
        erwarte(',')
    }
    return operation("tupel.konstruktion", elemente.mapIndexed { i, e -> "element-$i" to e }, FormelTyp.TUPEL)
}

/** Auch ein methodenwertiges Ergebnis kann unmittelbar erneut aufgerufen werden. */
internal fun LatexFormelParser.parseMethodenFolge(ziel: FormelAusdruck, signatur: MethodenSignatur): FormelAusdruck {
    if (!verbrauche('(')) return ziel
    val argumente = parseArgumentListe(')')
    if (argumente.size != signatur.argumente.size) fehler("Falsche Argumentanzahl der Methode.")
    val aufruf = FormelAusdruck.Operation(neueId("aufruf"), "methode.anwenden",
        listOf(FormelArgument("methode", 0, ziel)) + argumente.mapIndexed { i, a ->
            FormelArgument(signatur.argumente[i].id, i + 1, a)
        }, FormelTyp.TUPEL)
    val ergebnis = signatur.ergebnisse.singleOrNull() ?: return aufruf
    val methodenSignatur = ergebnis.methodenSignatur
    val typ = when {
        methodenSignatur != null -> FormelTyp.METHODE
        MathematischeTypen.typSystem.prüfe(ergebnis.typ,
            de.TeutonStudio.TypSystem.TypAusdruck.Atom(MathematischeTypen.Zahl)) == de.TeutonStudio.TypSystem.TypPrüfung.Kompatibel -> FormelTyp.ZAHL
        else -> FormelTyp.OBJEKT
    }
    val projektion = operation("tupel.projektion", listOf("tupel" to aufruf, "index" to literal(RationaleZahl.Eins)), typ)
    return if (methodenSignatur != null) parseMethodenFolge(projektion, methodenSignatur) else projektion
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
    "min" to "zahl.minimum",
    "max" to "zahl.maximum",
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
