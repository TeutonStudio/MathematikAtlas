package de.TeutonStudio.MathematikRechenSystem.kern

/** Dicht gespeicherter Tensor mit zeilenmajorer Indexabbildung. */
data class Tensor(
    val dimensionen: List<Int>,
    val werte: List<ZahlAusdruck>,
) : Ausdruck, Tensorartig {
    init {
        require(dimensionen.isNotEmpty()) { "Ein Tensor benötigt mindestens eine Achse." }
        require(dimensionen.all { it > 0 }) { "Tensordimensionen müssen positiv sein." }
        require(werte.size == dimensionen.faltungsProdukt()) {
            "Die Anzahl der Tensorwerte muss dem Produkt der Dimensionen entsprechen."
        }
    }

    val rang: Int get() = dimensionen.size
    override val tensorForm: List<Int> get() = dimensionen
    override val tensorZahlBereich: MengenAusdruck
        get() = maximaleZahlenGrundmenge(werte.map(::inferiereZahlenWertevorrat))
    override fun tensorKomponente(indizes: List<Int>): ZahlAusdruck = wertAn(indizes)

    fun wertAn(indizes: List<Int>): ZahlAusdruck = werte[linearerIndex(indizes, dimensionen)]

    fun permutiereAchsen(permutation: List<Int>): Tensor {
        prüfePermutation(permutation, rang)
        val neueDimensionen = permutation.map(dimensionen::get)
        val neueWerte = List(werte.size) { neuerLinearerIndex ->
            val neueIndizes = mehrdimensionaleIndizes(neuerLinearerIndex, neueDimensionen)
            val alteIndizes = MutableList(rang) { 0 }
            permutation.forEachIndexed { neueAchse, alteAchse ->
                alteIndizes[alteAchse] = neueIndizes[neueAchse]
            }
            wertAn(alteIndizes)
        }
        return Tensor(neueDimensionen, neueWerte)
    }

    override fun zuLatex(): String = when (rang) {
        1 -> werte.map(::listOf).zuTensorPmatrixLatex()
        2 -> List(dimensionen[0]) { zeile ->
            List(dimensionen[1]) { spalte -> wertAn(listOf(zeile, spalte)) }
        }.zuTensorPmatrixLatex()
        else -> "\\mathcal{T}_{${dimensionen.joinToString("\\times")}}"
    }
}

fun standardTensorPermutation(rang: Int): List<Int> {
    require(rang > 0) { "Ein Tensor benötigt mindestens eine Achse." }
    return if (rang == 1) listOf(0) else listOf(1, 0) + (2 until rang)
}

/** Liest eine vollständige, nullbasierte Achsenpermutation oder liefert bei ungültiger Eingabe `null`. */
fun parseTensorPermutationOderNull(text: String?, rang: Int): List<Int>? {
    if (rang <= 0 || text.isNullOrBlank()) return null
    val teile = text.split(',').map(String::trim)
    if (teile.size != rang || teile.any(String::isBlank)) return null
    val gelesen = teile.map { it.toIntOrNull() ?: return null }
    return gelesen.takeIf { runCatching { prüfePermutation(it, rang) }.isSuccess }
}

fun parseTensorPermutation(text: String?, rang: Int): List<Int> =
    parseTensorPermutationOderNull(text, rang) ?: standardTensorPermutation(rang)

fun prüfePermutation(permutation: List<Int>, rang: Int) {
    require(permutation.size == rang && permutation.toSet() == (0 until rang).toSet()) {
        "Die Achsenpermutation muss jede Achse genau einmal enthalten."
    }
}

private fun List<Int>.faltungsProdukt(): Int = fold(1) { produkt, faktor -> Math.multiplyExact(produkt, faktor) }

private fun linearerIndex(indizes: List<Int>, dimensionen: List<Int>): Int {
    require(indizes.size == dimensionen.size) { "Die Indexanzahl muss dem Tensorrang entsprechen." }
    var linear = 0
    indizes.zip(dimensionen).forEach { (index, dimension) ->
        require(index in 0 until dimension) { "Tensorindex $index liegt außerhalb der Dimension $dimension." }
        linear = linear * dimension + index
    }
    return linear
}

private fun mehrdimensionaleIndizes(linear: Int, dimensionen: List<Int>): List<Int> {
    var rest = linear
    val indizes = MutableList(dimensionen.size) { 0 }
    for (achse in dimensionen.indices.reversed()) {
        indizes[achse] = rest % dimensionen[achse]
        rest /= dimensionen[achse]
    }
    return indizes
}

private fun List<List<ZahlAusdruck>>.zuTensorPmatrixLatex(): String = joinToString(
    prefix = "\\begin{pmatrix}",
    separator = " \\\\ ",
    postfix = "\\end{pmatrix}",
) { zeile -> zeile.joinToString(" & ") { it.zuLatex() } }
