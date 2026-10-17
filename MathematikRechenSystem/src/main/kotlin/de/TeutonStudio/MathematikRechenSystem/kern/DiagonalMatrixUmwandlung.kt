package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Baut aus einem Tupel oder einem orientierten Vektor eine quadratische
 * Diagonalmatrix. Die Reihenfolge der Komponenten bleibt erhalten.
 */
fun diagonalmatrixErzeugen(argument: MathematischesObjekt, art: MatrixDiagonalArt = MatrixDiagonalArt.HAUPTDIAGONALE): Matrix {
    val komponenten: List<ZahlAusdruck> = when (argument) {
        is OrientierterVektor -> argument.werte
        is Tupel -> argument.elemente.mapIndexed { index, element ->
            element as? ZahlAusdruck
                ?: throw IllegalArgumentException("Tupelkomponente ${index + 1} ist keine Zahl.")
        }
        else -> throw IllegalArgumentException("Benötigt wird ein Tupel, eine Zeile oder eine Spalte.")
    }
    require(komponenten.isNotEmpty()) { "Die Diagonalmatrix benötigt mindestens einen Eintrag." }
    return Matrix(List(komponenten.size) { zeile ->
        List(komponenten.size) { spalte ->
            if (spalte == if (art == MatrixDiagonalArt.HAUPTDIAGONALE) zeile else komponenten.lastIndex-zeile) komponenten[zeile] else RationaleZahl.Null
        }
    })
}

/**
 * Extrahiert die Hauptdiagonale einer quadratischen Matrix.
 * Bei nichtdiagonalen Matrizen ist der Vorgang verlustbehaftet.
 */
fun diagonalmatrixAuslesen(matrix: Matrix, ausgabe: String = "tupel"): MathematischesObjekt {
    require(matrix.zeilenAnzahl == matrix.spaltenAnzahl) {
        "Die Hauptdiagonalumwandlung erfordert eine quadratische Matrix."
    }
    val form = VektorStrukturAusgabe.entries.singleOrNull { it.stabileId == ausgabe }
        ?: throw IllegalArgumentException("Ausgabe muss Tupel, Zeile oder Spalte sein.")
    return projiziereMatrixDiagonale(matrixDiagonale(matrix,MatrixDiagonalArt.HAUPTDIAGONALE),form)
}

fun istDiagonalmatrix(matrix: Matrix): Boolean =
    matrix.zeilenAnzahl == matrix.spaltenAnzahl &&
        matrix.zeilen.indices.all { i ->
            matrix.zeilen[i].indices.all { j ->
                i == j || vereinfache(matrix.zeilen[i][j]) == RationaleZahl.Null
            }
        }


fun projiziereMatrixDiagonale(tupel: Tupel, ausgabe: VektorStrukturAusgabe): MathematischesObjekt {
    val werte=tupel.elemente.mapIndexed {i,w -> w as? ZahlAusdruck ?: error("Diagonalwert ${i+1} ist keine Zahl.")}
    return when(ausgabe) {
        VektorStrukturAusgabe.TUPEL -> tupel
        VektorStrukturAusgabe.ZEILE -> ZeilenVektor(werte)
        VektorStrukturAusgabe.SPALTE -> SpaltenVektor(werte)
    }
}
