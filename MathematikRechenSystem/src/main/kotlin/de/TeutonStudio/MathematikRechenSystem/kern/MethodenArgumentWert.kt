package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Strukturelle mathematische Reflexionsansicht eines einzelnen Methodenarguments.
 *
 * Der Wert enthält bewusst die mathematische Komponente, nicht die neutrale
 * [MethodenKomponente]. Definitionsmengen sind eine Mathematik-Capability und keine
 * Eigenschaft beliebiger Script-/Engine-Argumente.
 */
data class MethodenArgumentWert(
    val argument: MathematischeArgumentKomponente,
) : MathematischesObjekt {
    val parameter: MethodenParameter get() = argument.parameter
    val name: String get() = argument.name
    val werteVorrat: MengenAusdruck get() = argument.definitionsMenge

    override fun zuLatex(): String = "${parameter.zuLatex()} \\in ${werteVorrat.zuLatex()}"
}
