package de.TeutonStudio.MathematikRechenSystem.kern

import de.TeutonStudio.TypSystem.TypPrüfung

/** Präzise Diagnose einer ungültigen Übergangsstelle in einer Methodenkette. */
data class KompositionsFehler(
    val äußerePosition: Int,
    val innerePosition: Int,
    val grund: String,
) {
    override fun toString(): String =
        "Komposition an Position $äußerePosition ← $innerePosition ist ungültig: $grund"
}

/** Ergebnis der semantischen Prüfung einer geordneten Methodenkette. */
data class KompositionsPrüfung(
    val fehler: List<KompositionsFehler>,
) {
    val istGültig: Boolean get() = fehler.isEmpty()
}

/**
 * Prüft eine sichtbare Methodenkette `f₁ ∘ f₂ ∘ … ∘ fₙ` von außen nach innen.
 *
 * Die neutrale Prüfung vergleicht die kanonischen Tupeltypen. Die mathematische
 * Zusatzprüfung vergleicht anschließend die einzelne weitergereichte Ergebniskomponente
 * mit der einzelnen Argumentkomponente der äußeren Methode. Dadurch wird ein
 * `Tupel<Z>` nicht mehr fälschlich direkt mit der Komponentenmenge `W` verglichen.
 */
fun prüfeKompositionsKette(
    methoden: List<Methode>,
    kontext: RechenKontext = RechenKontext(),
): KompositionsPrüfung {
    if (methoden.size < 2) {
        return KompositionsPrüfung(
            listOf(KompositionsFehler(1, 1, "Es werden mindestens zwei Methoden benötigt.")),
        )
    }

    val fehler = methoden.zipWithNext().mapIndexedNotNull { index, (außen, innen) ->
        val typ = MathematischeTypen.typSystem.prüfe(innen.methodenSignatur().ergebnisTyp, außen.methodenSignatur().argumentTyp)
        if (typ is TypPrüfung.Inkompatibel) {
            KompositionsFehler(index + 1, index + 2, "Die Tupeltypen passen nicht zusammen: ${typ.grund}")
        } else if (außen is MathematischeMethode && innen is MathematischeMethode) {
            runCatching { komponiere(außen, innen) }.exceptionOrNull()?.let {
                KompositionsFehler(index + 1, index + 2, it.message ?: "Ungültige Komposition.")
            }
        } else null
    }
    return KompositionsPrüfung(fehler)
}

/** Komponiert Tupelschnittstellen; offene Domänenbedingungen bleiben an der Methode. */
fun komponiere(methoden: List<Methode>): Methode {
    val prüfung = prüfeKompositionsKette(methoden)
    require(prüfung.istGültig) { prüfung.fehler.joinToString("\n") }
    var ergebnis = methoden.last()
    for (i in methoden.lastIndex - 1 downTo 0) ergebnis = komponiere(methoden[i], ergebnis)
    return ergebnis
}
