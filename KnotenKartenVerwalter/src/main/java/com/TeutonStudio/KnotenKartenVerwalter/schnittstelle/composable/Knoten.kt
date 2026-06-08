package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.composable

import androidx.annotation.FloatRange
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.istVertikal
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Anschluss
import com.TeutonStudio.KnotenKartenVerwalter.zuLeiste
import kotlin.collections.forEach

/**
 * Standarddarstellung eines Knotens.
 *
 * Der Inhalt bleibt innerhalb des Rahmens, während die Anschlüsse links und
 * rechts auf dem Rahmen liegen.
 */
@Composable
public fun KnotenRahmen(
    daten: KnotenDaten,
    anschlüsse:  Map<Anschluss, Int>,
    boxModiRect:  (Density) -> Modifier,
    inhaltSkalierung: Float = 1f,
    beiVerschiebung: (Offset) -> Unit,
    Inhalt: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val scope = currentRecomposeScope
//    val skalierung = inhaltSkalierung.coerceAtLeast(0.1f)
    Box(modifier = boxModiRect(density).draggable2D(
        state = rememberDraggable2DState { beiVerschiebung(it) },
        enabled = daten.beweglich,
    )) {
        Inhalt()
        AnschlussKante.entries.forEach { kante ->
//            val modi = if (kante.istVertikal()) Modifier.fillMaxHeight().offset(x = radius(kante)) else Modifier.fillMaxWidth().offset(y = radius(kante))
            val modi = Modifier.fillMaxKante(kante).offsetKante(kante,radius(kante))
            Box(
                modifier = modi.align(alignment(kante)), //.offset(x = (-5f * skalierung).dp),
                contentAlignment = Alignment.Center,
            ) { anschlüsse.zuLeiste(kante) }
        }
    }
}

private fun radius(kante: AnschlussKante, radius: Dp = (2.5f).dp): Dp = if (kante == AnschlussKante.Rechts || kante == AnschlussKante.Unten) radius else -radius

private fun alignment(kante: AnschlussKante): Alignment = when(kante) {
    AnschlussKante.Links -> Alignment.CenterStart
    AnschlussKante.Rechts -> Alignment.CenterEnd
    AnschlussKante.Oben -> Alignment.TopCenter
    AnschlussKante.Unten -> Alignment.BottomCenter
}

private fun Modifier.fillMaxKante(kante: AnschlussKante,@FloatRange fraction: Float = 1f): Modifier = if (kante.istVertikal()) fillMaxHeight(fraction) else fillMaxWidth(fraction)

private fun Modifier.offsetKante(kante: AnschlussKante, offset: Dp = 0.dp) = if(kante.istVertikal()) offset(x=offset) else offset(y=offset)
