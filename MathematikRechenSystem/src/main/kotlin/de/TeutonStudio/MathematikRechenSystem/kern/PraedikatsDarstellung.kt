package de.TeutonStudio.MathematikRechenSystem.kern

class MethodenSignaturFehler(message: String) : IllegalArgumentException(message)

sealed interface PrädikatsArgument {
    val name: String
    val identität: String

    data class Wert(
        override val name: String,
        val werteVorrat: MengenAusdruck,
        override val identität: String = "wert:$name",
    ) : PrädikatsArgument

    data class AussageWert(
        override val name: String,
        val latex: String = name,
        override val identität: String = "aussage:$name",
    ) : PrädikatsArgument
}

data class AufgelöstesPrädikat(
    val methode: Methode,
    val aussage: Aussage,
    val argumente: List<PrädikatsArgument>,
) {
    val enthältWerteArgument: Boolean get() = argumente.any { it is PrädikatsArgument.Wert }
}

fun Methode.istOffenesPrädikat(): Boolean = istPrädikat() && argumentAnzahl > 0

fun GebundeneMethode.istOffenesPrädikat(): Boolean =
    methode.istPrädikat() && freieParameter.isNotEmpty()

/**
 * Löst eine Prädikatsmethode ohne Wahrheitsentscheidung auf.
 *
 * Externe Argumentquellen dürfen stabile Graphidentitäten beisteuern. Fehlen sie,
 * wird die geordnete Methodensignatur verwendet. Dadurch bleibt der Rechenkern
 * Android- und Karteneditor-frei, während der Adapter semantische Deduplizierung
 * liefern kann.
 */
fun Methode.lösePrädikatAuf(
    bindungen: Map<String, MathematischesObjekt> = emptyMap(),
    argumentQuellen: List<PrädikatsArgument> = emptyList(),
): AufgelöstesPrädikat {
    if (!istPrädikat()) {
        throw MethodenSignaturFehler("Die Methode '$name' erfüllt das Prädikatskriterium nicht.")
    }

    val mathematisch = alsMathematischeMethode("Prädikatsdarstellung")
    val aussageVorlage = mathematisch.einzigeAusgabe().second as? Aussage
        ?: throw MethodenSignaturFehler("Die Methode '$name' besitzt keine einzelne Aussageausgabe.")
    val aussage = ersetze(aussageVorlage, bindungen) as? Aussage
        ?: throw MethodenSignaturFehler("Die gebundene Ausgabe der Methode '$name' ist keine Aussage.")
    val externeNachName = argumentQuellen.groupBy(PrädikatsArgument::name)
    val argumente = mathematisch.parameter
        .filterNot { it.name in bindungen }
        .flatMap { parameter ->
            externeNachName[parameter.name].orEmpty().ifEmpty {
                listOf(
                    when (parameter) {
                        is AussagenParameter -> PrädikatsArgument.AussageWert(parameter.name, parameter.zuLatex())
                        else -> PrädikatsArgument.Wert(
                            name = parameter.name,
                            werteVorrat = mathematisch.werteVorräte[parameter.name]
                                ?.let { ersetze(it, bindungen) as MengenAusdruck }
                                ?: throw MethodenSignaturFehler(
                                    "Für das Prädikatsargument '${parameter.name}' konnte kein Wertevorrat ermittelt werden.",
                                ),
                        )
                    },
                )
            }
        }
        .distinctBy(PrädikatsArgument::identität)

    return AufgelöstesPrädikat(mathematisch, aussage, argumente)
}

/** Nutzt die echten Methodendomänen, wenn keine externen Quellen übergeben wurden. */
private fun Methode.standardPrädikatsArgumente(
    bindungen: Map<String, MathematischesObjekt>,
): List<PrädikatsArgument> {
    val mathematisch = alsMathematischeMethode("Prädikatsdarstellung")
    return mathematisch.parameter
        .filterNot { it.name in bindungen }
        .map { parameter ->
            when (parameter) {
                is AussagenParameter -> PrädikatsArgument.AussageWert(parameter.name, parameter.zuLatex())
                else -> PrädikatsArgument.Wert(
                    name = parameter.name,
                    werteVorrat = mathematisch.werteVorräte[parameter.name]
                        ?.let { ersetze(it, bindungen) as MengenAusdruck }
                        ?: throw MethodenSignaturFehler(
                            "Für das Prädikatsargument '${parameter.name}' konnte kein Wertevorrat ermittelt werden.",
                        ),
                )
            }
        }
}

fun Methode.kompaktePrädikatsDarstellung(
    bindungen: Map<String, MathematischesObjekt> = emptyMap(),
    argumentQuellen: List<PrädikatsArgument> = emptyList(),
): String {
    val argumente = if (argumentQuellen.isEmpty()) standardPrädikatsArgumente(bindungen) else argumentQuellen
    return lösePrädikatAuf(bindungen, argumente).kompaktZuLatex()
}

fun AufgelöstesPrädikat.kompaktZuLatex(): String {
    val methodenName = methode.name.trim().ifEmpty { "P" }
    if (!enthältWerteArgument) return "$methodenName:${aussage.zuLatex()}"

    val signatur = argumente.joinToString("\\times") { argument ->
        when (argument) {
            is PrädikatsArgument.Wert -> argument.werteVorrat.zuLatex()
            is PrädikatsArgument.AussageWert -> "\\{${argument.latex}\\}"
        }
    }
    return "$methodenName:$signatur"
}

/** Vollständige Anwendung liefert eine Aussage, selbst wenn diese unentscheidbar bleibt. */
fun GebundeneMethode.wertePrädikatAus(): Aussage {
    require(methode.istPrädikat()) { "Die gebundene Methode '${methode.name}' ist kein Prädikat." }
    require(freieParameter.isEmpty()) { "Das Prädikat '${methode.name}' besitzt noch freie Argumente." }
    return methode.wendeKanonischAn(bindungen) as? Aussage
        ?: error("Das Prädikat '${methode.name}' hat keine Aussage geliefert.")
}

fun GebundeneMethode.wahrheitstabellenErgebnis(
    kontext: RechenKontext = RechenKontext(),
): AussageErgebnis = wertePrädikatAus().entscheide(kontext)
