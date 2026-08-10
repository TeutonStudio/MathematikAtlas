package de.TeutonStudio.MathematikRechenSystem.kern

import java.math.BigInteger

enum class MatrixRechnerOperator(val stabileId: String) {
    ADDITION("matrix.addition"),
    SUBTRAKTION("matrix.subtraktion"),
    SKALARMULTIPLIKATION("matrix.skalarmultiplikation"),
    MATRIXPRODUKT("matrix.produkt"),
    HADAMARD_PRODUKT("matrix.hadamard"),
    MATRIX_VEKTOR_PRODUKT("matrix.vektorProdukt"),
    TRANSPONIEREN("matrix.transponieren"),
    INVERSE("matrix.inverse"),
    POTENZ("matrix.potenz"),
    DETERMINANTE("matrix.determinante"),
    SPUR("matrix.spur"),
    RANG("matrix.rang"),
    HAUPTDIAGONALE("matrix.hauptdiagonale"),
    NEBENDIAGONALE("matrix.nebendiagonale"),
    CHARAKTERISTISCHES_POLYNOM("matrix.charakteristischesPolynom"),
    MINIMALPOLYNOM("matrix.minimalpolynom"),
}

data class MatrixForm(val zeilen: Int, val spalten: Int) {
    init { require(zeilen > 0 && spalten > 0) }
    val quadratisch: Boolean get() = zeilen == spalten
    override fun toString(): String = "$zeilen×$spalten"
}

data class MatrixOperand(
    val rollenId: String,
    val matrix: Matrix,
    val zahlbereich: FundamentalerZahlbereich,
) {
    val form: MatrixForm get() = MatrixForm(matrix.zeilenAnzahl, matrix.spaltenAnzahl)
}

data class MatrixRechnerAnfrage(
    val operator: MatrixRechnerOperator,
    val matrizen: List<MatrixOperand>,
    val skalare: List<ZahlAusdruck> = emptyList(),
    val vektoren: List<OrientierterVektor> = emptyList(),
)

sealed interface MatrixRechnerErgebnis {
    data class MatrixWert(
        val wert: Matrix,
        val zahlbereich: FundamentalerZahlbereich,
        val bedingungen: List<Aussage> = emptyList(),
    ) : MatrixRechnerErgebnis

    data class VektorWert(
        val wert: OrientierterVektor,
        val zahlbereich: FundamentalerZahlbereich,
    ) : MatrixRechnerErgebnis

    data class ZahlWert(
        val wert: ZahlAusdruck,
        val zahlbereich: FundamentalerZahlbereich,
        val bedingungen: List<Aussage> = emptyList(),
    ) : MatrixRechnerErgebnis

    data class MethodeWert(
        val wert: Methode,
        val zahlbereich: FundamentalerZahlbereich,
        val bedingungen: List<Aussage> = emptyList(),
    ) : MatrixRechnerErgebnis

    data class TupelWert(val wert: Tupel) : MatrixRechnerErgebnis

    data class Bedingt(
        val latex: String,
        val ergebnisTyp: FormelTyp,
        val bedingungen: List<String>,
    ) : MatrixRechnerErgebnis

    data class Ungueltig(val code: String, val nachricht: String) : MatrixRechnerErgebnis
}

object MatrixRechner {
    const val KNOTEN_ART = "mathematik.matrixrechner"

