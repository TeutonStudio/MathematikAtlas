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
 * Domänenneutrale, noch nicht ausführende Methodenkomposition.
 *
 * Die Signatur entsteht ausschließlich aus dem neutralen Typvertrag. Eine spätere
 * Script-/Engine-Capability kann die Ausführung ergänzen, ohne Mengen zu erfinden.
 */
data class KomponierteMethode(
    val methoden: List<Methode>,
) : SignaturtragendeMethode {
    init {
        require(methoden.size >= 2)
        val prüfung = prüfeKompositionsKette(methoden)
        require(prüfung.istGültig) { prüfung.fehler.joinToString("\n") }
    }

    override val name: String
        get() = methoden.joinToString("\\circ") { it.name }

    override val signatur: MethodenSignatur
        get() = MethodenSignatur(
            argumente = methoden.last().neutraleMethodenSignatur().argumente,
            ergebnisse = methoden.first().neutraleMethodenSignatur().ergebnisse,
        )
}

/**
 * Prüft eine sichtbare Methodenkette `f₁ ∘ f₂ ∘ … ∘ fₙ` von außen nach innen.
 *
 * Allgemein wird ausschließlich geprüft, ob das Ergebnis-Tupel der inneren Methode
 * typseitig in das Argument-Tupel der äußeren Methode passt. Für mathematische
 * Methoden kommt zusätzlich eine Mengen-/Raumprüfung hinzu. Mengengleichheit wird
 * ausdrücklich nicht verlangt.
 */
fun prüfeKompositionsKette(methoden: List<Methode>): KompositionsPrüfung {
    if (methoden.size < 2) {
        return KompositionsPrüfung(
            listOf(KompositionsFehler(1, 1, "Es werden mindestens zwei Methoden benötigt.")),
        )
    }

    val fehler = methoden.zipWithNext().mapIndexedNotNull { index, (außen, innen) ->
        val äußerePosition = index + 1
        val innerePosition = index + 2
        val außenSignatur = runCatching { außen.neutraleMethodenSignatur() }.getOrElse {
            return@mapIndexedNotNull KompositionsFehler(
                äußerePosition,
                innerePosition,
                "Die äußere Methode '${außen.name}' besitzt keine neutrale Methodensignatur.",
            )
        }
        val innenSignatur = runCatching { innen.neutraleMethodenSignatur() }.getOrElse {
            return@mapIndexedNotNull KompositionsFehler(
                äußerePosition,
                innerePosition,
                "Die innere Methode '${innen.name}' besitzt keine neutrale Methodensignatur.",
            )
        }

        when (val typPrüfung = MathematischeTypen.typSystem.prüfe(
            innenSignatur.ergebnisTupelTyp,
            außenSignatur.argumentTupelTyp,
        )) {
            is TypPrüfung.Inkompatibel -> KompositionsFehler(
                äußerePosition,
                innerePosition,
                "Die Ergebnis-Tupeltypen von '${innen.name}' sind nicht mit den Argument-Tupeltypen von " +
                    "'${außen.name}' kompatibel: ${typPrüfung.grund}",
            )
            is TypPrüfung.Unbestimmt -> KompositionsFehler(
                äußerePosition,
                innerePosition,
                "Die Typkompatibilität von '${innen.name}' nach '${außen.name}' ist unbestimmt: ${typPrüfung.grund}",
            )
            TypPrüfung.Kompatibel -> prüfeMathematischenÜbergangFallsVorhanden(
                außen = außen,
                innen = innen,
                äußerePosition = äußerePosition,
                innerePosition = innerePosition,
            )
        }
    }
    return KompositionsPrüfung(fehler)
}

private fun prüfeMathematischenÜbergangFallsVorhanden(
    außen: Methode,
    innen: Methode,
    äußerePosition: Int,
    innerePosition: Int,
): KompositionsFehler? {
    val außenMathematisch = außen as? MathematischeSignaturtragendeMethode ?: return null
    val innenMathematisch = innen as? MathematischeSignaturtragendeMethode ?: return null
    val außenSignatur = außenMathematisch.mathematischeSignatur
    val innenSignatur = innenMathematisch.mathematischeSignatur

    val mengenPrüfung = if (
        innen is MathematischeMethode && außen is MathematischeMethode &&
        innenSignatur.argumente.size == 1 && innenSignatur.ergebnisse.size == 1 &&
        außenSignatur.argumente.size == 1
    ) {
        prüfeMathematischenKompositionsÜbergang(innen, außen)
    } else {
        prüfeTeilmenge(innenSignatur.zielRaum, außenSignatur.definitionsRaum)
    }

    return when (mengenPrüfung.wahrheitswert) {
        Wahrheitswert.Wahr -> null
        Wahrheitswert.Lüge -> KompositionsFehler(
            äußerePosition,
            innerePosition,
            "Die Bild-/Zielmenge von '${innen.name}' liegt nicht im Definitionsraum von '${außen.name}'.",
        )
        null -> KompositionsFehler(
            äußerePosition,
            innerePosition,
            "Die mathematische Bild-/Zielmengenkompatibilität von '${innen.name}' nach '${außen.name}' ist unbestimmt.",
        )
    }
}

/**
 * Bildet eine beliebig lange Methodenkette.
 *
 * Rein typbasierte Methoden ergeben eine [KomponierteMethode]. Wenn alle Glieder
 * konkrete symbolische mathematische Methoden sind und die aktuelle symbolische
 * Ein-Ausgabe-Form unterstützen, wird weiterhin eine auswertbare mathematische
 * Vorschrift materialisiert.
 */
fun komponiere(methoden: List<Methode>): Methode {
    val prüfung = prüfeKompositionsKette(methoden)
    require(prüfung.istGültig) { prüfung.fehler.joinToString("\n") }

    val mathematischeMethoden = methoden.map { it as? MathematischeMethode }
    if (mathematischeMethoden.any { it == null }) return KomponierteMethode(methoden)
    val konkret = mathematischeMethoden.filterNotNull()

    val symbolischEinwertig = konkret.zipWithNext().all { (außen, innen) ->
        außen.parameter.size == 1 && innen.ausgabeNamen.size == 1
    } && konkret.all { it.ausgabeNamen.size == 1 }
    if (!symbolischEinwertig) return KomponierteMethode(methoden)

    val innerste = konkret.last()
    val (_, innererAusdruck) = innerste.einzigeAusgabe()
    var ausdruck = innererAusdruck

    for (index in konkret.lastIndex - 1 downTo 0) {
        val außen = konkret[index]
        val parameter = außen.parameter.single()
        val (_, äußererAusdruck) = außen.einzigeAusgabe()
        ausdruck = ersetze(äußererAusdruck, mapOf(parameter.name to ausdruck))
    }

    val äußerste = konkret.first()
    val äußererAusgabeName = äußerste.ausgabeNamen.single()
    return Methode(
        name = konkret.joinToString("\\circ") { it.name },
        parameter = innerste.parameter,
        vorschrift = ausdruck,
        zielMenge = äußerste.zielMengeFür(äußererAusgabeName),
        werteVorräte = innerste.werteVorräte,
        effektiverWerteVorrat = innerste.mathematischeSignatur.effektiverDefinitionsRaum,
    )
}
