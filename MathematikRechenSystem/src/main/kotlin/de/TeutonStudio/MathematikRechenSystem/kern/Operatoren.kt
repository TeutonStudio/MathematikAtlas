package de.TeutonStudio.MathematikRechenSystem.kern

import java.math.BigInteger

class Addition private constructor(val summanden: List<ZahlAusdruck>) : ZahlAusdruck {
    init { require(summanden.size >= 2) }
    override fun zuLatex(): String = summanden.joinToString(" + ") { it.zuLatex() }
    override fun equals(other: Any?) = other is Addition && summanden == other.summanden
    override fun hashCode() = summanden.hashCode()
    override fun toString() = summanden.joinToString(" + ")
    companion object { internal fun roh(summanden: List<ZahlAusdruck>) = Addition(summanden) }
}

class Multiplikation private constructor(val faktoren: List<ZahlAusdruck>) : ZahlAusdruck {
    init { require(faktoren.size >= 2) }
    override fun zuLatex(): String = faktoren.joinToString(" \\cdot ") {
        if (it is Addition) "\\left(${it.zuLatex()}\\right)" else it.zuLatex()
    }
    override fun equals(other: Any?) = other is Multiplikation && faktoren == other.faktoren
    override fun hashCode() = faktoren.hashCode()
    override fun toString() = faktoren.joinToString(" * ")
    companion object { internal fun roh(faktoren: List<ZahlAusdruck>) = Multiplikation(faktoren) }
}

class Maximum private constructor(val operanden: List<ZahlAusdruck>) : ZahlAusdruck {
    init { require(operanden.size >= 2) }
    override fun zuLatex(): String = "\\max\\left\\{${operanden.joinToString(",") { it.zuLatex() }}\\right\\}"
    override fun equals(other: Any?) = other is Maximum && operanden == other.operanden
    override fun hashCode() = operanden.hashCode()
    companion object { internal fun roh(operanden: List<ZahlAusdruck>) = Maximum(operanden) }
}

class Minimum private constructor(val operanden: List<ZahlAusdruck>) : ZahlAusdruck {
    init { require(operanden.size >= 2) }
    override fun zuLatex(): String = "\\min\\left\\{${operanden.joinToString(",") { it.zuLatex() }}\\right\\}"
    override fun equals(other: Any?) = other is Minimum && operanden == other.operanden
    override fun hashCode() = operanden.hashCode()
    companion object { internal fun roh(operanden: List<ZahlAusdruck>) = Minimum(operanden) }
}

data class Division(val dividend: ZahlAusdruck, val divisor: ZahlAusdruck) : ZahlAusdruck {
    override fun zuLatex() = "\\frac{${dividend.zuLatex()}}{${divisor.zuLatex()}}"
}

data class Potenz(val basis: ZahlAusdruck, val exponent: ZahlAusdruck) : ZahlAusdruck {
    override fun zuLatex() = "{${basis.zuLatex()}}^{${exponent.zuLatex()}}"
}

data class Betrag(val argument: ZahlAusdruck) : ZahlAusdruck {
    override fun zuLatex() = "\\left|${argument.zuLatex()}\\right|"
}

data class Sinus(val argument: ZahlAusdruck) : ZahlAusdruck { override fun zuLatex() = "\\sin\\left(${argument.zuLatex()}\\right)" }
data class Cosinus(val argument: ZahlAusdruck) : ZahlAusdruck { override fun zuLatex() = "\\cos\\left(${argument.zuLatex()}\\right)" }
data class Exponentialfunktion(val argument: ZahlAusdruck) : ZahlAusdruck { override fun zuLatex() = "e^{${argument.zuLatex()}}" }
data class NatürlicherLogarithmus(val argument: ZahlAusdruck) : ZahlAusdruck { override fun zuLatex() = "\\ln\\left(${argument.zuLatex()}\\right)" }
data class Wurzel(val argument: ZahlAusdruck) : ZahlAusdruck { override fun zuLatex() = "\\sqrt{${argument.zuLatex()}}" }
data class KomplexeZahl(val realteil: ZahlAusdruck, val imaginärteil: ZahlAusdruck) : ZahlAusdruck {
    override fun zuLatex() = when {
        imaginärteil == RationaleZahl.Eins -> "${realteil.zuLatex()} + i"
        else -> "${realteil.zuLatex()} + ${imaginärteil.zuLatex()}i"
    }
}
data class Argument(val zahl: KomplexeZahl) : ZahlAusdruck { override fun zuLatex() = "\\arg\\left(${zahl.zuLatex()}\\right)" }
data class Logarithmus(val basis: ZahlAusdruck, val argument: ZahlAusdruck) : ZahlAusdruck {
    override fun zuLatex() = when (basis) {
        EulerscheZahl -> "\\ln\\left(${argument.zuLatex()}\\right)"
        RationaleZahl.von(2) -> "\\operatorname{lb}\\left(${argument.zuLatex()}\\right)"
        RationaleZahl.von(10) -> "\\log\\left(${argument.zuLatex()}\\right)"
        else -> "\\log_{${basis.zuLatex()}}\\left(${argument.zuLatex()}\\right)"
    }
}