    fun erzeuge(anfrage: MatrixRechnerAnfrage): MatrixRechnerErgebnis {
        val bereich = anfrage.matrizen.takeIf { it.isNotEmpty() }?.let {
            FundamentaleZahlbereiche.kleinsterGemeinsamerBereich(it.map(MatrixOperand::zahlbereich))
        } ?: FundamentalerZahlbereich.REELL

        return when (anfrage.operator) {
            MatrixRechnerOperator.ADDITION -> {
                if (anfrage.matrizen.size < 2) return anzahlFehler("Addition", "mindestens zwei Matrizen")
                gleicheForm(anfrage.matrizen)?.let { return it }
                MatrixRechnerErgebnis.MatrixWert(
                    anfrage.matrizen.map(MatrixOperand::matrix).reduce(Matrix::plus),
                    bereich,
                )
            }
            MatrixRechnerOperator.SUBTRAKTION -> {
                if (anfrage.matrizen.size != 2) return anzahlFehler("Subtraktion", "genau zwei Matrizen")
                gleicheForm(anfrage.matrizen)?.let { return it }
                MatrixRechnerErgebnis.MatrixWert(
                    Matrix(
                        List(anfrage.matrizen[0].form.zeilen) { z ->
                            List(anfrage.matrizen[0].form.spalten) { s ->
                                subtraktion(
                                    anfrage.matrizen[0].matrix.zeilen[z][s],
                                    anfrage.matrizen[1].matrix.zeilen[z][s],
                                )
                            }
                        },
                    ),
                    bereich,
                )
            }
            MatrixRechnerOperator.SKALARMULTIPLIKATION -> {
                if (anfrage.matrizen.size != 1 || anfrage.skalare.size != 1) {
                    return anzahlFehler("Skalarmultiplikation", "eine Matrix und einen Skalar")
                }
                MatrixRechnerErgebnis.MatrixWert(
                    anfrage.matrizen.single().matrix.mapEintraege { multiplikation(anfrage.skalare.single(), it) },
                    bereich,
                )
            }
            MatrixRechnerOperator.MATRIXPRODUKT -> {
                if (anfrage.matrizen.size != 2) return anzahlFehler("Matrixprodukt", "genau zwei Matrizen")
                val links = anfrage.matrizen[0]
                val rechts = anfrage.matrizen[1]
                if (links.form.spalten != rechts.form.zeilen) return MatrixRechnerErgebnis.Ungueltig(
                    "innere_dimension",
                    "Matrixprodukt ${links.form}·${rechts.form} ist nicht definiert; ${links.form.spalten} ≠ ${rechts.form.zeilen}.",
                )
                MatrixRechnerErgebnis.MatrixWert(links.matrix * rechts.matrix, bereich)
            }
            MatrixRechnerOperator.HADAMARD_PRODUKT -> {
                if (anfrage.matrizen.size != 2) return anzahlFehler("Hadamard-Produkt", "genau zwei Matrizen")
                gleicheForm(anfrage.matrizen)?.let { return it }
                val links = anfrage.matrizen[0].matrix
                val rechts = anfrage.matrizen[1].matrix
                MatrixRechnerErgebnis.MatrixWert(
                    Matrix(
                        List(links.zeilenAnzahl) { z ->
                            List(links.spaltenAnzahl) { s -> multiplikation(links.zeilen[z][s], rechts.zeilen[z][s]) }
                        },
                    ),
                    bereich,
                )
            }
            MatrixRechnerOperator.MATRIX_VEKTOR_PRODUKT -> {
                if (anfrage.matrizen.size != 1 || anfrage.vektoren.size != 1) {
                    return anzahlFehler("Matrix-Vektor-Produkt", "eine Matrix und einen Vektor")
                }
                val matrix = anfrage.matrizen.single().matrix
                val vektor = anfrage.vektoren.single()
                if (matrix.spaltenAnzahl != vektor.werte.size) return MatrixRechnerErgebnis.Ungueltig(
                    "vektordimension",
                    "Die Matrix besitzt ${matrix.spaltenAnzahl} Spalten, der Vektor aber Dimension ${vektor.werte.size}.",
                )
                MatrixRechnerErgebnis.VektorWert(matrix * SpaltenVektor(vektor.werte), bereich)
            }
            MatrixRechnerOperator.TRANSPONIEREN -> einMatrix(anfrage) { operand ->
                MatrixRechnerErgebnis.MatrixWert(operand.matrix.transponiert(), operand.zahlbereich)
            }
            MatrixRechnerOperator.INVERSE -> einMatrix(anfrage) { operand ->
                if (!operand.form.quadratisch) return@einMatrix quadratFehler("Inverse", operand.form)
                val determinant = determinant(operand.matrix)
                if (determinant == RationaleZahl.Null) return@einMatrix MatrixRechnerErgebnis.Ungueltig(
                    "singulaer",
                    "Eine Matrix mit Determinante 0 ist nicht invertierbar.",
                )
                val rational = operand.matrix.zeilen.flatten().all { vereinfache(it) is RationaleZahl }
                if (!rational) MatrixRechnerErgebnis.Bedingt(
                    "${operand.matrix.zuLatex()}^{-1}",
                    FormelTyp.MATRIX,
                    listOf("Determinante muss ungleich null sein; symbolische Inversion bleibt unausgewertet."),
                ) else runCatching { operand.matrix.inverseRational() }
                    .fold(
                        onSuccess = { MatrixRechnerErgebnis.MatrixWert(it, operand.zahlbereich, listOf(Ungleichheit(determinant, RationaleZahl.Null))) },
                        onFailure = { MatrixRechnerErgebnis.Ungueltig("inverse_nicht_berechenbar", it.message ?: "Inverse konnte nicht berechnet werden.") },
                    )
            }
            MatrixRechnerOperator.POTENZ -> einMatrix(anfrage) { operand ->
                if (!operand.form.quadratisch) return@einMatrix quadratFehler("Matrixpotenz", operand.form)
                val exponent = anfrage.skalare.singleOrNull() as? RationaleZahl
                    ?: return@einMatrix MatrixRechnerErgebnis.Ungueltig(
                        "exponent",
                        "Eine Matrixpotenz benötigt einen ganzzahligen Exponenten.",
                    )
                if (exponent.nenner != BigInteger.ONE || exponent.zähler.bitLength() > 31) {
                    return@einMatrix MatrixRechnerErgebnis.Ungueltig("exponent", "Der Exponent muss eine darstellbare ganze Zahl sein.")
                }
                matrixPotenz(operand, exponent.zähler.toInt())
            }
            MatrixRechnerOperator.DETERMINANTE -> einMatrix(anfrage) { operand ->
                if (!operand.form.quadratisch) quadratFehler("Determinante", operand.form)
                else MatrixRechnerErgebnis.ZahlWert(determinant(operand.matrix), operand.zahlbereich)
            }
            MatrixRechnerOperator.SPUR -> einMatrix(anfrage) { operand ->
                if (!operand.form.quadratisch) quadratFehler("Spur", operand.form)
                else MatrixRechnerErgebnis.ZahlWert(
                    addition((0 until operand.form.zeilen).map { operand.matrix.zeilen[it][it] }),
                    operand.zahlbereich,
                )
            }
            MatrixRechnerOperator.RANG -> einMatrix(anfrage) { operand ->
                val rationaleEintraege = operand.matrix.zeilen.map { zeile ->
                    zeile.map { vereinfache(it) as? RationaleZahl }
                }
                if (rationaleEintraege.flatten().any { it == null }) {
                    MatrixRechnerErgebnis.Bedingt(
                        "\\operatorname{rang}\\left(${operand.matrix.zuLatex()}\\right)",
                        FormelTyp.ZAHL,
                        listOf("Der symbolische Rang hängt von den Matrixeinträgen und möglichen Nullbedingungen ab."),
                    )
                } else {
                    @Suppress("UNCHECKED_CAST")
                    MatrixRechnerErgebnis.ZahlWert(
                        RationaleZahl.von(rangRational(rationaleEintraege as List<List<RationaleZahl>>).toLong()),
                        FundamentalerZahlbereich.NATUERLICH_MIT_NULL,
                    )
                }
            }
            MatrixRechnerOperator.HAUPTDIAGONALE -> einMatrix(anfrage) { operand ->
                val n = minOf(operand.form.zeilen, operand.form.spalten)
                MatrixRechnerErgebnis.TupelWert(Tupel(List(n) { index -> operand.matrix.zeilen[index][index] }))
            }
            MatrixRechnerOperator.NEBENDIAGONALE -> einMatrix(anfrage) { operand ->
                val n = minOf(operand.form.zeilen, operand.form.spalten)
                MatrixRechnerErgebnis.TupelWert(
                    Tupel(List(n) { index -> operand.matrix.zeilen[index][operand.form.spalten - 1 - index] }),
                )
            }
            MatrixRechnerOperator.CHARAKTERISTISCHES_POLYNOM -> einMatrix(anfrage) { operand ->
                if (!operand.form.quadratisch) return@einMatrix quadratFehler("Charakteristisches Polynom", operand.form)
                matrixPolynomMethodeOderBedingt(
                    operand = operand,
                    name = "\\chi_A",
                    berechnen = ::charakteristischesPolynom,
                )
            }
            MatrixRechnerOperator.MINIMALPOLYNOM -> einMatrix(anfrage) { operand ->
                if (!operand.form.quadratisch) return@einMatrix quadratFehler("Minimalpolynom", operand.form)
                matrixPolynomMethodeOderBedingt(
                    operand = operand,
                    name = "m_A",
                    berechnen = ::minimalPolynom,
                )
            }
        }
    }

