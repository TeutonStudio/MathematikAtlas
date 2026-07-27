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
import androidx.compose.ui.geometry.Size
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
private const val KOPFZEILE_HÖHE_DP = 44f

@Composable
fun KnotenKartenEditor(
    zustand: KartenEditorZustand,
    modifier: Modifier = Modifier,
    rendererFür: (KnotenDaten) -> KnotenRenderer = { StandardKnotenRenderer },
    farbeFürAnschluss: @Composable (AnschlussDaten) -> Color = { MaterialTheme.colorScheme.primary },
    beiHintergrundKontext: (GraphPunkt) -> Unit = {},
    beiKnotenKontext: (KnotenDaten) -> Unit = {},
    beiVerbindungKontext: (VerbindungDaten) -> Unit = {},
    beiVerbindungAufHintergrund: (AnschlussVerweis, GraphPunkt) -> Unit = { _, _ -> },
    beiKnotenDoppelklick: (KnotenDaten) -> Unit = {},
) {
    val dichte = LocalDensity.current
    val karte = zustand.karte
    val ansicht = karte.ansicht
    val aktuelleKarte by rememberUpdatedState(karte)
    val aktuelleAnsicht by rememberUpdatedState(ansicht)
    val aktuelleDichte by rememberUpdatedState(dichte.density)
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
                        zustand.wähleKnoten(null)
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
        // Die Kameratransformation wird je Objekt aus Welt- in Bildschirmkoordinaten
        // berechnet. Anders als eine große, skalierte Welt-Box begrenzt das die Karte
        // nicht künstlich auf eine feste Fläche.
        Box(Modifier.fillMaxSize()) {
            // Die Schlüssel sorgen dafür, dass die Hintergrundebene bei jedem Drag sofort
            // mit den aktuellen Knotenpositionen neu gezeichnet wird.
            key(sichtbareVerbindungen, zustand.verbindungsStart, zustand.verbindungsVorschau) {
                Verbindungen(
                    karte = karte,
                    zustand = zustand,
                    verbindungen = sichtbareVerbindungen,
                    ansicht = ansicht,
                    beiHintergrundKontext = beiHintergrundKontext,
                    beiVerbindungKontext = beiVerbindungKontext,
                )
            }
            sichtbareKnoten.forEach { knoten ->
                key(knoten.id) {
                    KnotenDarstellung(
                        knoten = knoten,
                        ausgewählt = zustand.ausgewählterKnoten == knoten.id,
                        zustand = zustand,
                        ansicht = ansicht,
                        dichte = dichte.density,
                        renderer = rendererFür(knoten),
                        farbeFürAnschluss = farbeFürAnschluss,
                        beiKnotenKontext = beiKnotenKontext,
                        beiVerbindungAufHintergrund = beiVerbindungAufHintergrund,
                        beiDoppelklick = { beiKnotenDoppelklick(knoten) },
                    )
                }
            }
        }
        MiniMap(
            karte = karte,
            sichtbarerWeltBereich = sichtbarerWeltBereich,
            anzeigeGröße = anzeigeGröße,
            dichte = dichte.density,
            ausgewählterKnoten = zustand.ausgewählterKnoten,
            beiZentrieren = { weltPosition ->
                val neueVerschiebung = GraphPunkt(
                    anzeigeGröße.width / 2f - weltPosition.x * dichte.density * ansicht.zoom,
                    anzeigeGröße.height / 2f - weltPosition.y * dichte.density * ansicht.zoom,
                )
                zustand.führeAus(
                    KartenAktion.AnsichtÄndern(ansicht.copy(verschiebung = neueVerschiebung)),
                    mitHistorie = false,
                )
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
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
        val basisSchritt = 32.dp.toPx()
        val rasterStufe = ceil(12.dp.toPx() / (basisSchritt * ansicht.zoom)).toInt().coerceAtLeast(1)
        val schritt = basisSchritt * ansicht.zoom * rasterStufe
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
private fun MiniMap(
    karte: KartenDaten,
    sichtbarerWeltBereich: Rect?,
    anzeigeGröße: IntSize,
    dichte: Float,
    ausgewählterKnoten: KnotenId?,
    beiZentrieren: (GraphPunkt) -> Unit,
    modifier: Modifier = Modifier,
) {
    val inhalt = karte.inhaltsGrenzen(puffer = 80f)
    val grenzen = when {
        sichtbarerWeltBereich != null && inhalt != null -> inhalt.vereinigtMit(sichtbarerWeltBereich)
        sichtbarerWeltBereich != null -> sichtbarerWeltBereich
        inhalt != null -> inhalt
        else -> return
    }
    var miniGröße by remember(karte.id) { mutableStateOf(IntSize.Zero) }
    val aktuelleGrenzen by rememberUpdatedState(grenzen)
    val aktuelleMiniGröße by rememberUpdatedState(miniGröße)
    val aktuelleDichte by rememberUpdatedState(dichte)
    val aktuellesZentrieren by rememberUpdatedState(beiZentrieren)
    val rahmenFarbe = MaterialTheme.colorScheme.outlineVariant
    val knotenFarbe = MaterialTheme.colorScheme.outline
    val ausgewähltFarbe = MaterialTheme.colorScheme.primary
    val viewportFarbe = ausgewähltFarbe.copy(alpha = .18f)
    val hintergrundFarbe = MaterialTheme.colorScheme.surface
    val zentriereAufMiniMapPosition: (Offset) -> Unit = { position ->
        if (aktuelleMiniGröße.width > 0 && aktuelleMiniGröße.height > 0 && aktuelleDichte > 0f) {
            val projektion = MiniMapProjektion(
                grenzen = aktuelleGrenzen,
                größe = Size(aktuelleMiniGröße.width.toFloat(), aktuelleMiniGröße.height.toFloat()),
            )
            aktuellesZentrieren(projektion.zuWelt(position))
        }
    }

    Card(modifier.size(width = 180.dp, height = 120.dp)) {
        Canvas(
            Modifier.fillMaxSize()
                .onSizeChanged { miniGröße = it }
                .pointerInput(karte.id) {
                    detectTapGestures(onTap = zentriereAufMiniMapPosition)
                }
                .pointerInput(karte.id) {
                    detectDragGestures(
                        onDragStart = zentriereAufMiniMapPosition,
                        onDrag = { änderung, _ ->
                            änderung.consume()
                            zentriereAufMiniMapPosition(änderung.position)
                        },
                    )
                },
        ) {
            val projektion = MiniMapProjektion(grenzen, size)
            drawRect(hintergrundFarbe)
            karte.knoten.forEach { knoten ->
                val position = projektion.zuMiniMap(Offset(knoten.position.x, knoten.position.y))
                drawRect(
                    color = if (knoten.id == ausgewählterKnoten) ausgewähltFarbe else knotenFarbe,
                    topLeft = position,
                    size = Size(
                        (knoten.größe.breite * projektion.skalierung).coerceAtLeast(3f),
                        (knoten.größe.höhe * projektion.skalierung).coerceAtLeast(3f),
                    ),
                )
            }
            sichtbarerWeltBereich?.let { viewport ->
                val obenLinks = projektion.zuMiniMap(Offset(viewport.left, viewport.top))
                val untenRechts = projektion.zuMiniMap(Offset(viewport.right, viewport.bottom))
                val viewportGröße = Size(
                    (untenRechts.x - obenLinks.x).coerceAtLeast(0f),
                    (untenRechts.y - obenLinks.y).coerceAtLeast(0f),
                )
                drawRect(viewportFarbe, obenLinks, viewportGröße)
                drawRect(ausgewähltFarbe, obenLinks, viewportGröße, style = Stroke(2.dp.toPx()))
            }
            drawRect(rahmenFarbe, style = Stroke(1.dp.toPx()))
        }
    }
}

internal data class MiniMapProjektion(val grenzen: Rect, val größe: Size) {
    private val puffer = 10f
    private val breite = grenzen.width.coerceAtLeast(1f)
    private val höhe = grenzen.height.coerceAtLeast(1f)
    val skalierung = minOf(
        (größe.width - 2f * puffer).coerceAtLeast(1f) / breite,
        (größe.height - 2f * puffer).coerceAtLeast(1f) / höhe,
    ).coerceAtLeast(0.0001f)
    private val ursprung = Offset(
        (größe.width - breite * skalierung) / 2f,
        (größe.height - höhe * skalierung) / 2f,
    )

    fun zuMiniMap(welt: Offset): Offset = Offset(
        ursprung.x + (welt.x - grenzen.left) * skalierung,
        ursprung.y + (welt.y - grenzen.top) * skalierung,
    )

    fun zuWelt(miniMap: Offset): GraphPunkt = GraphPunkt(
        grenzen.left + (miniMap.x - ursprung.x) / skalierung,
        grenzen.top + (miniMap.y - ursprung.y) / skalierung,
    )
}

@Composable
private fun Verbindungen(
    karte: KartenDaten,
    zustand: KartenEditorZustand,
    verbindungen: List<VerbindungDaten>,
    ansicht: AnsichtsFenster,
    beiHintergrundKontext: (GraphPunkt) -> Unit,
    beiVerbindungKontext: (VerbindungDaten) -> Unit,
) {
    val dichte = LocalDensity.current
    val standard = MaterialTheme.colorScheme.outline
    val gewählt = MaterialTheme.colorScheme.primary
    Canvas(
        Modifier.fillMaxSize().pointerInput(verbindungen, karte.knoten, ansicht) {
            fun trefferAn(position: Offset): VerbindungDaten? {
                val treffer = verbindungen.minByOrNull { verbindung ->
                    val a = anschlussBildschirmPosition(karte, verbindung.von, dichte.density, ansicht)
                    val b = anschlussBildschirmPosition(karte, verbindung.zu, dichte.density, ansicht)
                    punktStreckenAbstand(position, a, b)
                }
                return treffer?.takeIf {
                    val a = anschlussBildschirmPosition(karte, it.von, dichte.density, ansicht)
                    val b = anschlussBildschirmPosition(karte, it.zu, dichte.density, ansicht)
                    punktStreckenAbstand(position, a, b) <= 14.dp.toPx() * ansicht.zoom
                }
            }
            fun weltPosition(position: Offset) = GraphPunkt(
                (position.x - ansicht.verschiebung.x) / ansicht.zoom / dichte.density,
                (position.y - ansicht.verschiebung.y) / ansicht.zoom / dichte.density,
            )
            detectTapGestures(
                onTap = { pos ->
                    val treffer = trefferAn(pos)
                    if (treffer != null) zustand.wähleVerbindung(treffer.id)
                    else zustand.wähleKnoten(null)
                },
                onLongPress = { pos ->
                    val treffer = trefferAn(pos)
                    if (treffer != null) {
                        zustand.wähleVerbindung(treffer.id)
                        beiVerbindungKontext(treffer)
                    } else {
                        zustand.wähleKnoten(null)
                        beiHintergrundKontext(weltPosition(pos))
                    }
                },
            )
        }
    ) {
        verbindungen.forEach { verbindung ->
            val start = anschlussBildschirmPosition(karte, verbindung.von, dichte.density, ansicht)
            val ende = anschlussBildschirmPosition(karte, verbindung.zu, dichte.density, ansicht)
            val abstand = max(72.dp.toPx() * ansicht.zoom, abs(ende.x - start.x) * .45f)
            val pfad = Path().apply {
                moveTo(start.x, start.y)
                cubicTo(start.x + abstand, start.y, ende.x - abstand, ende.y, ende.x, ende.y)
            }
            drawPath(
                pfad,
                if (zustand.ausgewählteVerbindung == verbindung.id) gewählt else standard,
                style = Stroke(width = (if (zustand.ausgewählteVerbindung == verbindung.id) 5.dp.toPx() else 3.dp.toPx()) * ansicht.zoom, cap = StrokeCap.Round),
            )
        }
        val startRef = zustand.verbindungsStart
        val vorschau = zustand.verbindungsVorschau
        if (startRef != null && vorschau != null) {
            val start = anschlussBildschirmPosition(karte, startRef, dichte.density, ansicht)
            val ende = weltZuBildschirm(vorschau, dichte.density, ansicht)
            drawLine(gewählt.copy(alpha = .75f), start, ende, 3.dp.toPx() * ansicht.zoom, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun KnotenDarstellung(
    knoten: KnotenDaten,
    ausgewählt: Boolean,
    zustand: KartenEditorZustand,
    ansicht: AnsichtsFenster,
    dichte: Float,
    renderer: KnotenRenderer,
    farbeFürAnschluss: @Composable (AnschlussDaten) -> Color,
    beiKnotenKontext: (KnotenDaten) -> Unit,
    beiVerbindungAufHintergrund: (AnschlussVerweis, GraphPunkt) -> Unit,
    beiDoppelklick: () -> Unit,
) {
    val zoom = zustand.karte.ansicht.zoom
    var ziehbar by remember(knoten.id) { mutableStateOf(false) }
    val bildschirmPosition = weltZuBildschirm(knoten.position, dichte, ansicht)
    Box(
        Modifier.offset { IntOffset(bildschirmPosition.x.roundToInt(), bildschirmPosition.y.roundToInt()) }
            .graphicsLayer {
                scaleX = ansicht.zoom
                scaleY = ansicht.zoom
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .size(knoten.größe.breite.dp, knoten.größe.höhe.dp)
    ) {
        Card(
            Modifier.fillMaxSize()
                .border(if (ausgewählt) 3.dp else 1.dp, if (ausgewählt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                .pointerInput(knoten.id, zoom) {
                    detectDragGestures(
                        onDragStart = { start ->
                            ziehbar = renderer.interaktionsModus == KnotenInteraktionsModus.GanzeFlächeZiehbar || start.y <= KOPFZEILE_HÖHE_DP * density
                            if (ziehbar) { zustand.wähleKnoten(knoten.id); zustand.beginneInteraktion() }
                        },
                        onDragEnd = { if (ziehbar) zustand.beendeInteraktion(); ziehbar = false },
                        onDragCancel = { if (ziehbar) zustand.beendeInteraktion(); ziehbar = false },
                        onDrag = { änderung, delta ->
                            if (!ziehbar) return@detectDragGestures
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
                        onLongPress = {
                            zustand.wähleKnoten(knoten.id)
                            beiKnotenKontext(knoten)
                        },
                        onDoubleTap = { beiDoppelklick() },
                    )
                },
            elevation = CardDefaults.cardElevation(if (ausgewählt) 8.dp else 2.dp),
        ) {
            renderer.Inhalt(knoten, ausgewählt, object : KnotenRendererAktionen {
                override fun eigenschaftenErsetzen(eigenschaften: Map<String, KnotenEigenschaft>) {
                    zustand.führeAus(KartenAktion.KnotenEigenschaftenErsetzen(knoten.id, eigenschaften))
                }
            })
        }

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
                                                (aktuell.größe.breite + delta.x / density).coerceAtLeast(120f),
                                                (aktuell.größe.höhe + delta.y / density).coerceAtLeast(72f),
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

private fun weltZuBildschirm(welt: GraphPunkt, dichte: Float, ansicht: AnsichtsFenster): Offset = Offset(
    ansicht.verschiebung.x + welt.x * dichte * ansicht.zoom,
    ansicht.verschiebung.y + welt.y * dichte * ansicht.zoom,
)

private fun anschlussBildschirmPosition(
    karte: KartenDaten,
    ref: AnschlussVerweis,
    dichte: Float,
    ansicht: AnsichtsFenster,
): Offset = anschlussPositionWelt(karte, ref)?.let { weltZuBildschirm(it, dichte, ansicht) } ?: Offset.Zero

/**
 * Der sichtbare Ausschnitt der Kartenwelt in Graph-Koordinaten.
 *
 * Die Verschiebung der Ansicht liegt in Bildschirm-Pixeln vor; die Welt selbst
 * wird dagegen in dp gespeichert. Deshalb wird nach der Rücktransformation des
 * Zooms zusätzlich durch die Pixeldichte geteilt.
 */
internal fun sichtbarerWeltBereich(
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

internal fun KnotenDaten.istImBereich(bereich: Rect, puffer: Float): Boolean = Rect(
    left = position.x - puffer,
    top = position.y - puffer,
    right = position.x + größe.breite + puffer,
    bottom = position.y + größe.höhe + puffer,
).überschneidet(bereich)

private fun KartenDaten.inhaltsGrenzen(puffer: Float): Rect? {
    val erste = knoten.firstOrNull() ?: return null
    var links = erste.position.x
    var oben = erste.position.y
    var rechts = erste.position.x + erste.größe.breite
    var unten = erste.position.y + erste.größe.höhe
    knoten.drop(1).forEach { knoten ->
        links = min(links, knoten.position.x)
        oben = min(oben, knoten.position.y)
        rechts = max(rechts, knoten.position.x + knoten.größe.breite)
        unten = max(unten, knoten.position.y + knoten.größe.höhe)
    }
    return Rect(links - puffer, oben - puffer, rechts + puffer, unten + puffer)
}

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

private fun Rect.vereinigtMit(anderer: Rect): Rect = Rect(
    left = min(left, anderer.left),
    top = min(top, anderer.top),
    right = max(right, anderer.right),
    bottom = max(bottom, anderer.bottom),
)

private fun punktStreckenAbstand(p: Offset, a: Offset, b: Offset): Float {
    val ab = b - a
    val länge2 = ab.x * ab.x + ab.y * ab.y
    if (länge2 == 0f) return (p - a).getDistance()
    val t = (((p - a).x * ab.x + (p - a).y * ab.y) / länge2).coerceIn(0f, 1f)
    return (p - (a + ab * t)).getDistance()
}
