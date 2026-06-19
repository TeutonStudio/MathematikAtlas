package de.TeutonStudio.KnotenKartenVerwalter.daten.auswahl

import de.TeutonStudio.KnotenKartenVerwalter.daten.auswahl.AuswahlDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjekt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphObjekt

/*
data class MultiAuswahl(
    */
/** IDs aller ausgewählten Knoten. *//*

    val knotenIds: Set<String> = emptySet(),

    */
/** IDs aller ausgewählten Verbindungen. *//*

    val verbindungIds: Set<String> = emptySet(),

    */
/** IDs aller ausgweählten Anschlüsse. *//*

    val anschlussIds: Set<String> = emptySet()
): AuswahlDaten {
    public fun nurKnoten(knotenId: String) = MultiAuswahl(knotenIds = setOf(knotenId))

    public fun nurVerbindung(verbindungId: String) = MultiAuswahl(verbindungIds = setOf(verbindungId))

    public fun mitKnoten(knotenId: String) = copy(knotenIds = knotenIds + knotenId)

    public fun ohneKnoten(knotenId: String) = copy(knotenIds = knotenIds - knotenId)

    public fun mitVerbindung(verbindungId: String) = copy(verbindungIds = verbindungIds + verbindungId)

    public fun ohneVerbindung(verbindungId: String) = copy(verbindungIds = verbindungIds - verbindungId)

    public fun umgeschalteterKnoten(knotenId: String) = if (knotenId in knotenIds) ohneKnoten(knotenId) else mitKnoten(knotenId)

    public fun umgeschalteteVerbindung(verbindungId: String) = if (verbindungId in verbindungIds) ohneVerbindung(verbindungId) else mitVerbindung(verbindungId)

    public fun plus(andere: MultiAuswahl) = MultiAuswahl(
        knotenIds = knotenIds + andere.knotenIds,
        verbindungIds = verbindungIds + andere.verbindungIds,
    )

    public override fun enthält(gO: GraphObjekt): Boolean = knotenIds.contains(gO.daten.id) || verbindungIds.contains(gO.daten.id) || anschlussIds.contains(gO.daten.id)
}
*/
