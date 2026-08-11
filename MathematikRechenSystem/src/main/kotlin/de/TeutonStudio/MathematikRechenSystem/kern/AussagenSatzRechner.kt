package de.TeutonStudio.MathematikRechenSystem.kern

enum class LogischerTyp(val latex: String) {
    OBJEKT("\\mathsf{Objekt}"),
    MENGE("\\mathsf{Menge}"),
    ZAHL("\\mathsf{Zahl}"),
    METHODE("\\mathsf{Methode}"),
    PRAEDIKAT("\\mathsf{Prädikat}"),
}

sealed interface QuantorBereich : MathematischesObjekt {
    data class Menge(val menge: MengenAusdruck) : QuantorBereich {
        override fun zuLatex(): String = menge.zuLatex()
    }

    data class Typ(val typ: LogischerTyp) : QuantorBereich {
        override fun zuLatex(): String = typ.latex
    }
}

data class LogischeVariable(
    val id: String,
    val name: String,
    val quantorBereich: QuantorBereich,
) : MathematischesObjekt {
    constructor(id: String, name: String, bereich: MengenAusdruck) :
        this(id, name, QuantorBereich.Menge(bereich))

    init {
        require(id.isNotBlank())
        require(name.isNotBlank())
    }

    /** Quellkompatibler Mengenbereich; Typquantoren besitzen absichtlich keine Universalmenge. */
    val bereich: MengenAusdruck?
        get() = (quantorBereich as? QuantorBereich.Menge)?.menge

    override fun zuLatex(): String = name
}

sealed interface LogischerAusdruck : MathematischesObjekt {
    val freieVariablen: Set<LogischeVariable>
    val gebundeneVariablen: Set<LogischeVariable>
    fun alsAussage(): Aussage
}

data class LogischesAtom(
    val aussage: Aussage,
    override val freieVariablen: Set<LogischeVariable> = emptySet(),
) : LogischerAusdruck {
    override val gebundeneVariablen: Set<LogischeVariable> = emptySet()
    override fun alsAussage(): Aussage = aussage
    override fun zuLatex(): String = aussage.zuLatex()
}

enum class AussagenSatzOperator(val stabileId: String) {
    NEGATION("aussage.negation"),
    KONJUNKTION("aussage.konjunktion"),
    DISJUNKTION("aussage.disjunktion"),
    ADJUNKTION("aussage.adjunktion"),
    IMPLIKATION("aussage.implikation"),
    AEQUIVALENZ("aussage.aequivalenz"),
    ALLQUANTOR("aussage.allquantor"),
    EXISTENZQUANTOR("aussage.existenzquantor"),
    EINDEUTIGER_EXISTENZQUANTOR("aussage.eindeutigerExistenzquantor"),
}

private val quantoren = setOf(
    AussagenSatzOperator.ALLQUANTOR,
    AussagenSatzOperator.EXISTENZQUANTOR,
    AussagenSatzOperator.EINDEUTIGER_EXISTENZQUANTOR,
)

data class LogischeVerknuepfung(
    val operator: AussagenSatzOperator,
    val argumente: List<LogischerAusdruck>,
) : LogischerAusdruck {
    init {
        require(operator !in quantoren)
        when (operator) {
            AussagenSatzOperator.NEGATION -> require(argumente.size == 1)
            AussagenSatzOperator.IMPLIKATION,
            AussagenSatzOperator.AEQUIVALENZ,
            -> require(argumente.size == 2)
            AussagenSatzOperator.KONJUNKTION,
            AussagenSatzOperator.DISJUNKTION,
            AussagenSatzOperator.ADJUNKTION,
            -> require(argumente.size >= 2)
            else -> Unit
        }
    }

    override val freieVariablen: Set<LogischeVariable> =
        argumente.flatMapTo(linkedSetOf()) { it.freieVariablen }
    override val gebundeneVariablen: Set<LogischeVariable> =
        argumente.flatMapTo(linkedSetOf()) { it.gebundeneVariablen }

    override fun alsAussage(): Aussage {
        val aussagen = argumente.map(LogischerAusdruck::alsAussage)
        return when (operator) {
            AussagenSatzOperator.NEGATION -> Negation(aussagen.single())
            AussagenSatzOperator.KONJUNKTION -> Konjunktion(aussagen)
            AussagenSatzOperator.DISJUNKTION -> Disjunktion(aussagen)
            AussagenSatzOperator.ADJUNKTION -> adjunktion(aussagen)
            AussagenSatzOperator.IMPLIKATION -> Implikation(aussagen[0], aussagen[1])
            AussagenSatzOperator.AEQUIVALENZ -> Äquivalenz(aussagen[0], aussagen[1])
            else -> error("Quantoren benötigen einen Bindungskontext.")
        }
    }

    override fun zuLatex(): String = alsAussage().zuLatex()
}

data class QuantifizierterAusdruck(
    val operator: AussagenSatzOperator,
    val variable: LogischeVariable,
    val rumpf: LogischerAusdruck,
) : LogischerAusdruck {
    init { require(operator in quantoren) }

    override val freieVariablen: Set<LogischeVariable> =
        rumpf.freieVariablen.filterTo(linkedSetOf()) { it.id != variable.id }
    override val gebundeneVariablen: Set<LogischeVariable> = rumpf.gebundeneVariablen + variable

    override fun alsAussage(): Aussage = QuantifizierteAussage(operator, variable, rumpf.alsAussage())
    override fun zuLatex(): String = alsAussage().zuLatex()
}

