package de.TeutonStudio.MathematikRechenSystem.kern

/** Semantischer Ergebnistyp eines Ausdrucksknotens, unabhängig von UI-Handles. */
enum class FormelTyp {
    ZAHL,
    MENGE,
    AUSSAGE,
    TUPEL,
    VEKTOR,
    MATRIX,
    TENSOR,
    METHODE,
    OBJEKT,
}

/**
 * Gemeinsame Ausdrucksquelle für Formelansicht und Knotengraph. Sie ist kein
 * LaTeX-String und keine UI-Komponente.
 */
sealed interface FormelAusdruck {
    val id: String
    val typ: FormelTyp

    data class Literal(
        override val id: String,
        val wert: MathematischesObjekt,
        override val typ: FormelTyp,
    ) : FormelAusdruck

    data class Variable(
        override val id: String,
        val name: String,
        val latex: String = name,
        override val typ: FormelTyp,
    ) : FormelAusdruck {
        init { require(name.isNotBlank()) }
    }

    data class Platzhalter(
        override val id: String,
        val rollenId: String,
        val beschriftung: String,
        override val typ: FormelTyp,
    ) : FormelAusdruck

    data class Operation(
        override val id: String,
        val operatorId: String,
        val argumente: List<FormelArgument>,
        override val typ: FormelTyp,
        val bedingungen: List<Aussage> = emptyList(),
        val explizitGruppiert: Boolean = false,
    ) : FormelAusdruck {
        init {
            require(operatorId.isNotBlank())
            require(argumente.map { it.position }.distinct().size == argumente.size) {
                "Argumentpositionen einer Formeloperation müssen eindeutig sein."
            }
        }
    }
}

data class FormelArgument(
    val rollenId: String,
    val position: Int,
    val ausdruck: FormelAusdruck,
) {
    init {
        require(rollenId.isNotBlank())
        require(position >= 0)
    }
}

sealed interface FormelPruefung {
    data object Gueltig : FormelPruefung
    data class Unvollstaendig(val platzhalterIds: List<String>) : FormelPruefung
    data class Ungueltig(val gruende: List<String>) : FormelPruefung
}

object FormelAusdruckPruefer {
    fun pruefe(wurzel: FormelAusdruck): FormelPruefung {
        val aktiv = linkedSetOf<String>()
        val abgeschlossen = linkedSetOf<String>()
        val platzhalter = mutableListOf<String>()
        val gruende = mutableListOf<String>()
        val objekte = linkedMapOf<String, FormelAusdruck>()

        fun besuche(ausdruck: FormelAusdruck) {
            val vorhanden = objekte[ausdruck.id]
            if (vorhanden != null && vorhanden != ausdruck) {
                gruende += "Ausdrucks-ID ${ausdruck.id} bezeichnet mehrere verschiedene Ausdrücke."
                return
            }
            objekte[ausdruck.id] = ausdruck
            if (ausdruck.id in abgeschlossen) return
            if (!aktiv.add(ausdruck.id)) {
                gruende += "Unzulässiger Ausdruckszyklus bei ${ausdruck.id}."
                return
            }
            when (ausdruck) {
                is FormelAusdruck.Platzhalter -> platzhalter += ausdruck.id
                is FormelAusdruck.Operation -> {
                    if (ausdruck.argumente.isEmpty()) {
                        gruende += "Operation ${ausdruck.operatorId} besitzt keine Argumente."
                    }
                    ausdruck.argumente.sortedBy { it.position }.forEach { besuche(it.ausdruck) }
                }
                else -> Unit
            }
            aktiv.remove(ausdruck.id)
            abgeschlossen += ausdruck.id
        }

        besuche(wurzel)
        return when {
            gruende.isNotEmpty() -> FormelPruefung.Ungueltig(gruende.distinct())
            platzhalter.isNotEmpty() -> FormelPruefung.Unvollstaendig(platzhalter.distinct())
            else -> FormelPruefung.Gueltig
        }
    }
}

data class FormelRenderErgebnis(
    val latex: String,
    val ausdrucksBereiche: Map<String, IntRange>,
    val bedingungen: List<Aussage>,
)

