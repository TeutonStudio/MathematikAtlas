package de.TeutonStudio.MathematikRechenSystem.kern

/** Kleine form- und indexbezogene Primitive für ausführbare Definitionsgraphen, keine Faktorisierungssolver. */
fun nichtnegativerGanzzahlExponent(wert: MathematischesObjekt): Int {
    val p = (wert as? ZahlAusdruck)?.let(::vereinfache) as? RationaleZahl
        ?: error("Der Exponent muss eine konkrete nichtnegative ganze Zahl sein.")
    require(p.nenner == java.math.BigInteger.ONE && p.zähler.signum() >= 0 && p.zähler.bitLength() < 32) {
        "Der Exponent muss eine nichtnegative ganze Zahl im unterstützten Bereich sein."
    }
    return p.zähler.toInt()
}

fun identitaetsMatrix(n: Int) = Matrix(List(n) { i -> List(n) { j -> if (i == j) RationaleZahl.Eins else RationaleZahl.Null } })

fun diagonaleKomponentenMethode(quelle: MathematischesObjekt): MathematischeMethode {
    val werte = when (quelle) {
        is OrientierterVektor -> quelle.werte
        is Tupel -> quelle.elemente.map { it as? ZahlAusdruck ?: error("Diagonalkomponenten müssen Zahlen sein.") }
        else -> error("Diagonalkomponenten benötigen Tupel oder Vektor.")
    }
    require(werte.isNotEmpty()) { "Eine Diagonalmatrix benötigt mindestens eine Komponente." }
    return strukturIndexMethode(Matrix(List(werte.size) { i -> List(werte.size) { j -> if (i == j) werte[i] else RationaleZahl.Null } }))
}

/** Konjugation und Achsentausch sind reine Komponentenoperationen. */
fun konjugiertTransponierteKomponenten(matrix: Matrix): Matrix = Matrix(List(matrix.spaltenAnzahl) { j ->
    List(matrix.zeilenAnzahl) { i -> vereinfache(Konjugierte(matrix.zeilen[i][j])) }
})

/** Kandidaten werden komponentenweise aufgebaut; die formale Parameterbindung bleibt gemeinsam. */
fun strukturParameterKomponenten(parameter: TypisiertesElement, form: List<Int>): MathematischesObjekt {
    require(form.isNotEmpty() && form.all { it > 0 } && form.fold(1L) { a, b -> a * b } <= 100_000) {
        "Die Kandidatenform überschreitet das Materialisierungsbudget von 100000 Komponenten."
    }
    val werte = form.indizesFolge().map { indices -> SymbolischeZahlKomponente(parameter,
        RationaleZahl.von((indices.first() + 1).toLong()), indices.drop(1).map { RationaleZahl.von((it + 1).toLong()) }) }
    return when (form.size) {
        1 -> SpaltenVektor(werte)
        2 -> Matrix(List(form[0]) { i -> List(form[1]) { j -> werte[i * form[1] + j] } })
        else -> Tensor(form, werte)
    }
}

fun untereDreiecksKomponenten(matrix: Matrix) = Matrix(List(matrix.zeilenAnzahl) { i ->
    List(matrix.spaltenAnzahl) { j -> if (i > j) matrix.zeilen[i][j] else RationaleZahl.Null }
})
fun ausserdiagonaleKomponenten(matrix: Matrix) = Matrix(List(matrix.zeilenAnzahl) { i ->
    List(matrix.spaltenAnzahl) { j -> if (i != j) matrix.zeilen[i][j] else RationaleZahl.Null }
})
fun nullKomponenten(matrix: Matrix) = Matrix(List(matrix.zeilenAnzahl) { List(matrix.spaltenAnzahl) { RationaleZahl.Null } })

fun jordanFormBedingungen(j: Matrix): Aussage {
    require(j.zeilenAnzahl == j.spaltenAnzahl)
    val bedingungen = mutableListOf<Aussage>()
    for (i in 0 until j.zeilenAnzahl) for (k in 0 until j.spaltenAnzahl) {
        val x = j.zeilen[i][k]
        if (k == i + 1) {
            bedingungen += Disjunktion(listOf(Gleichheit(x, RationaleZahl.Null), Gleichheit(x, RationaleZahl.Eins)))
            bedingungen += Disjunktion(listOf(Gleichheit(x, RationaleZahl.Null), Gleichheit(j.zeilen[i][i], j.zeilen[k][k])))
        } else if (k != i) bedingungen += Gleichheit(x, RationaleZahl.Null)
    }
    return bedingungen.fold<Aussage, Aussage>(WahrheitsKonstante(true)) { a, b -> Konjunktion(listOf(a, b)) }
}
fun nichtnegativeDiagonaleBedingungen(sigma: Matrix): Aussage =
    (0 until minOf(sigma.zeilenAnzahl, sigma.spaltenAnzahl)).map { i -> Vergleich(sigma.zeilen[i][i], VergleichsArt.GrößerGleich, RationaleZahl.Null) }
        .fold<Aussage, Aussage>(WahrheitsKonstante(true)) { a, b -> Konjunktion(listOf(a, b)) }

/** Vektorisierung und Spaltensammlung für bestehende Gauß-Gleichungssysteme. */
fun spaltenAusStrukturen(quelle: Tupel): Matrix {
    val spalten = quelle.elemente.map { x -> when (x) {
        is Matrix -> x.zeilen.flatten()
        is OrientierterVektor -> x.werte
        else -> error("Die Spaltensammlung benötigt Matrizen oder Vektoren.")
    } }
    require(spalten.isNotEmpty() && spalten.all { it.size == spalten.first().size }) { "Die vektorisierten Strukturen müssen dieselbe Größe besitzen." }
    return Matrix(List(spalten.first().size) { i -> spalten.map { it[i] } })
}
fun partikulaereLineareLoesung(a: Matrix, b: SpaltenVektor): Tupel {
    val loesung = loeseLinearesSystem(a, b).loesung
    val vektor = when (loesung) {
        KeineLineareLoesung -> SpaltenVektor(List(a.spaltenAnzahl) { RationaleZahl.Null })
        is EindeutigeLineareLoesung -> loesung.loesungsVektor
        is ParametrischeLineareLoesung -> SpaltenVektor(loesung.loesungsVektor.werte.map { ersetze(it, loesung.freieParameter.associate { p -> p.name to RationaleZahl.Null }) })
    }
    return Tupel(listOf(if (loesung == KeineLineareLoesung) WahrheitsKonstante(false) else WahrheitsKonstante(true), vektor))
}
