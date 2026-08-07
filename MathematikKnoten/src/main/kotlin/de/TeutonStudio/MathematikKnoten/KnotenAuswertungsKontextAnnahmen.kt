package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsKontext
import de.TeutonStudio.MathematikRechenSystem.kern.Aussage

/**
 * Paketweiter Vertrag für Auswerter, die die Annahmen aller belegten Eingänge
 * unverändert an ihre Ausgaben weiterreichen müssen.
 */
internal fun KnotenAuswertungsKontext.gemeinsameEingangsAnnahmen(): Set<Aussage> =
    eingänge.values.flatMap { it.annahmen }.toSet()
