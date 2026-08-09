package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator

/**
 * Nichtwerfende, familienweite Auflösung persistierter Zahlenrechner-IDs.
 *
 * Die Auswertung bleibt in den bestehenden Standard-/Erweiterungsregistern.
 * Diese Funktion dient ausschließlich gemeinsamer Persistenz- und UI-Logik.
 */
fun zahlenRechnerOperatorTitelOderNull(id: String?): String? =
    UniversellerZahlenOperator.vonIdOderNull(id)?.titel
        ?: ErweiterterZahlenOperator.vonId(id)?.titel
        ?: if (id == ZAHLENRECHNER_FORMEL_ID) "Formel" else null

/**
 * Ersetzt nur automatisch verwaltete Operatornamen. Ein vom Nutzer vergebener
 * Knotenname bleibt auch beim Wechsel des Operatorzustands erhalten.
 */
internal fun zahlenRechnerNameFürWechsel(
    knoten: KnotenDaten,
    neuerTitel: String,
): String {
    val bisherigerTitel = zahlenRechnerOperatorTitelOderNull(
        knoten.parameter[ZAHLENRECHNER_OPERATOR],
    )
    return if (bisherigerTitel != null && knoten.name == bisherigerTitel) {
        neuerTitel
    } else {
        knoten.name
    }
}
