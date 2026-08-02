package de.TeutonStudio.MathematikRechenSystem.kern

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
 * Die innerste Methode darf mehrere Parameter besitzen. Jede weiter außen liegende
 * Methode muss genau einen Parameter und jede weiter innen liegende Methode genau
 * eine Ausgabe besitzen, damit das Ergebnis eindeutig weitergereicht werden kann.
 */
fun prüfeKompositionsKette(methoden: List<Funktion>): KompositionsPrüfung {
    if (methoden.size < 2) {
        return KompositionsPrüfung(
            listOf(KompositionsFehler(1, 1, "Es werden mindestens zwei Methoden benötigt.")),
        )
    }

    val fehler = methoden.zipWithNext().mapIndexedNotNull { index, (außen, innen) ->
        val äußerePosition = index + 1
        val innerePosition = index + 2
        when {
            außen.parameter.size != 1 -> KompositionsFehler(
                äußerePosition,
                innerePosition,
                "Die äußere Methode '${außen.name}' benötigt genau einen Parameter, besitzt aber ${außen.parameter.size}.",
            )

            innen.ausgaben.size != 1 -> KompositionsFehler(
                äußerePosition,
                innerePosition,
                "Die innere Methode '${innen.name}' benötigt genau eine Ausgabe, besitzt aber ${innen.ausgaben.size}.",
            )

            außen.werteVorräte[außen.parameter.single().name] == null -> KompositionsFehler(
                äußerePosition,
                innerePosition,
                "Für den Parameter '${außen.parameter.single().name}' der äußeren Methode fehlt der Wertevorrat.",
            )

            innen.zielMengen[innen.ausgaben.keys.single()] == null -> KompositionsFehler(
                äußerePosition,
                innerePosition,
                "Für die Ausgabe '${innen.ausgaben.keys.single()}' der inneren Methode fehlt die Zielmenge.",
            )

            innen.zielMengen.getValue(innen.ausgaben.keys.single()) !=
                außen.werteVorräte.getValue(außen.parameter.single().name) -> KompositionsFehler(
                äußerePosition,
                innerePosition,
                "Zielmenge ${innen.zielMengen.getValue(innen.ausgaben.keys.single()).zuLatex()} " +
                    "von '${innen.name}' passt nicht zum Wertevorrat " +
                    "${außen.werteVorräte.getValue(außen.parameter.single().name).zuLatex()} von '${außen.name}'.",
            )

            else -> null
        }
    }
    return KompositionsPrüfung(fehler)
}

/** Bildet eine beliebig lange, semantisch geprüfte Methodenkette. */
fun komponiere(methoden: List<Funktion>): Funktion {
    val prüfung = prüfeKompositionsKette(methoden)
    require(prüfung.istGültig) { prüfung.fehler.joinToString("\n") }

    val innerste = methoden.last()
    val (_, innererAusdruck) = innerste.einzigeAusgabe()
    var ausdruck = innererAusdruck

    for (index in methoden.lastIndex - 1 downTo 0) {
        val außen = methoden[index]
        val parameter = außen.parameter.single()
        val (_, äußererAusdruck) = außen.einzigeAusgabe()
        ausdruck = ersetze(äußererAusdruck, mapOf(parameter.name to ausdruck))
    }

    val äußerste = methoden.first()
    val äußererAusgabeName = äußerste.ausgaben.keys.single()
    return Funktion(
        name = methoden.joinToString("\\circ") { it.name },
        parameter = innerste.parameter,
        ausgaben = mapOf("wert" to ausdruck),
        zielMengen = mapOf("wert" to äußerste.zielMengeFür(äußererAusgabeName)),
        werteVorräte = innerste.werteVorräte,
    )
}