/** Rendert Notation aus Semantik; LaTeX bleibt reine Projektion. */
object FormelRenderer {
    fun render(wurzel: FormelAusdruck): FormelRenderErgebnis {
        val bereiche = linkedMapOf<String, IntRange>()
        val bedingungen = linkedSetOf<Aussage>()
        val text = StringBuilder()

        fun schreibe(ausdruck: FormelAusdruck, elternPraezedenz: Int = 0) {
            val start = text.length
            when (ausdruck) {
                is FormelAusdruck.Literal -> text.append(ausdruck.wert.zuLatex())
                is FormelAusdruck.Variable -> text.append(ausdruck.latex)
                is FormelAusdruck.Platzhalter -> text.append("\\square_{${ausdruck.beschriftung.latexText()}}")
                is FormelAusdruck.Operation -> {
                    bedingungen += ausdruck.bedingungen
                    val praezedenz = praezedenz(ausdruck.operatorId)
                    val klammern = ausdruck.explizitGruppiert || praezedenz < elternPraezedenz
                    if (klammern) text.append("\\left(")
                    schreibeOperation(ausdruck, praezedenz, text, ::schreibe)
                    if (klammern) text.append("\\right)")
                }
            }
            bereiche[ausdruck.id] = start until text.length
        }

        schreibe(wurzel)
        return FormelRenderErgebnis(text.toString(), bereiche, bedingungen.toList())
    }

    private fun schreibeOperation(
        operation: FormelAusdruck.Operation,
        praezedenz: Int,
        text: StringBuilder,
        schreibe: (FormelAusdruck, Int) -> Unit,
    ) {
        val argumente = operation.argumente.sortedBy { it.position }.map { it.ausdruck }
        when (operation.operatorId.substringAfterLast('.')) {
            "addition" -> argumente.forEachIndexed { index, argument ->
                if (index > 0) text.append(" + ")
                schreibe(argument, praezedenz)
            }
            "subtraktion" -> binaer(argumente, " - ", praezedenz, text, schreibe)
            "multiplikation" -> argumente.forEachIndexed { index, argument ->
                if (index > 0) text.append(" \\cdot ")
                schreibe(argument, praezedenz)
            }
            "division" -> {
                text.append("\\frac{")
                schreibe(argumente.getOrPlatzhalter(0, operation.id, "zaehler"), 0)
                text.append("}{")
                schreibe(argumente.getOrPlatzhalter(1, operation.id, "nenner"), 0)
                text.append('}')
            }
            "potenz" -> {
                schreibe(argumente.getOrPlatzhalter(0, operation.id, "basis"), praezedenz)
                text.append("^{")
                schreibe(argumente.getOrPlatzhalter(1, operation.id, "exponent"), 0)
                text.append('}')
            }
            "wurzel" -> {
                text.append("\\sqrt{")
                schreibe(argumente.getOrPlatzhalter(0, operation.id, "radikand"), 0)
                text.append('}')
            }
            "betrag" -> {
                text.append("\\left|")
                schreibe(argumente.getOrPlatzhalter(0, operation.id, "argument"), 0)
                text.append("\\right|")
            }
            "minimum", "maximum" -> {
                text.append(if (operation.operatorId.endsWith("minimum")) "\\min" else "\\max")
                text.append("\\left\\{")
                argumente.forEachIndexed { index, argument ->
                    if (index > 0) text.append(',')
                    schreibe(argument, 0)
                }
                text.append("\\right\\}")
            }
            "ln", "sin", "cos", "arcsin", "arccos", "exp" -> {
                val name = operation.operatorId.substringAfterLast('.')
                if (name == "exp") text.append("\\exp") else text.append("\\$name")
                text.append("\\left(")
                schreibe(argumente.getOrPlatzhalter(0, operation.id, "argument"), 0)
                text.append("\\right)")
            }
            "logarithmus" -> {
                text.append("\\log_{")
                schreibe(argumente.getOrPlatzhalter(0, operation.id, "basis"), 0)
                text.append("}\\left(")
                schreibe(argumente.getOrPlatzhalter(1, operation.id, "argument"), 0)
                text.append("\\right)")
            }
            else -> {
                text.append("\\operatorname{${operation.operatorId.latexText()}}\\left(")
                argumente.forEachIndexed { index, argument ->
                    if (index > 0) text.append(',')
                    schreibe(argument, 0)
                }
                text.append("\\right)")
            }
        }
    }

