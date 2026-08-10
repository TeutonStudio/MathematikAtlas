package de.TeutonStudio.MathematikRechenSystem.kern

/** Exaktes, aufsteigend koeffizientiertes Matrixpolynom über Q. */
data class MatrixPolynom(
    val variable: Variable,
    val koeffizienten: List<RationaleZahl>,
) {
    init {
        require(koeffizienten.isNotEmpty())
        require(!koeffizienten.last().istNull()) { "Das führende Polynomglied darf nicht 0 sein." }
    }

    val grad: Int get() = koeffizienten.lastIndex
    fun alsAusdruck(): ZahlAusdruck = polynomAusKoeffizienten(koeffizienten, variable)
    fun zuLatex(): String = alsAusdruck().zuLatex()
}

data class CayleyHamiltonNachweis(
    val charakteristischesPolynom: MatrixPolynom,
    val eingesetzt: Matrix,
    val gilt: Boolean,
) {
    val aussageLatex: String
        get() = "\\chi_A(A)=0"
}

fun charakteristischesPolynom(
    matrix: Matrix,
    variablenName: String = "lambda",
): MatrixPolynom {
    require(matrix.zeilenAnzahl == matrix.spaltenAnzahl) { "Das charakteristische Polynom benötigt eine quadratische Matrix." }
    requireRationaleMatrix(matrix)
    val n = matrix.zeilenAnzahl
    val werte = (0..n).map { x ->
        val lambda = RationaleZahl.von(x.toLong())
        val verschoben = Matrix(
            List(n) { z ->
                List(n) { s ->
                    val diagonal = if (z == s) lambda else RationaleZahl.Null
                    subtraktion(matrix.zeilen[z][s], diagonal)
                }
            },
        )
        val wert = vereinfache(produktiveDeterminante(verschoben, FundamentalerZahlbereich.RATIONAL))
        wert as? RationaleZahl
            ?: error("Die Determinante der rationalen Stützmatrix muss rational auswertbar sein.")
    }
    val vandermonde = (0..n).map { x ->
        val basis = RationaleZahl.von(x.toLong())
        List(n + 1) { potenz -> rationalePotenz(basis, potenz) }
    }
    val koeffizienten = loeseLinearesSystem(vandermonde, werte)
        ?: error("Das Vandermonde-System des charakteristischen Polynoms ist unerwartet singulär.")
    return MatrixPolynom(Variable(variablenName), trimFuehrendeNullen(koeffizienten))
}

fun minimalPolynom(
    matrix: Matrix,
    variablenName: String = "lambda",
): MatrixPolynom {
    require(matrix.zeilenAnzahl == matrix.spaltenAnzahl) { "Das Minimalpolynom benötigt eine quadratische Matrix." }
    val rational = requireRationaleMatrix(matrix)
    val n = matrix.zeilenAnzahl
    val potenzen = mutableListOf(einheitsMatrixRational(n))
    val maxGrad = n * n

    for (grad in 1..maxGrad) {
        potenzen += potenzen.last() * rational
        val links = List(n * n) { zeilenIndex ->
            List(grad) { spaltenIndex ->
                rationalEintrag(potenzen[spaltenIndex], zeilenIndex)
            }
        }
        val rechts = List(n * n) { index ->
            RationaleZahl.Null - rationalEintrag(potenzen[grad], index)
        }
        val loesung = loeseLinearesSystem(links, rechts) ?: continue
        val koeffizienten = loesung + RationaleZahl.Eins
        val polynom = MatrixPolynom(Variable(variablenName), koeffizienten)
        if (istNullMatrix(polynomInMatrixEinsetzen(polynom, rational))) return polynom
    }
    error("Für die ${n}×${n}-Matrix wurde innerhalb des Cayley-Hamilton-Budgets kein Minimalpolynom gefunden.")
}

fun polynomInMatrixEinsetzen(polynom: MatrixPolynom, matrix: Matrix): Matrix {
    require(matrix.zeilenAnzahl == matrix.spaltenAnzahl)
    val n = matrix.zeilenAnzahl
    var potenz = einheitsMatrixRational(n)
    var summe = nullMatrixRational(n)
    polynom.koeffizienten.forEachIndexed { index, koeffizient ->
        if (index > 0) potenz *= matrix
        summe = matrixAddiere(summe, matrixSkaliere(potenz, koeffizient))
    }
    return Matrix(summe.zeilen.map { zeile -> zeile.map(::vereinfache) })
}