data class QuantifizierteAussage(
    val operator: AussagenSatzOperator,
    val variable: LogischeVariable,
    val rumpf: Aussage,
) : Aussage {
    init { require(operator in quantoren) }

    override fun entscheide(kontext: RechenKontext): AussageErgebnis {
        val mengenBereich = (variable.quantorBereich as? QuantorBereich.Menge)?.menge
        val elemente = (mengenBereich as? EndlicheMenge)?.elemente
            ?: return AussageErgebnis(
                null,
                EntscheidungsStatus.Unbekannt,
                if (variable.quantorBereich is QuantorBereich.Typ) {
                    "Typquantoren bleiben symbolisch; es wird keine Universalmenge konstruiert."
                } else {
                    "Quantoren über unendlichen oder symbolischen Bereichen bleiben symbolisch."
                },
            )
        val ergebnisse = elemente.map { element ->
            val eingesetzt = ersetze(rumpf, mapOf(variable.name to element)) as? Aussage
                ?: return AussageErgebnis(null, EntscheidungsStatus.NichtAuswertbar, "Quantorenrumpf ist keine Aussage.")
            eingesetzt.entscheide(kontext)
        }
        if (ergebnisse.any { it.wahrheitswert == null }) {
            return AussageErgebnis(null, EntscheidungsStatus.Unbekannt, "Mindestens eine Quantoreninstanz ist unbekannt.")
        }
        val wahrAnzahl = ergebnisse.count { it.wahrheitswert == Wahrheitswert.Wahr }
        val wahr = when (operator) {
            AussagenSatzOperator.ALLQUANTOR -> wahrAnzahl == ergebnisse.size
            AussagenSatzOperator.EXISTENZQUANTOR -> wahrAnzahl > 0
            AussagenSatzOperator.EINDEUTIGER_EXISTENZQUANTOR -> wahrAnzahl == 1
            else -> error("Kein Quantor: $operator")
        }
        return AussageErgebnis(
            if (wahr) Wahrheitswert.Wahr else Wahrheitswert.Lüge,
            if (wahr) EntscheidungsStatus.Bewiesen else EntscheidungsStatus.Widerlegt,
        )
    }

    override fun zuLatex(): String {
        val symbol = when (operator) {
            AussagenSatzOperator.ALLQUANTOR -> "\\forall"
            AussagenSatzOperator.EXISTENZQUANTOR -> "\\exists"
            AussagenSatzOperator.EINDEUTIGER_EXISTENZQUANTOR -> "\\exists!"
            else -> error("Kein Quantor: $operator")
        }
        val bindung = when (val bereich = variable.quantorBereich) {
            is QuantorBereich.Menge -> "${variable.name}\\in${bereich.menge.zuLatex()}"
            is QuantorBereich.Typ -> "${variable.name}:${bereich.typ.latex}"
        }
        return "$symbol $bindung:\\;${rumpf.zuLatex()}"
    }
}

data class PraedikatAusdruck(
    val name: String?,
    val parameter: List<LogischeVariable>,
    val formel: LogischerAusdruck,
) : MathematischesObjekt {
    init {
        require(parameter.map { it.id }.distinct().size == parameter.size)
        require(parameter.map { it.name }.distinct().size == parameter.size)
    }
    override fun zuLatex(): String =
        name?.takeIf(String::isNotBlank)?.let { "$it:${formel.zuLatex()}" } ?: formel.zuLatex()
}

sealed interface AussagenSatzErgebnis {
    val formel: LogischerAusdruck

    data class AussageWert(
        override val formel: LogischerAusdruck,
        val aussage: Aussage,
        val ergebnis: AussageErgebnis,
    ) : AussagenSatzErgebnis

    data class PraedikatWert(
        override val formel: LogischerAusdruck,
        val praedikat: PraedikatAusdruck,
    ) : AussagenSatzErgebnis

    data class Ungueltig(
        val code: String,
        val nachricht: String,
        override val formel: LogischerAusdruck =
            LogischesAtom(UnentscheidbareAussage("ungültig", "Aussagensatz")),
    ) : AussagenSatzErgebnis
}

object AussagenSatzRechner {
    const val KNOTEN_ART = "mathematik.aussagensatz"

    fun erzeuge(
        operator: AussagenSatzOperator,
        argumente: List<LogischerAusdruck>,
        variable: LogischeVariable? = null,
        praedikatName: String? = null,
        kontext: RechenKontext = RechenKontext(),
    ): AussagenSatzErgebnis {
        val formel = runCatching {
            if (operator in quantoren) {
                QuantifizierterAusdruck(operator, requireNotNull(variable), argumente.single())
            } else {
                LogischeVerknuepfung(operator, argumente)
            }
        }.getOrElse {
            return AussagenSatzErgebnis.Ungueltig(
                "argumentvertrag",
                it.message ?: "Die logische Operation ist unvollständig.",
            )
        }
        if (formel.freieVariablen.isNotEmpty()) {
            return AussagenSatzErgebnis.PraedikatWert(
                formel,
                PraedikatAusdruck(praedikatName, formel.freieVariablen.sortedBy { it.id }, formel),
            )
        }
        val aussage = formel.alsAussage()
        return AussagenSatzErgebnis.AussageWert(formel, aussage, aussage.entscheide(kontext))
    }

    fun alsFormelAusdruck(
        id: String,
        operator: AussagenSatzOperator,
        argumente: List<Pair<String, FormelAusdruck>>,
    ): FormelAusdruck.Operation = FormelAusdruck.Operation(
        id,
        operator.stabileId,
        argumente.mapIndexed { index, (rolle, ausdruck) -> FormelArgument(rolle, index, ausdruck) },
        FormelTyp.AUSSAGE,
    )
}
