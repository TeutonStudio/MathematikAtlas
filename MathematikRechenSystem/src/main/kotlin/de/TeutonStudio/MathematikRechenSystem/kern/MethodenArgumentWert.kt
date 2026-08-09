package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Strukturelle Reflexionsansicht eines einzelnen Methodenarguments.
 *
 * Der Wert enthält bewusst die echte Parameterinstanz und ihren Wertevorrat. Damit
 * bleibt die Information beim Verdrahten erhalten und wird nicht auf einen bloßen
 * Anzeigestring reduziert.
 */
data class MethodenArgumentWert(
    val argument: MethodenArgument,
) : MathematischesObjekt {
    val parameter: MethodenParameter get() = argument.parameter
    val name: String get() = parameter.name
    val werteVorrat: MengenAusdruck get() = argument.werteVorrat

    override fun zuLatex(): String = "${parameter.zuLatex()} \\in ${werteVorrat.zuLatex()}"
}