    private fun binaer(
        argumente: List<FormelAusdruck>,
        zeichen: String,
        praezedenz: Int,
        text: StringBuilder,
        schreibe: (FormelAusdruck, Int) -> Unit,
    ) {
        schreibe(argumente.getOrPlatzhalter(0, "binaer", "links"), praezedenz)
        text.append(zeichen)
        schreibe(argumente.getOrPlatzhalter(1, "binaer", "rechts"), praezedenz + 1)
    }

    private fun praezedenz(operatorId: String): Int = when (operatorId.substringAfterLast('.')) {
        "addition", "subtraktion" -> 10
        "multiplikation", "division" -> 20
        "potenz" -> 30
        else -> 40
    }
}

data class FormelGraphKnoten(
    val id: String,
    val ausdrucksId: String,
    val art: String,
    val typ: FormelTyp,
    val parameter: Map<String, String> = emptyMap(),
)

data class FormelGraphKante(
    val id: String,
    val quelleKnotenId: String,
    val zielKnotenId: String,
    val rollenId: String,
    val position: Int,
)

data class FormelGraph(
    val wurzelKnotenId: String,
    val knoten: List<FormelGraphKnoten>,
    val kanten: List<FormelGraphKante>,
)

/** Ausdrucks-DAG zu neutralem Graphmodell; Layout bleibt bewusst außerhalb. */
object FormelZuGraph {
    fun konvertiere(wurzel: FormelAusdruck): FormelGraph {
        val knoten = linkedMapOf<String, FormelGraphKnoten>()
        val kanten = linkedMapOf<String, FormelGraphKante>()

        fun besuche(ausdruck: FormelAusdruck): String {
            val knotenId = "formel-${ausdruck.id}"
            if (knotenId in knoten) return knotenId
            val graphKnoten = when (ausdruck) {
                is FormelAusdruck.Literal -> FormelGraphKnoten(
                    knotenId,
                    ausdruck.id,
                    "formel.literal",
                    ausdruck.typ,
                    mapOf("latex" to ausdruck.wert.zuLatex()),
                )
                is FormelAusdruck.Variable -> FormelGraphKnoten(
                    knotenId,
                    ausdruck.id,
                    "formel.variable",
                    ausdruck.typ,
                    mapOf("name" to ausdruck.name, "latex" to ausdruck.latex),
                )
                is FormelAusdruck.Platzhalter -> FormelGraphKnoten(
                    knotenId,
                    ausdruck.id,
                    "formel.platzhalter",
                    ausdruck.typ,
                    mapOf("rolle" to ausdruck.rollenId, "beschriftung" to ausdruck.beschriftung),
                )
                is FormelAusdruck.Operation -> FormelGraphKnoten(
                    knotenId,
                    ausdruck.id,
                    ausdruck.operatorId,
                    ausdruck.typ,
                )
            }
            knoten[knotenId] = graphKnoten
            if (ausdruck is FormelAusdruck.Operation) {
                ausdruck.argumente.forEach { argument ->
                    val quelle = besuche(argument.ausdruck)
                    val kantenId = "formel-edge-${argument.ausdruck.id}-${ausdruck.id}-${argument.rollenId}-${argument.position}"
                    kanten[kantenId] = FormelGraphKante(
                        kantenId,
                        quelle,
                        knotenId,
                        argument.rollenId,
                        argument.position,
                    )
                }
            }
            return knotenId
        }

        val wurzelKnotenId = besuche(wurzel)
        return FormelGraph(wurzelKnotenId, knoten.values.toList(), kanten.values.toList())
    }
}

sealed interface GraphZuFormelErgebnis {
    data class Erfolg(val wurzel: FormelAusdruck) : GraphZuFormelErgebnis
    data class Fehler(val gruende: List<String>) : GraphZuFormelErgebnis
}