    fun alsFormelAusdruck(
        id: String,
        operator: MatrixRechnerOperator,
        argumente: List<Pair<String, FormelAusdruck>>,
        ergebnisTyp: FormelTyp,
    ) = FormelAusdruck.Operation(
        id,
        operator.stabileId,
        argumente.mapIndexed { index, (rolle, ausdruck) -> FormelArgument(rolle, index, ausdruck) },
        ergebnisTyp,
    )

    private fun matrixPolynomMethodeOderBedingt(
        operand: MatrixOperand,
        name: String,
        berechnen: (Matrix) -> MatrixPolynom,
    ): MatrixRechnerErgebnis {
        if (operand.zahlbereich !in setOf(
                FundamentalerZahlbereich.NATUERLICH_POSITIV,
                FundamentalerZahlbereich.NATUERLICH_MIT_NULL,
                FundamentalerZahlbereich.GANZ,
                FundamentalerZahlbereich.RATIONAL,
            )
        ) {
            return MatrixRechnerErgebnis.Bedingt(
                "${name}(${operand.matrix.zuLatex()})",
                FormelTyp.METHODE,
                listOf("Die erste exakte Matrixpolynom-Auswertung benötigt rationale Matrixeinträge."),
            )
        }
        return runCatching { berechnen(operand.matrix) }.fold(
            onSuccess = { polynom ->
                val träger = operand.zahlbereich.alsMenge()
                val methode = Methode(
                    name = name,
                    parameter = listOf(polynom.variable),
                    vorschrift = polynom.alsAusdruck(),
                    zielMenge = träger,
                    werteVorräte = mapOf(polynom.variable.name to träger),
                )
                MatrixRechnerErgebnis.MethodeWert(methode, operand.zahlbereich)
            },
            onFailure = { fehler ->
                MatrixRechnerErgebnis.Ungueltig(
                    "matrixpolynom_nicht_berechenbar",
                    fehler.message ?: "Das Matrixpolynom konnte nicht exakt berechnet werden.",
                )
            },
        )
    }

