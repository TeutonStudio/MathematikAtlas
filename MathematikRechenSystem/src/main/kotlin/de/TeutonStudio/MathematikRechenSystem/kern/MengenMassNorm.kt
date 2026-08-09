package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Maßinduzierte Größe einer messbaren Menge.
 *
 * Sie ist bewusst keine Zahlen- oder Vektornorm, sondern die Identität
 *     ||A||_μ := ∫_A 1 dμ = μ(A).
 * Der strukturierte Integralvertrag bleibt als Herkunft erhalten, auch wenn die
 * vorhandene Integralauswertung den Wert bereits exakt reduzieren kann.
 */
data class MengenMassNormErgebnis(
    val wert: ZahlAusdruck,
    val integral: StrukturiertesIntegral,
    val status: IntegralUnterstuetzungsStatus,
    val voraussetzungen: Set<Aussage>,
    val regel: String,
    val schritte: List<UmformungsSchritt>,
)

fun normEinerMenge(
    menge: MengenAusdruck,
    mass: IntegralMass,
): MengenMassNormErgebnis {
    val variable = Variable("x")
    val messbarkeit = messbarkeitsVoraussetzungen(menge, mass)
    val integral = termIntegral(
        term = RationaleZahl.Eins,
        bereiche = listOf(menge),
        bindungen = listOf(IntegralBindung(variable, "mengenNorm.x")),
        mass = mass,
        vertrag = if (mass == IntegralMass.StandardReell && menge is ReellesIntervall) {
            standardRiemannVertrag(IntegralBereich(listOf(menge)))
        } else {
            null
        },
    ).copy(voraussetzungen = messbarkeit)
    val ausgewertet = werteIntegralAus(integral)
    val wert = ausgewertet.wert as? ZahlAusdruck
        ?: symbolischerZahlterm(
            identitaet = "mengenmassnorm-${menge.zuLatex()}-${mass.zuLatex()}",
            latex = "\\int_{${menge.zuLatex()}} 1\\,\\mathrm d${mass.zuLatex()}",
        )
    return MengenMassNormErgebnis(
        wert = wert,
        integral = integral,
        status = ausgewertet.status,
        voraussetzungen = ausgewertet.voraussetzungen + messbarkeit,
        regel = ausgewertet.regel,
        schritte = ausgewertet.schritte,
    )
}

private fun messbarkeitsVoraussetzungen(
    menge: MengenAusdruck,
    mass: IntegralMass,
): Set<Aussage> {
    val nachweisbarMessbar = when (mass) {
        IntegralMass.Zaehlmass -> true
        IntegralMass.StandardReell -> menge is ReellesIntervall || menge is EndlicheMenge || menge == LeereMenge
        is IntegralMass.Gewichtet -> when (mass.basis) {
            IntegralMass.Zaehlmass -> true
            IntegralMass.StandardReell -> menge is ReellesIntervall || menge is EndlicheMenge || menge == LeereMenge
            else -> false
        }
        is IntegralMass.Allgemein,
        is IntegralMass.NichtstandardZellgewicht,
        -> false
    }
    if (nachweisbarMessbar) return emptySet()
    return setOf(
        UnentscheidbareAussage(
            "${menge.zuLatex()}\\text{ ist }${mass.zuLatex()}\\text{-messbar}",
            "Mengenmaß",
        ),
    )
}
