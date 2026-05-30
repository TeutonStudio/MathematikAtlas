package com.TeutonStudio.KnotenKartenVerwalter.daten

/**
 * Beschreibt die aktuell ausgewaehlten Graph-Elemente.
 *
 * Die Auswahl ist als kontrollierter Zustand gedacht. Die UI kann eine neue Auswahl vorschlagen,
 * aber der aufrufende Code entscheidet, welche Elemente tatsaechlich als ausgewaehlt gespeichert
 * werden.
 */
data class AuswahlDaten(
    /** IDs aller ausgewaehlten Knoten. */
    val knotenIds: Set<String> = emptySet(),

    /** IDs aller ausgewaehlten Verbindungen. */
    val verbindungIds: Set<String> = emptySet(),
) {
    /** Wahr, wenn weder Knoten noch Verbindungen ausgewaehlt sind. */
    val istLeer: Boolean
        get() = knotenIds.isEmpty() && verbindungIds.isEmpty()
}
