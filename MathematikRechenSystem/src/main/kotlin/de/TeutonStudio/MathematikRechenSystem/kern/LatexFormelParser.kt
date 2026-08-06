package de.TeutonStudio.MathematikRechenSystem.kern

import java.math.BigInteger

internal class LatexFormelParser(quelle: String) {
    internal val text = quelle
        .replace("\\left", "")
        .replace("\\right", "")
        .replace("\\{", "{")
        .replace("\\}", "}")
        .replace("\\,", " ")
        .replace("\\;", " ")
        .replace("\\!", "")
        .trim()
    internal var position = 0
    internal var idZaehler = 0

    fun parse(): FormelAusdruck {
        if (text.isBlank()) fehler("Die Formel ist leer.")
        val ausdruck = parseSumme()
        leerraum()
        if (position != text.length) fehler("Unerwartetes Zeichen '${text[position]}'.")
        return ausdruck
    }

    internal fun parseSumme(): FormelAusdruck {
        var links = parseProdukt()
        while (true) {
            leerraum()
            links = when {
                verbrauche('+') -> operation("zahl.addition", links, parseProdukt(), "a", "b")
                verbrauche('-') -> operation("zahl.subtraktion", links, parseProdukt(), "a", "b")
                else -> return links
            }
        }
    }

    internal fun parseProdukt(): FormelAusdruck {
        var links = parsePotenz()
        while (true) {
            leerraum()
            links = when {
                verbraucheBefehl("cdot") || verbraucheBefehl("times") || verbrauche('*') ->
                    operation("zahl.multiplikation", links, parsePotenz(), "a", "b")
                verbraucheBefehl("div") -> {
                    val seite = parseDivisionsSeite()
                    operation(
                        if (seite == DivisionsSeite.RECHTS) "algebra.division.rechts" else "algebra.division.links",
                        links,
                        parsePotenz(),
                        "dividend",
                        "divisor",
                    )
                }
                verbrauche('/') -> operation("zahl.division", links, parsePotenz(), "zaehler", "nenner")
                beginntPrimaer() -> operation("zahl.multiplikation", links, parsePotenz(), "a", "b")
                else -> return links
            }
        }
    }

    internal fun parsePotenz(): FormelAusdruck {
        var basis = parseUnaer()
        leerraum()
        if (verbrauche('^')) {
            basis = parseIterationsExponent(basis)
        }
        while (true) {
            leerraum()
            if (!verbraucheBefehl("vert")) break
            erwarte('_')
            val menge = parseGruppenOderPrimaer()
            basis = operation(
                "methode.einschraenkung",
                listOf("methode" to basis, "menge" to menge),
                FormelTyp.METHODE,
            )
        }
        return basis
    }

    private fun parseIterationsExponent(basis: FormelAusdruck): FormelAusdruck {
        leerraum()
        if (position < text.length && text[position] == '{') {
            val roh = liesGruppenText().trim()
            val roemisch = parseRoemischeOrdnungOderNull(roh)
            return when {
                roh.startsWith("\\langle") && roh.endsWith("\\rangle") -> {
                    val innen = roh.removePrefix("\\langle").removeSuffix("\\rangle").trim()
                    operation(
                        "iteration.selbstkomposition",
                        listOf("methode" to basis, "ordnung" to importiereTeilAusdruck(innen)),
                        FormelTyp.METHODE,
                    )
                }
                roh.startsWith('(') && roh.endsWith(')') -> operation(
                    "iteration.differentiation",
                    listOf(
                        "methode" to basis,
                        "ordnung" to importiereTeilAusdruck(roh.substring(1, roh.length - 1)),
                    ),
                    FormelTyp.METHODE,
                )
                roemisch != null -> operation(
                    "iteration.differentiation",
                    listOf("methode" to basis, "ordnung" to literal(RationaleZahl.von(roemisch))),
                    FormelTyp.METHODE,
                )
                else -> operation(
                    "iteration.multiplikation",
                    listOf("basis" to basis, "ordnung" to importiereTeilAusdruck(roh)),
                )
            }
        }
        return operation(
            "iteration.multiplikation",
            listOf("basis" to basis, "ordnung" to parseGruppenOderPrimaer()),
        )
    }

    private fun parseDivisionsSeite(): DivisionsSeite {
        erwarte('_')
        leerraum()
        val marker = if (position < text.length && text[position] == '{') {
            liesGruppenText().trim()
        } else {
            if (position >= text.length) fehler("Divisionsseite R oder L erwartet.")
            text[position++].toString()
        }
        return when (marker.uppercase()) {
            "R" -> DivisionsSeite.RECHTS
            "L" -> DivisionsSeite.LINKS
            else -> fehler("Unbekannte Divisionsseite '$marker'.")
        }
    }

    private fun importiereTeilAusdruck(quelle: String): FormelAusdruck =
        vergibNeueIds(LatexFormelParser(quelle).parse())

