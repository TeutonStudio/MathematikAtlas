package de.TeutonStudio.MathematikRechenSystem.kern

import java.math.BigInteger

data class PolynomZerlegung(
    val variable: Variable,
    /** Aufsteigend gespeichert: c0, c1, ..., cn. */
    val koeffizienten: List<ZahlAusdruck>,
)

/**
 * Zerlegt einen Zahlterm exakt als Polynom in [variable]. Andere Variablen und
 * Ausdrücke, die [variable] nicht enthalten, bleiben symbolische Koeffizienten.
 */
fun zerlegeAlsPolynom(
    ausdruck: ZahlAusdruck,
    variable: Variable,
    maximalerGrad: Int = 256,
    maximaleOperationen: Int = 10_000,
): PolynomZerlegung? {
    require(maximalerGrad >= 0)
    require(maximaleOperationen > 0)
    val zerleger = PolynomZerleger(variable, maximalerGrad, maximaleOperationen)
    val koeffizienten = try {
        zerleger.zerlege(ausdruck)
    } catch (_: PolynomBudgetÜberschritten) {
        null
    } ?: return null
    return PolynomZerlegung(variable, koeffizienten.trimmeNullen())
}

private class PolynomZerleger(
    private val variable: Variable,
    private val maximalerGrad: Int,
    private var restOperationen: Int,
) {
    fun zerlege(ausdruck: ZahlAusdruck): List<ZahlAusdruck>? = when {
        ausdruck == variable -> listOf(RationaleZahl.Null, RationaleZahl.Eins)
        !ausdruck.enthältVariable(variable) -> listOf(ausdruck)
        ausdruck is Addition -> ausdruck.summanden.map { zerlege(it) ?: return null }.reduce(::addiere)
        ausdruck is Multiplikation -> ausdruck.faktoren.map { zerlege(it) ?: return null }.reduce(::multipliziere)
        ausdruck is Potenz -> potenz(ausdruck)
        else -> null
    }

    private fun potenz(potenz: Potenz): List<ZahlAusdruck>? {
        val exponent = potenz.exponent as? RationaleZahl ?: return null
        if (exponent.nenner != BigInteger.ONE || exponent.zähler.signum() < 0 || exponent.zähler.bitLength() >= 31) return null
        val n = exponent.zähler.toInt()
        val basis = zerlege(potenz.basis) ?: return null
        if ((basis.size - 1).toLong() * n > maximalerGrad) return null
        var ergebnis = listOf<ZahlAusdruck>(RationaleZahl.Eins)
        var faktor = basis
        var rest = n
        while (rest > 0) {
            if (rest and 1 == 1) ergebnis = multipliziere(ergebnis, faktor)
            rest = rest ushr 1
            if (rest > 0) faktor = multipliziere(faktor, faktor)
        }
        return ergebnis
    }

    private fun addiere(a: List<ZahlAusdruck>, b: List<ZahlAusdruck>): List<ZahlAusdruck> {
        verbrauche(maxOf(a.size, b.size))
        return List(maxOf(a.size, b.size)) { index ->
            vereinfache(addition(a.getOrElse(index) { RationaleZahl.Null }, b.getOrElse(index) { RationaleZahl.Null }))
        }.trimmeNullen()
    }

    private fun multipliziere(a: List<ZahlAusdruck>, b: List<ZahlAusdruck>): List<ZahlAusdruck> {
        val grad = a.lastIndex + b.lastIndex
        if (grad > maximalerGrad) throw PolynomBudgetÜberschritten
        verbrauche(a.size * b.size)
        val ergebnis = MutableList<ZahlAusdruck>(grad + 1) { RationaleZahl.Null }
        a.forEachIndexed { i, links ->
            b.forEachIndexed { j, rechts ->
                ergebnis[i + j] = vereinfache(addition(ergebnis[i + j], multiplikation(links, rechts)))
            }
        }
        return ergebnis.trimmeNullen()
    }

    private fun verbrauche(anzahl: Int) {
        restOperationen -= anzahl
        if (restOperationen < 0) throw PolynomBudgetÜberschritten
    }
}

private data object PolynomBudgetÜberschritten : RuntimeException()

private fun List<ZahlAusdruck>.trimmeNullen(): List<ZahlAusdruck> {
    val letzter = indexOfLast { it != RationaleZahl.Null }
    return if (letzter < 0) listOf(RationaleZahl.Null) else take(letzter + 1)
}

/**
 * Setzt die aufsteigend gespeicherten Koeffizienten c₀, …, cₙ in ein
 * beliebiges Zahlargument a ein und liefert Σ cᵢ·aⁱ als normalen Zahlterm.
 *
 * Die Potenzbasis stammt aus derselben [multinomFolge] wie der explizite
 * Multinomvektor. Damit existiert nur ein Vertrag für (1,a,a²,…,aⁿ).
 */
fun polynomAusKoeffizienten(
    koeffizienten: List<ZahlAusdruck>,
    argument: ZahlAusdruck,
): ZahlAusdruck {
    require(koeffizienten.isNotEmpty()) { "Ein Polynom benötigt mindestens einen Koeffizienten." }
    val monome = multinomFolge(argument, koeffizienten.lastIndex)
    val terme = koeffizienten.zip(monome) { koeffizient, monom ->
        multiplikation(koeffizient, monom)
    }
    return vereinfache(addition(terme.reversed()))
}
