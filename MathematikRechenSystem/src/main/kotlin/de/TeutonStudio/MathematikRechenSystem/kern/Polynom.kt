package de.TeutonStudio.MathematikRechenSystem.kern

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
