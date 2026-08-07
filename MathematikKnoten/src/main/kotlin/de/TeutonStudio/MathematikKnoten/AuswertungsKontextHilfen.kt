package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.Aussage

/**
 * Sammelt die an allen verbundenen Eingängen bereits nachgewiesenen Annahmen.
 *
 * Die Hilfe ist modulweit sichtbar, damit Auswerter ohne eigene dateilokale
 * Kopie denselben Laufzeitkontext weiterreichen können.
 */
internal fun KnotenAuswertungsKontext.annahmen(): Set<Aussage> =
    eingänge.values.flatMap { it.annahmen }.toSet()
