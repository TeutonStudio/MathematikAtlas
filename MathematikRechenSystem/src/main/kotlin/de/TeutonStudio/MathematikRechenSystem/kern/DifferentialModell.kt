package de.TeutonStudio.MathematikRechenSystem.kern

import java.math.BigInteger

enum class DifferentialAusgabeForm {
    METHODE,
    TERM,
}

enum class DifferentialBegriff {
    REELL_FRECHET,
    KOMPLEX,
}

sealed interface DifferentialOrdnung {
    fun zuLatex(): String

    data class Konkret(val wert: BigInteger) : DifferentialOrdnung {
        init { require(wert.signum() >= 0) { "Differentiationsordnungen müssen in ℕ₀ liegen." } }
        constructor(wert: Long) : this(BigInteger.valueOf(wert))
        override fun zuLatex(): String = wert.toString()
    }

    data class Symbolisch(
        val ausdruck: ZahlAusdruck,
        val annahmen: Set<Aussage> = emptySet(),
    ) : DifferentialOrdnung {
        override fun zuLatex(): String = ausdruck.zuLatex()
    }
}

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
) : Ausdruck {
    override fun zuLatex(): String =
        "${operator.zuLatexPraefix()}\\left(\\operatorname{id}\\vert_{${werteVorrat.zuLatex()}}\\right)"
}

data class DifferenzierbarkeitsBereich(
    val methode: Methode,
    val ursprungsBereich: MengenAusdruck,
    val ordnung: DifferentialOrdnung,
    val operator: DifferentialOperator,
    val voraussetzungen: Set<Aussage> = emptySet(),
) : MengenAusdruck {
    override fun zuLatex(): String = when (ordnung) {
        is DifferentialOrdnung.Konkret -> "D_{${ordnung.wert}}(${methode.name})"
        is DifferentialOrdnung.Symbolisch -> "D_{${ordnung.zuLatex()}}(${methode.name})"
    }
}

data class AbleitungsZielraum(
    val argumentRaum: MengenAusdruck,
    val ursprungsZiel: MengenAusdruck,
    val ordnung: DifferentialOrdnung,
    val eindimensionalSkalarIdentifiziert: Boolean,
) : MengenAusdruck {
    override fun zuLatex(): String = when {
        ordnung is DifferentialOrdnung.Konkret && ordnung.wert == BigInteger.ZERO ->
            ursprungsZiel.zuLatex()
        eindimensionalSkalarIdentifiziert -> ursprungsZiel.zuLatex()
        ordnung is DifferentialOrdnung.Konkret && ordnung.wert == BigInteger.ONE ->
            "\\mathcal L\\left(${argumentRaum.zuLatex()},${ursprungsZiel.zuLatex()}\\right)"
        else -> "\\mathcal L^{${ordnung.zuLatex()}}\\left(${argumentRaum.zuLatex()},${ursprungsZiel.zuLatex()}\\right)"
    }
}

data class AbleitungsMethodenAusdruck(
    val methode: Methode,
    val operator: DifferentialOperator = DifferentialOperator.Total,
    val ordnung: DifferentialOrdnung = DifferentialOrdnung.Konkret(1),
) : Ausdruck {
    init { operator.pruefeFuer(methode) }

    override fun zuLatex(): String = when (operator) {
        DifferentialOperator.Total -> differenzierungsLatex()
        is DifferentialOperator.Partiell -> when (ordnung) {
            is DifferentialOrdnung.Konkret -> if (ordnung.wert == BigInteger.ONE) {
                "\\partial_{${operator.argumentIndex}}${methode.name}"
            } else {
                "\\partial_{${operator.argumentIndex}}^{(${ordnung.wert})}${methode.name}"
            }
            is DifferentialOrdnung.Symbolisch ->
                "\\partial_{${operator.argumentIndex}}^{(${ordnung.zuLatex()})}${methode.name}"
        }
    }

    private fun differenzierungsLatex(): String = when (ordnung) {
        is DifferentialOrdnung.Symbolisch -> "${methode.name}^{(${ordnung.zuLatex()})}"
        is DifferentialOrdnung.Konkret -> when {
            ordnung.wert == BigInteger.ZERO -> methode.name
            ordnung.wert == BigInteger.ONE -> "${methode.name}'"
            else -> differentialRoemischeZahlOderNull(ordnung.wert)?.let { roemisch ->
                "${methode.name}^{\\mathrm{$roemisch}}"
            } ?: "${methode.name}^{(${ordnung.wert})}"
        }
    }
}

data class AbleitungsAusgabeAusdruck(
    val methode: Methode,
    val ausgabeName: String,
    val operator: DifferentialOperator,
    val ordnung: DifferentialOrdnung,
) : Ausdruck {
    init { require(ausgabeName in methode.ausgabeNamen) }

    override fun zuLatex(): String =
        "${AbleitungsMethodenAusdruck(methode, operator, ordnung).zuLatex()}_{${ausgabeName.differentialLatexText()}}"
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
        if (operator is DifferentialOperator.Partiell) require(operator.argumentIndex >= 1)
    }

    val differentialVariable = DifferentialVariable(variable, quellenId)

    override fun zuLatex(): String =
        "${operator.zuLatexPraefix()}\\left(${ursprung.zuLatex()}\\right)=" +
            "${ableitung.zuLatex()}\\cdot${differentialVariable.zuLatex()}"
}

