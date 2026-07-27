package de.TeutonStudio.MathematikRechenSystem.kern

sealed interface MathematischesObjekt {
    fun zuLatex(): String
}

/** Ein benannter, bei einer [Funktion] bindbarer Parameter. */
sealed interface FunktionsParameter : MathematischesObjekt {
    val name: String
}

sealed interface Ausdruck : MathematischesObjekt
sealed interface ZahlAusdruck : Ausdruck
sealed interface MengenAusdruck : Ausdruck

// V2_2_MERGE_NOTE: Geometrische Ausdrücke werden als eigener Zweig von Ausdruck ergänzt, niemals als MengenAusdruck.
// Beim Übernehmen des letzten v2.1.x-Stands neue Ausdrucksuntertypen additiv erhalten und keine Hierarchie automatisch zusammenführen.

interface MathematischesKonzept<T : MathematischesObjekt> {
    val artId: String
    fun passt(objekt: MathematischesObjekt): Boolean
    fun normalisiere(objekt: T, kontext: RechenKontext = RechenKontext()): T
}

data class RechenKontext(
    val annahmen: Set<Aussage> = emptySet(),
    val dezimalstellen: Int = 34,
)