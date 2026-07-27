package de.TeutonStudio.MathematikRechenSystem.kern

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext

class RationaleZahl private constructor(
    val zähler: BigInteger,
    val nenner: BigInteger,
) : ZahlAusdruck, Comparable<RationaleZahl> {
    init { require(nenner.signum() > 0) }

    operator fun plus(andere: RationaleZahl) = von(zähler * andere.nenner + andere.zähler * nenner, nenner * andere.nenner)
    operator fun minus(andere: RationaleZahl) = von(zähler * andere.nenner - andere.zähler * nenner, nenner * andere.nenner)
    operator fun times(andere: RationaleZahl) = von(zähler * andere.zähler, nenner * andere.nenner)
    operator fun div(andere: RationaleZahl): RationaleZahl {
        require(andere.zähler != BigInteger.ZERO) { "Division durch null" }
        return von(zähler * andere.nenner, nenner * andere.zähler)
    }
    operator fun unaryMinus() = von(-zähler, nenner)
    fun istNull() = zähler == BigInteger.ZERO
    fun istEins() = zähler == nenner
    fun zuDezimal(dezimalstellen: Int = 34): BigDecimal = BigDecimal(zähler).divide(BigDecimal(nenner), MathContext(dezimalstellen))
    override fun compareTo(other: RationaleZahl): Int = (zähler * other.nenner).compareTo(other.zähler * nenner)
    override fun zuLatex(): String = when {
        nenner == BigInteger.ONE -> zähler.toString()
        else -> "\\frac{$zähler}{$nenner}"
    }
    override fun equals(other: Any?) = other is RationaleZahl && zähler == other.zähler && nenner == other.nenner
    override fun hashCode() = 31 * zähler.hashCode() + nenner.hashCode()
    override fun toString() = if (nenner == BigInteger.ONE) zähler.toString() else "$zähler/$nenner"

    companion object {
        val Null = von(0)
        val Eins = von(1)
        fun von(ganzzahl: Long) = von(BigInteger.valueOf(ganzzahl), BigInteger.ONE)
        fun von(zähler: Long, nenner: Long) = von(BigInteger.valueOf(zähler), BigInteger.valueOf(nenner))
        fun von(zähler: BigInteger, nenner: BigInteger = BigInteger.ONE): RationaleZahl {
            require(nenner != BigInteger.ZERO) { "Der Nenner darf nicht null sein." }
            if (zähler == BigInteger.ZERO) return RationaleZahl(BigInteger.ZERO, BigInteger.ONE)
            val vorzeichen = if (nenner.signum() < 0) BigInteger.valueOf(-1) else BigInteger.ONE
            val z = zähler * vorzeichen
            val n = nenner * vorzeichen
            val ggt = z.abs().gcd(n)
            return RationaleZahl(z / ggt, n / ggt)
        }
        fun parse(text: String): RationaleZahl {
            val teile = text.trim().split('/')
            return when (teile.size) {
                1 -> von(BigInteger(teile[0]))
                2 -> von(BigInteger(teile[0]), BigInteger(teile[1]))
                else -> error("Ungültige rationale Zahl: $text")
            }
        }
    }
}

data class Variable(override val name: String, val latex: String = name) : ZahlAusdruck, FunktionsParameter {
    init { require(name.isNotBlank()) }
    override fun zuLatex(): String = latex
}

/** Ein allgemeiner, nicht auf Zahlterme beschränkter Funktionsparameter. */
data class AllgemeinerParameter(override val name: String, val latex: String = name) : FunktionsParameter {
    init { require(name.isNotBlank()) }
    override fun zuLatex(): String = latex
}

data class MathematischeKonstante(val name: String, val latex: String = name) : ZahlAusdruck {
    override fun zuLatex(): String = latex
}

val Pi = MathematischeKonstante("pi", "\\pi")
val EulerscheZahl = MathematischeKonstante("e", "e")
