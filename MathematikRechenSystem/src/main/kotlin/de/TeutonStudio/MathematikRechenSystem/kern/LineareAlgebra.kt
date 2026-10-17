package de.TeutonStudio.MathematikRechenSystem.kern

sealed interface OrientierterVektor : Ausdruck, Tensorartig {
    val werte: List<ZahlAusdruck>
    override val tensorForm: List<Int> get() = listOf(werte.size)
    override val tensorZahlBereich: MengenAusdruck
        get() = maximaleZahlenGrundmenge(werte.map(::inferiereZahlenWertevorrat))
    override fun tensorKomponente(indizes: List<Int>): ZahlAusdruck {
        require(indizes.size == 1) { "Ein Vektor besitzt genau eine Tensorachse." }
        return werte[indizes.single()]
    }
    fun skalarprodukt(andere: OrientierterVektor): ZahlAusdruck {
        require(this::class == andere::class) { "Das Skalarprodukt benötigt zwei gleich orientierte Vektoren." }
        require(werte.size == andere.werte.size) { "Vektoren müssen dieselbe Dimension haben." }
        return addition(werte.zip(andere.werte) { a, b -> multiplikation(a, b) })
    }
    fun radius(): ZahlAusdruck = reelleEuklidischeNorm(werte)
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

/** Explizite Brücke: Ein Tupel erhält erst für die lineare Algebra eine Spaltenorientierung. */
fun Tupel.alsSpaltenVektor(): SpaltenVektor = SpaltenVektor(elemente.mapIndexed { index, element ->
    element as? ZahlAusdruck ?: error("Tupelkomponente ${index + 1} ist keine Zahl.")
})

/** Beim Rückweg zum Koordinatenobjekt wird die Vektororientierung ausdrücklich abgelegt. */
fun OrientierterVektor.alsTupel(): Tupel = Tupel(werte)

/** Bildet die aufsteigend gespeicherten Koeffizienten c₀, …, cₙ auf Σ cᵢ·Xⁱ ab. */
fun polynomAusKoeffizienten(koeffizienten: List<ZahlAusdruck>, variable: Variable): ZahlAusdruck {
    require(koeffizienten.isNotEmpty()) { "Ein Polynom benötigt mindestens einen Koeffizienten." }
    require(variable.name.isNotBlank()) { "Die Polynomvariable darf nicht leer sein." }
    require(koeffizienten.none { it.enthältVariable(variable) }) {
        "Koeffizienten dürfen die Polynomvariable '${variable.name}' nicht enthalten."
    }
    return polynomAusKoeffizienten(koeffizienten, variable as ZahlAusdruck)
}

/** Rückwärtskompatible Bezeichnung: ein ungerichteter Altvektor wird als Spaltenvektor geführt. */
typealias Vektor = SpaltenVektor

fun einheitsSpaltenVektor(dimension: Int, index: Int): SpaltenVektor = SpaltenVektor(einheitsVektorWerte(dimension, index))
fun einheitsZeilenVektor(dimension: Int, index: Int): ZeilenVektor = ZeilenVektor(einheitsVektorWerte(dimension, index))
fun einheitsVektorWerte(dimension: Int, index: Int): List<ZahlAusdruck> {
    require(dimension > 0 && index in 1..dimension) { "Index des Einheitsvektors muss zwischen 1 und der Dimension liegen." }
    return List(dimension) { if (it == index - 1) RationaleZahl.Eins else RationaleZahl.Null }
}

fun kreuzprodukt(a: OrientierterVektor, b: OrientierterVektor): OrientierterVektor {
    require(a::class == b::class && a.werte.size == 3 && b.werte.size == 3) { "Das Kreuzprodukt benötigt gleich orientierte 3-Vektoren." }
    val w = kreuzproduktKomponenten(a.werte, b.werte)
    return if (a is ZeilenVektor) ZeilenVektor(w) else SpaltenVektor(w)
}

/** Klassisches bilineares Kreuzprodukt über einem kommutativen Koeffizientenbereich. */
fun kreuzproduktKomponenten(a: List<ZahlAusdruck>, b: List<ZahlAusdruck>,
    werteVorräte: Map<String, MengenAusdruck> = emptyMap(), annahmen: Set<Aussage> = emptySet()): List<ZahlAusdruck> {
    require(a.size == 3 && b.size == 3) { "Das Kreuzprodukt benötigt zwei dreidimensionale Vektoren." }
    require((a+b).all { inferiereZahlenWertevorrat(it,werteVorräte,annahmen).fundamentalerZahlbereichOderNull()?.let { bereich -> bereich <= FundamentalerZahlbereich.KOMPLEX } == true }) {
        "Das klassische Kreuzprodukt benötigt einen nachgewiesenen kommutativen Koeffizientenbereich."
    }
    return listOf(
        subtraktion(multiplikation(a[1], b[2]), multiplikation(a[2], b[1])),
        subtraktion(multiplikation(a[2], b[0]), multiplikation(a[0], b[2])),
        subtraktion(multiplikation(a[0], b[1]), multiplikation(a[1], b[0])),
    )
}

data class ReelleNormAnwendung(val wert: ZahlAusdruck, val bedingungen: Set<Aussage>)

/** Offene Definitionsbedingungen bleiben strukturiert; widerlegte verhindern die Anwendung. */
fun reelleEuklidischeNormAnwendung(werte: List<ZahlAusdruck>, werteVorräte: Map<String,MengenAusdruck> = emptyMap(),
    annahmen: Set<Aussage> = emptySet()): ReelleNormAnwendung {
    require(werte.isNotEmpty()) { "Die Norm benötigt mindestens eine Komponente." }
    val voraussetzungen=(annahmen+werte.flatMap(::definitionsBedingungen)).toSet()
    require(voraussetzungen.none {it.entscheide().wahrheitswert==Wahrheitswert.Lüge}) { "Die Normkomponenten verletzen eine Definitionsbedingung." }
    require(werte.all { istNachweisbarReell(it,{ v ->
        (v.bindungsId?.let(werteVorräte::get) ?: werteVorräte[v.name])?.let(::nachgewieseneZahlenGrundmenge)?.fundamentalerZahlbereichOderNull()?.let { b -> b <= FundamentalerZahlbereich.REELL } == true
    },voraussetzungen, { komponente -> nachgewieseneReelleQuellKomponente(komponente,werteVorräte) }) }) { "Diese euklidische Norm benötigt nachgewiesen reelle Komponenten." }
    return ReelleNormAnwendung(wurzel(addition(werte.map {Potenz(it,RationaleZahl.von(2))})),
        voraussetzungen.filterNot {it.entscheide().wahrheitswert==Wahrheitswert.Wahr}.toSet())
}

/** Unbedingter Legacy-Wertzugang: offene Bedingungen müssen vom Aufrufer ausdrücklich getragen werden. */
fun reelleEuklidischeNorm(werte: List<ZahlAusdruck>, werteVorräte: Map<String,MengenAusdruck> = emptyMap(),
    annahmen: Set<Aussage> = emptySet()): ZahlAusdruck {
    val anwendung=reelleEuklidischeNormAnwendung(werte,werteVorräte,annahmen)
    require(annahmen.containsAll(anwendung.bedingungen)) { "Die reelle Norm besitzt offene Definitionsbedingungen: ${anwendung.bedingungen.joinToString {it.zuLatex()}}" }
    return anwendung.wert
}

data class Matrix(val zeilen: List<List<ZahlAusdruck>>) : Ausdruck, Tensorartig {
    init {
        require(zeilen.isNotEmpty() && zeilen.first().isNotEmpty())
        require(zeilen.all { it.size == zeilen.first().size })
    }
    val zeilenAnzahl get() = zeilen.size
    val spaltenAnzahl get() = zeilen.first().size
    override val tensorForm: List<Int> get() = listOf(zeilenAnzahl, spaltenAnzahl)
    override val tensorZahlBereich: MengenAusdruck
        get() = maximaleZahlenGrundmenge(zeilen.flatten().map(::inferiereZahlenWertevorrat))
    override fun tensorKomponente(indizes: List<Int>): ZahlAusdruck {
        require(indizes.size == 2) { "Eine Matrix besitzt genau zwei Tensorachsen." }
        return zeilen[indizes[0]][indizes[1]]
    }
    override fun zuLatex() = zeilen.zuPmatrixLatex()
    operator fun plus(andere: Matrix): Matrix {
        require(zeilenAnzahl == andere.zeilenAnzahl && spaltenAnzahl == andere.spaltenAnzahl)
        return Matrix(zeilen.indices.map { z -> zeilen[z].indices.map { s -> addition(zeilen[z][s], andere.zeilen[z][s]) } })
    }
    operator fun times(andere: Matrix): Matrix {
        require(spaltenAnzahl == andere.zeilenAnzahl) { "Spaltenzahl der linken Matrix muss zur Zeilenzahl der rechten passen." }
        return Matrix(List(zeilenAnzahl) { z -> List(andere.spaltenAnzahl) { s -> addition((0 until spaltenAnzahl).map { k -> multiplikation(zeilen[z][k], andere.zeilen[k][s]) }) } })
    }
    operator fun times(vektor: SpaltenVektor): SpaltenVektor {
        require(spaltenAnzahl == vektor.werte.size) { "Spaltenzahl der Matrix muss zur Dimension des Spaltenvektors passen." }
        return SpaltenVektor(zeilen.map { zeile ->
            vereinfache(addition(zeile.zip(vektor.werte) { matrixWert, vektorWert -> multiplikation(matrixWert, vektorWert) }))
        })
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

/**
 * Kanonische Matrixansicht eines Operanden im Matrixprodukt.
 *
 * Orientierte Vektoren behalten ihre mathematische Orientierung:
 * Spaltenvektoren werden als n×1-, Zeilenvektoren als 1×n-Matrizen interpretiert.
 * Außerhalb eines Matrixprodukts bleiben die Typen weiterhin getrennt.
 */
fun MathematischesObjekt.alsMatrixFaktor(): Matrix = when (this) {
    is Matrix -> this
    is SpaltenVektor -> Matrix(werte.map(::listOf))
    is ZeilenVektor -> Matrix(listOf(werte))
    else -> error("Ein Matrixprodukt akzeptiert nur Matrizen, Spaltenvektoren und Zeilenvektoren.")
}

/** Multipliziert Matrizen und orientierte Vektoren über ihre kanonische Matrixansicht. */
fun matrixProdukt(links: MathematischesObjekt, rechts: MathematischesObjekt): Matrix =
    matrixProdukt(listOf(links, rechts))

/** Formatiert Zeilen als LaTeX-`pmatrix`; `\\` trennt die Zeilen, `&` die Spalten. */
private fun List<List<ZahlAusdruck>>.zuPmatrixLatex(): String = joinToString(
    prefix = "\\begin{pmatrix}",
    separator = " \\\\ ",
    postfix = "\\end{pmatrix}",
) { zeile -> zeile.joinToString(" & ") { it.zuLatex() } }

/** Geordnete, streng linksgefaltete Kette; Zwischenprodukte sind Teil des Fachmodells. */
data class MatrixProduktKette(val faktoren: List<Matrix>, val zwischenprodukte: List<Matrix>) {
    val ergebnis: Matrix get() = zwischenprodukte.last()
}

fun matrixProduktKette(faktoren: List<MathematischesObjekt>): MatrixProduktKette {
    require(faktoren.size >= 2) { "Ein Matrixprodukt benötigt mindestens zwei Faktoren." }
    val matrizen = faktoren.mapIndexed { index, faktor ->
        runCatching { faktor.alsMatrixFaktor() }.getOrElse {
            error("Faktor ${index + 1}: ${it.message}")
        }
    }
    var akk = matrizen.first()
    val zwischen = mutableListOf<Matrix>()
    matrizen.drop(1).forEachIndexed { index, rechts ->
        require(akk.spaltenAnzahl == rechts.zeilenAnzahl) {
            "Faktor ${index + 2}: Zwischenprodukt ${akk.zeilenAnzahl}×${akk.spaltenAnzahl} und Faktor ${rechts.zeilenAnzahl}×${rechts.spaltenAnzahl} sind inkompatibel; ${akk.spaltenAnzahl} ≠ ${rechts.zeilenAnzahl}."
        }
        akk = akk * rechts
        zwischen += akk
    }
    return MatrixProduktKette(matrizen, zwischen)
}

fun matrixProdukt(faktoren: List<MathematischesObjekt>): Matrix = matrixProduktKette(faktoren).ergebnis


/** Typabhängige Transposition; Vektororientierung ist keine Tensor-Achsenpermutation. */
fun transponiereStruktur(wert: MathematischesObjekt, permutation: List<Int>? = null): MathematischesObjekt = when(wert) {
    is SpaltenVektor -> wert.transponiert()
    is ZeilenVektor -> wert.transponiert()
    is Matrix -> wert.transponiert()
    is Tensor -> wert.permutiereAchsen(permutation ?: standardTensorPermutation(wert.rang))
    else -> error("Nur Vektoren, Matrizen und Tensoren können transponiert werden.")
}

fun Tensorartig.alsRangZweiMatrix(): Matrix {
    require(tensorStufe == 2) { "Erwartet wird ein Tensor zweiter Stufe." }
    if (this is Matrix) return this
    val (zeilen,spalten)=tensorForm
    return Matrix(List(zeilen) {z -> List(spalten) {s -> tensorKomponente(listOf(z,s))}})
}

/** Nur gültige Indizes einer explizit typisierten Quelle belegen einen Komponentenbereich. */
private fun nachgewieseneReelleQuellKomponente(k: SymbolischeZahlKomponente,
    bereiche: Map<String,MengenAusdruck>): Boolean {
    val quelle=k.quelle as? MethodenParameter ?: return false
    val raum=quelle.bindungsId?.let(bereiche::get) ?: bereiche[quelle.name] ?: return false
    val indizes=(listOf(k.index)+k.weitereIndizes).map {
        (it as? RationaleZahl)?.positiveGanzeZahlOderNull() ?: return false
    }
    val menge=when(raum) {
        is Vektorraum -> if(indizes.size==1 && indizes[0] in 1..raum.dimension) raum.skalarMenge else return false
        is Tensorraum -> {
            val matrix = raum.konkreterMatrizenraumOderNull() ?: return false
            if (indizes.size == 2 && indizes[0] in 1..matrix.zeilen && indizes[1] in 1..matrix.spalten) matrix.skalarMenge else return false
        }
        is Matrizenraum -> if(indizes.size==2 && indizes[0] in 1..raum.zeilen && indizes[1] in 1..raum.spalten) raum.skalarMenge else return false
        is Tupelraum -> if(indizes.size==1) raum.komponenten.getOrNull(indizes[0]-1) ?: return false else return false
        else -> return false
    }
    return nachgewieseneZahlenGrundmenge(menge)?.fundamentalerZahlbereichOderNull()?.let {it<=FundamentalerZahlbereich.REELL} == true
}
