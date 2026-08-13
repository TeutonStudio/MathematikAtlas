package de.TeutonStudio.MathematikRechenSystem.kern

import de.TeutonStudio.TypSystem.TypPrüfung

/**
 * Zentralisierte, bewusst zeitlich begrenzte Quellkompatibilität für den G0/#431-Refactor.
 *
 * Nichts in dieser Datei gehört zum allgemeinen [Methode]-Vertrag. Die Adapter existieren
 * nur, damit mathematischer Bestandscode an einer expliziten Mathematikgrenze weiterläuft,
 * während neue Script-/Engine-Methoden keinerlei mathematische API implementieren müssen.
 */

@Deprecated(
    "Historischer Name; verwende MathematischeArgumentKomponente.",
    ReplaceWith("MathematischeArgumentKomponente"),
)
typealias MethodenArgument = MathematischeArgumentKomponente

@Deprecated(
    "Historischer Name; verwende definitionsMenge.",
    ReplaceWith("definitionsMenge"),
)
val MathematischeArgumentKomponente.werteVorrat: MengenAusdruck
    get() = definitionsMenge

/** Eindeutige Substitutionsüberladung für bereits mathematisch verengte Methoden. */
@Deprecated("Verwende die symbolische MathematischeMethode direkt.")
fun ersetze(
    methode: MathematischeMethode,
    bindungen: Map<String, MathematischesObjekt>,
): MathematischeMethode = ersetze(
    methode as MathematischesObjekt,
    bindungen,
) as MathematischeMethode

/** Mathematische Substitution einer statisch nur als allgemeine Methode bekannten Methode. */
@Deprecated("Verenge den Aufrufer auf MathematischeMethode bzw. SymbolischMathematischeMethode.")
fun ersetze(
    methode: Methode,
    bindungen: Map<String, MathematischesObjekt>,
): Methode = ersetze(
    methode.alsMathematischeMethode("mathematische Substitution"),
    bindungen,
)

/** Mathematische Parameteranalyse an einer expliziten Capability-Grenze. */
@Deprecated("Verenge den Aufrufer auf eine mathematische Methode.")
fun Methode.enthalteneMethodenParameter(): Set<MethodenParameter> =
    (this as? MathematischesObjekt)?.enthalteneMethodenParameter().orEmpty()

/** Mathematische Variablenanalyse an einer expliziten Capability-Grenze. */
@Deprecated("Verenge den Aufrufer auf eine mathematische Methode.")
fun Methode.enthalteneVariablen(): Set<Variable> =
    enthalteneMethodenParameter().filterIsInstance<Variable>().toSet()

/** Unterstützt bestehende mathematische Traversierungen mit gemischten Mengen-/Methodenwerten. */
@Deprecated("Verenge gemischte Traversierungen auf ihre mathematischen Capabilities.")
fun Iterable<AtlasWert>.enthalteneMethodenParameter(): Set<MethodenParameter> =
    flatMap { wert ->
        when (wert) {
            is Methode -> wert.enthalteneMethodenParameter()
            is MathematischesObjekt -> wert.enthalteneMethodenParameter()
            else -> emptySet()
        }
    }.toSet()

/**
 * Neutrale Darstellungsprojektion. Sie ist eine Adapterfunktion und gerade deshalb kein
 * Bestandteil von [AtlasWert] oder [Methode].
 */
fun Methode.zuLatex(): String = when (this) {
    is LatexDarstellbar -> (this as LatexDarstellbar).zuLatex()
    else -> name
}

/** Einheitliche Diagnose für den offenen Typprüfungsstatus. */
val TypPrüfung.grund: String
    get() = when (this) {
        TypPrüfung.Kompatibel -> "kompatibel"
        is TypPrüfung.Unbestimmt -> grund
        is TypPrüfung.Inkompatibel -> grund
    }