enum class DifferentialUnterstuetzungsStatus {
    BERECHNET,
    SYMBOLISCH,
    BEDINGT,
    MATHEMATISCH_NICHT_MOEGLICH,
    NOCH_NICHT_IMPLEMENTIERT,
}

data class DifferentialMethodenErgebnis(
    val methode: Methode,
    val werteVorrat: MengenAusdruck,
    val zielRaum: MengenAusdruck,
    val status: DifferentialUnterstuetzungsStatus,
    val voraussetzungen: Set<Aussage> = emptySet(),
    val verwendeteRegel: String,
)

fun differenziereMethodeStrukturiert(
    methode: Methode,
    ordnung: DifferentialOrdnung,
    operator: DifferentialOperator = DifferentialOperator.Total,
    begriff: DifferentialBegriff = DifferentialBegriff.REELL_FRECHET,
    auswertungsBudget: Int = 16,
): DifferentialMethodenErgebnis {
    require(auswertungsBudget > 0)
    operator.pruefeFuer(methode)
    val ursprungsBereich = methode.argumentRaum()
    val bereich = DifferenzierbarkeitsBereich(
        methode = methode,
        ursprungsBereich = ursprungsBereich,
        ordnung = ordnung,
        operator = operator,
    )
    val zielRaum = methode.ableitungsZielRaum(ursprungsBereich, ordnung, begriff)

    if (ordnung is DifferentialOrdnung.Konkret && ordnung.wert == BigInteger.ZERO) {
        return DifferentialMethodenErgebnis(
            methode = methode,
            werteVorrat = ursprungsBereich,
            zielRaum = methode.zielMenge,
            status = DifferentialUnterstuetzungsStatus.BERECHNET,
            verwendeteRegel = "Differentiationsordnung null liefert die ursprüngliche Methode.",
        )
    }

    if (ordnung is DifferentialOrdnung.Symbolisch) {
        return DifferentialMethodenErgebnis(
            methode = methode.symbolischeAbleitung(operator, ordnung, zielRaum),
            werteVorrat = bereich,
            zielRaum = zielRaum,
            status = DifferentialUnterstuetzungsStatus.SYMBOLISCH,
            voraussetzungen = ordnung.annahmen,
            verwendeteRegel = "Symbolische n-fache Differentiation.",
        )
    }

    val konkret = ordnung as DifferentialOrdnung.Konkret
    if (konkret.wert > BigInteger.valueOf(auswertungsBudget.toLong())) {
        return DifferentialMethodenErgebnis(
            methode = methode.symbolischeAbleitung(operator, ordnung, zielRaum),
            werteVorrat = bereich,
            zielRaum = zielRaum,
            status = DifferentialUnterstuetzungsStatus.SYMBOLISCH,
            verwendeteRegel = "Konkrete Ordnung überschreitet das Auswertungsbudget; Ausdruck bleibt strukturiert.",
        )
    }

    var aktuell = methode
    repeat(konkret.wert.toInt()) {
        val schritt = differenziereMethodeEinmalOderNull(aktuell, operator, begriff)
            ?: return DifferentialMethodenErgebnis(
                methode = methode.symbolischeAbleitung(operator, ordnung, zielRaum),
                werteVorrat = bereich,
                zielRaum = zielRaum,
                status = DifferentialUnterstuetzungsStatus.NOCH_NICHT_IMPLEMENTIERT,
                voraussetzungen = setOf(
                    UnentscheidbareAussage(
                        bezeichnung = "Differenzierbarkeit(${methode.name},${ordnung.zuLatex()})",
                        system = "Differentialmodell $begriff",
                    ),
                ),
                verwendeteRegel = "Die Ableitung ist mathematisch darstellbar, aber für diese Signatur noch nicht konkret implementiert.",
            )
        aktuell = schritt
    }
    return DifferentialMethodenErgebnis(
        methode = aktuell.copy(
            name = AbleitungsMethodenAusdruck(methode, operator, ordnung).zuLatex(),
            zielMenge = zielRaum,
        ),
        werteVorrat = bereich,
        zielRaum = zielRaum,
        status = DifferentialUnterstuetzungsStatus.BERECHNET,
        verwendeteRegel = if (operator == DifferentialOperator.Total) {
            "Wiederholte totale Differentiation."
        } else {
            "Wiederholte partielle Differentiation nach dem gewählten formalen Argument."
        },
    )
}

private fun Methode.symbolischeAbleitung(
    operator: DifferentialOperator,
    ordnung: DifferentialOrdnung,
    zielRaum: MengenAusdruck,
): Methode {
    val name = AbleitungsMethodenAusdruck(this, operator, ordnung).zuLatex()
    val neueVorschrift = if (ausgabeNamen.size == 1) {
        AbleitungsMethodenAusdruck(this, operator, ordnung)
    } else {
        Tupel(ausgabeNamen.map { ausgabe ->
            AbleitungsAusgabeAusdruck(this, ausgabe, operator, ordnung)
        })
    }
    return copy(
        name = name,
        vorschrift = neueVorschrift,
        zielMenge = zielRaum,
    )
}

