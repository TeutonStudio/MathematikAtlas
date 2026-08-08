package de.TeutonStudio.MathematikRechenSystem.kern

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
    val terme = koeffizienten.mapIndexed { index, koeffizient ->
        when (index) {
            0 -> koeffizient
            1 -> multiplikation(koeffizient, argument)
            else -> multiplikation(
                koeffizient,
                Potenz(argument, RationaleZahl.von(index.toLong())),
            )
        }
    }
    return vereinfache(addition(terme.reversed()))
}