    private fun matrixPotenz(operand: MatrixOperand, exponent: Int): MatrixRechnerErgebnis {
        if (exponent == 0) return MatrixRechnerErgebnis.MatrixWert(einheitsMatrix(operand.form.zeilen), operand.zahlbereich)
        if (exponent < 0) {
            val inverse = runCatching { operand.matrix.inverseRational() }.getOrElse {
                return MatrixRechnerErgebnis.Ungueltig("negative_potenz", "Negative Matrixpotenz benötigt eine invertierbare rationale Matrix.")
            }
            return matrixPotenz(operand.copy(matrix = inverse), -exponent)
        }
        var ergebnis = einheitsMatrix(operand.form.zeilen)
        repeat(exponent) { ergebnis *= operand.matrix }
        return MatrixRechnerErgebnis.MatrixWert(ergebnis, operand.zahlbereich)
    }

    private fun determinant(matrix: Matrix): ZahlAusdruck {
        require(matrix.zeilenAnzahl == matrix.spaltenAnzahl)
        val n = matrix.zeilenAnzahl
        if (n == 1) return matrix.zeilen[0][0]
        if (n == 2) return subtraktion(
            multiplikation(matrix.zeilen[0][0], matrix.zeilen[1][1]),
            multiplikation(matrix.zeilen[0][1], matrix.zeilen[1][0]),
        )
        return addition((0 until n).map { spalte ->
            val minor = Matrix(
                matrix.zeilen.drop(1).map { zeile -> zeile.filterIndexed { index, _ -> index != spalte } },
            )
            val term = multiplikation(matrix.zeilen[0][spalte], determinant(minor))
            if (spalte % 2 == 0) term else negation(term)
        })
    }

