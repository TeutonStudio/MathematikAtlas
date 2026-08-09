package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import java.util.WeakHashMap
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.hypot

private sealed interface KartenDragInhalt {
    data class Karte(val daten: KartenDaten) : KartenDragInhalt

    data class Knotenvorlage(
        val vorlage: KnotenVorlage,
        val griffPosition: Offset,
        val quellGröße: Size,
    ) : KartenDragInhalt
}

internal class KartenDragZustand {
    private var inhalt by mutableStateOf<KartenDragInhalt?>(null)
    var positionImFenster by mutableStateOf<Offset?>(null)
        private set
    var editorBereich by mutableStateOf<Rect?>(null)
    var dichte by mutableFloatStateOf(1f)

    fun beginne(karte: KartenDaten, position: Offset) {
        inhalt = KartenDragInhalt.Karte(karte)
        positionImFenster = position
    }

    fun beginne(
        vorlage: KnotenVorlage,
        position: Offset,
        griffPosition: Offset,
        quellGröße: Size,
    ) {
        inhalt = KartenDragInhalt.Knotenvorlage(
            vorlage = vorlage,
            griffPosition = griffPosition,
            quellGröße = quellGröße,
        )
        positionImFenster = position
    }

    fun verschiebe(delta: Offset) {
        positionImFenster = positionImFenster?.plus(delta)
    }

    fun abbrechen() {
        inhalt = null
        positionImFenster = null
    }

    fun ablegen(zustand: AtlasZustand) {
        val gezogen = inhalt
        val position = positionImFenster
        val bereich = editorBereich
        if (gezogen != null && position != null && bereich != null && position in bereich) {
            val lokal = position - bereich.topLeft
            val ansicht = zustand.editor.karte.ansicht
            val faktor = (dichte * ansicht.zoom).coerceAtLeast(0.0001f)
            val weltAmZeiger = GraphPunkt(
                (lokal.x - ansicht.verschiebung.x) / faktor,
                (lokal.y - ansicht.verschiebung.y) / faktor,
            )

            when (gezogen) {
                is KartenDragInhalt.Karte -> {
                    val aufKnoten = zustand.editor.karte.knoten.any { knoten ->
                        weltAmZeiger.x >= knoten.position.x &&
                            weltAmZeiger.x <= knoten.position.x + knoten.größe.breite &&
                            weltAmZeiger.y >= knoten.position.y &&
                            weltAmZeiger.y <= knoten.position.y + knoten.größe.höhe
                    }
                    if (!aufKnoten) {
                        zustand.fügeKartenKnotenEin(
                            gezogen.daten,
                            weltAmZeiger - GraphPunkt(120f, 50f),
                        )
                    }
                }

                is KartenDragInhalt.Knotenvorlage -> {
                    val zielPosition = berechneKnotenAblagePosition(
                        positionImEditor = lokal,
                        ansicht = ansicht,
                        dichte = dichte,
                        griffPosition = gezogen.griffPosition,
                        quellGröße = gezogen.quellGröße,
                        knotenGröße = gezogen.vorlage.standardGröße,
                    )
                    zustand.fügeKnotenEin(gezogen.vorlage, zielPosition)
                }
            }
        }
        abbrechen()
    }
}

