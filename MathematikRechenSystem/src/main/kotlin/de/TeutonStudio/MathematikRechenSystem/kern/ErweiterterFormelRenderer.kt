package de.TeutonStudio.MathematikRechenSystem.kern

import java.math.BigInteger

/**
 * Semantischer Renderer für den kontrollierten LaTeX-Codec.
 * Operatoren werden ausschließlich aus stabilen Operator-IDs gerendert.
 */
internal object ErweiterterFormelRenderer {
    fun render(wurzel: FormelAusdruck): FormelRenderErgebnis {
        val text = StringBuilder()
        val bereiche = linkedMapOf<String, IntRange>()
        val bedingungen = linkedSetOf<Aussage>()

        fun schreibe(ausdruck: FormelAusdruck, elternPraezedenz: Int = 0) {
            val start = text.length
            when (ausdruck) {
                is FormelAusdruck.Literal -> {
                    val wert = ausdruck.wert
                    val klammern = wert is ZahlAusdruck && (elternPraezedenz >= 30 && positiverGegentermOderNull(wert) != null ||
                        elternPraezedenz > 10 && wert is Addition || elternPraezedenz > 20 && wert is Multiplikation)
                    if (klammern) text.append("\\left(")
                    text.append(wert.zuLatex())
                    if (klammern) text.append("\\right)")
                }
                is FormelAusdruck.Variable -> text.append(ausdruck.latex)
                is FormelAusdruck.Platzhalter -> text.append("\\square_{${ausdruck.beschriftung.atlasLatexText()}}")
                is FormelAusdruck.Operation -> {
                    bedingungen += ausdruck.bedingungen
                    val eigenePraezedenz = praezedenz(ausdruck.operatorId)
                    val klammern = ausdruck.explizitGruppiert || eigenePraezedenz < elternPraezedenz
                    if (klammern) text.append("\\left(")
                    schreibeOperation(ausdruck, eigenePraezedenz, text, ::schreibe)
                    if (klammern) text.append("\\right)")
                }
            }
            bereiche[ausdruck.id] = start until text.length
        }

        fun erfasseBedingungen(a: FormelAusdruck) {
            if (a is FormelAusdruck.Operation) {
                bedingungen += a.bedingungen
                a.argumente.forEach { erfasseBedingungen(it.ausdruck) }
            }
        }
        erfasseBedingungen(wurzel)
        schreibe(wurzel)
        // Vorzeichenhüllen bleiben im Bearbeitungsmodell adressierbar, auch wenn
        // die Darstellung ihren inneren Gegenwert ohne die Hülle schreibt.
        fun ergänzeBereiche(a: FormelAusdruck, eltern: IntRange) {
            val bereich = bereiche[a.id] ?: if (a is FormelAusdruck.Operation)
                a.argumente.mapNotNull { bereiche[it.ausdruck.id] }.firstOrNull() ?: eltern else eltern
            bereiche.putIfAbsent(a.id, bereich)
            if (a is FormelAusdruck.Operation) a.argumente.forEach { ergänzeBereiche(it.ausdruck, bereich) }
        }
        ergänzeBereiche(wurzel, 0 until text.length)
        return FormelRenderErgebnis(text.toString(), bereiche, bedingungen.toList())
    }

