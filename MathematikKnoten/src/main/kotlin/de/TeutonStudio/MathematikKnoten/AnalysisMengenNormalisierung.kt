package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.MengenAusdruck
import de.TeutonStudio.MathematikRechenSystem.kern.normalisiereMenge

/**
 * Nachnormalisierung des finalen Analysis-Eigenschaftsauswerters.
 *
 * Die fachliche Ableitungsanalyse bleibt an ihrer bisherigen Stelle. Hier wird
 * ausschließlich das resultierende Mengenobjekt kanonisiert, damit Ergebnis,
 * Renderer und Visualisierer denselben strukturellen Wert erhalten.
 */
fun MathematikAuswerterRegister.registriereAnalysisMengenNormalisierung() {
    val basis = requireNotNull(finde(ANALYSIS_EIGENSCHAFT_KNOTEN_ART))
    registriere(ANALYSIS_EIGENSCHAFT_KNOTEN_ART) { kontext ->
        val ergebnis = basis.auswerten(kontext)
        ergebnis.copy(
            ausgaben = ergebnis.ausgaben.mapValues { (_, wert) ->
                val menge = wert.objekt as? MengenAusdruck ?: return@mapValues wert
                wert.copy(objekt = normalisiereMenge(menge, kontext.rechenKontext), latexDarstellung = null)
            },
        )
    }
}
