package de.TeutonStudio.MathematikRechenSystem.kern

data class UmformungsSchritt(
    val vorher: MathematischesObjekt,
    val nachher: MathematischesObjekt,
    val regelId: String,
    val titel: String,
    val erklärung: String,
)

data class FallBedingung(val name: String, val bedingung: Aussage)

data class UmformungsErgebnis<T : MathematischesObjekt>(
    val ergebnis: T,
    val schritte: List<UmformungsSchritt> = emptyList(),
    val zusätzlicheFälle: List<FallBedingung> = emptyList(),
)

interface UmformungsRegel<T : MathematischesObjekt> {
    val id: String
    val name: String
    fun istAnwendbar(objekt: T, kontext: RechenKontext): Boolean
    fun wendeAn(objekt: T, kontext: RechenKontext): UmformungsErgebnis<T>
}

class RegelRegister {
    private val regeln = linkedMapOf<String, UmformungsRegel<out MathematischesObjekt>>()
    fun registriere(regel: UmformungsRegel<out MathematischesObjekt>) { regeln[regel.id] = regel }
    fun finde(id: String) = regeln[id]
    fun alle() = regeln.values.toList()
}

fun vereinfacheMitSchritten(ausdruck: ZahlAusdruck, kontext: RechenKontext = RechenKontext()): UmformungsErgebnis<ZahlAusdruck> {
    val neu = vereinfache(ausdruck, kontext)
    return if (neu == ausdruck) UmformungsErgebnis(neu) else UmformungsErgebnis(
        neu,
        listOf(UmformungsSchritt(ausdruck, neu, "standard.vereinfachen", "Vereinfachen", "Neutrale Elemente und exakte Zahlen wurden zusammengefasst.")),
    )
}

fun ableiten(ausdruck: ZahlAusdruck, variable: Variable): UmformungsErgebnis<ZahlAusdruck> {
    val roh: ZahlAusdruck = when (ausdruck) {
        is RationaleZahl, is MathematischeKonstante -> RationaleZahl.Null
        is Variable -> if (ausdruck == variable) RationaleZahl.Eins else RationaleZahl.Null
        is Addition -> addition(ausdruck.summanden.map { ableiten(it, variable).ergebnis })
        is Multiplikation -> addition(ausdruck.faktoren.indices.map { i ->
            multiplikation(ausdruck.faktoren.mapIndexed { j, f -> if (i == j) ableiten(f, variable).ergebnis else f })
        })
        is Division -> Division(
            subtraktion(multiplikation(ableiten(ausdruck.dividend, variable).ergebnis, ausdruck.divisor), multiplikation(ausdruck.dividend, ableiten(ausdruck.divisor, variable).ergebnis)),
            Potenz(ausdruck.divisor, RationaleZahl.von(2)),
        )
        is Potenz -> if (ausdruck.exponent is RationaleZahl) {
            multiplikation(ausdruck.exponent, Potenz(ausdruck.basis, subtraktion(ausdruck.exponent, RationaleZahl.Eins)), ableiten(ausdruck.basis, variable).ergebnis)
        } else error("Allgemeine Potenzableitung ist noch nicht registriert.")
        is Sinus -> multiplikation(Cosinus(ausdruck.argument), ableiten(ausdruck.argument, variable).ergebnis)
        is Cosinus -> negation(multiplikation(Sinus(ausdruck.argument), ableiten(ausdruck.argument, variable).ergebnis))
        is Exponentialfunktion -> multiplikation(ausdruck, ableiten(ausdruck.argument, variable).ergebnis)
        is NatürlicherLogarithmus -> Division(ableiten(ausdruck.argument, variable).ergebnis, ausdruck.argument)
        else -> error("Für ${ausdruck::class.simpleName} ist keine Ableitungsregel registriert.")
    }
    val ergebnis = vereinfache(roh)
    return UmformungsErgebnis(ergebnis, listOf(UmformungsSchritt(ausdruck, ergebnis, "analysis.ableiten", "Ableiten", "Der Ausdruck wurde nach ${variable.name} abgeleitet.")))
}

