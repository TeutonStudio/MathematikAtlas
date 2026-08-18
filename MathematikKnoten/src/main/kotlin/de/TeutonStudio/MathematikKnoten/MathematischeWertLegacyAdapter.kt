package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikRechenSystem.kern.AtlasWert
import de.TeutonStudio.MathematikRechenSystem.kern.LatexDarstellbar
import de.TeutonStudio.MathematikRechenSystem.kern.MathematischesObjekt
import de.TeutonStudio.MathematikRechenSystem.kern.MengenAusdruck
import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import de.TeutonStudio.MathematikRechenSystem.kern.NumerischeKomponentenAnsicht
import de.TeutonStudio.MathematikRechenSystem.kern.StrukturPruefung
import de.TeutonStudio.MathematikRechenSystem.kern.numerischeKomponentenAnsicht as kernNumerischeKomponentenAnsicht
import de.TeutonStudio.MathematikRechenSystem.kern.wendeAn as kernWendeAn

/**
 * Übergangsadapter für mathematische Knoten, die vor G0.5 direkt mit
 * `BedingterWert.objekt` gerechnet haben. Jede Operation verengt am Aufrufpunkt und
 * lässt den allgemeinen AtlasWert-Kanal unverändert neutral.
 */
internal fun AtlasWert.zuLatex(): String = when (this) {
    is MathematischesObjekt -> zuLatex()
    is LatexDarstellbar -> zuLatex()
    is Methode -> name
    else -> error("Für ${this::class.simpleName ?: "AtlasWert"} existiert keine mathematische LaTeX-Darstellung.")
}

internal fun AtlasWert.numerischeKomponentenAnsicht(
    zahlAlsSingleton: Boolean = false,
    werteVorraete: Map<String, MengenAusdruck> = emptyMap(),
): StrukturPruefung<NumerischeKomponentenAnsicht> =
    alsMathematischesObjekt("Numerische Komponentenansicht")
        .kernNumerischeKomponentenAnsicht(zahlAlsSingleton, werteVorraete)

internal fun Methode.wendeAn(argumente: List<AtlasWert>): MathematischesObjekt =
    this.kernWendeAn(argumente.mapIndexed { index, wert ->
        wert.alsMathematischesObjekt("Methodenargument ${index + 1}")
    })
