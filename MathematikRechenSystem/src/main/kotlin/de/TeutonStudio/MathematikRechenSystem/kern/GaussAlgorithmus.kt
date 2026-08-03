package de.TeutonStudio.MathematikRechenSystem.kern

/** Strukturierte Operation eines mathematischen Umformungsschritts. */
sealed interface StrukturOperation {
    val betroffeneZeilen: Set<Int>
    fun zuKurztext(): String
    fun zuLatex(): String
}

/** Elementare Zeilenoperation mit einsbasierten Zeilenindizes. */
sealed interface ZeilenOperation : StrukturOperation {
    data class Tausche(
        val ersteZeile: Int,
        val zweiteZeile: Int,
    ) : ZeilenOperation {
        init {
            require(ersteZeile > 0 && zweiteZeile > 0 && ersteZeile != zweiteZeile)
        }

        override val betroffeneZeilen: Set<Int> = setOf(ersteZeile, zweiteZeile)
        override fun zuKurztext(): String = "${roemischeZeile(ersteZeile)} ↔ ${roemischeZeile(zweiteZeile)}"
        override fun zuLatex(): String =
            "\\mathrm{${roemischeZeile(ersteZeile)}}\\leftrightarrow\\mathrm{${roemischeZeile(zweiteZeile)}}"
    }

    data class Skaliere(
        val zeile: Int,
        val faktor: RationaleZahl,
    ) : ZeilenOperation {
        init {
            require(zeile > 0 && !faktor.istNull())
        }

        override val betroffeneZeilen: Set<Int> = setOf(zeile)
        override fun zuKurztext(): String = "${roemischeZeile(zeile)} ← $faktor · ${roemischeZeile(zeile)}"
        override fun zuLatex(): String =
            "\\mathrm{${roemischeZeile(zeile)}}\\leftarrow${faktor.zuLatex()}\\mathrm{${roemischeZeile(zeile)}}"
    }

    data class Kombiniere(
        val zielZeile: Int,
        val zielFaktor: RationaleZahl,
        val quellZeile: Int,
        val quellFaktor: RationaleZahl,
    ) : ZeilenOperation {
        init {
            require(zielZeile > 0 && quellZeile > 0 && zielZeile != quellZeile)
            require(!zielFaktor.istNull() || !quellFaktor.istNull())
        }

        override val betroffeneZeilen: Set<Int> = setOf(zielZeile)
        override fun zuKurztext(): String =
            "${roemischeZeile(zielZeile)} ← $zielFaktor · ${roemischeZeile(zielZeile)} + " +
                "$quellFaktor · ${roemischeZeile(quellZeile)}"

        override fun zuLatex(): String =
            "\\mathrm{${roemischeZeile(zielZeile)}}\\leftarrow" +
                "${zielFaktor.zuLatex()}\\mathrm{${roemischeZeile(zielZeile)}}+" +
                "${quellFaktor.zuLatex()}\\mathrm{${roemischeZeile(quellZeile)}}"
    }
}

/** Stabile römische Bezeichnung für Tabellenzeilen. */
fun roemischeZeile(index: Int): String {
    require(index > 0)
    var rest = index
    val werte = listOf(
        1000 to "M", 900 to "CM", 500 to "D", 400 to "CD",
        100 to "C", 90 to "XC", 50 to "L", 40 to "XL",
        10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I",
    )
    return buildString {
        for ((wert, symbol) in werte) {
            while (rest >= wert) {
                append(symbol)
                rest -= wert
            }
        }
    }
}

enum class GaussZiel {
    ZEILENSTUFENFORM,
    REDUZIERTE_ZEILENSTUFENFORM,
}

data class UmformungsTabellenSpalte(
    val id: String,
    val titel: String,
    val istRechteSeite: Boolean = false,
)

data class UmformungsTabellenZeile(
    val index: Int,
    val name: String,
    val werte: List<ZahlAusdruck>,
    val operation: StrukturOperation? = null,
)

data class UmformungsTabellenBlock(
    val schritt: Int,
    val zeilen: List<UmformungsTabellenZeile>,
)

data class UmformungsTabelle(
    val spalten: List<UmformungsTabellenSpalte>,
    val bloecke: List<UmformungsTabellenBlock>,
) {
    init {
        require(spalten.isNotEmpty())
        require(bloecke.isNotEmpty())
        require(bloecke.all { block -> block.zeilen.all { it.werte.size == spalten.size } })
    }
}

