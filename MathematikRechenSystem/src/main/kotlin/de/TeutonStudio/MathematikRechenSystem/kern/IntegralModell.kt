package de.TeutonStudio.MathematikRechenSystem.kern

enum class IntegralAusgabeform {
    METHODE,
    TERM,
}

enum class IntegralMethodenDarstellung {
    VOLLSTAENDIG,
    KURZ,
}

enum class IntegralArt {
    RIEMANN,
    DISKRET,
    SYMBOLISCH,
    NICHTSTANDARD,
}

data class IntegralBereich(
    val komponenten: List<MengenAusdruck>,
) : Ausdruck {
    init { require(komponenten.isNotEmpty()) { "Ein Integral benötigt mindestens einen Integrationsbereich." } }

    val dimension: Int get() = komponenten.size
    val alsMenge: MengenAusdruck get() = when (komponenten.size) {
        1 -> komponenten.single()
        else -> KartesischesProdukt(komponenten)
    }

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

sealed interface IntegralIntegrand : Ausdruck {
    data class MethodenIntegrand(val methode: Methode) : IntegralIntegrand {
        override fun zuLatex(): String = methode.name
    }

    data class TermIntegrand(val term: MathematischesObjekt) : IntegralIntegrand {
        override fun zuLatex(): String = term.zuLatex()
    }
}

sealed interface IntegralMass : Ausdruck {
    val operatorId: String

    data object StandardReell : IntegralMass {
        override val operatorId: String = "analysis.mass.standardReell"
        override fun zuLatex(): String = "\\lambda"
    }

    data object Zaehlmass : IntegralMass {
        override val operatorId: String = "analysis.mass.zaehlen"
        override fun zuLatex(): String = "\\#"
    }

    data class Allgemein(
        val symbol: String = "\\mu",
        override val operatorId: String = "analysis.mass.allgemein",
    ) : IntegralMass {
        init { require(symbol.isNotBlank()) }
        override fun zuLatex(): String = symbol
    }

    data class Gewichtet(
        val basis: IntegralMass,
        val gewicht: MathematischesObjekt,
    ) : IntegralMass {
        override val operatorId: String = "analysis.mass.gewichtet"
        override fun zuLatex(): String = "${gewicht.zuLatex()}\\cdot${basis.zuLatex()}"
    }

    data class NichtstandardZellgewicht(
        val symbol: String = "\\Delta x",
    ) : IntegralMass {
        override val operatorId: String = "analysis.mass.hyperzelle"
        override fun zuLatex(): String = symbol
    }
}

sealed interface IntegralVolumenElement : Ausdruck {
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

    data class MassDifferential(
        val mass: IntegralMass,
        override val quellenIds: List<String>,
    ) : IntegralVolumenElement {
        override fun zuLatex(): String = "d${mass.zuLatex()}"
    }

    data class ZellGewicht(
        val mass: IntegralMass.NichtstandardZellgewicht,
        override val quellenIds: List<String>,
    ) : IntegralVolumenElement {
        override fun zuLatex(): String = mass.zuLatex()
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

    val voraussetzungen: Set<Aussage>
        get() = buildSet {
            if (beschraenkt != true) add(
                UnentscheidbareAussage(
                    "beschränkt(${bereich.zuLatex()})",
                    "Riemann-Integralvertrag",
                ),
            )
            if (kartesischesProduktVonIntervallen != true) add(
                UnentscheidbareAussage(
                    "Intervallprodukt(${bereich.zuLatex()})",
                    "Riemann-Integralvertrag",
                ),
            )
        }
}

data class StrukturiertesIntegral(
    val integrand: IntegralIntegrand,
    val bereich: IntegralBereich,
    val ausgabeform: IntegralAusgabeform,
    val mass: IntegralMass,
    val methodenDarstellung: IntegralMethodenDarstellung = IntegralMethodenDarstellung.KURZ,
    val bindungen: List<IntegralBindung> = emptyList(),
    val art: IntegralArt = IntegralArt.SYMBOLISCH,
    val vertrag: RiemannIntegralVertrag? = null,
    val voraussetzungen: Set<Aussage> = emptySet(),
) : Ausdruck {
    val operatorId: String = "analysis.integral"

    init {
        require(vertrag == null || vertrag.bereich == bereich)
        when (ausgabeform) {
            IntegralAusgabeform.METHODE -> {
                require(integrand is IntegralIntegrand.MethodenIntegrand) {
                    "Die Methodenform benötigt einen Methodenintegranden."
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

    val freieVariablen: Set<Variable>
        get() = when (val wert = integrand) {
            is IntegralIntegrand.MethodenIntegrand -> wert.methode.enthalteneVariablen()
            is IntegralIntegrand.TermIntegrand -> wert.term.enthalteneVariablen()
        }.filterNot { variable -> bindungen.any { it.variable.name == variable.name } }.toSet()

    val volumenElement: IntegralVolumenElement
        get() = when (mass) {
            IntegralMass.StandardReell -> when (ausgabeform) {
                IntegralAusgabeform.METHODE -> IntegralVolumenElement.MethodenDifferential(bereich)
                IntegralAusgabeform.TERM -> IntegralVolumenElement.GebundeneDifferentiale(bindungen)
            }
            IntegralMass.Zaehlmass,
            is IntegralMass.Allgemein,
            is IntegralMass.Gewichtet,
            -> IntegralVolumenElement.MassDifferential(mass, bindungen.map(IntegralBindung::quellenId))
            is IntegralMass.NichtstandardZellgewicht -> IntegralVolumenElement.ZellGewicht(
                mass,
                bindungen.map(IntegralBindung::quellenId),
            )
        }

    override fun zuLatex(): String = when (ausgabeform) {
        IntegralAusgabeform.METHODE -> {
            val basis = "\\int_{${bereich.zuLatex()}}${integrand.zuLatex()}"
            if (methodenDarstellung == IntegralMethodenDarstellung.KURZ) basis
            else "$basis\\cdot${volumenElement.zuLatex()}"
        }
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

enum class IntegralUnterstuetzungsStatus {
    EXAKT,
    SYMBOLISCH,
    BEDINGT,
    NICHT_EXISTENT,
    NOCH_NICHT_IMPLEMENTIERT,
}

data class IntegralAuswertungsErgebnis(
    val wert: MathematischesObjekt,
    val status: IntegralUnterstuetzungsStatus,
    val voraussetzungen: Set<Aussage> = emptySet(),
    val regel: String,
    val schritte: List<UmformungsSchritt> = emptyList(),
)

data class NichtstandardIntegralDarstellung(
    val integral: StrukturiertesIntegral,
    val hyperIndex: ZahlAusdruck = Variable("H"),
    val voraussetzungen: Set<Aussage>,
) : Ausdruck {
    override fun zuLatex(): String =
        "\\operatorname{st}\\left(\\sum_{i=1}^{${hyperIndex.zuLatex()}}" +
            "{}^*f(\\xi_i)\\cdot\\Delta x_i\\right)"
}

fun leiteIntegralMassOderNull(bereich: IntegralBereich): IntegralMass? = when {
    bereich.komponenten.all { it is ReellesIntervall } -> IntegralMass.StandardReell
    bereich.komponenten.all { it is EndlicheMenge } -> IntegralMass.Zaehlmass
    else -> null
}

fun standardRiemannVertrag(bereich: IntegralBereich): RiemannIntegralVertrag =
    RiemannIntegralVertrag(
        bereich = bereich,
        beschraenkt = bereich.komponenten.all { it is ReellesIntervall },
        kartesischesProduktVonIntervallen = bereich.komponenten.all { it is ReellesIntervall },
    )

fun methodenIntegral(
    methode: Methode,
    bereich: IntegralBereich,
    kurz: Boolean = true,
    mass: IntegralMass = requireNotNull(leiteIntegralMassOderNull(bereich)) {
        "Für diesen Bereich kann kein Maß eindeutig abgeleitet werden."
    },
    vertrag: RiemannIntegralVertrag? = if (mass == IntegralMass.StandardReell) {
        standardRiemannVertrag(bereich)
    } else {
        null
    },
): StrukturiertesIntegral {
    require(methode.parameter.size == bereich.dimension) {
        "Methodenstelligkeit und Dimension des Integrationsbereichs müssen übereinstimmen."
    }
    val kompatibilitaet = pruefeMethodenBereich(methode, bereich)
    require(kompatibilitaet != BereichsKompatibilitaet.Unvereinbar) {
        "Methodendomäne und Integrationsbereich sind nicht kompatibel."
    }
    return StrukturiertesIntegral(
        integrand = IntegralIntegrand.MethodenIntegrand(methode),
        bereich = bereich,
        ausgabeform = IntegralAusgabeform.METHODE,
        mass = mass,
        methodenDarstellung = if (kurz) {
            IntegralMethodenDarstellung.KURZ
        } else {
            IntegralMethodenDarstellung.VOLLSTAENDIG
        },
        art = when (mass) {
            IntegralMass.StandardReell -> IntegralArt.RIEMANN
            IntegralMass.Zaehlmass -> IntegralArt.DISKRET
            is IntegralMass.NichtstandardZellgewicht -> IntegralArt.NICHTSTANDARD
            else -> IntegralArt.SYMBOLISCH
        },
        vertrag = vertrag,
        voraussetzungen = if (kompatibilitaet == BereichsKompatibilitaet.Unentscheidbar) {
            setOf(
                UnentscheidbareAussage(
                    "${bereich.zuLatex()}\\subseteq\\operatorname{Dom}(${methode.name})",
                    "Integralbereich",
                ),
            )
        } else {
            emptySet()
        },
    )
}

fun termIntegral(
    term: MathematischesObjekt,
    bereiche: List<MengenAusdruck>,
    bindungen: List<IntegralBindung>,
    mass: IntegralMass = requireNotNull(leiteIntegralMassOderNull(IntegralBereich(bereiche))) {
        "Für diesen Bereich kann kein Maß eindeutig abgeleitet werden."
    },
    vertrag: RiemannIntegralVertrag? = if (mass == IntegralMass.StandardReell) {
        standardRiemannVertrag(IntegralBereich(bereiche))
    } else {
        null
    },
): StrukturiertesIntegral {
    val bereich = IntegralBereich(bereiche)
    require(vertrag == null || vertrag.bereich == bereich)
    return StrukturiertesIntegral(
        integrand = IntegralIntegrand.TermIntegrand(term),
        bereich = bereich,
        ausgabeform = IntegralAusgabeform.TERM,
        mass = mass,
        bindungen = bindungen,
        art = when (mass) {
            IntegralMass.StandardReell -> IntegralArt.RIEMANN
            IntegralMass.Zaehlmass -> IntegralArt.DISKRET
            is IntegralMass.NichtstandardZellgewicht -> IntegralArt.NICHTSTANDARD
            else -> IntegralArt.SYMBOLISCH
        },
        vertrag = vertrag,
    )
}

fun werteIntegralAus(integral: StrukturiertesIntegral): IntegralAuswertungsErgebnis = when {
    integral.mass == IntegralMass.Zaehlmass -> werteDiskretesIntegralAus(integral)
    integral.mass == IntegralMass.StandardReell && integral.bereich.dimension == 1 ->
        werteEindimensionalesRiemannIntegralAus(integral)
    integral.mass is IntegralMass.NichtstandardZellgewicht -> {
        val voraussetzungen = integral.voraussetzungen + setOf(
            UnentscheidbareAussage("hyperendliche Partition", "Nichtstandardintegral"),
            UnentscheidbareAussage("Standardteil existiert", "Nichtstandardintegral"),
            UnentscheidbareAussage("Unabhängigkeit von Stützstellen", "Nichtstandardintegral"),
        )
        IntegralAuswertungsErgebnis(
            wert = NichtstandardIntegralDarstellung(integral, voraussetzungen = voraussetzungen),
            status = IntegralUnterstuetzungsStatus.BEDINGT,
            voraussetzungen = voraussetzungen,
            regel = "Nichtstandardmäßige hyperendliche Summendarstellung.",
        )
    }
    else -> IntegralAuswertungsErgebnis(
        wert = integral,
        status = IntegralUnterstuetzungsStatus.SYMBOLISCH,
        voraussetzungen = integral.voraussetzungen + integral.vertrag.orEmptyVoraussetzungen(),
        regel = "Das Integral bleibt als gültiger strukturierter Ausdruck erhalten.",
    )
}

private fun werteDiskretesIntegralAus(
    integral: StrukturiertesIntegral,
): IntegralAuswertungsErgebnis {
    val elemente = integral.bereich.endlicheElementeOderNull() ?: return IntegralAuswertungsErgebnis(
        wert = integral,
        status = IntegralUnterstuetzungsStatus.BEDINGT,
        voraussetzungen = integral.voraussetzungen + UnentscheidbareAussage(
            "Endlichkeit(${integral.bereich.zuLatex()})",
            "Zählmaß",
        ),
        regel = "Das Zählmaß benötigt einen endlichen oder summierbaren Bereich.",
    )
    val werte = runCatching {
        elemente.map { element -> integral.wertAnDiskretemElement(element) }
    }.getOrElse { fehler ->
        return IntegralAuswertungsErgebnis(
            wert = integral,
            status = IntegralUnterstuetzungsStatus.BEDINGT,
            voraussetzungen = integral.voraussetzungen + UnentscheidbareAussage(
                fehler.message ?: "Integrand auf diskretem Bereich auswertbar",
                "Zählmaß",
            ),
            regel = "Die diskrete Summe konnte nicht vollständig ausgewertet werden.",
        )
    }
    val summe = addition(werte)
    return IntegralAuswertungsErgebnis(
        wert = summe,
        status = IntegralUnterstuetzungsStatus.EXAKT,
        voraussetzungen = integral.voraussetzungen,
        regel = "Zählmaß: Integral und endliche Summe stimmen überein.",
        schritte = listOf(
            UmformungsSchritt(
                vorher = integral,
                nachher = summe,
                regelId = "analysis.integral.zaehlmass",
                titel = "Mit Zählmaß summieren",
                erklärung = "Der Integrand wurde über alle endlichen Bereichselemente summiert.",
            ),
        ),
    )
}

private fun werteEindimensionalesRiemannIntegralAus(
    integral: StrukturiertesIntegral,
): IntegralAuswertungsErgebnis {
    val intervall = integral.bereich.komponenten.single() as? ReellesIntervall
        ?: return IntegralAuswertungsErgebnis(
            wert = integral,
            status = IntegralUnterstuetzungsStatus.SYMBOLISCH,
            voraussetzungen = integral.voraussetzungen + integral.vertrag.orEmptyVoraussetzungen(),
            regel = "Die erste konkrete Riemann-Auswertung benötigt ein beschränktes reelles Intervall.",
        )
    val (term, variable) = integral.eindimensionalerZahlIntegrandOderNull()
        ?: return IntegralAuswertungsErgebnis(
            wert = integral,
            status = IntegralUnterstuetzungsStatus.NOCH_NICHT_IMPLEMENTIERT,
            voraussetzungen = integral.voraussetzungen + UnentscheidbareAussage(
                "eindimensionaler Zahltegrand",
                "Riemann-Integral",
            ),
            regel = "Für diese Signatur ist noch keine konkrete Riemann-Auswertung registriert.",
        )
    val stammfunktion = runCatching { integrieren(term, variable) }.getOrElse {
        return IntegralAuswertungsErgebnis(
            wert = integral,
            status = IntegralUnterstuetzungsStatus.SYMBOLISCH,
            voraussetzungen = integral.voraussetzungen + integral.vertrag.orEmptyVoraussetzungen(),
            regel = "Keine geschlossene Stammfunktion registriert; das Integral bleibt symbolisch gültig.",
        )
    }
    val rechts = ersetze(
        stammfunktion.ergebnis,
        mapOf(variable.name to intervall.rechts),
    ) as ZahlAusdruck
    val links = ersetze(
        stammfunktion.ergebnis,
        mapOf(variable.name to intervall.links),
    ) as ZahlAusdruck
    val wert = vereinfache(subtraktion(rechts, links))
    return IntegralAuswertungsErgebnis(
        wert = wert,
        status = IntegralUnterstuetzungsStatus.EXAKT,
        voraussetzungen = integral.voraussetzungen,
        regel = "Hauptsatz auf einer registrierten Stammfunktion.",
        schritte = stammfunktion.schritte + UmformungsSchritt(
            vorher = integral,
            nachher = wert,
            regelId = "analysis.integral.hauptsatz",
            titel = "Bestimmtes Integral auswerten",
            erklärung = "Die Stammfunktion wurde an rechter und linker Intervallgrenze ausgewertet.",
        ),
    )
}

private enum class BereichsKompatibilitaet { Vereinbar, Unvereinbar, Unentscheidbar }

private fun pruefeMethodenBereich(
    methode: Methode,
    bereich: IntegralBereich,
): BereichsKompatibilitaet {
    if (methode.parameter.size != bereich.dimension) return BereichsKompatibilitaet.Unvereinbar
    val status = methode.parameter.zip(bereich.komponenten).map { (parameter, komponentenBereich) ->
        val erwartet = methode.werteVorräte[parameter.name] ?: return@map BereichsKompatibilitaet.Unentscheidbar
        when {
            erwartet == komponentenBereich -> BereichsKompatibilitaet.Vereinbar
            erwartet == ReelleZahlen && komponentenBereich is ReellesIntervall -> BereichsKompatibilitaet.Vereinbar
            erwartet == ReelleZahlen && komponentenBereich is EndlicheMenge &&
                komponentenBereich.elemente.all { it is ZahlAusdruck } -> BereichsKompatibilitaet.Vereinbar
            else -> BereichsKompatibilitaet.Unentscheidbar
        }
    }
    return when {
        status.any { it == BereichsKompatibilitaet.Unvereinbar } -> BereichsKompatibilitaet.Unvereinbar
        status.any { it == BereichsKompatibilitaet.Unentscheidbar } -> BereichsKompatibilitaet.Unentscheidbar
        else -> BereichsKompatibilitaet.Vereinbar
    }
}

private fun IntegralBereich.endlicheElementeOderNull(): List<MathematischesObjekt>? {
    if (komponenten.any { it !is EndlicheMenge }) return null
    if (komponenten.size == 1) return (komponenten.single() as EndlicheMenge)
        .elemente
        .sortedBy(::strukturellerSchlüssel)
    val produkt = kartesischesProdukt(komponenten) as? EndlicheMenge ?: return null
    return produkt.elemente.sortedBy(::strukturellerSchlüssel)
}

private fun StrukturiertesIntegral.wertAnDiskretemElement(
    element: MathematischesObjekt,
): ZahlAusdruck = when (val wert = integrand) {
    is IntegralIntegrand.MethodenIntegrand -> {
        val argumente = if (bereich.dimension == 1) listOf(element) else (element as Tupel).elemente
        wert.methode.wendeAn(argumente) as? ZahlAusdruck
            ?: error("Die Integrationsmethode muss einen Zahlwert ausgeben.")
    }
    is IntegralIntegrand.TermIntegrand -> {
        val argumente = if (bereich.dimension == 1) listOf(element) else (element as Tupel).elemente
        val bindungenNachName = bindungen.map(IntegralBindung::variable)
            .map(Variable::name)
            .zip(argumente)
            .toMap()
        ersetze(wert.term, bindungenNachName) as? ZahlAusdruck
            ?: error("Der diskrete Integrand muss ein Zahlterm sein.")
    }
}

private fun StrukturiertesIntegral.eindimensionalerZahlIntegrandOderNull(): Pair<ZahlAusdruck, Variable>? =
    when (val wert = integrand) {
        is IntegralIntegrand.MethodenIntegrand -> {
            val variable = wert.methode.parameter.singleOrNull() as? Variable ?: return null
            val term = wert.methode.vorschrift as? ZahlAusdruck ?: return null
            term to variable
        }
        is IntegralIntegrand.TermIntegrand -> {
            val variable = bindungen.singleOrNull()?.variable ?: return null
            val term = wert.term as? ZahlAusdruck ?: return null
            term to variable
        }
    }

private fun RiemannIntegralVertrag?.orEmptyVoraussetzungen(): Set<Aussage> =
    this?.voraussetzungen.orEmpty()
