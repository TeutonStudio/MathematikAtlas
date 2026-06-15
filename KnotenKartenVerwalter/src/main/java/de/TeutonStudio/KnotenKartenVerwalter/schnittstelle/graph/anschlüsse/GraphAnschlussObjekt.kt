package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse

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
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import de.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.IDEhe
import de.TeutonStudio.KnotenKartenVerwalter.daten.verbindung.VerbindungDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.GraphDatenObjekt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.Knoten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.verbindungen.BezierVerbindung

/**
 * Vertrag für Anschlussobjekte, die [AnschlussDaten] an einem [Knoten] darstellen.
 * [Anschluss], [BasisAnschluss], [BasisEingang] und [BasisAusgang] sind die vorgesehenen Erweiterungspunkte.
 *
 * Anschlüsse liefern ihre Kartenposition, behandeln das Ziehen neuer Verbindungen und prüfen,
 * ob andere Anschlüsse als Verbindungsziel zulässig sind.
 */
interface GraphAnschlussObjekt<D: AnschlussDaten>: GraphDatenObjekt<D> {
    public val besitzer: Knoten
    public val karte get() = besitzer.besitzer

    /** Aktuelle Position des Anschlusses im Kartenkoordinatenraum. */
    val pos get() = erhaltePos() ?: Offset.Zero

    private fun erhaltePos(): KartenPosition? =
        layoutCoordinates.value?.let { anschlussCoordinates ->
            besitzer.layoutCoordinates.value?.localPositionOf(
                sourceCoordinates = anschlussCoordinates,
                relativeToSource = anschlussCoordinates.size.center.toOffset(),
            )?.let { lokalePosition ->
                besitzer.daten.position + lokalePosition
            }
        }

    /** Erstellt die Standarddarstellung eines Anschlusses. */
    @Composable
    override fun Modifier.vorher(): Modifier = size(5.dp).background(Color.Black, CircleShape)

    /** Kombiniert Tap- und Drag-Gesten für das Verbindungsziehen. */
    @Composable
    override fun Modifier.modifier(): Modifier = vorher().tapping().position()
        .pointerInput(daten.id) {
            detectDragGestures(
                orientationLock = null,
                shouldAwaitTouchSlop = { false },
                onDragStart = ::beiVerbindungZiehenStart,
                onDrag = ::beiVerbindungZiehenDelta,
                onDragEnd = ::beiVerbindungZiehenEnde,
                onDragCancel = ::beiVerbindungZiehenAbbruch,
            )
        }

    /** Startet das Ziehen einer neuen Verbindung an diesem Anschluss. */
    public fun beiVerbindungZiehenStart(start: PointerInputChange,change: PointerInputChange,klickPos: Offset)

    /** Aktualisiert die Zielposition einer gezogenen Verbindung. */
    public fun beiVerbindungZiehenDelta(change: PointerInputChange, dragAmount:Offset)

    /** Schließt eine gezogene Verbindung ab. */
    public fun beiVerbindungZiehenEnde(change: PointerInputChange)

    /** Bricht das Ziehen einer Verbindung ab. */
    public fun beiVerbindungZiehenAbbruch()

    /** Gibt an, ob dieser Anschluss als Eingang behandelt wird. */
    public open fun istEingang(): Boolean = false

    /** Gibt an, ob dieser Anschluss als Ausgang behandelt wird. */
    public open fun istAusgang(): Boolean = false

}
