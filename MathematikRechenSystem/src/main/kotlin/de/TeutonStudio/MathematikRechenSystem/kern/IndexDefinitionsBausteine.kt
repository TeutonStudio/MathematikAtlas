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

fun konjugiertTransponierteProjektorKomponenten(p: Tensorartig): Tensor {
    val form = p.tensorForm
    require(form.size == 3 && form[1] == form[2]) { "Projektoren benötigen die Form (Anzahl, Zeilen, Spalten)." }
    return Tensor(form, form.indizesFolge().map { (k, i, j) -> vereinfache(Konjugierte(p.tensorKomponente(listOf(k, j, i)))) })
}
fun orthogonaleProjektorBedingungen(p: Tensorartig): Aussage {
    val form = p.tensorForm
    require(form.size == 3 && form[1] == form[2] && form.max() <= 7) { "Die Projektorbedingungen materialisieren Familien bis Dimension 7." }
    val bedingungen = mutableListOf<Aussage>()
    for (k in 0 until form[0]) for (l in 0 until form[0]) for (i in 0 until form[1]) for (j in 0 until form[2]) {
        val summe = addition((0 until form[1]).map { m -> multiplikation(p.tensorKomponente(listOf(k, i, m)), p.tensorKomponente(listOf(l, m, j))) })
        bedingungen += Gleichheit(summe, if (k == l) p.tensorKomponente(listOf(k, i, j)) else RationaleZahl.Null)
    }
    return Konjunktion(bedingungen)
}

fun nichtverschwindendeSpaltenBedingungen(v: Matrix): Aussage = Konjunktion(List(v.spaltenAnzahl) { j ->
    Ungleichheit(addition(List(v.zeilenAnzahl) { i -> multiplikation(Konjugierte(v.zeilen[i][j]), v.zeilen[i][j]) }), RationaleZahl.Null)
})

/** Indexfamilie skalarer Komponentenmethoden mit unveränderter öffentlicher Argumentbindung. */
fun vektorfeldKomponentenMethoden(methode: Methode): MathematischeMethode {
    val m = methode.alsMathematischeMethode("Komponenten eines Vektorfeldes")
    val objekt = if (m.ausgabeNamen.size == 1) m.einzigeAusgabe().second else m.ergebnisTupel
    val komponenten = when (objekt) {
        is OrientierterVektor -> objekt.werte
        is Tupel -> objekt.elemente.map { it as? ZahlAusdruck ?: error("Vektorfeldkomponenten müssen Zahlen sein.") }
        else -> error("Das Vektorfeld muss einen Vektor oder ein Zahlentupel ausgeben.")
    }
    require(komponenten.isNotEmpty()) { "Ein Vektorfeld benötigt mindestens eine Komponente." }
    var name = "komponente"; val belegt = (m as MathematischesObjekt).enthalteneMethodenParameter().map { it.name }.toSet()
    while (name in belegt) name += "_"
    val i = Variable(name)
    val skalar = m.copy(name = "${m.name}_i", vorschrift = SymbolischeZahlKomponente(Tupel(komponenten), i), zielMenge = ReelleZahlen, ausgabeNamen = listOf("wert"))
    return MathematischeMethode("Komponentenmethoden", listOf(i), skalar, BenannteMenge("Skalare Methoden"),
        mapOf(i.name to EndlicheIndexMenge(RationaleZahl.von(komponenten.size.toLong()))))
}
