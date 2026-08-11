package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis

/**
 * Verbindet statische Portverträge mit der präziseren, tatsächlich ausgewerteten
 * Mathematik. Der Laufzeitwert darf den Porttyp nur verfeinern; ist er nicht
 * typisierbar, bleibt der deklarierte Vertrag die Wahrheit.
 */
fun KnotenAuswertungsErgebnis?.effektiverMathematikTyp(
    anschluss: AnschlussDaten,
): TypAusdruck {
    val wert = when (anschluss.richtung) {
        AnschlussRichtung.Eingang -> this?.eingänge?.get(anschluss.name)
        AnschlussRichtung.Ausgang -> this?.ausgaben?.get(anschluss.name)
        AnschlussRichtung.Neutral -> this?.ausgaben?.get(anschluss.name) ?: this?.eingänge?.get(anschluss.name)
    }
    val laufzeitTyp = wert?.objekt?.let(MathematikTypResolver::objektTyp)
    return laufzeitTyp?.takeUnless { it == TypAusdruck.Unbekannt }
        ?: anschluss.vertrag.typ
}

fun KnotenAuswertungsErgebnis?.typVisualisierungFür(
    anschluss: AnschlussDaten,
): TypVisualDescriptor = MathematikTypVisualResolver.beschreibe(
    effektiverMathematikTyp(anschluss),
)
