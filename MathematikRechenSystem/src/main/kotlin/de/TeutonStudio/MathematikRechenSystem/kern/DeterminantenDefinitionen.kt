package de.TeutonStudio.MathematikRechenSystem.kern

/** Wiederverwendbarer Minor M_ij ohne zweite Determinantenimplementierung. */
fun matrixMinor(matrix: Matrix, zeile: Int, spalte: Int): Matrix {
    require(matrix.zeilenAnzahl == matrix.spaltenAnzahl) { "Minoren sind hier nur für quadratische Matrizen definiert." }
    require(zeile in 0 until matrix.zeilenAnzahl && spalte in 0 until matrix.spaltenAnzahl)
    require(matrix.zeilenAnzahl > 1) { "Eine 1×1-Matrix besitzt keinen nichtleeren Matrixminor." }
    return Matrix(
        matrix.zeilen
            .filterIndexed { index, _ -> index != zeile }
            .map { matrixZeile -> matrixZeile.filterIndexed { index, _ -> index != spalte } },
    )
}

/**
 * Determinantenwert über den produktiven Matrixrechner. Definitionskarten und
 * Erklärpfade erhalten dadurch exakt dieselbe Rechensemantik wie der Knoten.
 */
fun produktiveDeterminante(
    matrix: Matrix,
    zahlbereich: FundamentalerZahlbereich = FundamentalerZahlbereich.REELL,
): ZahlAusdruck {
    val ergebnis = MatrixRechner.erzeuge(
        MatrixRechnerAnfrage(
            operator = MatrixRechnerOperator.DETERMINANTE,
            matrizen = listOf(MatrixOperand("matrix", matrix, zahlbereich)),
        ),
    )
    return when (ergebnis) {
        is MatrixRechnerErgebnis.ZahlWert -> ergebnis.wert
        is MatrixRechnerErgebnis.Bedingt -> SymbolischerZahlAusdruck(ergebnis.latex)
        is MatrixRechnerErgebnis.Ungueltig -> error(ergebnis.nachricht)
        else -> error("Determinantenoperator lieferte unerwartet ${ergebnis::class.simpleName}.")
    }
}

/** Kofaktor C_ij=(-1)^(i+j) M_ij mit nullbasierten technischen Indizes. */
fun matrixKofaktor(
    matrix: Matrix,
    zeile: Int,
    spalte: Int,
    zahlbereich: FundamentalerZahlbereich = FundamentalerZahlbereich.REELL,
): ZahlAusdruck {
    val minorDeterminante = produktiveDeterminante(matrixMinor(matrix, zeile, spalte), zahlbereich)
    return if ((zeile + spalte) % 2 == 0) minorDeterminante else negation(minorDeterminante)
}

/** Laplace-Entwicklung nach einer wählbaren Zeile oder Spalte. */
fun laplaceDeterminante(
    matrix: Matrix,
    index: Int = 0,
    nachZeile: Boolean = true,
    zahlbereich: FundamentalerZahlbereich = FundamentalerZahlbereich.REELL,
): ZahlAusdruck {
    require(matrix.zeilenAnzahl == matrix.spaltenAnzahl)
    require(index in 0 until matrix.zeilenAnzahl)
    if (matrix.zeilenAnzahl == 1) return matrix.zeilen[0][0]
    val terme = (0 until matrix.zeilenAnzahl).map { laufIndex ->
        val zeile = if (nachZeile) index else laufIndex
        val spalte = if (nachZeile) laufIndex else index
        multiplikation(
            matrix.zeilen[zeile][spalte],
            matrixKofaktor(matrix, zeile, spalte, zahlbereich),
        )
    }
    return vereinfache(addition(terme))
}

/** Leibniz-/Permutationsformel, bewusst nur für kleine erklärbare Matrizen materialisiert. */
fun permutationsDeterminante(matrix: Matrix, maxDimension: Int = 7): ZahlAusdruck {
    require(matrix.zeilenAnzahl == matrix.spaltenAnzahl)
    val n = matrix.zeilenAnzahl
    require(n <= maxDimension) {
        "Die Permutationsdefinition wird für n>$maxDimension nur symbolisch angezeigt, nicht materialisiert."
    }
    if (n == 1) return matrix.zeilen[0][0]
    val terme = permutationVon(n).map { permutation ->
        val produkt = multiplikation(
            permutation.indices.map { zeile -> matrix.zeilen[zeile][permutation[zeile]] },
        )
        if (permutationsVorzeichen(permutation) > 0) produkt else negation(produkt)
    }
    return vereinfache(addition(terme))
}

data class DeterminantenErklaerung(
    val direkt: ZahlAusdruck,
    val laplaceZeile0: ZahlAusdruck,
    val permutation: ZahlAusdruck?,
    val geometrieZulaessig: Boolean,
)

fun erklaereDeterminante(
    matrix: Matrix,
    zahlbereich: FundamentalerZahlbereich = FundamentalerZahlbereich.REELL,
): DeterminantenErklaerung = DeterminantenErklaerung(
    direkt = produktiveDeterminante(matrix, zahlbereich),
    laplaceZeile0 = laplaceDeterminante(matrix, 0, true, zahlbereich),
    permutation = if (matrix.zeilenAnzahl <= 7) permutationsDeterminante(matrix) else null,
    geometrieZulaessig = zahlbereich in setOf(
        FundamentalerZahlbereich.NATUERLICH,
        FundamentalerZahlbereich.NATUERLICH_MIT_NULL,
        FundamentalerZahlbereich.GANZ,
        FundamentalerZahlbereich.RATIONAL,
        FundamentalerZahlbereich.REELL,
    ),
)

private fun permutationVon(n: Int): List<List<Int>> {
    fun baue(rest: List<Int>): List<List<Int>> = when (rest.size) {
        0 -> listOf(emptyList())
        1 -> listOf(rest)
        else -> rest.flatMap { kopf ->
            baue(rest - kopf).map { listOf(kopf) + it }
        }
    }
    return baue((0 until n).toList())
}

private fun permutationsVorzeichen(permutation: List<Int>): Int {
    var inversionen = 0
    permutation.indices.forEach { i ->
        for (j in i + 1 until permutation.size) {
            if (permutation[i] > permutation[j]) inversionen++
        }
    }
    return if (inversionen % 2 == 0) 1 else -1
}

/** Symbolischer Fallback für produktive Operatoren, deren Wert noch Bedingungen trägt. */
private data class SymbolischerZahlAusdruck(val latex: String) : ZahlAusdruck {
    override fun zuLatex(): String = latex
}