    private fun schreibeOperation(
        operation: FormelAusdruck.Operation,
        praezedenz: Int,
        text: StringBuilder,
        schreibe: (FormelAusdruck, Int) -> Unit,
    ) {
        val argumente = operation.argumente.sortedBy { it.position }.map { it.ausdruck }
        when (operation.operatorId) {
            "methode.anwenden" -> {
                schreibe(argumente[0], 40)
                text.append("\\left(")
                argumente.drop(1).forEachIndexed { i, a -> if (i > 0) text.append(","); schreibe(a, 0) }
                text.append("\\right)")
            }
            "tupel.konstruktion" -> {
                text.append("\\operatorname{Tupel}\\left(")
                argumente.forEachIndexed { i, a -> if (i > 0) text.append(","); schreibe(a, 0) }
                text.append("\\right)")
            }
            "tupel.projektion" -> {
                val quelle = argumente[0]
                text.append("\\operatorname{proj}_{")
                schreibe(argumente[1], 0); text.append("}{")
                schreibe(quelle, 0); text.append("}")
            }

            "formel.gruppierung" -> {
                text.append("\\left(")
                schreibe(argumente.argumentOderPlatzhalter(0, operation.id, "inhalt"), 0)
                text.append("\\right)")
            }
            "zahl.addition" -> argumente.forEachIndexed { index, argument ->
                val positiv = positiverFormelGegenterm(argument)
                if (positiv != null) {
                    text.append(if (index == 0) "-" else " - ")
                    schreibe(positiv, praezedenz + 1)
                } else {
                    if (index > 0) text.append(" + ")
                    schreibe(argument, praezedenz)
                }
            }
            "zahl.subtraktion" -> {
                val links = argumente.argumentOderPlatzhalter(0, operation.id, "links")
                val rechts = argumente.argumentOderPlatzhalter(1, operation.id, "rechts")
                val positiv = positiverFormelGegenterm(rechts)
                if ((links as? FormelAusdruck.Literal)?.wert == RationaleZahl.Null) {
                    if (positiv != null) schreibe(positiv, praezedenz + 1)
                    else { text.append("-"); schreibe(rechts, praezedenz + 1) }
                } else if (positiv != null) {
                    schreibe(links, praezedenz); text.append(" + "); schreibe(positiv, praezedenz + 1)
                } else binaer(argumente, " - ", praezedenz, text, schreibe)
            }
            "zahl.multiplikation" -> argumente.forEachIndexed { index, argument ->
                if (index > 0) text.append(" \\cdot ")
                schreibe(argument, praezedenz)
            }
            "zahl.division" -> schreibeBruch(argumente, operation.id, text, schreibe)
            "algebra.division.rechts" -> schreibeSeitigeDivision(argumente, "R", praezedenz, text, schreibe)
            "algebra.division.links" -> schreibeSeitigeDivision(argumente, "L", praezedenz, text, schreibe)
            "zahl.potenz", "iteration.multiplikation" -> schreibePotenz(argumente, praezedenz, text, schreibe)
            "iteration.differentiation" -> schreibeDifferentiation(argumente, praezedenz, text, schreibe)
            "iteration.selbstkomposition" -> {
                schreibe(argumente.argumentOderPlatzhalter(0, operation.id, "methode"), praezedenz)
                text.append("^{\\langle ")
                schreibe(argumente.argumentOderPlatzhalter(1, operation.id, "ordnung"), 0)
                text.append("\\rangle}")
            }
            "methode.umkehrfunktion" -> {
                schreibe(argumente.argumentOderPlatzhalter(0, operation.id, "methode"), praezedenz)
                text.append("^{\\langle -1\\rangle}")
            }
            "methode.einschraenkung" -> {
                schreibe(argumente.argumentOderPlatzhalter(0, operation.id, "methode"), praezedenz)
                text.append("\\vert_{")
                schreibe(argumente.argumentOderPlatzhalter(1, operation.id, "menge"), 0)
                text.append('}')
            }
            "algebra.vorzeichen.plusMinus" -> {
                text.append("\\pm\\,")
                schreibe(argumente.argumentOderPlatzhalter(0, operation.id, "operand"), praezedenz)
            }
            "algebra.vorzeichen.minusPlus" -> {
                text.append("\\mp\\,")
                schreibe(argumente.argumentOderPlatzhalter(0, operation.id, "operand"), praezedenz)
            }
            "zahl.wurzel" -> {
                val grad = argumente.getOrNull(1)
                text.append("\\sqrt")
                if (grad != null) {
                    text.append('[')
                    schreibe(grad, 0)
                    text.append(']')
                }
                text.append('{')
                schreibe(argumente.argumentOderPlatzhalter(0, operation.id, "radikand"), 0)
                text.append('}')
            }
            "zahl.betrag" -> {
                text.append("\\left|")
                schreibe(argumente.argumentOderPlatzhalter(0, operation.id, "argument"), 0)
                text.append("\\right|")
            }
            "zahl.minimum", "zahl.maximum" -> {
                text.append(if (operation.operatorId == "zahl.minimum") "\\min" else "\\max")
                text.append("\\left\\{")
                argumente.forEachIndexed { index, argument ->
                    if (index > 0) text.append(',')
                    schreibe(argument, 0)
                }
                text.append("\\right\\}")
            }
            "zahl.ln", "zahl.sin", "zahl.cos", "zahl.arcsin", "zahl.arccos", "zahl.exp",
            "zahl.tan", "zahl.cot", "zahl.sec", "zahl.csc", "zahl.arctan",
            "zahl.sinh", "zahl.cosh", "zahl.tanh", "zahl.coth", "zahl.sech", "zahl.csch",
            -> schreibeFunktion(operation.operatorId, argumente, operation.id, text, schreibe)
            "zahl.logarithmus" -> {
                text.append("\\log_{")
                schreibe(argumente.argumentOderPlatzhalter(0, operation.id, "basis"), 0)
                text.append("}\\left(")
                schreibe(argumente.argumentOderPlatzhalter(1, operation.id, "argument"), 0)
                text.append("\\right)")
            }
            else -> {
                text.append("\\operatorname{${operation.operatorId.atlasLatexText()}}\\left(")
                argumente.forEachIndexed { index, argument ->
                    if (index > 0) text.append(',')
                    schreibe(argument, 0)
                }
                text.append("\\right)")
            }
        }
    }

