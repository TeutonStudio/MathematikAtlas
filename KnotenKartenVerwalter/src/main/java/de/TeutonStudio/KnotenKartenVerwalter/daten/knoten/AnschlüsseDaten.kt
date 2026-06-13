package de.TeutonStudio.KnotenKartenVerwalter.daten.knoten

import androidx.compose.runtime.snapshots.SnapshotStateList
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten

interface AnschlüsseDaten<D: AnschlussDaten>: GraphDaten {
    val anschlüsse: SnapshotStateList<D>
}