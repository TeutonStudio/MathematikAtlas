package de.TeutonStudio.MathematikRechenSystem.kern

internal class LatexFormelParser(quelle: String) {
    internal val text = quelle
        .replace("\\left", "")
        .replace("\\right", "")
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
            val exponent = parseGruppenOderPrimaer()
            basis = operation("zahl.potenz", basis, exponent, "basis", "exponent")
        }
        return basis
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
            return !schautBefehl("cdot") && !schautBefehl("times")
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

    internal fun variable(name: String, latex: String): FormelAusdruck.Variable =
        FormelAusdruck.Variable(neueId("variable"), name, latex, FormelTyp.ZAHL)

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
    ): FormelAusdruck.Operation = FormelAusdruck.Operation(
        id = neueId("operation"),
        operatorId = operatorId,
        argumente = argumente.mapIndexed { index, (rolle, ausdruck) ->
            FormelArgument(rolle, index, ausdruck)
        },
        typ = FormelTyp.ZAHL,
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
