package de.TeutonStudio.KnotenKartenVerwalter.daten.knoten

import androidx.compose.runtime.snapshots.SnapshotStateList
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussGraphDaten

/** Datenvertrag für Graphdaten, die Anschlussdaten besitzen. */
interface AnschlüsseDaten<D: AnschlussGraphDaten>: GraphDaten {
    val anschlüsse: SnapshotStateList<D>
}
