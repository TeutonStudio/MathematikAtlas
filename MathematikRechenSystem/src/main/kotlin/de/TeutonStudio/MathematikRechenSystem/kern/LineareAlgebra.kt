package de.TeutonStudio.MathematikRechenSystem.kern

sealed interface OrientierterVektor : Ausdruck {
    val werte: List<ZahlAusdruck>
    fun skalarprodukt(andere: OrientierterVektor): ZahlAusdruck {
        require(this::class == andere::class) { "Das Skalarprodukt benötigt zwei gleich orientierte Vektoren." }
        require(werte.size == andere.werte.size) { "Vektoren müssen dieselbe Dimension haben." }
        return addition(werte.zip(andere.werte) { a, b -> multiplikation(a, b) })
    }
    fun radius(): ZahlAusdruck = wurzel(addition(werte.map { Potenz(it, RationaleZahl.von(2)) }))
}

data class SpaltenVektor(override val werte: List<ZahlAusdruck>) : OrientierterVektor {
    init { require(werte.isNotEmpty()) }
    override fun zuLatex() = werte.map(::listOf).zuPmatrixLatex()
    fun transponiert() = ZeilenVektor(werte)
}

data class ZeilenVektor(override val werte: List<ZahlAusdruck>) : OrientierterVektor {
    init { require(werte.isNotEmpty()) }
    override fun zuLatex() = listOf(werte).zuPmatrixLatex()
    fun transponiert() = SpaltenVektor(werte)
}

// V2_2_MERGE_NOTE: Punktkoordinaten bleiben Tupel. v2.2 ergänzt ausschließlich explizite Tupel↔Spalte-
// Konvertierungen und Matrix-mal-Spaltenvektor; die bestehende Zeilen-/Spaltenorientierung darf beim Merge nicht vereinheitlicht werden.

/** Bildet die aufsteigend gespeicherten Koeffizienten c₀, …, cₙ auf Σ cᵢ·Xⁱ ab. */
fun polynomAusKoeffizienten(koeffizienten: List<ZahlAusdruck>, variable: Variable): ZahlAusdruck {
    require(koeffizienten.isNotEmpty()) { "Ein Polynom benötigt mindestens einen Koeffizienten." }
    require(variable.name.isNotBlank()) { "Die Polynomvariable darf nicht leer sein." }
    require(koeffizienten.none { it.enthältVariable(variable) }) {
        "Koeffizienten dürfen die Polynomvariable '${variable.name}' nicht enthalten."
    }
    val terme = koeffizienten.mapIndexed { index, koeffizient ->
        when (index) {
            0 -> koeffizient
            1 -> multiplikation(koeffizient, variable)
            else -> multiplikation(koeffizient, Potenz(variable, RationaleZahl.von(index.toLong())))
        }
    }
    return vereinfache(addition(terme.reversed()))
}

/** Rückwärtskompatible Bezeichnung: ein ungerichteter Altvektor wird als Spaltenvektor geführt. */
typealias Vektor = SpaltenVektor

fun einheitsSpaltenVektor(dimension: Int, index: Int): SpaltenVektor = SpaltenVektor(einheitsWerte(dimension, index))
fun einheitsZeilenVektor(dimension: Int, index: Int): ZeilenVektor = ZeilenVektor(einheitsWerte(dimension, index))
private fun einheitsWerte(dimension: Int, index: Int): List<ZahlAusdruck> {
    require(dimension > 0 && index in 1..dimension) { "Index des Einheitsvektors muss zwischen 1 und der Dimension liegen." }
    return List(dimension) { if (it == index - 1) RationaleZahl.Eins else RationaleZahl.Null }
}

fun kreuzprodukt(a: OrientierterVektor, b: OrientierterVektor): OrientierterVektor {
    require(a::class == b::class && a.werte.size == 3 && b.werte.size == 3) { "Das Kreuzprodukt benötigt gleich orientierte 3-Vektoren." }
    val w = listOf(
        subtraktion(multiplikation(a.werte[1], b.werte[2]), multiplikation(a.werte[2], b.werte[1])),
        subtraktion(multiplikation(a.werte[2], b.werte[0]), multiplikation(a.werte[0], b.werte[2])),
        subtraktion(multiplikation(a.werte[0], b.werte[1]), multiplikation(a.werte[1], b.werte[0])),
    )
    return if (a is ZeilenVektor) ZeilenVektor(w) else SpaltenVektor(w)
}

data class Matrix(val zeilen: List<List<ZahlAusdruck>>) : Ausdruck {
    init {
        require(zeilen.isNotEmpty() && zeilen.first().isNotEmpty())
        require(zeilen.all { it.size == zeilen.first().size })
    }
    val zeilenAnzahl get() = zeilen.size
    val spaltenAnzahl get() = zeilen.first().size
    override fun zuLatex() = zeilen.zuPmatrixLatex()
    operator fun plus(andere: Matrix): Matrix {
        require(zeilenAnzahl == andere.zeilenAnzahl && spaltenAnzahl == andere.spaltenAnzahl)
        return Matrix(zeilen.indices.map { z -> zeilen[z].indices.map { s -> addition(zeilen[z][s], andere.zeilen[z][s]) } })
    }
    operator fun times(andere: Matrix): Matrix {
        require(spaltenAnzahl == andere.zeilenAnzahl) { "Spaltenzahl der linken Matrix muss zur Zeilenzahl der rechten passen." }
        return Matrix(List(zeilenAnzahl) { z -> List(andere.spaltenAnzahl) { s -> addition((0 until spaltenAnzahl).map { k -> multiplikation(zeilen[z][k], andere.zeilen[k][s]) }) } })
    }
    fun transponiert() = Matrix(List(spaltenAnzahl) { s -> List(zeilenAnzahl) { z -> zeilen[z][s] } })
    fun inverseRational(): Matrix {
        require(zeilenAnzahl == spaltenAnzahl) { "Nur quadratische Matrizen sind invertierbar." }
        val n = zeilenAnzahl
        val a = Array(n) { z -> Array(2 * n) { s -> when { s < n -> vereinfache(zeilen[z][s]) as? RationaleZahl ?: error("Die Matrix muss rational auswertbar sein."); s - n == z -> RationaleZahl.Eins; else -> RationaleZahl.Null } } }
        for (spalte in 0 until n) {
            val pivot = (spalte until n).firstOrNull { !a[it][spalte].istNull() } ?: error("Die Matrix ist singulär.")
            if (pivot != spalte) { val tmp = a[pivot]; a[pivot] = a[spalte]; a[spalte] = tmp }
            val p = a[spalte][spalte]
            for (s in 0 until 2 * n) a[spalte][s] = a[spalte][s] / p
            for (z in 0 until n) if (z != spalte) { val faktor = a[z][spalte]; for (s in 0 until 2 * n) a[z][s] = a[z][s] - faktor * a[spalte][s] }
        }
        return Matrix(List(n) { z -> List(n) { s -> a[z][s + n] } })
    }
}

/** Formatiert Zeilen als LaTeX-`pmatrix`; `\\` trennt die Zeilen, `&` die Spalten. */
private fun List<List<ZahlAusdruck>>.zuPmatrixLatex(): String = joinToString(
    prefix = "\\begin{pmatrix}",
    separator = " \\\\ ",
    postfix = "\\end{pmatrix}",
) { zeile -> zeile.joinToString(" & ") { it.zuLatex() } }