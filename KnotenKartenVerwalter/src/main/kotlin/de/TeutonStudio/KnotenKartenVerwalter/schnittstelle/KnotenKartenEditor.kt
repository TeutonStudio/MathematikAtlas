package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.zustand.KartenEditorZustand
import kotlin.math.*

private const val KNOTEN_VIEWPORT_PUFFER = 200f
private const val VERBINDUNG_VIEWPORT_PUFFER = 80f

@Composable
fun KnotenKartenEditor(
    zustand: KartenEditorZustand,
    modifier: Modifier = Modifier,
    rendererFür: (KnotenDaten) -> KnotenRenderer = { StandardKnotenRenderer },
    farbeFürAnschluss: @Composable (AnschlussDaten) -> Color = { MaterialTheme.colorScheme.primary },
    beiHintergrundKontext: (GraphPunkt) -> Unit = {},
    beiVerbindungAufHintergrund: (AnschlussVerweis, GraphPunkt) -> Unit = { _, _ -> },
    beiKnotenDoppelklick: (KnotenDaten) -> Unit = {},
) {
    val dichte = LocalDensity.current
    val karte = zustand.karte
    val ansicht = karte.ansicht
    val aktuelleKarte by rememberUpdatedState(karte)
    val aktuelleAnsicht by rememberUpdatedState(ansicht)
    val aktuelleDichte by rememberUpdatedState(dichte.density)
    val weltGröße = 6000.dp
    var anzeigeGröße by remember { mutableStateOf(IntSize.Zero) }
    val sichtbarerWeltBereich = sichtbarerWeltBereich(ansicht, anzeigeGröße, dichte.density)
    val sichtbareKnoten = sichtbarerWeltBereich?.let { bereich ->
        karte.knoten.filter { it.istImBereich(bereich, KNOTEN_VIEWPORT_PUFFER) }
    } ?: karte.knoten
    val sichtbareVerbindungen = sichtbarerWeltBereich?.let { bereich ->
        karte.verbindungen.filter { it.istImBereich(karte, bereich, VERBINDUNG_VIEWPORT_PUFFER) }
    } ?: karte.verbindungen

    Box(
        modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .clipToBounds()
            .onSizeChanged { anzeigeGröße = it }
            .pointerInput(zustand) {
                awaitEachGesture {
                    var gesamterPan = Offset.Zero
                    var gesamterZoom = 1f
                    var hintergrundGesteAktiv = false
                    val ersterDruck = awaitFirstDown(requireUnconsumed = false)
                    if (trifftKnoten(ersterDruck.position, aktuelleKarte, aktuelleAnsicht, aktuelleDichte)) {
                        do {
                            val ereignis = awaitPointerEvent()
                        } while (ereignis.changes.any { it.pressed })
                    } else {
                        do {
                            // In Main wurden die Ereignisse bereits von Knoten und Anschlüssen verarbeitet.
                            // Nur eine nicht konsumierte Geste transformiert deshalb die Hintergrundansicht.
                            val ereignis = awaitPointerEvent()
                            if (ereignis.changes.none { it.isConsumed }) {
                                val pan = ereignis.calculatePan()
                                val zoom = ereignis.calculateZoom()
                                if (!hintergrundGesteAktiv) {
                                    gesamterPan += pan
                                    gesamterZoom *= zoom
                                    val zoomBewegung = abs(1f - gesamterZoom) * ereignis.calculateCentroidSize(useCurrent = false)
                                    hintergrundGesteAktiv = gesamterPan.getDistance() > viewConfiguration.touchSlop || zoomBewegung > viewConfiguration.touchSlop
                                }
                                if (hintergrundGesteAktiv && (pan != Offset.Zero || zoom != 1f)) {
                                    val bisherigeAnsicht = aktuelleAnsicht
                                    val neuerZoom = (bisherigeAnsicht.zoom * zoom).coerceIn(0.25f, 3.5f)
                                    // Der Mittelpunkt der Finger bleibt beim Skalieren unter
                                    // demselben visuellen Punkt. Der effektive Faktor berücksichtigt
                                    // auch die Zoom-Grenzen.
                                    val zoomFaktor = neuerZoom / bisherigeAnsicht.zoom
                                    val vorherigerMittelpunkt = ereignis.calculateCentroid(useCurrent = false)
                                    val aktuellerMittelpunkt = ereignis.calculateCentroid(useCurrent = true)
                                    val neueVerschiebung = GraphPunkt(
                                        aktuellerMittelpunkt.x - (vorherigerMittelpunkt.x - bisherigeAnsicht.verschiebung.x) * zoomFaktor,
                                        aktuellerMittelpunkt.y - (vorherigerMittelpunkt.y - bisherigeAnsicht.verschiebung.y) * zoomFaktor,
                                    )
                                    zustand.führeAus(KartenAktion.AnsichtÄndern(AnsichtsFenster(neueVerschiebung, neuerZoom)), mitHistorie = false)
                                    ereignis.changes.forEach { änderung -> if (änderung.positionChanged()) änderung.consume() }
                                }
                            }
                        } while (ereignis.changes.any { it.pressed })
                    }
                }
            }
            .pointerInput(karte.id, ansicht) {
                detectTapGestures(
                    onTap = { zustand.wähleKnoten(null) },
                    onLongPress = { pos ->
                        val welt = GraphPunkt(
                            (pos.x - ansicht.verschiebung.x) / ansicht.zoom / dichte.density,
                            (pos.y - ansicht.verschiebung.y) / ansicht.zoom / dichte.density,
                        )
                        beiHintergrundKontext(welt)
                    },
                )
            }
    ) {
        Raster(ansicht)
        Box(
            Modifier.size(weltGröße)
                .graphicsLayer {
                    scaleX = ansicht.zoom
                    scaleY = ansicht.zoom
                    translationX = ansicht.verschiebung.x
                    translationY = ansicht.verschiebung.y
                    transformOrigin = TransformOrigin(0f, 0f)
                }
        ) {
            // Die Schlüssel sorgen dafür, dass die Hintergrundebene bei jedem Drag sofort
            // mit den aktuellen Knotenpositionen neu gezeichnet wird.
            key(sichtbareVerbindungen, zustand.verbindungsStart, zustand.verbindungsVorschau) {
                Verbindungen(karte, zustand, sichtbareVerbindungen)
            }
            sichtbareKnoten.forEach { knoten ->
                key(knoten.id) {
                    KnotenDarstellung(
                        knoten = knoten,
                        ausgewählt = zustand.ausgewählterKnoten == knoten.id,
                        zustand = zustand,
                        renderer = rendererFür(knoten),
                        farbeFürAnschluss = farbeFürAnschluss,
                        beiVerbindungAufHintergrund = beiVerbindungAufHintergrund,
                        beiDoppelklick = { beiKnotenDoppelklick(knoten) },
                    )
                }
            }
        }
        zustand.letzteMeldung?.let { meldung ->
            Surface(
                Modifier.align(Alignment.BottomCenter).padding(16.dp),
                tonalElevation = 4.dp,
                shape = MaterialTheme.shapes.medium,
            ) { Text(meldung, Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) }
        }
    }
}

