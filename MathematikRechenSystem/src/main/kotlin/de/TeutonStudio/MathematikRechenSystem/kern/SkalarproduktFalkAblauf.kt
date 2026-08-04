package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Darstellungsmodell für das interaktive Falk-Schema eines Standardskalarprodukts.
 *
 * Es hält ausschließlich LaTeX-Fragmente und verändert weder die mathematischen
 * Eingaben noch die eigentliche Auswertung. Dadurch kann die Oberfläche jeden
 * Summanden und jede geordnete Teilsumme anzeigen, ohne eine zweite Rechenlogik
 * neben dem Rechenkern einzuführen.
 */
data class SkalarproduktFalkAblauf(
    val linkeKomponenten: List<String>,
    val rechteKomponenten: List<String>,
    val linearitaet: SkalarproduktLinearitaet,
    val konjugiert: Boolean,
) {
    init {
        require(linkeKomponenten.isNotEmpty()) {
            "Ein Falk-Schema benötigt mindestens eine Komponente."
        }
        require(linkeKomponenten.size == rechteKomponenten.size) {
            "Das Falk-Schema benötigt gleich lange Komponentenfolgen."
        }
    }

    val dimension: Int get() = linkeKomponenten.size

    fun produktLatex(index: Int): String {
        require(index in 0 until dimension) {
            "Der Falk-Index $index liegt außerhalb der Dimension $dimension."
        }
        val links = linkeKomponenten[index]
        val rechts = rechteKomponenten[index]
        return when (linearitaet) {
            SkalarproduktLinearitaet.RECHTSLINEAR -> {
                val ersterFaktor = if (konjugiert) "\\overline{$links}" else links
                "$ersterFaktor\\,$rechts"
            }
            SkalarproduktLinearitaet.LINKSLINEAR -> {
                val zweiterFaktor = if (konjugiert) "\\overline{$rechts}" else rechts
                "$links\\,$zweiterFaktor"
            }
        }
    }

    fun teilsummeLatex(bisIndex: Int): String {
        require(bisIndex in 0 until dimension) {
            "Der Falk-Index $bisIndex liegt außerhalb der Dimension $dimension."
        }
        return (0..bisIndex).joinToString("+") { produktLatex(it) }
    }

    fun vollständigeSummeLatex(): String = teilsummeLatex(dimension - 1)
}
