package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussFabrik
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussModifier
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussRichtung
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.AusgangDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.EingangDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.RichtungsAnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.erzeugeAnschluss
import com.TeutonStudio.KnotenKartenVerwalter.filterKante
import com.TeutonStudio.KnotenKartenVerwalter.istHorizontal
import com.TeutonStudio.KnotenKartenVerwalter.istVertikal
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Anschluss
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BasisVerbindung
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Knoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Verbindung
import kotlin.collections.filterKeys
import kotlin.collections.maxBy

/**
 * Rendert eine gemischte Anschlussliste als vertikale Anschluss-Spalte.
 */
/*
@JvmName("list_of_connectdata_2_path")
@Composable
public fun List<AnschlussDaten>.zuPfad(istEingang: Boolean, modifier: (Int) -> Modifier = { AnschlussModifierStandard }) = AnschlussSpalte(this,modifier,istEingang)
*/

/**
 * Positioniert Anschlüsse gleichmäßig an einer Knotenkante.
 */
/*
@Composable
public fun Map<Anschluss,Int>.zuLeiste(kante: AnschlussKante, modifier: AnschlussModifier) {
    val listeComposable = this.filterKante(kante).map { (anschluss,idx) -> @Composable { anschluss.zuComposable(modifier(anschluss.daten,idx)) } }
    if (kante.istVertikal()) AnschlussSpalte(listeComposable)
    else if (kante.istHorizontal()) AnschlussZeile(listeComposable)
    else TODO()
}
*/

/**
 * Rendert Eingänge als linke Anschluss-Spalte.
 */
/*
@JvmName("list_of_inputdata_2_path")
@Composable
public fun List<EingangDaten>.zuPfad(modifier: (Int) -> Modifier = { AnschlussModifierStandard }) = (this as List<AnschlussDaten>).zuPfad(true,modifier)
*/

/**
 * Rendert Ausgänge als rechte Anschluss-Spalte.
 */
/*
@JvmName("list_of_outputdata_2_path")
@Composable
public fun List<AusgangDaten>.zuPfad(modifier: (Int) -> Modifier = { AnschlussModifierStandard }) = (this as List<AnschlussDaten>).zuPfad(false,modifier)
*/

/**
 * Zum Ausgleich des Zooms der Karte auf dem Graph
 */
internal fun Modifier.anschlussModifierSkaliert(skalierung: Float): Modifier {
    val faktor = skalierung.coerceAtLeast(0.1f)
    val vFaktor = faktor * 4f
    val sFaktor = faktor * 10f
    return this
        .padding(vertical = vFaktor.dp)
        .size(sFaktor.dp)
}


/**
 * Grundlegende Darstellung eines Anschlusses
 */
@Composable
public fun Anschluss(
    daten: AnschlussDaten,
    onDragStarted: (Offset) -> Unit,
    onDragStopped: () -> Unit,
    onDragging: (Offset) -> Unit,
    radius: Dp,
    farbe: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(radius).background(farbe, CircleShape).draggable2D(
            rememberDraggable2DState(onDragging),
            onDragStarted = onDragStarted,
            onDragStopped = { onDragStopped() }
        )
/*            .pointerInput(daten.id) {
            detectDragGestures(
                onDragStart = onDragStarted,
                onDragEnd = onDragStopped,
                onDrag = { change, dragAmount ->
                    change.consume()
                    // change.position ist die ABSOLUTE Position während des Drags
                    // Kein manuelles Aufsummieren von Deltas nötig!
                    onDragging(change.position)
                }
            )
        },*/
    ) { /* */ }
}

/**
 * Gemeinsames Anschluss-Rendering.
 *
 * Eingänge und Ausgänge unterscheiden sich nur in der Farbe. Die runde Form
 * macht den Anschluss als interaktiven Handle erkennbar.
 */
@Composable
public fun RichtungsAnschluss(
    daten: RichtungsAnschlussDaten,
    onDragStarted: (Offset) -> Unit,
    onDragStopped: () -> Unit,
    onDraggin: (Offset) -> Unit,
    radius: Dp,
    modifier: Modifier = Modifier,
) {
    Anschluss(
        daten,onDragStarted,onDragStopped,onDraggin,radius,when (daten.richtung) {
            AnschlussRichtung.Eingang -> Color(0xFF2563EB)
            AnschlussRichtung.Ausgang -> Color(0xFF059669)
        }, modifier,

        )
}

@Composable
public fun Eingang(
    daten: EingangDaten,
    onDragStarted: (Offset) -> Unit,
    onDragStopped: () -> Unit,
    onDraggin: (Offset) -> Unit,
    radius: Dp,
    modifier: Modifier = Modifier,
) = RichtungsAnschluss(daten,onDragStarted,onDragStopped,onDraggin,radius,modifier)

@Composable
public fun Ausgang(
    daten: AusgangDaten,
    onDragStarted: (Offset) -> Unit,
    onDragStopped: () -> Unit,
    onDraggin: (Offset) -> Unit,
    radius: Dp,
    modifier: Modifier = Modifier,
) = RichtungsAnschluss(daten,onDragStarted,onDragStopped,onDraggin,radius,modifier)

/**
 * Ordnet Anschlüsse gleichmäßig über die Kante eines Knotens an.
 */
@Composable
public fun AnschlussSpalte(
    leisteModifier: Modifier,
    inhalt: Iterable<@Composable (() -> Unit)>,
) {
    Column(
        modifier = leisteModifier.fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceEvenly,
    ) { inhalt.forEach { it() } }
}

/**
 * Ordnet Anschlüsse gleichmäßig über die Kante eines Knotens an.
 */
@Composable
public fun AnschlussZeile(
    leisteModifier: Modifier,
    inhalt: Iterable<@Composable (() -> Unit)>,
) {
    Row(
        modifier = leisteModifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) { inhalt.forEach { it() } }
}

// @Composable
// public fun Anschluss(anschluss: Anschluss, modifier: Modifier) = anschluss.zuComposable(modifier)