package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Baut aus einem Tupel oder einem orientierten Vektor eine quadratische
 * Diagonalmatrix. Die Reihenfolge der Komponenten bleibt erhalten.
 */
fun diagonalmatrixErzeugen(argument: MathematischesObjekt): Matrix {
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
            if (zeile == spalte) komponenten[zeile] else RationaleZahl.Null
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
    val werte = List(matrix.zeilenAnzahl) { i -> matrix.zeilen[i][i] }
    return when (ausgabe) {
        "tupel" -> Tupel(werte)
        "zeile" -> ZeilenVektor(werte)
        "spalte" -> SpaltenVektor(werte)
        else -> throw IllegalArgumentException("Ausgabe muss Tupel, Zeile oder Spalte sein.")
    }
}

fun istDiagonalmatrix(matrix: Matrix): Boolean =
    matrix.zeilenAnzahl == matrix.spaltenAnzahl &&
        matrix.zeilen.indices.all { i ->
            matrix.zeilen[i].indices.all { j ->
                i == j || vereinfache(matrix.zeilen[i][j]) == RationaleZahl.Null
            }
        }