    private fun rangRational(zeilen: List<List<RationaleZahl>>): Int {
        val a = zeilen.map { it.toMutableList() }.toMutableList()
        var rang = 0
        var spalte = 0
        while (rang < a.size && spalte < a.first().size) {
            val pivot = (rang until a.size).firstOrNull { !a[it][spalte].istNull() }
            if (pivot == null) {
                spalte++
                continue
            }
            val tmp = a[pivot]; a[pivot] = a[rang]; a[rang] = tmp
            val pivotWert = a[rang][spalte]
            for (s in spalte until a[rang].size) a[rang][s] = a[rang][s] / pivotWert
            for (z in a.indices) if (z != rang) {
                val faktor = a[z][spalte]
                for (s in spalte until a[z].size) a[z][s] = a[z][s] - faktor * a[rang][s]
            }
            rang++
            spalte++
        }
        return rang
    }

    private fun gleicheForm(operanden: List<MatrixOperand>): MatrixRechnerErgebnis.Ungueltig? {
        val formen = operanden.map(MatrixOperand::form).distinct()
        return if (formen.size <= 1) null else MatrixRechnerErgebnis.Ungueltig(
            "formen_inkompatibel",
            "Die Matrixformen müssen übereinstimmen; erhalten: ${formen.joinToString()}.",
        )
    }

    private fun einMatrix(
        anfrage: MatrixRechnerAnfrage,
        block: (MatrixOperand) -> MatrixRechnerErgebnis,
    ): MatrixRechnerErgebnis = if (anfrage.matrizen.size != 1) {
        anzahlFehler(anfrage.operator.name, "genau eine Matrix")
    } else block(anfrage.matrizen.single())

    private fun quadratFehler(operation: String, form: MatrixForm) = MatrixRechnerErgebnis.Ungueltig(
        "matrix_nicht_quadratisch",
        "$operation benötigt eine quadratische Matrix; erhalten: $form.",
    )

    private fun anzahlFehler(operation: String, erwartet: String) = MatrixRechnerErgebnis.Ungueltig(
        "argumentanzahl",
        "$operation benötigt $erwartet.",
    )

    private fun Matrix.mapEintraege(transform: (ZahlAusdruck) -> ZahlAusdruck): Matrix =
        Matrix(zeilen.map { zeile -> zeile.map(transform) })

    private fun einheitsMatrix(dimension: Int): Matrix = Matrix(
        List(dimension) { z -> List(dimension) { s -> if (z == s) RationaleZahl.Eins else RationaleZahl.Null } },
    )
}

object MatrixRechnerMigration {
    val alteKnotenArten: Map<String, MatrixRechnerOperator> = mapOf(
        "mathematik.matrixaddition" to MatrixRechnerOperator.ADDITION,
        "mathematik.matrixprodukt" to MatrixRechnerOperator.MATRIXPRODUKT,
        "mathematik.transponieren" to MatrixRechnerOperator.TRANSPONIEREN,
        "mathematik.matrixinverse" to MatrixRechnerOperator.INVERSE,
        "mathematik.determinante" to MatrixRechnerOperator.DETERMINANTE,
        "mathematik.spur" to MatrixRechnerOperator.SPUR,
        "mathematik.matrixdiagonale.haupt" to MatrixRechnerOperator.HAUPTDIAGONALE,
        "mathematik.matrixdiagonale.neben" to MatrixRechnerOperator.NEBENDIAGONALE,
    )
}
