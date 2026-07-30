package de.TeutonStudio.MathematikRechenSystem.kern

private data class GerichteteRelation(
    val von: ZahlAusdruck,
    val art: VergleichsArt,
    val nach: ZahlAusdruck,
    val original: Vergleich,
    val index: Int,
)

private sealed interface KonjunktionsDarstellung {
    val index: Int
    fun zuLatex(): String

    data class EinzelneAussage(
        override val index: Int,
        val aussage: Aussage,
    ) : KonjunktionsDarstellung {
        override fun zuLatex() = aussage.zuLatex()
    }

    data class RelationsKomponente(
        override val index: Int,
        val relationen: List<GerichteteRelation>,
    ) : KonjunktionsDarstellung {
        override fun zuLatex(): String = relationen.alsKetteOderKonjunktion()
    }
}

/**
 * Verkürzt zusammenhängende lineare Ordnungsrelationen rein für die Darstellung.
 *
 * Beispiele:
 * `a < b ∧ b ≤ c` wird zu `a < b ≤ c` und
 * `c > b ∧ b ≥ a` wird zu `a ≤ b < c`.
 * Verzweigungen, Zyklen und nicht vergleichende Aussagen bleiben Konjunktionen.
 */
internal fun Konjunktion.zuRelationsKettenLatex(): String {
    val flach = aussagen.flach()
    if (flach.size < 2) return flach.joinToString(" \\land ") { it.zuLatex() }

    val relationen = flach.mapIndexedNotNull { index, aussage ->
        (aussage as? Vergleich)?.normalisiert(index)
    }
    if (relationen.size < 2) return flach.joinToString(" \\land ") { it.zuLatex() }

    val relationNachIndex = relationen.associateBy { it.index }
    val nochOffen = relationen.toMutableSet()
    val komponenten = mutableListOf<List<GerichteteRelation>>()

    while (nochOffen.isNotEmpty()) {
        val start = nochOffen.first()
        val knoten = mutableSetOf<ZahlAusdruck>()
        val komponente = mutableSetOf<GerichteteRelation>()
        val warteschlange = ArrayDeque<ZahlAusdruck>()
        warteschlange += start.von
        warteschlange += start.nach

        while (warteschlange.isNotEmpty()) {
            val aktuell = warteschlange.removeFirst()
            if (!knoten.add(aktuell)) continue
            val angrenzend = nochOffen.filter { it.von == aktuell || it.nach == aktuell }
            angrenzend.forEach { relation ->
                if (komponente.add(relation)) {
                    warteschlange += relation.von
                    warteschlange += relation.nach
                }
            }
        }
        nochOffen.removeAll(komponente)
        komponenten += komponente.sortedBy { it.index }
    }

    val verwendeteVergleiche = relationNachIndex.keys
    val darstellungen = buildList<KonjunktionsDarstellung> {
        komponenten.forEach { komponente ->
            add(KonjunktionsDarstellung.RelationsKomponente(komponente.minOf { it.index }, komponente))
        }
        flach.forEachIndexed { index, aussage ->
            if (index !in verwendeteVergleiche) add(KonjunktionsDarstellung.EinzelneAussage(index, aussage))
        }
    }

    return darstellungen.sortedBy { it.index }.joinToString(" \\land ") { it.zuLatex() }
}

private fun List<Aussage>.flach(): List<Aussage> = flatMap { aussage ->
    if (aussage is Konjunktion) aussage.aussagen.flach() else listOf(aussage)
}

private fun Vergleich.normalisiert(index: Int): GerichteteRelation = when (art) {
    VergleichsArt.Kleiner -> GerichteteRelation(links, VergleichsArt.Kleiner, rechts, this, index)
    VergleichsArt.KleinerGleich -> GerichteteRelation(links, VergleichsArt.KleinerGleich, rechts, this, index)
    VergleichsArt.Größer -> GerichteteRelation(rechts, VergleichsArt.Kleiner, links, this, index)
    VergleichsArt.GrößerGleich -> GerichteteRelation(rechts, VergleichsArt.KleinerGleich, links, this, index)
}

private fun List<GerichteteRelation>.alsKetteOderKonjunktion(): String {
    if (size < 2) return single().original.zuLatex()

    val knoten = flatMap { listOf(it.von, it.nach) }.toSet()
    val ausgehend = groupBy { it.von }
    val eingehend = groupBy { it.nach }
    val istLinear = knoten.all { ausgehend[it].orEmpty().size <= 1 && eingehend[it].orEmpty().size <= 1 }
    if (!istLinear || size != knoten.size - 1) return originalKonjunktion()

    val starts = knoten.filter { eingehend[it].isNullOrEmpty() }
    val enden = knoten.filter { ausgehend[it].isNullOrEmpty() }
    if (starts.size != 1 || enden.size != 1) return originalKonjunktion()

    val besucht = mutableSetOf<GerichteteRelation>()
    val teile = mutableListOf<String>()
    var aktuell = starts.single()
    teile += aktuell.zuLatex()

    while (true) {
        val relation = ausgehend[aktuell]?.singleOrNull() ?: break
        if (!besucht.add(relation)) return originalKonjunktion()
        teile += relation.art.latex
        teile += relation.nach.zuLatex()
        aktuell = relation.nach
    }

    return if (besucht.size == size && aktuell == enden.single()) teile.joinToString(" ") else originalKonjunktion()
}

private fun List<GerichteteRelation>.originalKonjunktion(): String =
    sortedBy { it.index }.joinToString(" \\land ") { it.original.zuLatex() }
