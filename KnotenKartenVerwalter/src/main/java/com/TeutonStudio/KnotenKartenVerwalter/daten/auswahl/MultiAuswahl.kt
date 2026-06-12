package com.TeutonStudio.KnotenKartenVerwalter.daten.auswahl

import com.TeutonStudio.KnotenKartenVerwalter.daten.GraphDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjekt
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphObjekt

data class MultiAuswahl(
    /** IDs aller ausgewählten Knoten. */
    val knotenIds: Set<String> = emptySet(),

    /** IDs aller ausgewählten Verbindungen. */
    val verbindungIds: Set<String> = emptySet(),

    /** IDs aller ausgweählten Anschlüsse. */
    val anschlussIds: Set<String> = emptySet()
): AuswahlDaten {
    fun nurKnoten(knotenId: String) = MultiAuswahl(knotenIds = setOf(knotenId))

    fun nurVerbindung(verbindungId: String) = MultiAuswahl(verbindungIds = setOf(verbindungId))

    fun mitKnoten(knotenId: String) = copy(knotenIds = knotenIds + knotenId)

    fun ohneKnoten(knotenId: String) = copy(knotenIds = knotenIds - knotenId)

    fun mitVerbindung(verbindungId: String) = copy(verbindungIds = verbindungIds + verbindungId)

    fun ohneVerbindung(verbindungId: String) = copy(verbindungIds = verbindungIds - verbindungId)

    fun umgeschalteterKnoten(knotenId: String) = if (knotenId in knotenIds) ohneKnoten(knotenId) else mitKnoten(knotenId)

    fun umgeschalteteVerbindung(verbindungId: String) = if (verbindungId in verbindungIds) ohneVerbindung(verbindungId) else mitVerbindung(verbindungId)

    fun plus(andere: MultiAuswahl) = MultiAuswahl(
        knotenIds = knotenIds + andere.knotenIds,
        verbindungIds = verbindungIds + andere.verbindungIds,
    )

    override fun enthält(gO: GraphObjekt): Boolean = knotenIds.contains(gO.daten.id) || verbindungIds.contains(gO.daten.id) || anschlussIds.contains(gO.daten.id)
}