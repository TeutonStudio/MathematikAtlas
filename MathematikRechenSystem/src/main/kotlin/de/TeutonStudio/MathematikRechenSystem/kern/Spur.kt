package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Berechnet die Spur einer quadratischen Matrix als Summe ihrer Hauptdiagonale.
 *
 * Die Implementierung verwendet bewusst dieselbe Diagonalextraktion und Addition wie die
 * zugehörige Definitionskarte. Zusätzliche Spureigenschaften werden daraus nicht abgeleitet.
 */
fun spur(matrix: Tensorartig): ZahlAusdruck {
    require(matrix.tensorStufe == 2) {
        "Der Eingang ist keine Matrix; erwartet wird ein Tensor zweiter Stufe."
    }
    val (zeilen, spalten) = matrix.tensorForm
    require(zeilen == spalten) {
        "Die Spur ist nur für quadratische Matrizen definiert; erhalten wurde ${zeilen}×${spalten}."
    }

    val diagonale = matrixDiagonale(matrix, MatrixDiagonalArt.HAUPTDIAGONALE)
        .elemente
        .mapIndexed { index, element ->
            element as? ZahlAusdruck
                ?: error("Der Hauptdiagonaleintrag ${index + 1} ist keine Zahl.")
        }
    return addition(diagonale)
}