data class UmformungsVerlauf(
    val eingabe: MathematischesObjekt,
    val schritte: List<UmformungsSchritt>,
    val ergebnis: MathematischesObjekt,
) {
    fun alsMatrixTabelle(
        spaltenNamen: List<String> = emptyList(),
        rechteSeitenSpalten: Int = 0,
    ): UmformungsTabelle {
        val start = eingabe as? Matrix ?: error("Eine Matrixtabelle benötigt eine Matrix als Eingabe.")
        val ende = ergebnis as? Matrix ?: error("Eine Matrixtabelle benötigt eine Matrix als Ergebnis.")
        require(start.spaltenAnzahl == ende.spaltenAnzahl)
        require(rechteSeitenSpalten in 0..start.spaltenAnzahl)
        val namen = if (spaltenNamen.isEmpty()) {
            List(start.spaltenAnzahl) { index ->
                val rhsStart = start.spaltenAnzahl - rechteSeitenSpalten
                if (index >= rhsStart) {
                    if (rechteSeitenSpalten == 1) "b" else "b_${index - rhsStart + 1}"
                } else "x_${index + 1}"
            }
        } else {
            require(spaltenNamen.size == start.spaltenAnzahl)
            spaltenNamen
        }
        val spalten = namen.mapIndexed { index, name ->
            UmformungsTabellenSpalte(
                id = "spalte.$index",
                titel = name,
                istRechteSeite = index >= start.spaltenAnzahl - rechteSeitenSpalten,
            )
        }
        val zustaende = buildList {
            add(start to null)
            schritte.forEach { schritt ->
                add((schritt.nachher as? Matrix ?: error("Gauß-Schritte müssen Matrizen enthalten.")) to schritt.strukturOperation)
            }
        }
        return UmformungsTabelle(
            spalten = spalten,
            bloecke = zustaende.mapIndexed { schritt, (matrix, operation) ->
                UmformungsTabellenBlock(
                    schritt = schritt,
                    zeilen = matrix.zeilen.mapIndexed { index, werte ->
                        val zeile = index + 1
                        UmformungsTabellenZeile(
                            index = zeile,
                            name = roemischeZeile(zeile),
                            werte = werte,
                            operation = operation?.takeIf { zeile in it.betroffeneZeilen },
                        )
                    },
                )
            },
        )
    }
}

data class GaussErgebnis(
    val matrix: Matrix,
    val pivotSpalten: List<Int>,
    val rang: Int,
    val schritte: List<UmformungsSchritt>,
    val verlauf: UmformungsVerlauf,
)

sealed interface LinearesSystemLoesung : MathematischesObjekt

data object KeineLineareLoesung : LinearesSystemLoesung {
    override fun zuLatex(): String = "\\varnothing"
}

data class EindeutigeLineareLoesung(
    val loesungsVektor: SpaltenVektor,
) : LinearesSystemLoesung {
    override fun zuLatex(): String = "x=${loesungsVektor.zuLatex()}"
}

data class ParametrischeLineareLoesung(
    val loesungsVektor: SpaltenVektor,
    val freieParameter: List<Variable>,
) : LinearesSystemLoesung {
    init {
        require(freieParameter.isNotEmpty())
    }

    override fun zuLatex(): String =
        "x=${loesungsVektor.zuLatex()},\\quad " +
            freieParameter.joinToString(",") { "${it.zuLatex()}\\in\\mathbb{Q}" }
}

data class LinearesSystemErgebnis(
    val loesung: LinearesSystemLoesung,
    val reduzierteErweiterteMatrix: Matrix,
    val schritte: List<UmformungsSchritt>,
    val rangKoeffizienten: Int,
    val rangErweitert: Int,
    val variablenNamen: List<String>,
    val verlauf: UmformungsVerlauf,
)

data class InverseMitGaussErgebnis(
    val inverse: Matrix,
    val reduzierteErweiterteMatrix: Matrix,
    val schritte: List<UmformungsSchritt>,
    val verlauf: UmformungsVerlauf,
)

private fun Matrix.rationalOderFehler(): Array<Array<RationaleZahl>> =
    Array(zeilenAnzahl) { zeile ->
        Array(spaltenAnzahl) { spalte ->
            vereinfache(zeilen[zeile][spalte]) as? RationaleZahl
                ?: error("Der Gauß-Algorithmus benötigt rational auswertbare Matrixeinträge.")
        }
    }

private fun Array<Array<RationaleZahl>>.alsMatrix(): Matrix =
    Matrix(map { zeile -> zeile.toList<ZahlAusdruck>() })

