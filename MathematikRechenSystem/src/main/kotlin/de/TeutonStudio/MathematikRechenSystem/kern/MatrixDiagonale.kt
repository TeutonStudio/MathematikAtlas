package de.TeutonStudio.MathematikRechenSystem.kern

/** Auswahl der aus einer Matrix gelesenen Diagonale. */
enum class MatrixDiagonalArt(val parameterWert: String) {
    HAUPTDIAGONALE("hauptdiagonale"),
    NEBENDIAGONALE("nebendiagonale"),
    ;

    companion object {
        fun ausParameterOderNull(wert: String?): MatrixDiagonalArt? =
            entries.firstOrNull { it.parameterWert == wert }

        fun vonParameter(wert: String?): MatrixDiagonalArt =
            ausParameterOderNull(wert) ?: HAUPTDIAGONALE
    }
}

/**
 * Liest eine Diagonale aus einem Tensor zweiter Stufe, ohne die Matrix zu kopieren.
 *
 * Die Nebendiagonale ist auch bei rechteckigen Matrizen rechts oben verankert:
 * für eine m×n-Matrix werden (0,n-1), (1,n-2), … gelesen.
 */
fun matrixDiagonale(
    matrix: Tensorartig,
    art: MatrixDiagonalArt,
): Tupel {
    require(matrix.tensorStufe == 2) {
        "Der Eingang ist keine Matrix; erwartet wird ein Tensor zweiter Stufe."
    }
    val (zeilen, spalten) = matrix.tensorForm
    val laenge = minOf(zeilen, spalten)
    val elemente = List(laenge) { zeile ->
        val spalte = when (art) {
            MatrixDiagonalArt.HAUPTDIAGONALE -> zeile
            MatrixDiagonalArt.NEBENDIAGONALE -> spalten - 1 - zeile
        }
        matrix.tensorKomponente(listOf(zeile, spalte))
    }
    return Tupel(elemente)
}
