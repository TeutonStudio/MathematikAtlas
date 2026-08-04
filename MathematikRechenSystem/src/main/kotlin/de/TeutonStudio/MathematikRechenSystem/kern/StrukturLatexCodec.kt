package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Kanonische vollständige LaTeX-Darstellung strukturierter mathematischer Werte.
 *
 * Der Codec verändert ausschließlich die Darstellung. Insbesondere bleiben Tupel,
 * Vektoren und Matrizen fachlich verschiedene Objekte, auch wenn ihre sichtbare
 * `pmatrix`-Form übereinstimmen kann.
 */
fun MathematischesObjekt.zuStrukturLatex(): String = when (this) {
    is Tupel -> elemente.joinToString(
        prefix = "\\begin{pmatrix}",
        separator = " & ",
        postfix = "\\end{pmatrix}",
    ) { element -> element.zuStrukturLatex() }
    else -> zuLatex()
}
