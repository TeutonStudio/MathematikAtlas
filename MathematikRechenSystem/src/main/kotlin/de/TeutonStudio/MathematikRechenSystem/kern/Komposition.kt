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
        val äußerePosition = index + 1
        val innerePosition = index + 2
        val außenSignatur = außen.methodenSignatur()
        val innenSignatur = innen.methodenSignatur()

        when {
            außenSignatur.argumente.size != 1 -> KompositionsFehler(
                äußerePosition,
                innerePosition,
                "Die äußere Methode '${außen.name}' benötigt genau eine Argumentkomponente, besitzt aber ${außenSignatur.argumente.size}.",
            )

            innenSignatur.ergebnisse.size != 1 -> KompositionsFehler(
                äußerePosition,
                innerePosition,
                "Die innere Methode '${innen.name}' benötigt genau eine Ergebniskomponente, besitzt aber ${innenSignatur.ergebnisse.size}.",
            )

            else -> {
                val typPrüfung = MathematischeTypen.typSystem.prüfe(
                    innenSignatur.ergebnisTyp,
                    außenSignatur.argumentTyp,
                )
                if (typPrüfung is TypPrüfung.Inkompatibel) {
                    KompositionsFehler(
                        äußerePosition,
                        innerePosition,
                        "Die Tupeltypen von '${innen.name}' und '${außen.name}' passen nicht zusammen: ${typPrüfung.grund}",
                    )
                } else {
                    val außenMathematisch = außen as? MathematischeSignaturtragendeMethode
                    val innenMathematisch = innen as? MathematischeSignaturtragendeMethode
                    if (außenMathematisch == null || innenMathematisch == null) {
                        null
                    } else {
                        val definitionsMenge = außenMathematisch.mathematischeSignatur.argumente.single().definitionsMenge
                        val zielMenge = innenMathematisch.mathematischeSignatur.ergebnisse.single().zielMenge
                        val teilmenge = prüfeTeilmenge(zielMenge, definitionsMenge, kontext)
                        when (teilmenge.wahrheitswert) {
                            Wahrheitswert.Lüge -> KompositionsFehler(
                                äußerePosition,
                                innerePosition,
                                "Zielmenge ${zielMenge.zuLatex()} von '${innen.name}' passt nicht zum Wertevorrat ${definitionsMenge.zuLatex()} von '${außen.name}'.",
                            )
                            null -> KompositionsFehler(
                                äußerePosition,
                                innerePosition,
                                "Für ${zielMenge.zuLatex()} ⊆ ${definitionsMenge.zuLatex()} ist die Totalität der Komposition noch nicht nachgewiesen.",
                            )
                            Wahrheitswert.Wahr -> null
                        }
                    }
                }
            }
        }
    }
    return KompositionsPrüfung(fehler)
}

/** Bildet eine beliebig lange, semantisch geprüfte Methodenkette. */
fun komponiere(methoden: List<Methode>): Methode {
    val prüfung = prüfeKompositionsKette(methoden)
    require(prüfung.istGültig) { prüfung.fehler.joinToString("\n") }

    val innerste = methoden.last().alsMathematischeMethode("symbolische Komposition")
    var ausdruck = innerste.einzigeAusgabe().second

    for (index in methoden.lastIndex - 1 downTo 0) {
        val außen = methoden[index].alsMathematischeMethode("symbolische Komposition")
        val parameter = außen.parameter.single()
        val äußererAusdruck = außen.einzigeAusgabe().second
        ausdruck = ersetze(äußererAusdruck, mapOf(parameter.name to ausdruck))
    }

    val äußerste = methoden.first().alsMathematischeMethode("symbolische Komposition")
    val äußererAusgabeName = äußerste.ausgabeNamen.single()
    return Methode(
        name = methoden.joinToString("\\circ") { it.name },
        parameter = innerste.parameter,
        vorschrift = ausdruck,
        zielMenge = äußerste.zielMengeFür(äußererAusgabeName),
        werteVorräte = innerste.werteVorräte,
        effektiverWerteVorrat = innerste.effektiverWerteVorrat,
    )
}
