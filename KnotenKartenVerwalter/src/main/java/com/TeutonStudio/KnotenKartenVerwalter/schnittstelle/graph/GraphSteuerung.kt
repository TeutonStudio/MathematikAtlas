package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.Karte

interface GraphSteuerung {
    var aktuell: Int
    val verlauf: SnapshotStateMap<Int, Any>

    public fun neueAktion(aktion: Any) {
        val idx = verlauf.maxBy { it.key }.key + 1
        verlauf.filter { it.key > idx }.keys.forEach(verlauf::remove)
        verlauf[idx] = aktion
    }

    @Composable public fun Karte.zuSteuerung() {
        Card(Modifier.padding(5.dp).fillMaxHeight()) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.SpaceEvenly) {
                Box(Modifier.size(15.dp)) {
                    // Auf Inhalt zoomen
                }
                Box(Modifier.size(15.dp)) {
                    // Rückgängig
                }
                Box(Modifier.size(15.dp)) {
                    // Wiederholen
                }
            }
        }
    }
}