package de.TeutonStudio.MathematikRechenSystem.kern

enum class IntegralAusgabeform {
    METHODE_VOLLSTAENDIG,
    METHODE_KURZ,
    TERM,
}

enum class IntegralArt {
    RIEMANN,
}

data class IntegralBereich(
    val komponenten: List<MengenAusdruck>,
) : MathematischesObjekt {
    init { require(komponenten.isNotEmpty()) { "Ein Integral benötigt mindestens einen Integrationsbereich." } }

    val dimension: Int get() = komponenten.size

    override fun zuLatex(): String = when (komponenten.size) {
        1 -> komponenten.single().zuLatex()
        else -> komponenten.joinToString(
            prefix = "\\left(",
            separator = "\\times",
            postfix = "\\right)",
        ) { it.zuLatex() }
    }
}

data class IntegralBindung(
    val variable: Variable,
    val quellenId: String = variable.name,
) {
    init { require(quellenId.isNotBlank()) { "Eine Integralbindung benötigt eine stabile Quellen-ID." } }
}

sealed interface IntegralIntegrand : MathematischesObjekt {
    data class MethodenIntegrand(val methode: Methode) : IntegralIntegrand {
        override fun zuLatex(): String = methode.name
    }

    data class TermIntegrand(val term: MathematischesObjekt) : IntegralIntegrand {
        override fun zuLatex(): String = term.zuLatex()
    }
}

sealed interface IntegralVolumenElement : MathematischesObjekt {
    val quellenIds: List<String>

    data class MethodenDifferential(
        val bereich: IntegralBereich,
    ) : IntegralVolumenElement {
        override val quellenIds: List<String> = emptyList()
        override fun zuLatex(): String =
            "d\\left(\\operatorname{id}\\vert_{${bereich.zuLatex()}}\\right)"
    }

    data class GebundeneDifferentiale(
        val bindungen: List<IntegralBindung>,
    ) : IntegralVolumenElement {
        init { require(bindungen.isNotEmpty()) }
        override val quellenIds: List<String> = bindungen.map(IntegralBindung::quellenId)
        override fun zuLatex(): String = bindungen.joinToString("\\cdot") {
            "d${it.variable.zuLatex()}"
        }
    }
}

data class RiemannIntegralVertrag(
    val bereich: IntegralBereich,
    val beschraenkt: Boolean?,
    val kartesischesProduktVonIntervallen: Boolean?,
    val symbolischZulaessig: Boolean = true,
) {
    val ersteUmsetzungUnterstuetzt: Boolean
        get() = beschraenkt == true && kartesischesProduktVonIntervallen == true

    val voraussetzungen: Set<String>
        get() = buildSet {
            if (beschraenkt != true) add("Der Integrationsbereich muss für die erste Riemann-Umsetzung beschränkt sein.")
            if (kartesischesProduktVonIntervallen != true) {
                add("Der Integrationsbereich muss als kartesisches Produkt von Intervallen nachgewiesen sein.")
            }
        }
}

data class StrukturiertesIntegral(
    val integrand: IntegralIntegrand,
    val bereich: IntegralBereich,
    val ausgabeform: IntegralAusgabeform,
    val bindungen: List<IntegralBindung> = emptyList(),
    val art: IntegralArt = IntegralArt.RIEMANN,
    val vertrag: RiemannIntegralVertrag = RiemannIntegralVertrag(
        bereich = bereich,
        beschraenkt = null,
        kartesischesProduktVonIntervallen = null,
    ),
) : MathematischesObjekt {
    val operatorId: String = "analysis.integral"

    init {
        require(vertrag.bereich == bereich)
        when (ausgabeform) {
            IntegralAusgabeform.METHODE_VOLLSTAENDIG,
            IntegralAusgabeform.METHODE_KURZ,
            -> {
                require(integrand is IntegralIntegrand.MethodenIntegrand) {
                    "Eine Methodenform benötigt einen Methodenintegranden."
                }
                require(bindungen.isEmpty()) { "Die Methodenform bindet keine ausgeschriebenen Termvariablen." }
            }
            IntegralAusgabeform.TERM -> {
                require(integrand is IntegralIntegrand.TermIntegrand) {
                    "Die Termform benötigt einen Termintegranden."
                }
                require(bindungen.size == bereich.dimension) {
                    "Jede Bereichskomponente benötigt genau eine gebundene Variable."
                }
                require(bindungen.map(IntegralBindung::quellenId).distinct().size == bindungen.size) {
                    "Integralbindungen benötigen eindeutige Quellen-IDs."
                }
            }
        }
    }

    val volumenElement: IntegralVolumenElement
        get() = when (ausgabeform) {
            IntegralAusgabeform.METHODE_VOLLSTAENDIG -> IntegralVolumenElement.MethodenDifferential(bereich)
            IntegralAusgabeform.METHODE_KURZ -> IntegralVolumenElement.MethodenDifferential(bereich)
            IntegralAusgabeform.TERM -> IntegralVolumenElement.GebundeneDifferentiale(bindungen)
        }

    override fun zuLatex(): String = when (ausgabeform) {
        IntegralAusgabeform.METHODE_VOLLSTAENDIG ->
            "\\int_{${bereich.zuLatex()}}${integrand.zuLatex()}\\cdot${volumenElement.zuLatex()}"
        IntegralAusgabeform.METHODE_KURZ ->
            "\\int_{${bereich.zuLatex()}}${integrand.zuLatex()}"
        IntegralAusgabeform.TERM ->
            "\\int_{${bindungsLatex()}\\in${bereich.zuLatex()}}" +
                "${integrandAlsProduktFaktor()}\\cdot${volumenElement.zuLatex()}"
    }

    private fun bindungsLatex(): String = when (bindungen.size) {
        1 -> bindungen.single().variable.zuLatex()
        else -> bindungen.joinToString(prefix = "\\left(", separator = ",", postfix = "\\right)") {
            it.variable.zuLatex()
        }
    }

    private fun integrandAlsProduktFaktor(): String {
        val term = (integrand as? IntegralIntegrand.TermIntegrand)?.term
        return when (term) {
            is Addition -> "\\left(${term.zuLatex()}\\right)"
            else -> integrand.zuLatex()
        }
    }
}

fun methodenIntegral(
    methode: Methode,
    bereich: IntegralBereich,
    kurz: Boolean = false,
    vertrag: RiemannIntegralVertrag = RiemannIntegralVertrag(bereich, null, null),
): StrukturiertesIntegral = StrukturiertesIntegral(
    integrand = IntegralIntegrand.MethodenIntegrand(methode),
    bereich = bereich,
    ausgabeform = if (kurz) IntegralAusgabeform.METHODE_KURZ else IntegralAusgabeform.METHODE_VOLLSTAENDIG,
    vertrag = vertrag,
)

fun termIntegral(
    term: MathematischesObjekt,
    bereiche: List<MengenAusdruck>,
    bindungen: List<IntegralBindung>,
    vertrag: RiemannIntegralVertrag = RiemannIntegralVertrag(IntegralBereich(bereiche), null, null),
): StrukturiertesIntegral {
    val bereich = IntegralBereich(bereiche)
    require(vertrag.bereich == bereich)
    return StrukturiertesIntegral(
        integrand = IntegralIntegrand.TermIntegrand(term),
        bereich = bereich,
        ausgabeform = IntegralAusgabeform.TERM,
        bindungen = bindungen,
        vertrag = vertrag,
    )
}