    private fun schreibeBruch(
        argumente: List<FormelAusdruck>,
        operationId: String,
        text: StringBuilder,
        schreibe: (FormelAusdruck, Int) -> Unit,
    ) {
        text.append("\\frac{")
        schreibe(argumente.argumentOderPlatzhalter(0, operationId, "zaehler"), 0)
        text.append("}{")
        schreibe(argumente.argumentOderPlatzhalter(1, operationId, "nenner"), 0)
        text.append('}')
    }

    private fun schreibeSeitigeDivision(
        argumente: List<FormelAusdruck>,
        seite: String,
        praezedenz: Int,
        text: StringBuilder,
        schreibe: (FormelAusdruck, Int) -> Unit,
    ) {
        schreibe(argumente.argumentOderPlatzhalter(0, "division", "dividend"), praezedenz)
        text.append(" \\div_$seite ")
        schreibe(argumente.argumentOderPlatzhalter(1, "division", "divisor"), praezedenz + 1)
    }

    private fun schreibePotenz(
        argumente: List<FormelAusdruck>,
        praezedenz: Int,
        text: StringBuilder,
        schreibe: (FormelAusdruck, Int) -> Unit,
    ) {
        schreibe(argumente.argumentOderPlatzhalter(0, "potenz", "basis"), praezedenz)
        text.append("^{")
        schreibe(argumente.argumentOderPlatzhalter(1, "potenz", "ordnung"), 0)
        text.append('}')
    }

    private fun schreibeDifferentiation(
        argumente: List<FormelAusdruck>,
        praezedenz: Int,
        text: StringBuilder,
        schreibe: (FormelAusdruck, Int) -> Unit,
    ) {
        val basis = argumente.argumentOderPlatzhalter(0, "differentiation", "methode")
        val ordnung = argumente.argumentOderPlatzhalter(1, "differentiation", "ordnung")
        val konkret = konkreteNichtnegativeOrdnung(ordnung)
        if (konkret == BigInteger.ZERO) {
            schreibe(basis, praezedenz)
            return
        }
        schreibe(basis, praezedenz)
        val roemisch = konkret?.let(::roemischeZahlOderNull)
        when {
            roemisch != null -> text.append("^{\\mathrm{$roemisch}}")
            konkret != null -> text.append("^{($konkret)}")
            else -> {
                text.append("^{(")
                schreibe(ordnung, 0)
                text.append(")}")
            }
        }
    }

    private fun konkreteNichtnegativeOrdnung(ausdruck: FormelAusdruck): BigInteger? {
        val literal = ausdruck as? FormelAusdruck.Literal ?: return null
        return literal.wert.zuLatex().toBigIntegerOrNull()?.takeIf { it.signum() >= 0 }
    }