private fun differenziereMethodeEinmalOderNull(
    methode: Methode,
    operator: DifferentialOperator,
    begriff: DifferentialBegriff,
): Methode? {
    if (begriff == DifferentialBegriff.KOMPLEX) return null
    val variable = when (operator) {
        DifferentialOperator.Total -> methode.parameter.singleOrNull() as? Variable ?: return null
        is DifferentialOperator.Partiell -> methode.parameter.getOrNull(operator.argumentIndex - 1) as? Variable ?: return null
    }
    val abgeleitet = differenziereAusgabeObjektOderNull(methode.vorschrift, variable) ?: return null
    return methode.copy(vorschrift = abgeleitet)
}

private fun differenziereAusgabeObjektOderNull(
    wert: MathematischesObjekt,
    variable: Variable,
): MathematischesObjekt? = when (wert) {
    is ZahlAusdruck -> vereinfache(ableiten(wert, variable).ergebnis)
    is SpaltenVektor -> SpaltenVektor(wert.werte.map { vereinfache(ableiten(it, variable).ergebnis) })
    is ZeilenVektor -> ZeilenVektor(wert.werte.map { vereinfache(ableiten(it, variable).ergebnis) })
    is Tupel -> {
        val elemente = wert.elemente.map { differenziereAusgabeObjektOderNull(it, variable) }
        if (elemente.any { it == null }) null else Tupel(elemente.filterNotNull())
    }
    else -> null
}

fun bildeDifferentialTerm(
    term: ZahlAusdruck,
    variable: Variable,
    operator: DifferentialOperator = DifferentialOperator.Total,
    quellenId: String = variable.name,
): DifferentialTerm = DifferentialTerm(
    ursprung = term,
    variable = variable,
    ableitung = vereinfache(ableiten(term, variable).ergebnis),
    operator = operator,
    quellenId = quellenId,
)

fun partielleAbleitung(
    methode: Methode,
    argumentIndex: Int,
    ordnung: DifferentialOrdnung = DifferentialOrdnung.Konkret(1),
): AbleitungsMethodenAusdruck {
    val operator = DifferentialOperator.Partiell(argumentIndex)
    operator.pruefeFuer(methode)
    return AbleitungsMethodenAusdruck(methode, operator, ordnung)
}

fun totaleAbleitung(
    methode: Methode,
    ordnung: DifferentialOrdnung = DifferentialOrdnung.Konkret(1),
): AbleitungsMethodenAusdruck = AbleitungsMethodenAusdruck(methode, DifferentialOperator.Total, ordnung)

fun eindimensionaleAbleitungenStimmenUeberein(methode: Methode): Boolean =
    methode.parameter.size == 1

/**
 * Differentialoperationen verwenden denselben kanonischen mathematischen Argumentraum
 * wie der übrige Methodenkern. Insbesondere bleibt ein einstelliger Bereich Tupelraum(W)
 * und nullstellige Methoden werden an dieser fachlichen Grenze abgelehnt.
 */
private fun Methode.argumentRaum(): MengenAusdruck {
    val signatur = mathematischeMethodenSignatur()
    require(signatur.argumente.isNotEmpty()) { "Eine Differentiation benötigt mindestens ein formales Argument." }
    return signatur.definitionsRaum
}

private fun Methode.ableitungsZielRaum(
    argumentRaum: MengenAusdruck,
    ordnung: DifferentialOrdnung,
    begriff: DifferentialBegriff,
): MengenAusdruck {
    fun zielFuer(ausgabe: String): MengenAusdruck {
        val ziel = zielMengeFür(ausgabe)
        val skalarIdentifiziert = parameter.size == 1 &&
            ziel == ReelleZahlen &&
            begriff == DifferentialBegriff.REELL_FRECHET
        if (skalarIdentifiziert) return ReelleZahlen
        return AbleitungsZielraum(
            argumentRaum = argumentRaum,
            ursprungsZiel = ziel,
            ordnung = ordnung,
            eindimensionalSkalarIdentifiziert = false,
        )
    }
    return if (ausgabeNamen.size == 1) {
        zielFuer(ausgabeNamen.single())
    } else {
        Tupelraum(ausgabeNamen.map(::zielFuer))
    }
}

private fun differentialRoemischeZahlOderNull(wert: BigInteger): String? {
    if (wert < BigInteger.ONE || wert > BigInteger.valueOf(3999)) return null
    var rest = wert.toInt()
    val teile = listOf(
        1000 to "M", 900 to "CM", 500 to "D", 400 to "CD",
        100 to "C", 90 to "XC", 50 to "L", 40 to "XL",
        10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I",
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

private fun String.differentialLatexText(): String =
    replace("\\", "").replace("_", "\\_").replace(" ", "\\ ")