package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
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
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.AnschlussDaten
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
    knoten: Knoten, anschlussFabrik: AnschlussFabrik,
    modifierKnoten: Modifier = Modifier,
    modifierAnschluss: (AnschlussDaten, Int) -> Modifier = { daten, idx -> AnschlussModifierStandard },
    inhaltSkalierung: Float = 1f,
) {
    val daten = knoten.daten
    val randFarbe = if (daten.ausgewaehlt) Color(0xFF2563EB) else Color(0xFF64748B)
    val skalierung = inhaltSkalierung.coerceAtLeast(0.1f)
    val form = RoundedCornerShape((8f * skalierung).dp)
    Box(
        modifier = modifierKnoten
            .border((1f * skalierung).dp, randFarbe, form)
            .background(Color.White, form),
    ) {
        // Der eigentliche Textinhalt bekommt seitlichen Abstand, damit er nicht
        // unter den auf dem Rahmen liegenden Anschlüssen liegt.
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = (18f * skalierung).dp, vertical = (10f * skalierung).dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
            ) {
                BasicText(
                    text = daten.name,
                    style = TextStyle(color = Color(0xFF0F172A), fontSize = (14f * skalierung).sp),
                )
            }
        }
        AnschlussKante.entries.forEach { kante ->
            Box(
                modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight().offset(x = (-5f * skalierung).dp),
                contentAlignment = Alignment.Center,
            ) {
                knoten.erhalteAnschlüsse().zuLeiste(knoten,kante,anschlussFabrik,modifierAnschluss)
            }
        }
    }
}
