package de.TeutonStudio.MathematikRechenSystem.kern

/** Argumentabhängiger linearer beziehungsweise affiner Spann in R^dimension. */
data class UnterraumMenge(
    val richtungen: List<List<ZahlAusdruck>>,
    val stuetzpunkt: List<ZahlAusdruck>? = null,
    val dimension: Int = richtungen.firstOrNull()?.size ?: stuetzpunkt?.size ?: 0,
) : MengenAusdruck {
    init {
        require(dimension > 0) { "Eine Umgebungsdimension wird benötigt." }
        require(richtungen.all { it.size == dimension } && (stuetzpunkt == null || stuetzpunkt.size == dimension)) { "Alle Vektoren müssen dieselbe Dimension haben." }
        require((richtungen.flatten() + stuetzpunkt.orEmpty()).none { it is KomplexeZahl }) { "Ein Unterraum von R^d benötigt reelle Komponenten." }
    }
    val grundMenge: MengenAusdruck get() = Tupelraum(List(dimension) { ReelleZahlen })
    override fun zuLatex(): String = "\\left\\{" + (stuetzpunkt?.let { Tupel(it).zuLatex() + "+" } ?: "") +
        (if (richtungen.isEmpty()) Tupel(List(dimension) { RationaleZahl.Null }).zuLatex() else richtungen.mapIndexed { k, a -> "b_{${k+1}}${Tupel(a).zuLatex()}" }.joinToString("+")) +
        "\\mid b\\in\\mathbb{R}^{${richtungen.size}}\\right\\}"

    /** Kein symbolischer Pivot wird als von null verschieden vorausgesetzt. */
    fun rangOderNull(): Int? = rationalerRang(richtungen.transpose(dimension))

    fun mitgliedschaft(element: MathematischesObjekt, kontext: RechenKontext): AussageErgebnis {
        val tupel = element as? Tupel ?: return AussageErgebnis(Wahrheitswert.Lüge, EntscheidungsStatus.Widerlegt, "R^d enthält Tupel.")
        if (tupel.elemente.size != dimension) return AussageErgebnis(Wahrheitswert.Lüge, EntscheidungsStatus.Widerlegt, "Falsche Umgebungsdimension.")
        val zahlen = tupel.elemente.map { it as? ZahlAusdruck ?: return AussageErgebnis(Wahrheitswert.Lüge, EntscheidungsStatus.Widerlegt) }
        val reell = zahlen.map { ElementBeziehung(it, ReelleZahlen).entscheide(kontext).wahrheitswert }
        if (reell.any { it == Wahrheitswert.Lüge }) return AussageErgebnis(Wahrheitswert.Lüge, EntscheidungsStatus.Widerlegt)
        val matrix = richtungen.transpose(dimension)
        val differenz = zahlen.mapIndexed { i, x -> vereinfache(subtraktion(x, stuetzpunkt?.get(i) ?: RationaleZahl.Null), kontext) }
        val rang = rationalerRang(matrix) ?: return AussageErgebnis(null, EntscheidungsStatus.Unbekannt, "Symbolische Pivots sind nicht entschieden.")
        val erweitert = rationalerRang(matrix.mapIndexed { i, zeile -> zeile + differenz[i] })
            ?: return AussageErgebnis(null, EntscheidungsStatus.Unbekannt, "Symbolische Mitgliedschaft ist nicht entschieden.")
        return if (rang == erweitert) AussageErgebnis(Wahrheitswert.Wahr, EntscheidungsStatus.Bewiesen)
        else AussageErgebnis(Wahrheitswert.Lüge, EntscheidungsStatus.Widerlegt)
    }
}

private fun List<List<ZahlAusdruck>>.transpose(d: Int): List<List<ZahlAusdruck>> = List(d) { i -> map { it[i] } }

private fun rationalerRang(matrix: List<List<ZahlAusdruck>>): Int? {
    val a = matrix.map { row -> row.map { vereinfache(it) as? RationaleZahl ?: return null }.toMutableList() }.toMutableList()
    val n = a.firstOrNull()?.size ?: return 0
    var pivot = 0
    for (j in 0 until n) {
        val k = (pivot until a.size).firstOrNull { a[it][j] != RationaleZahl.Null } ?: continue
        val tmp = a[pivot]; a[pivot] = a[k]; a[k] = tmp
        val divisor = a[pivot][j]
        for (c in j until n) a[pivot][c] = vereinfache(Division(a[pivot][c], divisor)) as RationaleZahl
        for (r in pivot + 1 until a.size) {
            val faktor = a[r][j]
            for (c in j until n) a[r][c] = vereinfache(subtraktion(a[r][c], multiplikation(faktor, a[pivot][c]))) as RationaleZahl
        }
        pivot++
        if (pivot == a.size) break
    }
    return pivot
}

fun unterraumKomponenten(objekt: MathematischesObjekt): List<ZahlAusdruck> = when (objekt) {
    is OrientierterVektor -> objekt.werte
    is Tupel -> objekt.elemente.map { it as? ZahlAusdruck ?: error("Unterraum benötigt flache Zahlentupel.") }
    else -> error("Unterraum benötigt Tupel, Zeile oder Spalte.")
}
