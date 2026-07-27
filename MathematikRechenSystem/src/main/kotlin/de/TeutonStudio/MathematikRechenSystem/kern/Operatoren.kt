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

fun negation(ausdruck: ZahlAusdruck) = multiplikation(RationaleZahl.von(-1), ausdruck)
fun subtraktion(a: ZahlAusdruck, b: ZahlAusdruck) = addition(a, negation(b))

fun vereinfache(ausdruck: ZahlAusdruck, kontext: RechenKontext = RechenKontext()): ZahlAusdruck = when (ausdruck) {
    is RationaleZahl, is Variable, is MathematischeKonstante -> ausdruck
    is Addition -> addition(ausdruck.summanden.map { vereinfache(it, kontext) })
    is Multiplikation -> multiplikation(ausdruck.faktoren.map { vereinfache(it, kontext) })
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