fun addition(vararg summanden: ZahlAusdruck): ZahlAusdruck = addition(summanden.toList())
fun addition(summanden: Iterable<ZahlAusdruck>): ZahlAusdruck {
    val flach = summanden.flatMap { if (it is Addition) it.summanden else listOf(it) }
    var konstante = RationaleZahl.Null
    val andere = mutableListOf<ZahlAusdruck>()
    flach.forEach { term ->
        when (term) {
            is RationaleZahl -> konstante += term
            else -> andere += term
        }
    }
    if (!konstante.istNull()) andere += konstante
    return when (andere.size) {
        0 -> RationaleZahl.Null
        1 -> andere.single()
        else -> Addition.roh(andere)
    }
}

fun multiplikation(vararg faktoren: ZahlAusdruck): ZahlAusdruck = multiplikation(faktoren.toList())
fun multiplikation(faktoren: Iterable<ZahlAusdruck>): ZahlAusdruck {
    val flach = faktoren.flatMap { if (it is Multiplikation) it.faktoren else listOf(it) }
    var konstante = RationaleZahl.Eins
    val andere = mutableListOf<ZahlAusdruck>()
    flach.forEach { faktor ->
        when (faktor) {
            is RationaleZahl -> konstante *= faktor
            else -> andere += faktor
        }
    }
    if (konstante.istNull()) return RationaleZahl.Null
    if (!konstante.istEins()) andere.add(0, konstante)
    return when (andere.size) {
        0 -> RationaleZahl.Eins
        1 -> andere.single()
        else -> Multiplikation.roh(andere)
    }
}

fun maximum(vararg operanden: ZahlAusdruck): ZahlAusdruck = maximum(operanden.toList())
fun maximum(operanden: Iterable<ZahlAusdruck>): ZahlAusdruck = extremwert(operanden, true)
fun minimum(vararg operanden: ZahlAusdruck): ZahlAusdruck = minimum(operanden.toList())
fun minimum(operanden: Iterable<ZahlAusdruck>): ZahlAusdruck = extremwert(operanden, false)

private fun extremwert(operanden: Iterable<ZahlAusdruck>, maximum: Boolean): ZahlAusdruck {
    val flach = operanden.flatMap { operand ->
        when (operand) {
            is Maximum if maximum -> operand.operanden
            is Minimum if !maximum -> operand.operanden
            else -> listOf(operand)
        }
    }
    require(flach.size >= 2) { "Ein Extremwert benötigt mindestens zwei Operanden." }
    val eindeutig = flach.distinct()
    if (eindeutig.size == 1) return eindeutig.single()
    if (eindeutig.all { it is RationaleZahl }) {
        @Suppress("UNCHECKED_CAST")
        return (eindeutig as List<RationaleZahl>).let { werte -> if (maximum) werte.maxOrNull()!! else werte.minOrNull()!! }
    }
    return if (maximum) Maximum.roh(eindeutig) else Minimum.roh(eindeutig)
}

fun negation(ausdruck: ZahlAusdruck) = multiplikation(RationaleZahl.von(-1), ausdruck)
fun subtraktion(a: ZahlAusdruck, b: ZahlAusdruck) = addition(a, negation(b))