fun pruefeCayleyHamilton(matrix: Matrix): CayleyHamiltonNachweis {
    val charakteristisch = charakteristischesPolynom(matrix)
    val eingesetzt = polynomInMatrixEinsetzen(charakteristisch, requireRationaleMatrix(matrix))
    return CayleyHamiltonNachweis(charakteristisch, eingesetzt, istNullMatrix(eingesetzt))
}

fun teiltMinimalpolynomDasCharakteristische(matrix: Matrix): Boolean {
    val minimal = minimalPolynom(matrix).koeffizienten
    var rest = charakteristischesPolynom(matrix).koeffizienten.toMutableList()
    while (rest.size >= minimal.size) {
        val faktor = rest.last() / minimal.last()
        val versatz = rest.size - minimal.size
        minimal.indices.forEach { index ->
            rest[index + versatz] = rest[index + versatz] - faktor * minimal[index]
        }
        while (rest.isNotEmpty() && rest.last().istNull()) rest.removeAt(rest.lastIndex)
    }
    return rest.all { it.istNull() }
}

private fun requireRationaleMatrix(matrix: Matrix): Matrix = Matrix(
    matrix.zeilen.mapIndexed { z, zeile ->
        zeile.mapIndexed { s, wert ->
            vereinfache(wert) as? RationaleZahl
                ?: error("Matrixeintrag (${z + 1},${s + 1}) ist nicht rational auswertbar.")
        }
    },
)

private fun einheitsMatrixRational(n: Int): Matrix = Matrix(
    List(n) { z -> List(n) { s -> if (z == s) RationaleZahl.Eins else RationaleZahl.Null } },
)

private fun nullMatrixRational(n: Int): Matrix = Matrix(List(n) { List(n) { RationaleZahl.Null } })

private fun matrixSkaliere(matrix: Matrix, faktor: RationaleZahl): Matrix = Matrix(
    matrix.zeilen.map { zeile -> zeile.map { multiplikation(faktor, it) } },
)

private fun matrixAddiere(links: Matrix, rechts: Matrix): Matrix = links + rechts

private fun rationalEintrag(matrix: Matrix, flachIndex: Int): RationaleZahl {
    val spalten = matrix.spaltenAnzahl
    val wert = vereinfache(matrix.zeilen[flachIndex / spalten][flachIndex % spalten])
    return wert as? RationaleZahl ?: error("Matrixpotenz ist nicht rational auswertbar.")
}

private fun istNullMatrix(matrix: Matrix): Boolean = matrix.zeilen.flatten().all { wert ->
    (vereinfache(wert) as? RationaleZahl)?.istNull() == true
}

private fun rationalePotenz(basis: RationaleZahl, exponent: Int): RationaleZahl {
    var wert = RationaleZahl.Eins
    repeat(exponent) { wert *= basis }
    return wert
}

private fun trimFuehrendeNullen(koeffizienten: List<RationaleZahl>): List<RationaleZahl> {
    var ende = koeffizienten.lastIndex
    while (ende > 0 && koeffizienten[ende].istNull()) ende--
    return koeffizienten.take(ende + 1)
}

private fun loeseLinearesSystem(
    matrix: List<List<RationaleZahl>>,
    rechteSeite: List<RationaleZahl>,
): List<RationaleZahl>? {
    require(matrix.size == rechteSeite.size)
    if (matrix.isEmpty()) return emptyList()
    val variablen = matrix.first().size
    require(matrix.all { it.size == variablen })
    val a = matrix.mapIndexed { zeile, werte ->
        (werte + rechteSeite[zeile]).toMutableList()
    }.toMutableList()
    val pivotZeilen = mutableMapOf<Int, Int>()
    var zeile = 0
    for (spalte in 0 until variablen) {
        val pivot = (zeile until a.size).firstOrNull { !a[it][spalte].istNull() } ?: continue
        val temp = a[zeile]; a[zeile] = a[pivot]; a[pivot] = temp
        val pivotWert = a[zeile][spalte]
        for (s in spalte..variablen) a[zeile][s] = a[zeile][s] / pivotWert
        for (z in a.indices) if (z != zeile) {
            val faktor = a[z][spalte]
            if (!faktor.istNull()) {
                for (s in spalte..variablen) a[z][s] = a[z][s] - faktor * a[zeile][s]
            }
        }
        pivotZeilen[spalte] = zeile
        zeile++
        if (zeile == a.size) break
    }
    if (a.any { row -> row.take(variablen).all { it.istNull() } && !row.last().istNull() }) return null
    return List(variablen) { spalte ->
        pivotZeilen[spalte]?.let { pivotZeile -> a[pivotZeile].last() } ?: RationaleZahl.Null
    }
}
