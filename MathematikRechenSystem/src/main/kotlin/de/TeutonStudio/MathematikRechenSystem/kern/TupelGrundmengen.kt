package de.TeutonStudio.MathematikRechenSystem.kern

/** Alle gelieferten Obermengen bleiben komponentenweise erhalten. */
fun Tupelraum.gemeinsameGrundmenge(): MengenAusdruck = vereinige(komponenten)
fun Tupelraum.potenzLatex(): String = if (komponenten.isEmpty()) zuLatex()
    else "\\left(${gemeinsameGrundmenge().zuLatex()}\\right)^{${komponenten.size}}"

fun strukturRaumÜbersetzung(quelle: MengenAusdruck?, ergebnis: MathematischesObjekt): MengenAusdruck? {
    val komponenten = when (quelle) {
        is Tupelraum -> quelle.komponenten
        is Vektorraum -> List(quelle.dimension) { quelle.skalarMenge }
        else -> return null
    }
    return when (ergebnis) {
        is Tupel -> Tupelraum(komponenten).takeIf { komponenten.size == ergebnis.elemente.size }
        is ZeilenVektor -> Vektorraum(VektorOrientierung.Zeile, ergebnis.werte.size, vereinige(komponenten))
        is SpaltenVektor -> Vektorraum(VektorOrientierung.Spalte, ergebnis.werte.size, vereinige(komponenten))
        else -> null
    }
}

/** Öffnet die Komponentenräume zusammen mit den zugehörigen Tupelkomponenten. */
fun tupelAbflachenRaum(tupel: Tupel, raum: Tupelraum? = null): Tupelraum {
    val räume = raum?.komponenten?.takeIf { it.size == tupel.elemente.size }
    return Tupelraum(tupel.elemente.flatMapIndexed { index, element ->
        val bereich = räume?.get(index)
        if (element is Tupel) tupelAbflachenRaum(element, bereich as? Tupelraum).komponenten
        else listOf(bereich ?: runCatching { inferiereZielmenge(element) }.getOrElse { FehlendeObermenge("mathematik.objekt") })
    })
}