fun gauss(
    matrix: Matrix,
    ziel: GaussZiel = GaussZiel.REDUZIERTE_ZEILENSTUFENFORM,
    pivotSpaltenGrenze: Int = matrix.spaltenAnzahl,
): GaussErgebnis {
    require(pivotSpaltenGrenze in 0..matrix.spaltenAnzahl)
    val werte = matrix.rationalOderFehler()
    val schritte = mutableListOf<UmformungsSchritt>()
    val pivots = mutableListOf<Int>()
    var pivotZeile = 0

    fun protokolliere(
        vorher: Matrix,
        operation: ZeilenOperation,
        titel: String,
        erklaerung: String,
    ) {
        val nachher = werte.alsMatrix()
        schritte += UmformungsSchritt(
            vorher = vorher,
            nachher = nachher,
            regelId = "lina.gauss.${operation::class.simpleName}",
            titel = titel,
            erklärung = erklaerung,
            operationen = listOf(operation),
        )
    }

    for (spalte in 0 until pivotSpaltenGrenze) {
        if (pivotZeile >= matrix.zeilenAnzahl) break
        val pivot = (pivotZeile until matrix.zeilenAnzahl)
            .firstOrNull { !werte[it][spalte].istNull() }
            ?: continue

        if (pivot != pivotZeile) {
            val vorher = werte.alsMatrix()
            val temp = werte[pivot]
            werte[pivot] = werte[pivotZeile]
            werte[pivotZeile] = temp
            protokolliere(
                vorher,
                ZeilenOperation.Tausche(pivotZeile + 1, pivot + 1),
                "Zeilen tauschen",
                "Ein von null verschiedener Pivot wurde in die aktuelle Pivotzeile verschoben.",
            )
        }

        val pivotWert = werte[pivotZeile][spalte]
        if (!pivotWert.istEins()) {
            val vorher = werte.alsMatrix()
            val faktor = RationaleZahl.Eins / pivotWert
            for (j in spalte until matrix.spaltenAnzahl) {
                werte[pivotZeile][j] = werte[pivotZeile][j] * faktor
            }
            protokolliere(
                vorher,
                ZeilenOperation.Skaliere(pivotZeile + 1, faktor),
                "Pivot normieren",
                "Die Pivotzeile wurde so skaliert, dass ihr führender Eintrag eins ist.",
            )
        }

        val zeilen = when (ziel) {
            GaussZiel.ZEILENSTUFENFORM -> (pivotZeile + 1 until matrix.zeilenAnzahl).toList()
            GaussZiel.REDUZIERTE_ZEILENSTUFENFORM ->
                (0 until matrix.zeilenAnzahl).filter { it != pivotZeile }
        }
        for (zeile in zeilen) {
            val wert = werte[zeile][spalte]
            if (wert.istNull()) continue
            val vorher = werte.alsMatrix()
            val faktor = -wert
            for (j in spalte until matrix.spaltenAnzahl) {
                werte[zeile][j] = werte[zeile][j] + faktor * werte[pivotZeile][j]
            }
            protokolliere(
                vorher,
                ZeilenOperation.Kombiniere(
                    zielZeile = zeile + 1,
                    zielFaktor = RationaleZahl.Eins,
                    quellZeile = pivotZeile + 1,
                    quellFaktor = faktor,
                ),
                "Pivotspalte bereinigen",
                "Ein Vielfaches der Pivotzeile wurde addiert, um den Eintrag in der Pivotspalte zu eliminieren.",
            )
        }

        pivots += spalte
        pivotZeile += 1
    }

    val ergebnisMatrix = werte.alsMatrix()
    return GaussErgebnis(
        matrix = ergebnisMatrix,
        pivotSpalten = pivots,
        rang = pivots.size,
        schritte = schritte,
        verlauf = UmformungsVerlauf(matrix, schritte, ergebnisMatrix),
    )
}

