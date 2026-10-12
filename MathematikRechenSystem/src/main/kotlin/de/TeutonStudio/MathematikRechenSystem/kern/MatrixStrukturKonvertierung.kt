package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Identifiziert eine zweiachsige Tensorstruktur formtreu mit einer Matrix.
 * Die Achsenreihenfolge und die Komponenten bleiben unverändert.
 */
fun matrixAusZweiachsigerStruktur(quelle: MathematischesObjekt): Matrix {
    if (quelle is Matrix) return quelle
    val tensor = quelle as? Tensorartig
        ?: throw IllegalArgumentException("Eine Matrixstruktur mit zwei Achsen wird benötigt.")
    require(tensor.tensorForm.size == 2) {
        "Eine Matrixstruktur benötigt genau zwei Achsen."
    }
    return Matrix(List(tensor.tensorForm[0]) { zeile ->
        List(tensor.tensorForm[1]) { spalte ->
            tensor.tensorKomponente(listOf(zeile, spalte))
        }
    })
}
