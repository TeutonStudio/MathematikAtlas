package com.TeutonStudio.KnotenKartenVerwalter.daten.knoten

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.TeutonStudio.KnotenKartenVerwalter.daten.GraphDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten

interface AnschlüsseDaten<D: AnschlussDaten>: GraphDaten {
    val anschlüsse: SnapshotStateList<D>
}