internal fun berechneKnotenAblagePosition(
    positionImEditor: Offset,
    ansicht: AnsichtsFenster,
    dichte: Float,
    griffPosition: Offset,
    quellGröße: Size,
    knotenGröße: GraphGröße,
): GraphPunkt {
    val faktor = (dichte * ansicht.zoom).coerceAtLeast(0.0001f)
    val weltAmZeiger = GraphPunkt(
        (positionImEditor.x - ansicht.verschiebung.x) / faktor,
        (positionImEditor.y - ansicht.verschiebung.y) / faktor,
    )
    val griffAnteilX = (griffPosition.x / quellGröße.width.coerceAtLeast(1f)).coerceIn(0f, 1f)
    val griffAnteilY = (griffPosition.y / quellGröße.height.coerceAtLeast(1f)).coerceIn(0f, 1f)
    return weltAmZeiger - GraphPunkt(
        knotenGröße.breite * griffAnteilX,
        knotenGröße.höhe * griffAnteilY,
    )
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

internal fun Modifier.konzeptVorlagenInteraktion(
    zustand: AtlasZustand,
    vorlage: KnotenVorlage,
    onEinfügen: () -> Unit,
    onDefinition: () -> Unit,
): Modifier = composed {
    var ursprung by remember { mutableStateOf(Offset.Zero) }
    var quellGröße by remember { mutableStateOf(Size.Zero) }
    onGloballyPositioned {
        val bounds = it.boundsInWindow()
        ursprung = bounds.topLeft
        quellGröße = bounds.size
    }.pointerInput(vorlage.art, vorlage.name, vorlage.standardParameter) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val primär = when (down.type) {
                PointerType.Mouse -> currentEvent.buttons.isPrimaryPressed
                else -> true
            }
            if (!primär || down.isConsumed) return@awaitEachGesture

            val art = when (down.type) {
                PointerType.Touch -> KonzeptZeigerArt.Touch
                PointerType.Stylus, PointerType.Eraser -> KonzeptZeigerArt.Stift
                PointerType.Mouse -> KonzeptZeigerArt.Maus
                else -> KonzeptZeigerArt.Unbekannt
            }
            val touchSlopDp = viewConfiguration.touchSlop / density
            val schwelle = KonzeptGestenSchwellen.für(art, touchSlopDp).bewegungDp * density
            val automat = KonzeptGestenAutomat(schwelle)
            automat.drücken()
            val start = down.position
            var letztePosition = down.position
            var gesamtX = 0f
            var gesamtY = 0f
            var gehalten = false
            var verbleibendeHaltezeit = viewConfiguration.longPressTimeoutMillis

            fun wende(effekte: List<KonzeptGestenEffekt>, position: Offset) {
                effekte.forEach { effekt ->
                    when (effekt) {
                        KonzeptGestenEffekt.Einfügen -> onEinfügen()
                        KonzeptGestenEffekt.DefinitionÖffnen -> onDefinition()
                        KonzeptGestenEffekt.DragBeginnen -> zustand.kartenDragZustand.beginne(
                            vorlage = vorlage,
                            position = ursprung + position,
                            griffPosition = start,
                            quellGröße = quellGröße,
                        )
                        KonzeptGestenEffekt.DragVerschieben -> zustand.kartenDragZustand.verschiebe(position - letztePosition)
                        KonzeptGestenEffekt.DragBeenden -> zustand.kartenDragZustand.ablegen(zustand)
                        KonzeptGestenEffekt.DragAbbrechen -> zustand.kartenDragZustand.abbrechen()
                    }
                }
            }

            while (true) {
                val ereignis = if (!gehalten) {
                    withTimeoutOrNull(verbleibendeHaltezeit.coerceAtLeast(1L)) { awaitPointerEvent() }
                } else awaitPointerEvent()
                if (ereignis == null) {
                    gehalten = true
                    automat.haltezeitErreicht()
                    continue
                }
                val änderung = ereignis.changes.firstOrNull { it.id == down.id }
                if (änderung == null || ereignis.changes.count { it.pressed } > 1) {
                    wende(automat.abbrechen(), letztePosition)
                    break
                }
                if (!änderung.pressed) {
                    wende(automat.loslassen(), änderung.position)
                    break
                }
                if (!gehalten) {
                    verbleibendeHaltezeit = (
                        viewConfiguration.longPressTimeoutMillis - (änderung.uptimeMillis - down.uptimeMillis)
                    ).coerceAtLeast(0L)
                    if (verbleibendeHaltezeit == 0L) {
                        gehalten = true
                        automat.haltezeitErreicht()
                    }
                }
                if (änderung.isConsumed && automat.zustand != KonzeptGestenZustand.Ziehen) {
                    wende(automat.abbrechen(), änderung.position)
                    break
                }
                val delta = änderung.position - letztePosition
                gesamtX += delta.x
                gesamtY += delta.y
                val effekte = automat.bewegen(hypot(gesamtX, gesamtY))
                wende(effekte, änderung.position)
                if (automat.zustand == KonzeptGestenZustand.Ziehen) änderung.consume()
                letztePosition = änderung.position
            }
        }
    }
}

internal fun Modifier.kartenDropZiel(zustand: AtlasZustand, dichte: Float): Modifier =
    onGloballyPositioned {
        zustand.kartenDragZustand.editorBereich = it.boundsInWindow()
        zustand.kartenDragZustand.dichte = dichte
    }