    private fun vergibNeueIds(ausdruck: FormelAusdruck): FormelAusdruck = when (ausdruck) {
        is FormelAusdruck.Literal -> ausdruck.copy(id = neueId("literal"))
        is FormelAusdruck.Variable -> ausdruck.copy(id = neueId("variable"))
        is FormelAusdruck.Platzhalter -> ausdruck.copy(id = neueId("platzhalter"))
        is FormelAusdruck.Operation -> ausdruck.copy(
            id = neueId("operation"),
            argumente = ausdruck.argumente.map { argument ->
                argument.copy(ausdruck = vergibNeueIds(argument.ausdruck))
            },
        )
    }

    private fun parseRoemischeOrdnungOderNull(roh: String): BigInteger? {
        if (!roh.startsWith("\\mathrm{") || !roh.endsWith('}')) return null
        val zeichen = roh.removePrefix("\\mathrm{").dropLast(1).trim().uppercase()
        if (zeichen.isBlank()) return null
        val werte = mapOf('I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100, 'D' to 500, 'M' to 1000)
        var summe = 0
        for (index in zeichen.indices) {
            val wert = werte[zeichen[index]] ?: return null
            val naechster = zeichen.getOrNull(index + 1)?.let(werte::get) ?: 0
            summe += if (wert < naechster) -wert else wert
        }
        val zahl = BigInteger.valueOf(summe.toLong())
        return zahl.takeIf { roemischeZahlOderNull(it) == zeichen }
    }

    internal fun parseUnaer(): FormelAusdruck {
        leerraum()
        if (verbrauche('-')) {
            return operation(
                "zahl.subtraktion",
                literal(RationaleZahl.Null),
                parseUnaer(),
                "a",
                "b",
            )
        }
        if (verbrauche('+')) return parseUnaer()
        return parsePrimaer()
    }

    internal fun beginntPrimaer(): Boolean {
        leerraum()
        if (position >= text.length) return false
        val zeichen = text[position]
        if (zeichen in listOf(')', '}', ']', ',', '+', '-', '/', '*', '^', '|')) return false
        if (zeichen == '\\') {
            return !schautBefehl("cdot") && !schautBefehl("times") && !schautBefehl("div") && !schautBefehl("vert")
        }
        return zeichen == '(' || zeichen == '{' || zeichen == '.' || zeichen.isLetterOrDigit() || zeichen == '_'
    }

    internal fun schautBefehl(name: String): Boolean = text.startsWith("\\$name", position)

    internal fun verbraucheBefehl(name: String): Boolean {
        leerraum()
        if (!schautBefehl(name)) return false
        val ende = position + name.length + 1
        if (ende < text.length && text[ende].isLetter()) return false
        position = ende
        return true
    }

    internal fun liesBefehlsName(): String {
        val start = position
        while (position < text.length && text[position].isLetter()) position++
        if (start == position) fehler("LaTeX-Befehlsname erwartet.")
        return text.substring(start, position)
    }

    internal fun gruppiere(ausdruck: FormelAusdruck): FormelAusdruck = when (ausdruck) {
        is FormelAusdruck.Operation -> ausdruck.copy(explizitGruppiert = true)
        else -> ausdruck
    }

    internal fun literal(wert: MathematischesObjekt): FormelAusdruck.Literal =
        FormelAusdruck.Literal(neueId("literal"), wert, FormelTyp.ZAHL)

    internal fun variable(
        name: String,
        latex: String,
        typ: FormelTyp = FormelTyp.ZAHL,
    ): FormelAusdruck.Variable = FormelAusdruck.Variable(neueId("variable"), name, latex, typ)

    internal fun operation(
        operatorId: String,
        links: FormelAusdruck,
        rechts: FormelAusdruck,
        linkeRolle: String,
        rechteRolle: String,
    ): FormelAusdruck.Operation = operation(
        operatorId,
        listOf(linkeRolle to links, rechteRolle to rechts),
    )

    internal fun operation(
        operatorId: String,
        argumente: List<Pair<String, FormelAusdruck>>,
        typ: FormelTyp = FormelTyp.ZAHL,
    ): FormelAusdruck.Operation = FormelAusdruck.Operation(
        id = neueId("operation"),
        operatorId = operatorId,
        argumente = argumente.mapIndexed { index, (rolle, ausdruck) ->
            FormelArgument(rolle, index, ausdruck)
        },
        typ = typ,
    )

    internal fun neueId(art: String): String = "latex-$art-${++idZaehler}"

    internal fun leerraum() {
        while (position < text.length && text[position].isWhitespace()) position++
    }

    internal fun verbrauche(zeichen: Char): Boolean {
        leerraum()
        if (position >= text.length || text[position] != zeichen) return false
        position++
        return true
    }

    internal fun erwarte(zeichen: Char) {
        if (!verbrauche(zeichen)) fehler("'$zeichen' erwartet.")
    }

    internal fun fehler(nachricht: String): Nothing = throw LatexFormelParseFehler(position, nachricht)
}
