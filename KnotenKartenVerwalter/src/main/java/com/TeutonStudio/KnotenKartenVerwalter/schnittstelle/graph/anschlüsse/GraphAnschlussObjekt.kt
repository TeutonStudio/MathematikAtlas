package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.IDEhe
import com.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.VerbindungDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjekt
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.BezierVerbindung

interface GraphAnschlussObjekt<D: AnschlussDaten>: GraphDatenObjekt<D> {
    public val besitzer: Knoten
    public val karte get() = besitzer.besitzer


    @Composable
    override fun Modifier.vorher(): Modifier = size(5.dp).background(Color.Black, CircleShape)

    @Composable
    override fun Modifier.modifier(): Modifier = vorher().tapping().position()
        .pointerInput(daten.id) {
            detectDragGestures(
                onDragStart = ::beiVerbindungZiehenStart,
                onDrag = ::beiVerbindungZiehenDelta,
                onDragEnd = ::beiVerbindungZiehenEnde,
                onDragCancel = ::beiVerbindungZiehenAbbruch,
            )
        }


    public fun beiVerbindungZiehenStart(klickPos: Offset)
    public fun beiVerbindungZiehenDelta(change: PointerInputChange, dragAmount:Offset)
    public fun beiVerbindungZiehenEnde()
    public fun beiVerbindungZiehenAbbruch()


}