fun integrieren(ausdruck: ZahlAusdruck, variable: Variable): UmformungsErgebnis<ZahlAusdruck> {
    val roh: ZahlAusdruck = when (ausdruck) {
        is RationaleZahl -> multiplikation(ausdruck, variable)
        is Variable -> if (ausdruck == variable) Division(Potenz(variable, RationaleZahl.von(2)), RationaleZahl.von(2)) else multiplikation(ausdruck, variable)
        is Addition -> addition(ausdruck.summanden.map { integrieren(it, variable).ergebnis })
        is Multiplikation -> {
            val konstanten = ausdruck.faktoren.filterIsInstance<RationaleZahl>()
            val rest = ausdruck.faktoren.filterNot { it is RationaleZahl }
            if (konstanten.isNotEmpty() && rest.size == 1) multiplikation(konstanten + integrieren(rest.single(), variable).ergebnis) else error("Für dieses Produkt ist keine elementare Integrationsregel registriert.")
        }
        is Potenz -> if (ausdruck.basis == variable && ausdruck.exponent is RationaleZahl && ausdruck.exponent != RationaleZahl.von(-1)) {
            val neu = ausdruck.exponent + RationaleZahl.Eins
            Division(Potenz(variable, neu), neu)
        } else error("Für diese Potenz ist keine Integrationsregel registriert.")
        is Sinus -> if (ausdruck.argument == variable) negation(Cosinus(variable)) else error("Substitution ist noch nicht registriert.")
        is Cosinus -> if (ausdruck.argument == variable) Sinus(variable) else error("Substitution ist noch nicht registriert.")
        is Exponentialfunktion -> if (ausdruck.argument == variable) ausdruck else error("Substitution ist noch nicht registriert.")
        else -> error("Für ${ausdruck::class.simpleName} ist keine Integrationsregel registriert.")
    }
    val ergebnis = vereinfache(roh)
    return UmformungsErgebnis(ergebnis, listOf(UmformungsSchritt(ausdruck, ergebnis, "analysis.integrieren", "Integrieren", "Eine Stammfunktion bezüglich ${variable.name} wurde bestimmt.")))
}

data class LösungsErgebnis(
    val lösungen: List<ZahlAusdruck>,
    val bedingungen: List<Aussage>,
    val schritte: List<UmformungsSchritt>,
)

fun löseLinear(gleichung: Gleichheit, variable: Variable): LösungsErgebnis {
    val links = gleichung.links as? ZahlAusdruck ?: error("Linke Seite ist kein Zahlterm.")
    val rechts = gleichung.rechts as? ZahlAusdruck ?: error("Rechte Seite ist kein Zahlterm.")
    fun koeffizienten(term: ZahlAusdruck): Pair<RationaleZahl, RationaleZahl> = when (val t = vereinfache(term)) {
        is RationaleZahl -> RationaleZahl.Null to t
        is Variable -> if (t == variable) RationaleZahl.Eins to RationaleZahl.Null else error("Weitere Variable ${t.name}")
        is Addition -> t.summanden.map(::koeffizienten).fold(RationaleZahl.Null to RationaleZahl.Null) { acc, p -> (acc.first + p.first) to (acc.second + p.second) }
        is Multiplikation -> {
            val zahl = t.faktoren.filterIsInstance<RationaleZahl>().fold(RationaleZahl.Eins) { a, b -> a * b }
            val vars = t.faktoren.filterIsInstance<Variable>()
            if (vars.size == 1 && vars.single() == variable && t.faktoren.all { it is RationaleZahl || it == variable }) zahl to RationaleZahl.Null else error("Nichtlinearer Term")
        }
        else -> error("Nichtlinearer Term")
    }
    val (al, bl) = koeffizienten(links)
    val (ar, br) = koeffizienten(rechts)
    val a = al - ar
    val b = br - bl
    if (a.istNull()) {
        return if (b.istNull()) LösungsErgebnis(emptyList(), emptyList(), emptyList()) else LösungsErgebnis(emptyList(), emptyList(), emptyList())
    }
    val lösung = b / a
    val nachher = Gleichheit(variable, lösung)
    return LösungsErgebnis(listOf(lösung), emptyList(), listOf(UmformungsSchritt(gleichung, nachher, "algebra.linear-lösen", "Lineare Gleichung lösen", "Variablenterme und Konstanten wurden getrennt.")))
}