object GraphZuFormel {
    fun konvertiere(
        graph: FormelGraph,
        literale: Map<String, MathematischesObjekt>,
    ): GraphZuFormelErgebnis {
        val knoten = graph.knoten.associateBy { it.id }
        val eingehend = graph.kanten.groupBy { it.zielKnotenId }
        val aktiv = mutableSetOf<String>()
        val cache = mutableMapOf<String, FormelAusdruck>()
        val fehler = mutableListOf<String>()

        fun baue(knotenId: String): FormelAusdruck? {
            cache[knotenId]?.let { return it }
            if (!aktiv.add(knotenId)) {
                fehler += "Unzulässiger Zyklus bei Graphknoten $knotenId."
                return null
            }
            val graphKnoten = knoten[knotenId]
            if (graphKnoten == null) {
                fehler += "Graphknoten $knotenId fehlt."
                aktiv.remove(knotenId)
                return null
            }
            val ausdruck = when (graphKnoten.art) {
                "formel.literal" -> literale[graphKnoten.ausdrucksId]?.let {
                    FormelAusdruck.Literal(graphKnoten.ausdrucksId, it, graphKnoten.typ)
                } ?: run {
                    fehler += "Literal ${graphKnoten.ausdrucksId} fehlt im Literalregister."
                    null
                }
                "formel.variable" -> FormelAusdruck.Variable(
                    graphKnoten.ausdrucksId,
                    graphKnoten.parameter["name"].orEmpty().ifBlank { "x" },
                    graphKnoten.parameter["latex"].orEmpty().ifBlank { graphKnoten.parameter["name"].orEmpty().ifBlank { "x" } },
                    graphKnoten.typ,
                )
                "formel.platzhalter" -> FormelAusdruck.Platzhalter(
                    graphKnoten.ausdrucksId,
                    graphKnoten.parameter["rolle"].orEmpty().ifBlank { "argument" },
                    graphKnoten.parameter["beschriftung"].orEmpty().ifBlank { "?" },
                    graphKnoten.typ,
                )
                else -> {
                    val argumente = eingehend[knotenId].orEmpty().sortedBy { it.position }.mapNotNull { kante ->
                        baue(kante.quelleKnotenId)?.let { FormelArgument(kante.rollenId, kante.position, it) }
                    }
                    FormelAusdruck.Operation(
                        graphKnoten.ausdrucksId,
                        graphKnoten.art,
                        argumente,
                        graphKnoten.typ,
                    )
                }
            }
            aktiv.remove(knotenId)
            if (ausdruck != null) cache[knotenId] = ausdruck
            return ausdruck
        }

        val wurzel = baue(graph.wurzelKnotenId)
        return if (wurzel == null || fehler.isNotEmpty()) GraphZuFormelErgebnis.Fehler(fehler.distinct())
        else GraphZuFormelErgebnis.Erfolg(wurzel)
    }
}

data class FormelGraphDiff(
    val knotenBehalten: Set<String>,
    val knotenErzeugen: List<FormelGraphKnoten>,
    val knotenEntfernen: Set<String>,
    val kantenBehalten: Set<String>,
    val kantenErzeugen: List<FormelGraphKante>,
    val kantenEntfernen: Set<String>,
)

object FormelGraphDifferenz {
    fun plane(alt: FormelGraph, neu: FormelGraph): FormelGraphDiff {
        val alteKnoten = alt.knoten.associateBy { it.ausdrucksId }
        val neueKnoten = neu.knoten.associateBy { it.ausdrucksId }
        val alteKanten = alt.kanten.associateBy { it.id }
        val neueKanten = neu.kanten.associateBy { it.id }
        return FormelGraphDiff(
            knotenBehalten = alteKnoten.keys.intersect(neueKnoten.keys),
            knotenErzeugen = neu.knoten.filter { it.ausdrucksId !in alteKnoten },
            knotenEntfernen = alteKnoten.keys - neueKnoten.keys,
            kantenBehalten = alteKanten.keys.intersect(neueKanten.keys),
            kantenErzeugen = neu.kanten.filter { it.id !in alteKanten },
            kantenEntfernen = alteKanten.keys - neueKanten.keys,
        )
    }
}

data class FormelAuswahlKorrespondenz(
    val ausdruckZuKnoten: Map<String, String>,
    val knotenZuAusdruck: Map<String, String>,
) {
    companion object {
        fun aus(graph: FormelGraph): FormelAuswahlKorrespondenz {
            val vorwaerts = graph.knoten.associate { it.ausdrucksId to it.id }
            return FormelAuswahlKorrespondenz(vorwaerts, vorwaerts.entries.associate { (a, k) -> k to a })
        }
    }
}

private fun List<FormelAusdruck>.getOrPlatzhalter(
    index: Int,
    operationId: String,
    rolle: String,
): FormelAusdruck = getOrNull(index) ?: FormelAusdruck.Platzhalter(
    id = "$operationId-$rolle-fehlt",
    rollenId = rolle,
    beschriftung = rolle,
    typ = FormelTyp.OBJEKT,
)

private fun String.latexText(): String = replace("\\", "").replace("_", "\\_").replace(" ", "\\ ")
