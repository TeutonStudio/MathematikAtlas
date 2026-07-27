package de.TeutonStudio.MathematikRechenSystem.kern

sealed interface MathematischesObjekt {
    fun zuLatex(): String
}

sealed interface Ausdruck : MathematischesObjekt
sealed interface ZahlAusdruck : Ausdruck
sealed interface MengenAusdruck : Ausdruck

interface MathematischesKonzept<T : MathematischesObjekt> {
    val artId: String
    fun passt(objekt: MathematischesObjekt): Boolean
    fun normalisiere(objekt: T, kontext: RechenKontext = RechenKontext()): T
}

data class RechenKontext(
    val annahmen: Set<Aussage> = emptySet(),
    val dezimalstellen: Int = 34,
)