fun vereinfache(ausdruck: ZahlAusdruck, kontext: RechenKontext = RechenKontext()): ZahlAusdruck = when (ausdruck) {
    is RationaleZahl, is Variable, is MathematischeKonstante -> ausdruck
    is Addition -> addition(ausdruck.summanden.map { vereinfache(it, kontext) })
    is Multiplikation -> multiplikation(ausdruck.faktoren.map { vereinfache(it, kontext) })
    is Maximum -> maximum(ausdruck.operanden.map { vereinfache(it, kontext) })
    is Minimum -> minimum(ausdruck.operanden.map { vereinfache(it, kontext) })
    is Division -> {
        val a = vereinfache(ausdruck.dividend, kontext)
        val b = vereinfache(ausdruck.divisor, kontext)
        when {
            a is RationaleZahl && b is RationaleZahl && !b.istNull() -> a / b
            a == b && kontext.annahmen.any { it == Ungleichheit(a, RationaleZahl.Null) } -> RationaleZahl.Eins
            else -> Division(a, b)
        }
    }
    is Potenz -> {
        val basis = vereinfache(ausdruck.basis, kontext)
        val exponent = vereinfache(ausdruck.exponent, kontext)
        when {
            exponent == RationaleZahl.Null -> RationaleZahl.Eins
            exponent == RationaleZahl.Eins -> basis
            basis is RationaleZahl && exponent is RationaleZahl && exponent.nenner == BigInteger.ONE && exponent.zähler.bitLength() < 31 -> {
                val n = exponent.zähler.toInt()
                if (n >= 0) RationaleZahl.von(basis.zähler.pow(n), basis.nenner.pow(n))
                else RationaleZahl.von(basis.nenner.pow(-n), basis.zähler.pow(-n))
            }
            else -> Potenz(basis, exponent)
        }
    }
    is Betrag -> Betrag(vereinfache(ausdruck.argument, kontext))
    is Sinus -> Sinus(vereinfache(ausdruck.argument, kontext))
    is Cosinus -> Cosinus(vereinfache(ausdruck.argument, kontext))
    is Exponentialfunktion -> Exponentialfunktion(vereinfache(ausdruck.argument, kontext))
    is NatürlicherLogarithmus -> NatürlicherLogarithmus(vereinfache(ausdruck.argument, kontext))
    is Wurzel -> vereinfacheWurzel(vereinfache(ausdruck.argument, kontext))
    is KomplexeZahl -> KomplexeZahl(vereinfache(ausdruck.realteil, kontext), vereinfache(ausdruck.imaginärteil, kontext))
    is Logarithmus -> Logarithmus(vereinfache(ausdruck.basis, kontext), vereinfache(ausdruck.argument, kontext))
    else -> ausdruck
}

/** Konservativer Nachweis, dass ein Zahlterm reell ist; unbekannte Variablen bleiben unbeweisbar. */
fun istNachweisbarReell(
    ausdruck: ZahlAusdruck,
    variableIstReell: (Variable) -> Boolean = { false },
    annahmen: Set<Aussage> = emptySet(),
): Boolean = when (ausdruck) {
    is RationaleZahl -> true
    is MathematischeKonstante -> ausdruck == Pi || ausdruck == EulerscheZahl
    is Variable -> variableIstReell(ausdruck)
    is Addition -> ausdruck.summanden.all { istNachweisbarReell(it, variableIstReell, annahmen) }
    is Multiplikation -> ausdruck.faktoren.all { istNachweisbarReell(it, variableIstReell, annahmen) }
    is Division -> istNachweisbarReell(ausdruck.dividend, variableIstReell, annahmen) &&
        istNachweisbarReell(ausdruck.divisor, variableIstReell, annahmen) && ausdruck.divisor.istNachweisbarNichtNull(annahmen)
    is Potenz -> istNachweisbarReell(ausdruck.basis, variableIstReell, annahmen) &&
        (ausdruck.exponent as? RationaleZahl)?.let { exponent ->
            exponent.nenner == BigInteger.ONE && (exponent.zähler.signum() >= 0 || ausdruck.basis.istNachweisbarNichtNull(annahmen))
        } == true
    is Betrag -> true
    is Sinus -> istNachweisbarReell(ausdruck.argument, variableIstReell, annahmen)
    is Cosinus -> istNachweisbarReell(ausdruck.argument, variableIstReell, annahmen)
    is Exponentialfunktion -> istNachweisbarReell(ausdruck.argument, variableIstReell, annahmen)
    is NatürlicherLogarithmus -> istNachweisbarReell(ausdruck.argument, variableIstReell, annahmen) && ausdruck.argument.istNachweisbarPositiv(annahmen)
    is Logarithmus -> istNachweisbarReell(ausdruck.basis, variableIstReell, annahmen) &&
        istNachweisbarReell(ausdruck.argument, variableIstReell, annahmen) &&
        ausdruck.basis.istNachweisbarPositiv(annahmen) && ausdruck.basis.istNachweisbarNichtEins() &&
        ausdruck.argument.istNachweisbarPositiv(annahmen)
    is Maximum -> ausdruck.operanden.all { istNachweisbarReell(it, variableIstReell, annahmen) }
    is Minimum -> ausdruck.operanden.all { istNachweisbarReell(it, variableIstReell, annahmen) }
    is Argument -> false
    is Wurzel -> istNachweisbarReell(ausdruck.argument, variableIstReell, annahmen) && ausdruck.argument.istNachweisbarNichtNegativ(annahmen)
    is KomplexeZahl -> false
    is IterierteSumme, is IteriertesProdukt -> false
}