    private fun roemischeZahlOderNull(wert: BigInteger): String? {
        if (wert < BigInteger.ONE || wert > BigInteger.valueOf(3999)) return null
        var rest = wert.toInt()
        val teile = listOf(
            1000 to "M", 900 to "CM", 500 to "D", 400 to "CD", 100 to "C", 90 to "XC",
            50 to "L", 40 to "XL", 10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I",
        )
        return buildString {
            teile.forEach { (zahl, zeichen) ->
                while (rest >= zahl) {
                    append(zeichen)
                    rest -= zahl
                }
            }
        }
    }

    private fun schreibeFunktion(
        operatorId: String,
        argumente: List<FormelAusdruck>,
        operationId: String,
        text: StringBuilder,
        schreibe: (FormelAusdruck, Int) -> Unit,
    ) {
        val name = operatorId.substringAfterLast('.')
        when (name) {
            "exp" -> text.append("\\exp")
            "sech", "csch" -> text.append("\\operatorname{$name}")
            else -> text.append("\\$name")
        }
        text.append("\\left(")
        schreibe(argumente.argumentOderPlatzhalter(0, operationId, "argument"), 0)
        text.append("\\right)")
    }

    private fun binaer(
        argumente: List<FormelAusdruck>,
        zeichen: String,
        praezedenz: Int,
        text: StringBuilder,
        schreibe: (FormelAusdruck, Int) -> Unit,
    ) {
        schreibe(argumente.argumentOderPlatzhalter(0, "binaer", "links"), praezedenz)
        text.append(zeichen)
        schreibe(argumente.argumentOderPlatzhalter(1, "binaer", "rechts"), praezedenz + 1)
    }

    private fun positiverFormelGegenterm(ausdruck: FormelAusdruck): FormelAusdruck? = when (ausdruck) {
        is FormelAusdruck.Literal -> (ausdruck.wert as? ZahlAusdruck)?.let(::positiverGegentermOderNull)?.let { ausdruck.copy(wert = it) }
        is FormelAusdruck.Operation -> {
            val args = ausdruck.argumente.sortedBy { it.position }
            when {
                ausdruck.operatorId == "zahl.subtraktion" && args.size == 2 &&
                    (args[0].ausdruck as? FormelAusdruck.Literal)?.wert == RationaleZahl.Null ->
                    args[1].ausdruck.takeIf { positiverFormelGegenterm(it) == null }
                ausdruck.operatorId == "formel.gruppierung" && args.size == 1 ->
                    positiverFormelGegenterm(args[0].ausdruck)?.let { ausdruck.copy(argumente = listOf(args[0].copy(ausdruck = it))) }
                ausdruck.operatorId == "zahl.multiplikation" -> {
                    val negativ = args.indexOfFirst { positiverFormelGegenterm(it.ausdruck) != null }
                    if (negativ < 0) null else ausdruck.copy(argumente = args.mapIndexed { i, a ->
                        if (i == negativ) a.copy(ausdruck = requireNotNull(positiverFormelGegenterm(a.ausdruck))) else a
                    })
                }
                else -> null
            }
        }
        else -> null
    }

    private fun praezedenz(operatorId: String): Int = when (operatorId) {
        "zahl.addition", "zahl.subtraktion" -> 10
        "zahl.multiplikation", "zahl.division", "algebra.division.rechts", "algebra.division.links" -> 20
        "zahl.potenz", "iteration.multiplikation", "iteration.differentiation", "iteration.selbstkomposition",
        "methode.umkehrfunktion",
        -> 30
        "methode.einschraenkung" -> 35
        "formel.gruppierung" -> 50
        else -> 40
    }

    private fun List<FormelAusdruck>.argumentOderPlatzhalter(
        index: Int,
        operationId: String,
        rolle: String,
    ): FormelAusdruck = getOrNull(index) ?: FormelAusdruck.Platzhalter(
        id = "$operationId-$rolle-fehlt",
        rollenId = rolle,
        beschriftung = rolle,
        typ = FormelTyp.OBJEKT,
    )
}