fun loeseLinearesSystem(
    koeffizienten: Matrix,
    rechteSeite: SpaltenVektor,
    variablenNamen: List<String> = emptyList(),
): LinearesSystemErgebnis {
    require(koeffizienten.zeilenAnzahl == rechteSeite.werte.size) {
        "Die rechte Seite benötigt genau einen Eintrag je Matrixzeile."
    }
    val namen = if (variablenNamen.isEmpty()) {
        List(koeffizienten.spaltenAnzahl) { index -> "x_${index + 1}" }
    } else {
        require(variablenNamen.size == koeffizienten.spaltenAnzahl) {
            "Es wird genau ein Variablenname je Matrixspalte benötigt."
        }
        require(variablenNamen.all { it.isNotBlank() } && variablenNamen.distinct().size == variablenNamen.size) {
            "Variablennamen müssen nichtleer und eindeutig sein."
        }
        variablenNamen
    }

    val erweitert = Matrix(
        koeffizienten.zeilen.mapIndexed { index, zeile -> zeile + rechteSeite.werte[index] },
    )
    val gauss = gauss(
        erweitert,
        GaussZiel.REDUZIERTE_ZEILENSTUFENFORM,
        pivotSpaltenGrenze = koeffizienten.spaltenAnzahl,
    )
    val rational = gauss.matrix.rationalOderFehler()
    val variablenAnzahl = koeffizienten.spaltenAnzahl

    val widerspruch = rational.indices.firstOrNull { zeile ->
        (0 until variablenAnzahl).all { rational[zeile][it].istNull() } &&
            !rational[zeile][variablenAnzahl].istNull()
    }
    val pivotKoeffizienten = gauss.pivotSpalten.filter { it < variablenAnzahl }
    val rangKoeffizienten = pivotKoeffizienten.size
    val rangErweitert = if (widerspruch == null) rangKoeffizienten else rangKoeffizienten + 1

    if (widerspruch != null) {
        return LinearesSystemErgebnis(
            loesung = KeineLineareLoesung,
            reduzierteErweiterteMatrix = gauss.matrix,
            schritte = gauss.schritte,
            rangKoeffizienten = rangKoeffizienten,
            rangErweitert = rangErweitert,
            variablenNamen = namen,
            verlauf = gauss.verlauf,
        )
    }

    val freieSpalten = (0 until variablenAnzahl).filterNot(pivotKoeffizienten::contains)
    val freieParameter = freieSpalten.mapIndexed { index, _ -> Variable("t_${index + 1}") }
    val ausdruck = MutableList<ZahlAusdruck>(variablenAnzahl) { RationaleZahl.Null }

    freieSpalten.forEachIndexed { index, spalte -> ausdruck[spalte] = freieParameter[index] }
    pivotKoeffizienten.forEachIndexed { zeile, pivotSpalte ->
        val konstante = rational[zeile][variablenAnzahl]
        val freieTerme = freieSpalten.mapIndexedNotNull { index, freieSpalte ->
            val koeffizient = rational[zeile][freieSpalte]
            if (koeffizient.istNull()) null else multiplikation(-koeffizient, freieParameter[index])
        }
        ausdruck[pivotSpalte] = vereinfache(addition(listOf(konstante) + freieTerme))
    }

    val vektor = SpaltenVektor(ausdruck)
    val loesung: LinearesSystemLoesung = if (freieParameter.isEmpty()) {
        EindeutigeLineareLoesung(vektor)
    } else {
        ParametrischeLineareLoesung(vektor, freieParameter)
    }

    return LinearesSystemErgebnis(
        loesung = loesung,
        reduzierteErweiterteMatrix = gauss.matrix,
        schritte = gauss.schritte,
        rangKoeffizienten = rangKoeffizienten,
        rangErweitert = rangErweitert,
        variablenNamen = namen,
        verlauf = gauss.verlauf,
    )
}

/** Interpretiert die letzten [rechteSeitenSpalten] einer erweiterten Matrix als rechte Seite. */
fun loeseErweiterteMatrix(
    erweitert: Matrix,
    rechteSeitenSpalten: Int = 1,
    variablenNamen: List<String> = emptyList(),
): LinearesSystemErgebnis {
    require(rechteSeitenSpalten == 1) {
        "Die erste Ausbaustufe unterstützt genau eine rechte Seite für lineare Systeme."
    }
    require(erweitert.spaltenAnzahl >= 2)
    val trennSpalte = erweitert.spaltenAnzahl - rechteSeitenSpalten
    val koeffizienten = Matrix(erweitert.zeilen.map { it.take(trennSpalte) })
    val rhs = SpaltenVektor(erweitert.zeilen.map { it[trennSpalte] })
    return loeseLinearesSystem(koeffizienten, rhs, variablenNamen)
}

fun inverseMitGauss(matrix: Matrix): InverseMitGaussErgebnis {
    require(matrix.zeilenAnzahl == matrix.spaltenAnzahl) {
        "Nur quadratische Matrizen besitzen eine Inverse."
    }
    val n = matrix.zeilenAnzahl
    val erweitert = Matrix(
        matrix.zeilen.mapIndexed { zeile, werte ->
            werte + List<ZahlAusdruck>(n) { spalte ->
                if (zeile == spalte) RationaleZahl.Eins else RationaleZahl.Null
            }
        },
    )
    val gauss = gauss(erweitert, GaussZiel.REDUZIERTE_ZEILENSTUFENFORM, pivotSpaltenGrenze = n)
    require(gauss.rang == n) { "Die Matrix ist singulär." }
    val links = gauss.matrix.zeilen.map { it.take(n) }
    require(links.indices.all { zeile ->
        links[zeile].indices.all { spalte ->
            links[zeile][spalte] == if (zeile == spalte) RationaleZahl.Eins else RationaleZahl.Null
        }
    }) { "Die Matrix ist singulär." }
    val inverse = Matrix(gauss.matrix.zeilen.map { it.drop(n) })
    return InverseMitGaussErgebnis(inverse, gauss.matrix, gauss.schritte, gauss.verlauf)
}
