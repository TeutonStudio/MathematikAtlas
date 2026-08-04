package de.TeutonStudio.MathematikRechenSystem.kern

/** Ein einzelner geordneter Summand aᵢₖ·bₖⱼ des Matrixprodukts. */
data class GeordneterProduktSummand(
    val index: Int,
    val linkerFaktor: ZahlAusdruck,
    val rechterFaktor: ZahlAusdruck,
) {
    /** Die Reihenfolge ist absichtlich unveränderlich und wird nicht vereinfacht oder sortiert. */
    fun zuLatex(): String = "${linkerFaktor.zuLatex()}\\cdot ${rechterFaktor.zuLatex()}"
}

/** Detailansicht des bereits im Tensorrechner vorhandenen Falk-Schema-Vertrags. */
data class DetailliertesFalkSchemaModell(
    val linkerFaktor: Matrix,
    val rechterFaktor: Matrix,
    val ergebnis: Matrix,
    val zeilenIndex: Int,
    val spaltenIndex: Int,
    val summanden: List<GeordneterProduktSummand>,
) {
    val ergebnisEintrag: ZahlAusdruck get() = ergebnis.zeilen[zeilenIndex][spaltenIndex]

    fun summenLatex(): String = summanden.joinToString(" + ") { it.zuLatex() }
}

sealed interface DetailliertesFalkSchemaErgebnis {
    data class Gültig(val modell: DetailliertesFalkSchemaModell) : DetailliertesFalkSchemaErgebnis
    data class Inkompatibel(
        val linkeSpalten: Int,
        val rechteZeilen: Int,
    ) : DetailliertesFalkSchemaErgebnis {
        val meldung: String
            get() = "Die Matrizen sind nicht multiplizierbar: Die linke Matrix besitzt $linkeSpalten Spalten, die rechte $rechteZeilen Zeilen."
    }
}

/**
 * Erzeugt die faktorhaltige Detailansicht direkt aus der vorhandenen Matrixproduktsemantik.
 * Der allgemeine Formvertrag bleibt `falkSchema` im Tensorrechner; dieser Zusatz liefert
 * die tatsächlichen geordneten Summanden für Darstellung und Erklärung.
 */
fun detailliertesFalkSchema(
    links: Matrix,
    rechts: Matrix,
    zeilenIndex: Int = 0,
    spaltenIndex: Int = 0,
): DetailliertesFalkSchemaErgebnis {
    if (links.spaltenAnzahl != rechts.zeilenAnzahl) {
        return DetailliertesFalkSchemaErgebnis.Inkompatibel(links.spaltenAnzahl, rechts.zeilenAnzahl)
    }
    require(zeilenIndex in 0 until links.zeilenAnzahl) {
        "Der ausgewählte Zeilenindex liegt außerhalb der Ergebnismatrix."
    }
    require(spaltenIndex in 0 until rechts.spaltenAnzahl) {
        "Der ausgewählte Spaltenindex liegt außerhalb der Ergebnismatrix."
    }
    val summanden = List(links.spaltenAnzahl) { index ->
        GeordneterProduktSummand(
            index = index,
            linkerFaktor = links.zeilen[zeilenIndex][index],
            rechterFaktor = rechts.zeilen[index][spaltenIndex],
        )
    }
    return DetailliertesFalkSchemaErgebnis.Gültig(
        DetailliertesFalkSchemaModell(
            linkerFaktor = links,
            rechterFaktor = rechts,
            ergebnis = links * rechts,
            zeilenIndex = zeilenIndex,
            spaltenIndex = spaltenIndex,
            summanden = summanden,
        ),
    )
}
