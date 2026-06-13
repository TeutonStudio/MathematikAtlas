package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.Karte

interface GraphKarte {

    @Composable public fun Karte.zuÜbersicht() {
        Card(Modifier.padding(5.dp).fillMaxSize().clipToBounds()) {
            Box(Modifier) {
                // Übersichtkarte
            }
        }
    }
}