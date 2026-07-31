package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Allgemeines gebundenes Symbol für Anschlussarten ohne eigenes symbolisches
 * CAS-Interface. Die konkrete Anschlussart bleibt als Metadatum erhalten.
 */
data class TypisiertesElement(
    override val name: String,
    val anschlussArt: String,
    val latex: String = name,
) : FunktionsParameter {
    init {
        require(name.isNotBlank()) { "Der Name eines gebundenen Elements darf nicht leer sein." }
        require(anschlussArt.isNotBlank()) { "Ein gebundenes Element benötigt eine Anschlussart." }
    }

    override fun zuLatex(): String = latex
}

/** Symbolische Aussagevariable, die selbst als Prädikat verwendet werden kann. */
data class AussagenParameter(
    override val name: String,
    val latex: String = name,
) : Aussage, FunktionsParameter {
    init { require(name.isNotBlank()) { "Der Name einer Aussagevariable darf nicht leer sein." } }

    override fun entscheide(kontext: RechenKontext) = AussageErgebnis(
        wahrheitswert = null,
        status = EntscheidungsStatus.Unbekannt,
        begründung = "Die gebundene Aussage '$name' ist nicht belegt.",
    )

    override fun zuLatex(): String = latex
}

/** Symbolische Mengenvariable für Mengenoperationen innerhalb eines Prädikats. */
data class MengenParameter(
    override val name: String,
    val latex: String = name,
) : MengenAusdruck, FunktionsParameter {
    init { require(name.isNotBlank()) { "Der Name einer Mengenvariable darf nicht leer sein." } }
    override fun zuLatex(): String = latex
}

/**
 * Metadatum dafür, dass ein Mengenkonstruktor bewusst keine Obermenge festlegt.
 * Es darf nicht als mathematisch behauptete Universalmenge interpretiert werden.
 */
data class FehlendeObermenge(val anschlussArt: String) : MengenAusdruck {
    init { require(anschlussArt.isNotBlank()) }
    override fun zuLatex(): String = "\\operatorname{Obermenge\\ nicht\\ festgelegt}"
}

/** Mengendefinition ausschließlich durch ein gebundenes Element und ein Prädikat. */
data class PrädikatsMenge(
    val element: FunktionsParameter,
    val bedingung: Aussage,
) : MengenAusdruck {
    override fun zuLatex(): String =
        "\\left\\{${element.zuLatex()}\\mid ${bedingung.zuLatex()}\\right\\}"
}

/**
 * Erzeugt eine grundmengenfreie Prädikatsmenge und wertet eindeutig endliche
 * Spezialfälle symbolisch aus. Insbesondere werden Gleichheitsfälle wie
 * `{x | x ∈ R ∧ x = 2}` zur Einzelmenge `{2}` normalisiert.
 */
fun definierePrädikatsMenge(
    element: FunktionsParameter,
    bedingung: Aussage,
    kontext: RechenKontext = RechenKontext(),
): MengenAusdruck {
    if (element is AussagenParameter) {
        if (bedingung == element) return EndlicheMenge(setOf(WahrheitsKonstante(true)))
        if (bedingung is Negation && bedingung.aussage == element) {
            return EndlicheMenge(setOf(WahrheitsKonstante(false)))
        }
    }

    val lösungen = bedingung.disjunktiveFälle().map { fall ->
        löseEindeutigenFall(element, fall, kontext)
    }
    if (lösungen.any { it is FallLösung.Offen }) return PrädikatsMenge(element, bedingung)

    val elemente = lösungen.filterIsInstance<FallLösung.Gelöst>().map { it.element }.toSet()
    return if (elemente.isEmpty()) LeereMenge else EndlicheMenge(elemente)
}

private sealed interface FallLösung {
    data class Gelöst(val element: MathematischesObjekt) : FallLösung
    data object Verworfen : FallLösung
    data object Offen : FallLösung
}

private fun löseEindeutigenFall(
    element: FunktionsParameter,
    fall: Aussage,
    kontext: RechenKontext,
): FallLösung {
    val teile = fall.konjunktiveTeile()
    val kandidaten = teile.filterIsInstance<Gleichheit>()
        .mapNotNull { it.kandidatFür(element) }
        .distinct()
    if (kandidaten.size != 1) return FallLösung.Offen

    val kandidat = kandidaten.single()
    if (kandidat.freieFunktionsParameter().any { it.name == element.name }) return FallLösung.Offen

    val bindung = mapOf(element.name to kandidat)
    val ergebnisse = teile.map { teil -> ersetze(teil, bindung).entscheide(kontext) }
    return when {
        ergebnisse.any { it.wahrheitswert == Wahrheitswert.Lüge } -> FallLösung.Verworfen
        ergebnisse.all { it.wahrheitswert == Wahrheitswert.Wahr } -> FallLösung.Gelöst(kandidat)
        else -> FallLösung.Offen
    }
}

private fun Gleichheit.kandidatFür(element: FunktionsParameter): MathematischesObjekt? = when {
    links == element -> rechts
    rechts == element -> links
    else -> null
}

private fun Aussage.disjunktiveFälle(): List<Aussage> = when (this) {
    is Disjunktion -> aussagen.flatMap { it.disjunktiveFälle() }
    else -> listOf(this)
}

private fun Aussage.konjunktiveTeile(): List<Aussage> = when (this) {
    is Konjunktion -> aussagen.flatMap { it.konjunktiveTeile() }
    else -> listOf(this)
}
