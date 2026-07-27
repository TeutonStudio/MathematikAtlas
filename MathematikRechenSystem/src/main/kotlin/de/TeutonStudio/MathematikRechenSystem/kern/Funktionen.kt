package de.TeutonStudio.MathematikRechenSystem.kern

data class Funktion(
    val name: String,
    val parameter: List<Variable>,
    val ausgaben: Map<String, ZahlAusdruck>,
) : MathematischesObjekt {
    init { require(parameter.map { it.name }.distinct().size == parameter.size) }
    override fun zuLatex(): String {
        val p = parameter.joinToString(",") { it.zuLatex() }
        return if (ausgaben.size == 1) "$name($p) = ${ausgaben.values.single().zuLatex()}" else "$name($p) = \\left(${ausgaben.values.joinToString(",") { it.zuLatex() }}\\right)"
    }
    fun binde(bindungen: Map<String, ZahlAusdruck>): GebundeneFunktion = GebundeneFunktion(this, bindungen.filterKeys { key -> parameter.any { it.name == key } })
    fun wendeAn(argumente: Map<String, ZahlAusdruck>): Map<String, ZahlAusdruck> {
        require(parameter.all { it.name in argumente }) { "Nicht alle Parameter sind gebunden." }
        return ausgaben.mapValues { (_, wert) -> vereinfache(ersetze(wert, argumente)) }
    }
}

data class GebundeneFunktion(val funktion: Funktion, val bindungen: Map<String, ZahlAusdruck>) : MathematischesObjekt {
    val freieParameter get() = funktion.parameter.filterNot { it.name in bindungen }
    override fun zuLatex(): String = funktion.copy(
        parameter = freieParameter,
        ausgaben = funktion.ausgaben.mapValues { ersetze(it.value, bindungen) },
    ).zuLatex()
    fun binde(weitere: Map<String, ZahlAusdruck>) = GebundeneFunktion(funktion, bindungen + weitere)
    fun auswerten(): Map<String, ZahlAusdruck> {
        require(freieParameter.isEmpty())
        return funktion.wendeAn(bindungen)
    }
}

fun ersetze(ausdruck: ZahlAusdruck, bindungen: Map<String, ZahlAusdruck>): ZahlAusdruck = when (ausdruck) {
    is Variable -> bindungen[ausdruck.name] ?: ausdruck
    is Addition -> addition(ausdruck.summanden.map { ersetze(it, bindungen) })
    is Multiplikation -> multiplikation(ausdruck.faktoren.map { ersetze(it, bindungen) })
    is Division -> Division(ersetze(ausdruck.dividend, bindungen), ersetze(ausdruck.divisor, bindungen))
    is Potenz -> Potenz(ersetze(ausdruck.basis, bindungen), ersetze(ausdruck.exponent, bindungen))
    is Betrag -> Betrag(ersetze(ausdruck.argument, bindungen))
    is Sinus -> Sinus(ersetze(ausdruck.argument, bindungen))
    is Cosinus -> Cosinus(ersetze(ausdruck.argument, bindungen))
    is Exponentialfunktion -> Exponentialfunktion(ersetze(ausdruck.argument, bindungen))
    is NatürlicherLogarithmus -> NatürlicherLogarithmus(ersetze(ausdruck.argument, bindungen))
    else -> ausdruck
}
