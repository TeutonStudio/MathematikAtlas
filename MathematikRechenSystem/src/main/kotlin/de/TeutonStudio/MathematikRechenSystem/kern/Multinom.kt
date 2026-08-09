package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Geordnete Monomfolge zum Argument x:
 * M_n(x) = (x^k)_{0<=k<=n} = (1, x, x^2, ..., x^n).
 *
 * `dimension` bezeichnet bewusst den höchsten Exponenten und nicht die Anzahl
 * der Einträge. Die Ergebnisliste besitzt daher `dimension + 1` Elemente.
 */
fun multinomFolge(x: ZahlAusdruck, dimension: Int): List<ZahlAusdruck> {
    require(dimension >= 0) { "Die Multinomdimension muss nichtnegativ sein." }
    return List(dimension + 1) { exponent ->
        when (exponent) {
            0 -> RationaleZahl.Eins
            1 -> x
            else -> vereinfache(Potenz(x, RationaleZahl.von(exponent.toLong())))
        }
    }
}
