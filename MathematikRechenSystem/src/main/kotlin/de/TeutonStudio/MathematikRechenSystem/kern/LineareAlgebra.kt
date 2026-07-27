package de.TeutonStudio.MathematikRechenSystem.kern

data class Vektor(val werte: List<ZahlAusdruck>) : Ausdruck {
    init { require(werte.isNotEmpty()) }
    override fun zuLatex() = werte.joinToString(prefix = "\\begin{pmatrix}", separator = " \\ ", postfix = "\\end{pmatrix}") { it.zuLatex() }
    operator fun plus(andere: Vektor): Vektor {
        require(werte.size == andere.werte.size)
        return Vektor(werte.zip(andere.werte) { a, b -> addition(a, b) })
    }
    fun skalarprodukt(andere: Vektor): ZahlAusdruck {
        require(werte.size == andere.werte.size)
        return addition(werte.zip(andere.werte) { a, b -> multiplikation(a, b) })
    }
}

data class Matrix(val zeilen: List<List<ZahlAusdruck>>) : Ausdruck {
    init {
        require(zeilen.isNotEmpty() && zeilen.first().isNotEmpty())
        require(zeilen.all { it.size == zeilen.first().size })
    }
    val zeilenAnzahl get() = zeilen.size
    val spaltenAnzahl get() = zeilen.first().size
    override fun zuLatex() = zeilen.joinToString(prefix = "\\begin{pmatrix}", separator = " \\ ", postfix = "\\end{pmatrix}") { zeile -> zeile.joinToString(" & ") { it.zuLatex() } }

    operator fun plus(andere: Matrix): Matrix {
        require(zeilenAnzahl == andere.zeilenAnzahl && spaltenAnzahl == andere.spaltenAnzahl)
        return Matrix(zeilen.indices.map { z -> zeilen[z].indices.map { s -> addition(zeilen[z][s], andere.zeilen[z][s]) } })
    }

    operator fun times(andere: Matrix): Matrix {
        require(spaltenAnzahl == andere.zeilenAnzahl)
        return Matrix(List(zeilenAnzahl) { z ->
            List(andere.spaltenAnzahl) { s ->
                addition((0 until spaltenAnzahl).map { k -> multiplikation(zeilen[z][k], andere.zeilen[k][s]) })
            }
        })
    }

    fun transponiert() = Matrix(List(spaltenAnzahl) { s -> List(zeilenAnzahl) { z -> zeilen[z][s] } })

    fun inverseRational(): Matrix {
        require(zeilenAnzahl == spaltenAnzahl) { "Nur quadratische Matrizen sind invertierbar." }
        val n = zeilenAnzahl
        val a = Array(n) { z -> Array(2 * n) { s ->
            when {
                s < n -> vereinfache(zeilen[z][s]) as? RationaleZahl ?: error("Die Matrix muss rational auswertbar sein.")
                s - n == z -> RationaleZahl.Eins
                else -> RationaleZahl.Null
            }
        } }
        for (spalte in 0 until n) {
            val pivot = (spalte until n).firstOrNull { !a[it][spalte].istNull() } ?: error("Die Matrix ist singulär.")
            if (pivot != spalte) { val tmp = a[pivot]; a[pivot] = a[spalte]; a[spalte] = tmp }
            val p = a[spalte][spalte]
            for (s in 0 until 2 * n) a[spalte][s] = a[spalte][s] / p
            for (z in 0 until n) if (z != spalte) {
                val faktor = a[z][spalte]
                for (s in 0 until 2 * n) a[z][s] = a[z][s] - faktor * a[spalte][s]
            }
        }
        return Matrix(List(n) { z -> List(n) { s -> a[z][s + n] } })
    }
}
