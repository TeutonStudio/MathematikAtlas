package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import java.util.WeakHashMap

internal class KartenDragZustand {
    var karte by mutableStateOf<KartenDaten?>(null)
        private set
    var positionImFenster by mutableStateOf<Offset?>(null)
        private set
    var editorBereich by mutableStateOf<Rect?>(null)
    var dichte by mutableFloatStateOf(1f)

    fun beginne(karte: KartenDaten, position: Offset) {
        this.karte = karte
        positionImFenster = position
    }

    fun verschiebe(delta: Offset) {
        positionImFenster = positionImFenster?.plus(delta)
    }

    fun abbrechen() {
        karte = null
        positionImFenster = null
    }

    fun ablegen(zustand: AtlasZustand) {
        val gezogen = karte
        val position = positionImFenster
        val bereich = editorBereich
        if (gezogen != null && position != null && bereich != null && position in bereich) {
            val lokal = position - bereich.topLeft
            val ansicht = zustand.editor.karte.ansicht
            val faktor = (dichte * ansicht.zoom).coerceAtLeast(0.0001f)
            val welt = GraphPunkt(
                (lokal.x - ansicht.verschiebung.x) / faktor,
                (lokal.y - ansicht.verschiebung.y) / faktor,
            )
            val aufKnoten = zustand.editor.karte.knoten.any { knoten ->
                welt.x >= knoten.position.x && welt.x <= knoten.position.x + knoten.größe.breite &&
                    welt.y >= knoten.position.y && welt.y <= knoten.position.y + knoten.größe.höhe
            }
            if (!aufKnoten) zustand.fügeKartenKnotenEin(gezogen, welt - GraphPunkt(120f, 50f))
        }
        abbrechen()
    }
}

private val dragZustände = WeakHashMap<AtlasZustand, KartenDragZustand>()
internal val AtlasZustand.kartenDragZustand: KartenDragZustand
    get() = synchronized(dragZustände) { dragZustände.getOrPut(this) { KartenDragZustand() } }

internal fun Modifier.kartenDragQuelle(zustand: AtlasZustand, karte: KartenDaten): Modifier = composed {
    var ursprung by remember { mutableStateOf(Offset.Zero) }
    onGloballyPositioned { ursprung = it.boundsInWindow().topLeft }
        .pointerInput(karte.id, karte.version) {
            detectDragGesturesAfterLongPress(
                onDragStart = { lokal -> zustand.kartenDragZustand.beginne(karte, ursprung + lokal) },
                onDrag = { änderung, delta ->
                    änderung.consume()
                    zustand.kartenDragZustand.verschiebe(delta)
                },
                onDragEnd = { zustand.kartenDragZustand.ablegen(zustand) },
                onDragCancel = { zustand.kartenDragZustand.abbrechen() },
            )
        }
}

internal fun Modifier.kartenDropZiel(zustand: AtlasZustand, dichte: Float): Modifier =
    onGloballyPositioned {
        zustand.kartenDragZustand.editorBereich = it.boundsInWindow()
        zustand.kartenDragZustand.dichte = dichte
    }
