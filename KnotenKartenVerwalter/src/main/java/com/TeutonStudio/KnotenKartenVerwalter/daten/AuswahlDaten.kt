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

    fun nurKnoten(knotenId: String): AuswahlDaten = AuswahlDaten(knotenIds = setOf(knotenId))

    fun nurVerbindung(verbindungId: String): AuswahlDaten = AuswahlDaten(verbindungIds = setOf(verbindungId))

    fun mitKnoten(knotenId: String): AuswahlDaten = copy(knotenIds = knotenIds + knotenId)

    fun ohneKnoten(knotenId: String): AuswahlDaten = copy(knotenIds = knotenIds - knotenId)

    fun mitVerbindung(verbindungId: String): AuswahlDaten = copy(verbindungIds = verbindungIds + verbindungId)

    fun ohneVerbindung(verbindungId: String): AuswahlDaten = copy(verbindungIds = verbindungIds - verbindungId)

    fun umgeschalteterKnoten(knotenId: String): AuswahlDaten =
        if (knotenId in knotenIds) ohneKnoten(knotenId) else mitKnoten(knotenId)

    fun umgeschalteteVerbindung(verbindungId: String): AuswahlDaten =
        if (verbindungId in verbindungIds) ohneVerbindung(verbindungId) else mitVerbindung(verbindungId)

    fun plus(andere: AuswahlDaten): AuswahlDaten = AuswahlDaten(
        knotenIds = knotenIds + andere.knotenIds,
        verbindungIds = verbindungIds + andere.verbindungIds,
    )
}