private fun ZahlAusdruck.istNachweisbarPositiv(annahmen: Set<Aussage>): Boolean = when (this) {
    is RationaleZahl -> zähler.signum() > 0
    else -> Vergleich(this, VergleichsArt.Größer, RationaleZahl.Null) in annahmen
}

private fun ZahlAusdruck.istNachweisbarNichtNegativ(annahmen: Set<Aussage>): Boolean = when (this) {
    is RationaleZahl -> zähler.signum() >= 0
    else -> Vergleich(this, VergleichsArt.GrößerGleich, RationaleZahl.Null) in annahmen || istNachweisbarPositiv(annahmen)
}

private fun ZahlAusdruck.istNachweisbarNichtNull(annahmen: Set<Aussage>): Boolean = when (this) {
    is RationaleZahl -> !istNull()
    else -> Ungleichheit(this, RationaleZahl.Null) in annahmen ||
        istNachweisbarPositiv(annahmen) || Vergleich(this, VergleichsArt.Kleiner, RationaleZahl.Null) in annahmen
}

private fun ZahlAusdruck.istNachweisbarNichtEins(): Boolean =
    (this as? RationaleZahl)?.let { it != RationaleZahl.Eins } == true

/** Die Hauptwurzel liefert genau einen Wert; bei negativen reellen Zahlen einen komplexen. */
fun wurzel(argument: ZahlAusdruck, kontext: RechenKontext = RechenKontext()): ZahlAusdruck =
    vereinfacheWurzel(vereinfache(argument, kontext))

private fun vereinfacheWurzel(argument: ZahlAusdruck): ZahlAusdruck {
    val rational = argument as? RationaleZahl ?: return Wurzel(argument)
    if (rational.istNull()) return RationaleZahl.Null
    val negativ = rational.zähler.signum() < 0
    val zähler = rational.zähler.abs()
    val zWurzel = zähler.sqrtOrNull()
    val nWurzel = rational.nenner.sqrtOrNull()
    val wurzel = if (zWurzel != null && nWurzel != null) RationaleZahl.von(zWurzel, nWurzel) else Wurzel(RationaleZahl.von(zähler, rational.nenner))
    return if (negativ) KomplexeZahl(RationaleZahl.Null, wurzel) else wurzel
}

private fun BigInteger.sqrtOrNull(): BigInteger? {
    if (signum() < 0) return null
    val wurzel = sqrt()
    return wurzel.takeIf { it * it == this }
}

fun konjugiere(zahl: KomplexeZahl) = KomplexeZahl(zahl.realteil, negation(zahl.imaginärteil))
fun komplexerBetrag(zahl: KomplexeZahl): ZahlAusdruck = wurzel(addition(Potenz(zahl.realteil, RationaleZahl.von(2)), Potenz(zahl.imaginärteil, RationaleZahl.von(2))))
fun komplexAusKartesisch(tupel: Tupel): KomplexeZahl {
    require(tupel.elemente.size == 2 && tupel.elemente.all { it is ZahlAusdruck }) { "Eine komplexe Zahl benötigt ein Tupel aus zwei Zahlen." }
    return KomplexeZahl(tupel.elemente[0] as ZahlAusdruck, tupel.elemente[1] as ZahlAusdruck)
}
fun komplexAusPolar(tupel: Tupel): KomplexeZahl {
    require(tupel.elemente.size == 2 && tupel.elemente.all { it is ZahlAusdruck }) { "Eine komplexe Zahl benötigt ein Tupel aus Radius und Winkel." }
    val r = tupel.elemente[0] as ZahlAusdruck; val phi = tupel.elemente[1] as ZahlAusdruck
    return KomplexeZahl(multiplikation(r, Cosinus(phi)), multiplikation(r, Sinus(phi)))
}
