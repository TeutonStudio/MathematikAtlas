package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.Aussage

/**
 * Sammelt die an allen verbundenen Eingängen bereits nachgewiesenen Annahmen.
 *
 * Die Hilfe ist modulweit sichtbar, damit Auswerter ohne eigene dateilokale
 * Kopie denselben Laufzeitkontext weiterreichen können. Der optionale Marker
 * hält die Signatur absichtlich von älteren dateiprivaten annahmen()-Hilfen
 * getrennt: Dort gewinnt der exakte parameterlose Overload, während neue
 * Auswerter diesen gemeinsamen Fallback weiterhin parameterlos aufrufen.
 */
internal fun KnotenAuswertungsKontext.annahmen(
    @Suppress("UNUSED_PARAMETER") gemeinsamerFallback: Unit = Unit,
): Set<Aussage> = eingänge.values.flatMap { it.annahmen }.toSet()
