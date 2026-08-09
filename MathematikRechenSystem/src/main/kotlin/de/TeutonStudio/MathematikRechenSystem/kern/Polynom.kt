package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Liefert die Monomfolge
 *
 *     M_n(x) = (x^k)_{0 <= k <= n} = (1, x, x^2, ..., x^n)
 *
 * als reine geordnete Liste von Zahltermen. Die Funktion ist absichtlich frei
 * von Vektor-/Tupeltypen, damit Polynomoperator, Multinomvektor und
 * Definitionskarten exakt dieselbe mathematische Konstruktion verwenden.
 */
fun multinomFolge(
    argument: ZahlAusdruck,
    dimension: Int,
): List<ZahlAusdruck> {
    require(dimension >= 0) { "Die Multinomdimension muss mindestens 0 sein." }
    return (0..dimension).map { exponent ->
        when (exponent) {
            0 -> RationaleZahl.Eins
            1 -> argument
            else -> Potenz(argument, RationaleZahl.von(exponent.toLong()))
        }
    }
}

/**
 * Setzt die aufsteigend gespeicherten Koeffizienten c₀, …, cₙ in ein
 * beliebiges Zahlargument a ein und liefert Σ cᵢ·aⁱ als normalen Zahlterm.
 *
 * Anders als die variablenspezifische Überladung ist dies bewusst keine
 * Methodenkonstruktion: Das Argument ist bereits ein konkreter Zahlterm.
 */
fun polynomAusKoeffizienten(
    koeffizienten: List<ZahlAusdruck>,
    argument: ZahlAusdruck,
): ZahlAusdruck {
    require(koeffizienten.isNotEmpty()) { "Ein Polynom benötigt mindestens einen Koeffizienten." }
    val monome = multinomFolge(argument, koeffizienten.lastIndex)
    val terme = koeffizienten.zip(monome).map { (koeffizient, monom) ->
        when {
            koeffizient == RationaleZahl.Null -> RationaleZahl.Null
            monom == RationaleZahl.Eins -> koeffizient
            else -> multiplikation(koeffizient, monom)
        }
    }
    return vereinfache(addition(terme.reversed()))
}
