package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Symbolischer Ausdruck einer Fallunterscheidung.
 *
 * Solange [aussage] nicht entschieden werden kann, bleiben beide Zweige erhalten.
 * Nach dem Binden einer Methode kann der Ausdruck auf den zutreffenden Zweig
 * reduziert werden.
 */
data class FallAusdruck(
    val wahr: MathematischesObjekt,
    val aussage: Aussage,
    val lüge: MathematischesObjekt,
) : Ausdruck {
    override fun zuLatex(): String =
        "\\begin{cases}${wahr.zuLatex()} & \\text{falls } ${aussage.zuLatex()} \\\\ ${lüge.zuLatex()} & \\text{sonst}\\end{cases}"
}