@Composable
private fun Raster(ansicht: AnsichtsFenster) {
    val farbe = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f)
    Canvas(Modifier.fillMaxSize()) {
        val schritt = 32.dp.toPx() * ansicht.zoom
        if (schritt <= 0f) return@Canvas
        var x = ansicht.verschiebung.x % schritt
        var y = ansicht.verschiebung.y % schritt
        while (x > 0f) x -= schritt
        while (y > 0f) y -= schritt
        while (x < size.width) { drawLine(farbe, Offset(x, 0f), Offset(x, size.height), 1f); x += schritt }
        while (y < size.height) { drawLine(farbe, Offset(0f, y), Offset(size.width, y), 1f); y += schritt }
    }
}

@Composable
private fun Verbindungen(
    karte: KartenDaten,
    zustand: KartenEditorZustand,
    verbindungen: List<VerbindungDaten>,
) {
    val dichte = LocalDensity.current
    val standard = MaterialTheme.colorScheme.outline
    val gewählt = MaterialTheme.colorScheme.primary
    Canvas(
        Modifier.fillMaxSize().pointerInput(verbindungen, karte.knoten) {
            detectTapGestures { pos ->
                val treffer = verbindungen.minByOrNull { verbindung ->
                    val a = anschlussPosition(karte, verbindung.von, dichte.density)
                    val b = anschlussPosition(karte, verbindung.zu, dichte.density)
                    punktStreckenAbstand(pos, a, b)
                }
                if (treffer != null) {
                    val a = anschlussPosition(karte, treffer.von, dichte.density)
                    val b = anschlussPosition(karte, treffer.zu, dichte.density)
                    if (punktStreckenAbstand(pos, a, b) <= 14.dp.toPx()) zustand.wähleVerbindung(treffer.id)
                }
            }
        }
    ) {
        verbindungen.forEach { verbindung ->
            val start = anschlussPosition(karte, verbindung.von, dichte.density)
            val ende = anschlussPosition(karte, verbindung.zu, dichte.density)
            val abstand = max(72.dp.toPx(), abs(ende.x - start.x) * .45f)
            val pfad = Path().apply {
                moveTo(start.x, start.y)
                cubicTo(start.x + abstand, start.y, ende.x - abstand, ende.y, ende.x, ende.y)
            }
            drawPath(
                pfad,
                if (zustand.ausgewählteVerbindung == verbindung.id) gewählt else standard,
                style = Stroke(width = if (zustand.ausgewählteVerbindung == verbindung.id) 5.dp.toPx() else 3.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        val startRef = zustand.verbindungsStart
        val vorschau = zustand.verbindungsVorschau
        if (startRef != null && vorschau != null) {
            val start = anschlussPosition(karte, startRef, dichte.density)
            val ende = Offset(vorschau.x * dichte.density, vorschau.y * dichte.density)
            drawLine(gewählt.copy(alpha = .75f), start, ende, 3.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun KnotenDarstellung(
    knoten: KnotenDaten,
    ausgewählt: Boolean,
    zustand: KartenEditorZustand,
    renderer: KnotenRenderer,
    farbeFürAnschluss: @Composable (AnschlussDaten) -> Color,
    beiVerbindungAufHintergrund: (AnschlussVerweis, GraphPunkt) -> Unit,
    beiDoppelklick: () -> Unit,
) {
    val zoom = zustand.karte.ansicht.zoom
    Box(
        Modifier.offset(knoten.position.x.dp, knoten.position.y.dp)
            .size(knoten.größe.breite.dp, knoten.größe.höhe.dp)
    ) {
        Card(
            Modifier.fillMaxSize()
                .border(if (ausgewählt) 3.dp else 1.dp, if (ausgewählt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                .pointerInput(knoten.id, zoom) {
                    detectDragGestures(
                        onDragStart = { zustand.wähleKnoten(knoten.id); zustand.beginneInteraktion() },
                        onDragEnd = { zustand.beendeInteraktion() },
                        onDragCancel = { zustand.beendeInteraktion() },
                        onDrag = { änderung, delta ->
                            änderung.consume()
                            val aktuell = zustand.karte.knoten.firstOrNull { it.id == knoten.id }
                            if (aktuell != null) {
                                zustand.führeAus(
                                    // PointerInput liefert innerhalb des skalierten Layers lokale,
                                    // bereits entzoomte Koordinaten. Nur die Pixeldichte muss noch
                                    // in die Graph-Koordinaten umgerechnet werden.
                                    KartenAktion.KnotenVerschieben(aktuell.id, aktuell.position + GraphPunkt(delta.x / density, delta.y / density)),
                                    mitHistorie = false,
                                )
                            }
                        },
                    )
                }
                .pointerInput(knoten.id) {
                    detectTapGestures(
                        onTap = { zustand.wähleKnoten(knoten.id) },
                        onDoubleTap = { beiDoppelklick() },
                    )
                },
            elevation = CardDefaults.cardElevation(if (ausgewählt) 8.dp else 2.dp),
        ) { renderer.Inhalt(knoten, ausgewählt) }

        knoten.anschlüsse.groupBy { it.kante }.forEach { (kante, anschlüsse) ->
            anschlüsse.sortedBy { it.reihenfolge }.forEachIndexed { index, anschluss ->
                AnschlussGriff(
                    knoten = knoten,
                    anschluss = anschluss,
                    index = index,
                    anzahl = anschlüsse.size,
                    zustand = zustand,
                    farbe = farbeFürAnschluss(anschluss),
                    beiVerbindungAufHintergrund = beiVerbindungAufHintergrund,
                )
            }
        }

        if (ausgewählt) {
            Box(
                Modifier.align(Alignment.BottomEnd).offset(6.dp, 6.dp).size(18.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .pointerInput(knoten.id, zoom) {
                        detectDragGestures(
                            onDragStart = { zustand.beginneInteraktion() },
                            onDragEnd = { zustand.beendeInteraktion() },
                            onDragCancel = { zustand.beendeInteraktion() },
                            onDrag = { änderung, delta ->
                                änderung.consume()
                                val aktuell = zustand.karte.knoten.firstOrNull { it.id == knoten.id }
                                if (aktuell != null) {
                                    zustand.führeAus(
                                        KartenAktion.KnotenGrößeÄndern(
                                            aktuell.id,
                                            GraphGröße(
                                                (aktuell.größe.breite + delta.x / zoom / density).coerceAtLeast(120f),
                                                (aktuell.größe.höhe + delta.y / zoom / density).coerceAtLeast(72f),
                                            ),
                                        ),
                                        mitHistorie = false,
                                    )
                                }
                            },
                        )
                    }
            )
        }
    }
}

@Composable
private fun BoxScope.AnschlussGriff(
    knoten: KnotenDaten,
    anschluss: AnschlussDaten,
    index: Int,
    anzahl: Int,
    zustand: KartenEditorZustand,
    farbe: Color,
    beiVerbindungAufHintergrund: (AnschlussVerweis, GraphPunkt) -> Unit,
) {
    val anteil = (index + 1f) / (anzahl + 1f)
    val ausrichtung = Alignment.TopStart
    val x = when (anschluss.kante) {
        AnschlussKante.Links -> (-7).dp
        AnschlussKante.Rechts -> (knoten.größe.breite - 7).dp
        AnschlussKante.Oben, AnschlussKante.Unten -> (knoten.größe.breite * anteil - 7).dp
    }
    val y = when (anschluss.kante) {
        AnschlussKante.Oben -> (-7).dp
        AnschlussKante.Unten -> (knoten.größe.höhe - 7).dp
        AnschlussKante.Links, AnschlussKante.Rechts -> (knoten.größe.höhe * anteil - 7).dp
    }
    val ref = AnschlussVerweis(knoten.id, anschluss.id)
    val kompatibel = zustand.kompatibelMitStart(ref)
    var zugPosition by remember(knoten.id, anschluss.id) { mutableStateOf<GraphPunkt?>(null) }
    val startWelt = anschlussPositionWelt(knoten, anschluss)
    Box(
        Modifier.align(ausrichtung).offset(x, y).size(14.dp)
            .background(if (kompatibel) farbe else farbe.copy(alpha = .2f), CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            .clickable(enabled = kompatibel) { zustand.anschlussAngeklickt(ref) }
            .pointerInput(ref) {
                if (!kompatibel) return@pointerInput
                detectDragGestures(
                    onDragStart = { druckPosition ->
                        // Der Griff ist um den Anschluss zentriert. Der Vorschauendpunkt
                        // beginnt deshalb an der tatsächlichen Druckposition, nicht pauschal
                        // in der Griffmitte.
                        val start = startWelt + GraphPunkt(
                            (druckPosition.x - 7.dp.toPx()) / density,
                            (druckPosition.y - 7.dp.toPx()) / density,
                        )
                        zugPosition = start
                        zustand.beginneVerbindung(ref, start)
                    },
                    onDrag = { änderung, delta ->
                        änderung.consume()
                        val aktuell = (zugPosition ?: startWelt) + GraphPunkt(
                            // PointerInput liefert bereits in den lokalen, also nicht mehr
                            // gezoomten Koordinaten des Griffs.
                            delta.x / density,
                            delta.y / density,
                        )
                        zugPosition = aktuell
                        zustand.aktualisiereVerbindungsVorschau(aktuell)
                    },
                    onDragEnd = {
                        val ende = zugPosition ?: startWelt
                        val ziel = nächsterKompatiblerAnschluss(zustand, ref, ende)
                        if (ziel != null) {
                            zustand.anschlussAngeklickt(ziel)
                        } else {
                            zustand.beendeVerbindungsVorschau(startBeibehalten = false)
                            beiVerbindungAufHintergrund(ref, ende)
                        }
                        zugPosition = null
                    },
                    onDragCancel = {
                        zugPosition = null
                        zustand.beendeVerbindungsVorschau()
                    },
                )
            }
    )
}


private fun anschlussPositionWelt(knoten: KnotenDaten, anschluss: AnschlussDaten): GraphPunkt {
    val aufKante = knoten.anschlüsse.filter { it.kante == anschluss.kante }.sortedBy { it.reihenfolge }
    val index = aufKante.indexOfFirst { it.id == anschluss.id }.coerceAtLeast(0)
    val anteil = (index + 1f) / (aufKante.size + 1f)
    val x = when (anschluss.kante) {
        AnschlussKante.Links -> knoten.position.x
        AnschlussKante.Rechts -> knoten.position.x + knoten.größe.breite
        AnschlussKante.Oben, AnschlussKante.Unten -> knoten.position.x + knoten.größe.breite * anteil
    }
    val y = when (anschluss.kante) {
        AnschlussKante.Oben -> knoten.position.y
        AnschlussKante.Unten -> knoten.position.y + knoten.größe.höhe
        AnschlussKante.Links, AnschlussKante.Rechts -> knoten.position.y + knoten.größe.höhe * anteil
    }
    return GraphPunkt(x, y)
}

private fun trifftKnoten(position: Offset, karte: KartenDaten, ansicht: AnsichtsFenster, dichte: Float): Boolean {
    val rand = 18f * dichte
    return karte.knoten.any { knoten ->
        val links = ansicht.verschiebung.x + knoten.position.x * dichte * ansicht.zoom - rand
        val oben = ansicht.verschiebung.y + knoten.position.y * dichte * ansicht.zoom - rand
        val rechts = links + knoten.größe.breite * dichte * ansicht.zoom + 2 * rand
        val unten = oben + knoten.größe.höhe * dichte * ansicht.zoom + 2 * rand
        position.x in links..rechts && position.y in oben..unten
    }
}

private fun nächsterKompatiblerAnschluss(
    zustand: KartenEditorZustand,
    start: AnschlussVerweis,
    ende: GraphPunkt,
): AnschlussVerweis? = zustand.karte.knoten.asSequence().flatMap { knoten ->
    knoten.anschlüsse.asSequence().map { anschluss -> AnschlussVerweis(knoten.id, anschluss.id) to anschlussPositionWelt(knoten, anschluss) }
}.filter { (ref, _) -> ref != start && zustand.kompatibelMitStart(ref) }
    .map { (ref, pos) -> ref to hypot((pos.x - ende.x).toDouble(), (pos.y - ende.y).toDouble()).toFloat() }
    .filter { it.second <= 28f / zustand.karte.ansicht.zoom }
    .minByOrNull { it.second }?.first

private fun anschlussPosition(karte: KartenDaten, ref: AnschlussVerweis, dichte: Float): Offset {
    val knoten = karte.knoten.firstOrNull { it.id == ref.knotenId } ?: return Offset.Zero
    val anschluss = knoten.anschlüsse.firstOrNull { it.id == ref.anschlussId } ?: return Offset.Zero
    val aufKante = knoten.anschlüsse.filter { it.kante == anschluss.kante }.sortedBy { it.reihenfolge }
    val index = aufKante.indexOfFirst { it.id == anschluss.id }.coerceAtLeast(0)
    val anteil = (index + 1f) / (aufKante.size + 1f)
    val x = when (anschluss.kante) {
        AnschlussKante.Links -> knoten.position.x
        AnschlussKante.Rechts -> knoten.position.x + knoten.größe.breite
        AnschlussKante.Oben, AnschlussKante.Unten -> knoten.position.x + knoten.größe.breite * anteil
    }
    val y = when (anschluss.kante) {
        AnschlussKante.Oben -> knoten.position.y
        AnschlussKante.Unten -> knoten.position.y + knoten.größe.höhe
        AnschlussKante.Links, AnschlussKante.Rechts -> knoten.position.y + knoten.größe.höhe * anteil
    }
    return Offset(x * dichte, y * dichte)
}

/**
 * Der sichtbare Ausschnitt der Kartenwelt in Graph-Koordinaten.
 *
 * Die Verschiebung der Ansicht liegt in Bildschirm-Pixeln vor; die Welt selbst
 * wird dagegen in dp gespeichert. Deshalb wird nach der Rücktransformation des
 * Zooms zusätzlich durch die Pixeldichte geteilt.
 */
private fun sichtbarerWeltBereich(
    ansicht: AnsichtsFenster,
    anzeigeGröße: IntSize,
    dichte: Float,
): Rect? {
    if (anzeigeGröße.width <= 0 || anzeigeGröße.height <= 0 || dichte <= 0f) return null
    val zoom = ansicht.zoom.coerceAtLeast(0.0001f)
    return Rect(
        left = -ansicht.verschiebung.x / zoom / dichte,
        top = -ansicht.verschiebung.y / zoom / dichte,
        right = (anzeigeGröße.width - ansicht.verschiebung.x) / zoom / dichte,
        bottom = (anzeigeGröße.height - ansicht.verschiebung.y) / zoom / dichte,
    )
}

private fun KnotenDaten.istImBereich(bereich: Rect, puffer: Float): Boolean = Rect(
    left = position.x - puffer,
    top = position.y - puffer,
    right = position.x + größe.breite + puffer,
    bottom = position.y + größe.höhe + puffer,
).überschneidet(bereich)

/**
 * Die Kontrollpunkte der kubischen Verbindung liegen horizontal neben den
 * Endpunkten. Ihre Umhüllung ist damit eine sichere, günstige Obergrenze für
 * das sichtbare Béziersegment.
 */
private fun VerbindungDaten.istImBereich(
    karte: KartenDaten,
    bereich: Rect,
    puffer: Float,
): Boolean {
    val start = anschlussPositionWelt(karte, von) ?: return false
    val ende = anschlussPositionWelt(karte, zu) ?: return false
    val abstand = max(72f, abs(ende.x - start.x) * .45f)
    return Rect(
        left = minOf(start.x, ende.x, start.x + abstand, ende.x - abstand) - puffer,
        top = min(start.y, ende.y) - puffer,
        right = maxOf(start.x, ende.x, start.x + abstand, ende.x - abstand) + puffer,
        bottom = max(start.y, ende.y) + puffer,
    ).überschneidet(bereich)
}

private fun anschlussPositionWelt(karte: KartenDaten, ref: AnschlussVerweis): GraphPunkt? {
    val knoten = karte.knoten.firstOrNull { it.id == ref.knotenId } ?: return null
    val anschluss = knoten.anschlüsse.firstOrNull { it.id == ref.anschlussId } ?: return null
    return anschlussPositionWelt(knoten, anschluss)
}

private fun Rect.überschneidet(anderer: Rect): Boolean =
    left <= anderer.right && right >= anderer.left && top <= anderer.bottom && bottom >= anderer.top

private fun punktStreckenAbstand(p: Offset, a: Offset, b: Offset): Float {
    val ab = b - a
    val länge2 = ab.x * ab.x + ab.y * ab.y
    if (länge2 == 0f) return (p - a).getDistance()
    val t = (((p - a).x * ab.x + (p - a).y * ab.y) / länge2).coerceIn(0f, 1f)
    return (p - (a + ab * t)).getDistance()
}
