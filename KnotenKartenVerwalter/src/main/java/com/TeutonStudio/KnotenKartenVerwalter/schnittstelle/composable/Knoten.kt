package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussFabrik
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.KnotenAnschlüsse
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Anschluss
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.AnschlussModifierStandard
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Knoten
import kotlin.collections.forEach

/**
 * Standarddarstellung eines Knotens.
 *
 * Der Inhalt bleibt innerhalb des Rahmens, während die Anschlüsse links und
 * rechts auf dem Rahmen liegen.
 */
@Composable
public fun KnotenRahmen(
    knoten: Knoten,
    anschlüsse:  Map<Anschluss, Int>,
    modifierKnoten: Modifier = Modifier,
    modifierAnschluss: (AnschlussDaten, Int) -> Modifier = { daten, idx -> AnschlussModifierStandard },
    inhaltSkalierung: Float = 1f,
) {
    val randFarbe = if (knoten.daten.ausgewaehlt) Color(0xFF2563EB) else Color(0xFF64748B)
    val skalierung = inhaltSkalierung.coerceAtLeast(0.1f)
    val form = RoundedCornerShape((8f * skalierung).dp)
    Box(
        modifier = modifierKnoten
            .border((1f * skalierung).dp, randFarbe, form)
            .background(Color.White, form)
            .draggable2D(
                state = rememberDraggable2DState {
                    knoten.daten.position += it
                },
                enabled = knoten.daten.beweglich,
            )
            .size(140.dp,80.dp)
            .offset(knoten.daten.position.x.dp,knoten.daten.position.y.dp),
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = (18f * skalierung).dp, vertical = (10f * skalierung).dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(15.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
            ) { Text(knoten.daten.name) }
        }
        AnschlussKante.entries.forEach { kante ->
            Box(
                modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight().offset(x = (-5f * skalierung).dp),
                contentAlignment = Alignment.Center,
            ) { anschlüsse.zuLeiste(kante,modifierAnschluss) }
        }
    }
}
