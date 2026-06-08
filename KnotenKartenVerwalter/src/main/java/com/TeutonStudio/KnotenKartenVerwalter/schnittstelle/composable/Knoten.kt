package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.composable

import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.istVertikal
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Anschluss
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.AnschlussModifierStandard
import com.TeutonStudio.KnotenKartenVerwalter.zuLeiste
import kotlin.collections.forEach

/**
 * Standarddarstellung eines Knotens.
 *
 * Der Inhalt bleibt innerhalb des Rahmens, während die Anschlüsse links und
 * rechts auf dem Rahmen liegen.
 */
@Composable
public fun KnotenCard(
    daten: KnotenDaten,
    anschlüsse:  Map<Anschluss, Int>,
    boxModiRect:  (Density) -> Modifier,
    anschlussModifier: Map<AnschlussKante, Modifier>,
    inhaltSkalierung: Float = 1f,
    Inhalt: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val scope = currentRecomposeScope
    val randFarbe = if (daten.ausgewaehlt) Color(0xFF2563EB) else Color(0xFF64748B)
    val skalierung = inhaltSkalierung.coerceAtLeast(0.1f)
    Box(modifier = boxModiRect(density).draggable2D(
        state = rememberDraggable2DState { daten.position += it; scope.invalidate() },
        enabled = daten.beweglich,
    )) {
        Inhalt()
        AnschlussKante.entries.forEach { kante ->
            val modi = if (kante.istVertikal()) Modifier.fillMaxHeight().offset(x = radius(kante)) else Modifier.fillMaxWidth().offset(y = radius(kante))
            Box(
                modifier = modi.align(alignment(kante)).offset(x = (-5f * skalierung).dp),
                contentAlignment = Alignment.Center,
            ) { anschlüsse.zuLeiste(kante) }
        }
    }
}

private fun radius(kante: AnschlussKante): Dp = if (kante == AnschlussKante.Rechts || kante == AnschlussKante.Unten) 5.dp else (-5).dp

private fun alignment(kante: AnschlussKante): Alignment = when(kante) {
    AnschlussKante.Links -> Alignment.CenterStart
    AnschlussKante.Rechts -> Alignment.CenterEnd
    AnschlussKante.Oben -> Alignment.TopCenter
    AnschlussKante.Unten -> Alignment.BottomCenter
}
