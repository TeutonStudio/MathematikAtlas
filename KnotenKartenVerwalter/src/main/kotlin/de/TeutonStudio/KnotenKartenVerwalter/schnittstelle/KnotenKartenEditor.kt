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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.*
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.zustand.*
import kotlin.math.*

private const val KNOTEN_VIEWPORT_PUFFER = 200f
private const val VERBINDUNG_VIEWPORT_PUFFER = 80f
private const val KOPFZEILE_HÖHE_DP = 44f
private const val VERBINDUNG_TREFFER_RADIUS_DP = 14f
private const val ANSCHLUSS_TREFFER_GRÖSSE_DP = 28f
private const val ANSCHLUSS_SICHTBAR_GRÖSSE_DP = 14f
private const val SNAP_EINTRITT_DP = 28f
private const val SNAP_AUSTRITT_DP = 34f

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun KnotenKartenEditor(
    zustand: KartenEditorZustand,
    modifier: Modifier = Modifier,
    rendererFür: (KnotenDaten) -> KnotenRenderer = { StandardKnotenRenderer },
    farbeFürAnschluss: @Composable (AnschlussDaten) -> Color = { MaterialTheme.colorScheme.primary },
    beiHintergrundKontext: (GraphPunkt) -> Unit = {},
    beiKnotenKontext: (KnotenDaten) -> Unit = {},
    beiVerbindungKontext: (VerbindungDaten) -> Unit = {},
    beiAnschlussKontext: (AnschlussVerweis) -> Unit = {},
    beiVerbindungAufHintergrund: (AnschlussVerweis, GraphPunkt) -> Unit = { _, _ -> },
    beiKnotenDoppelklick: (KnotenDaten) -> Unit = {},
    beiHintergrundDoppelklick: (GraphPunkt) -> Unit = {},
    beiZeigerPosition: (GraphPunkt) -> Unit = {},
    zeigeKnotenInspektor: Boolean = true,
    beiKnotenInspektor: (KnotenDaten) -> Unit = { InspektorSichtbarkeit.öffnen() },
) {
    val dichte = LocalDensity.current
    val karte = zustand.karte
    val ansicht = karte.ansicht
    val aktuelleKarte by rememberUpdatedState(karte)
    val aktuelleAnsicht by rememberUpdatedState(ansicht)
    val aktuelleDichte by rememberUpdatedState(dichte.density)
    var anzeigeGröße by remember { mutableStateOf(IntSize.Zero) }
    var magnetischesZiel by remember(karte.id) { mutableStateOf<AnschlussVerweis?>(null) }
    var auswahlRechteckBildschirm by remember(karte.id) { mutableStateOf<Rect?>(null) }
    var aktuelleAuswahlÄnderung by remember { mutableStateOf(AuswahlÄnderung.Ersetzen) }
    var leertasteGedrückt by remember { mutableStateOf(false) }
    var umschaltGedrückt by remember { mutableStateOf(false) }
    var primärModifierGedrückt by remember { mutableStateOf(false) }
    LaunchedEffect(zustand.verbindungsStart) {
        if (zustand.verbindungsStart == null) magnetischesZiel = null
    }
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
            .onPreviewKeyEvent { event ->
                umschaltGedrückt = event.isShiftPressed
                primärModifierGedrückt = event.isCtrlPressed || event.isMetaPressed
                if (event.key == Key.Spacebar) {
                    leertasteGedrückt = event.type == KeyEventType.KeyDown
                }
                false
            }
            .pointerInput(zustand) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val änderung = event.changes.firstOrNull() ?: continue
                        beiZeigerPosition(bildschirmZuWelt(änderung.position, aktuelleAnsicht, aktuelleDichte))
                        when (event.type) {
                            PointerEventType.Press -> aktuelleAuswahlÄnderung = when {
                                primärModifierGedrückt -> AuswahlÄnderung.Umschalten
                                umschaltGedrückt -> AuswahlÄnderung.Hinzufügen
                                else -> AuswahlÄnderung.Ersetzen
                            }
                            PointerEventType.Scroll -> {
                                val bisher = aktuelleAnsicht
                                if (umschaltGedrückt) {
                                    zustand.führeAus(
                                        KartenAktion.AnsichtÄndern(bisher.copy(verschiebung = bisher.verschiebung + GraphPunkt(-änderung.scrollDelta.y * 36f, 0f))),
                                        mitHistorie = false,
                                    )
                                } else {
                                    val faktor = if (änderung.scrollDelta.y < 0f) 1.1f else 1f / 1.1f
                                    val neuerZoom = (bisher.zoom * faktor).coerceIn(.25f, 3.5f)
                                    val effektiv = neuerZoom / bisher.zoom
                                    val zentrum = änderung.position
                                    zustand.führeAus(
                                        KartenAktion.AnsichtÄndern(AnsichtsFenster(
                                            verschiebung = GraphPunkt(
                                                zentrum.x - (zentrum.x - bisher.verschiebung.x) * effektiv,
                                                zentrum.y - (zentrum.y - bisher.verschiebung.y) * effektiv,
                                            ),
                                            zoom = neuerZoom,
                                        )),
                                        mitHistorie = false,
                                    )
                                }
                                änderung.consume()
                            }
                        }
                    }
                }
            }
            .sekundärKlick(zustand, karte.id) { pos ->
                if (!trifftKnoten(pos, aktuelleKarte, aktuelleAnsicht, aktuelleDichte)) {
                    zustand.wähleKnoten(null)
                    beiHintergrundKontext(bildschirmZuWelt(pos, aktuelleAnsicht, aktuelleDichte))
                }
            }
            .pointerInput(zustand) {
                awaitEachGesture {
                    var gesamterPan = Offset.Zero
                    var gesamterZoom = 1f
                    var hintergrundGesteAktiv = false
                    val ersterDruck = awaitFirstDown(requireUnconsumed = false)
                    val maus = ersterDruck.type == PointerType.Mouse
                    val mittlereTaste = maus && currentEvent.buttons.isTertiaryPressed
                    val auswahlGeste = maus && currentEvent.buttons.isPrimaryPressed && !leertasteGedrückt
                    if (trifftKnoten(ersterDruck.position, aktuelleKarte, aktuelleAnsicht, aktuelleDichte) && !mittlereTaste) {
                        do {
                            val ereignis = awaitPointerEvent()
                        } while (ereignis.changes.any { it.pressed })
                    } else if (auswahlGeste) {
                        val start = ersterDruck.position
                        var ende = start
                        do {
                            val ereignis = awaitPointerEvent()
                            val änderung = ereignis.changes.firstOrNull { it.id == ersterDruck.id } ?: break
                            ende = änderung.position
                            if (!hintergrundGesteAktiv && (ende - start).getDistance() > viewConfiguration.touchSlop) {
                                hintergrundGesteAktiv = true
                            }
                            if (hintergrundGesteAktiv) {
                                auswahlRechteckBildschirm = Rect(
                                    min(start.x, ende.x), min(start.y, ende.y),
                                    max(start.x, ende.x), max(start.y, ende.y),
                                )
                                änderung.consume()
                            }
                        } while (ereignis.changes.any { it.pressed })
                        if (hintergrundGesteAktiv) {
                            val startWelt = bildschirmZuWelt(start, aktuelleAnsicht, aktuelleDichte)
                            val endeWelt = bildschirmZuWelt(ende, aktuelleAnsicht, aktuelleDichte)
                            zustand.wähleKnotenImBereich(
                                Rect(min(startWelt.x, endeWelt.x), min(startWelt.y, endeWelt.y), max(startWelt.x, endeWelt.x), max(startWelt.y, endeWelt.y)),
                                aktuelleAuswahlÄnderung,
                            )
                        }
                        auswahlRechteckBildschirm = null
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
                    onDoubleTap = { pos -> beiHintergrundDoppelklick(bildschirmZuWelt(pos, aktuelleAnsicht, aktuelleDichte)) },
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
                        ausgewählt = knoten.id in zustand.ausgewählteKnoten,
                        zustand = zustand,
                        ansicht = ansicht,
                        dichte = dichte.density,
                        renderer = rendererFür(knoten),
                        farbeFürAnschluss = farbeFürAnschluss,
                        magnetischesZiel = magnetischesZiel,
                        beiMagnetischemZiel = { magnetischesZiel = it },
                        beiKnotenKontext = beiKnotenKontext,
                        beiAnschlussKontext = beiAnschlussKontext,
                        beiVerbindungAufHintergrund = beiVerbindungAufHintergrund,
                        beiDoppelklick = { beiKnotenDoppelklick(knoten) },
                        auswahlÄnderung = aktuelleAuswahlÄnderung,
                        zeigeKnotenInspektor = zeigeKnotenInspektor,
                        beiInspektorÖffnen = { beiKnotenInspektor(knoten) },
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
        auswahlRechteckBildschirm?.let { rechteck ->
            val füllung = MaterialTheme.colorScheme.primary.copy(alpha = .12f)
            val rahmen = MaterialTheme.colorScheme.primary
            Canvas(Modifier.fillMaxSize()) {
                drawRect(füllung, rechteck.topLeft, rechteck.size)
                drawRect(rahmen, rechteck.topLeft, rechteck.size, style = Stroke(1.dp.toPx()))
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

internal data class VerbindungsGeometrie(
    val start: Offset,
    val kontrollpunkt1: Offset,
    val kontrollpunkt2: Offset,
    val ende: Offset,
) {
    val pfad: Path by lazy(LazyThreadSafetyMode.NONE) {
        Path().apply {
            moveTo(start.x, start.y)
            cubicTo(
                kontrollpunkt1.x,
                kontrollpunkt1.y,
                kontrollpunkt2.x,
                kontrollpunkt2.y,
                ende.x,
                ende.y,
            )
        }
    }

    val umhüllung: Rect = Rect(
        left = minOf(start.x, kontrollpunkt1.x, kontrollpunkt2.x, ende.x),
        top = minOf(start.y, kontrollpunkt1.y, kontrollpunkt2.y, ende.y),
        right = maxOf(start.x, kontrollpunkt1.x, kontrollpunkt2.x, ende.x),
        bottom = maxOf(start.y, kontrollpunkt1.y, kontrollpunkt2.y, ende.y),
    )

    fun punktBei(t: Float): Offset {
        val geklemmt = t.coerceIn(0f, 1f)
        val gegen = 1f - geklemmt
        val a = gegen * gegen * gegen
        val b = 3f * gegen * gegen * geklemmt
        val c = 3f * gegen * geklemmt * geklemmt
        val d = geklemmt * geklemmt * geklemmt
        return Offset(
            start.x * a + kontrollpunkt1.x * b + kontrollpunkt2.x * c + ende.x * d,
            start.y * a + kontrollpunkt1.y * b + kontrollpunkt2.y * c + ende.y * d,
        )
    }

    fun abstandZu(position: Offset, maximaleSegmentLänge: Float = 8f): Float {
        val kontrollPolygonLänge =
            (kontrollpunkt1 - start).getDistance() +
                (kontrollpunkt2 - kontrollpunkt1).getDistance() +
                (ende - kontrollpunkt2).getDistance()
        val schritte = ceil(kontrollPolygonLänge / maximaleSegmentLänge.coerceAtLeast(1f))
            .toInt()
            .coerceIn(1, 256)
        var vorher = start
        var kleinsterAbstand = Float.POSITIVE_INFINITY
        for (index in 1..schritte) {
            val aktuell = punktBei(index.toFloat() / schritte)
            kleinsterAbstand = min(kleinsterAbstand, punktStreckenAbstand(position, vorher, aktuell))
            vorher = aktuell
        }
        return kleinsterAbstand
    }
}

internal fun berechneVerbindungsGeometrie(
    start: Offset,
    ende: Offset,
    mindestKontrollAbstand: Float,
): VerbindungsGeometrie {
    val abstand = max(mindestKontrollAbstand, abs(ende.x - start.x) * .45f)
    return VerbindungsGeometrie(
        start = start,
        kontrollpunkt1 = Offset(start.x + abstand, start.y),
        kontrollpunkt2 = Offset(ende.x - abstand, ende.y),
        ende = ende,
    )
}

internal data class VerbindungsVorschauEndpunkte(
    val quelle: Offset,
    val ziel: Offset,
)

/**
 * Normalisiert die Vorschau in dieselbe semantische Richtung wie gespeicherte Edges.
 * Wird an einem Eingang gezogen, ist der Zeiger die vorläufige Quelle und der feste
 * Eingang das Ziel. Bei einem Ausgang bleibt der feste Anschluss die Quelle.
 */
internal fun normalisiereVerbindungsVorschauEndpunkte(
    festerAnschluss: Offset,
    zeiger: Offset,
    startRichtung: AnschlussRichtung?,
): VerbindungsVorschauEndpunkte = if (startRichtung == AnschlussRichtung.Eingang) {
    VerbindungsVorschauEndpunkte(quelle = zeiger, ziel = festerAnschluss)
} else {
    VerbindungsVorschauEndpunkte(quelle = festerAnschluss, ziel = zeiger)
}

private data class SichtbareVerbindungsGeometrie(
    val verbindung: VerbindungDaten,
    val geometrie: VerbindungsGeometrie,
    val zeichnungsIndex: Int,
)

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
    var schwebendeVerbindung by remember { mutableStateOf<VerbindungsId?>(null) }
    val geometrien = remember(verbindungen, karte.knoten, ansicht, dichte.density) {
        verbindungen.mapIndexedNotNull { index, verbindung ->
            val start = anschlussPositionWelt(karte, verbindung.von) ?: return@mapIndexedNotNull null
            val ende = anschlussPositionWelt(karte, verbindung.zu) ?: return@mapIndexedNotNull null
            SichtbareVerbindungsGeometrie(
                verbindung = verbindung,
                geometrie = berechneVerbindungsGeometrie(
                    start = weltZuBildschirm(start, dichte.density, ansicht),
                    ende = weltZuBildschirm(ende, dichte.density, ansicht),
                    mindestKontrollAbstand = 72f * dichte.density * ansicht.zoom,
                ),
                zeichnungsIndex = index,
            )
        }
    }
    val aktuelleGeometrien by rememberUpdatedState(geometrien)
    val aktuelleAuswahl by rememberUpdatedState(zustand.ausgewählteVerbindung)
    val trefferRadius = with(dichte) { VERBINDUNG_TREFFER_RADIUS_DP.dp.toPx() }

    Canvas(
        Modifier.fillMaxSize()
            .sekundärKlick(zustand, verbindungen) { pos ->
                val treffer = findeVerbindungsTreffer(
                    position = pos,
                    geometrien = aktuelleGeometrien,
                    trefferRadius = trefferRadius,
                    ausgewählteVerbindung = aktuelleAuswahl,
                )
                if (treffer != null) {
                    zustand.wähleVerbindung(treffer.id)
                    beiVerbindungKontext(treffer)
                } else {
                    zustand.wähleKnoten(null)
                    beiHintergrundKontext(GraphPunkt(
                        (pos.x - ansicht.verschiebung.x) / ansicht.zoom / dichte.density,
                        (pos.y - ansicht.verschiebung.y) / ansicht.zoom / dichte.density,
                    ))
                }
            }
            .pointerInput(verbindungen, karte.knoten, ansicht) {
                fun trefferAn(position: Offset): VerbindungDaten? = findeVerbindungsTreffer(
                    position = position,
                    geometrien = aktuelleGeometrien,
                    trefferRadius = trefferRadius,
                    ausgewählteVerbindung = aktuelleAuswahl,
                )
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
            .pointerInput(verbindungen, karte.knoten, ansicht) {
                awaitPointerEventScope {
                    while (true) {
                        val ereignis = awaitPointerEvent()
                        schwebendeVerbindung = when (ereignis.type) {
                            PointerEventType.Exit -> null
                            PointerEventType.Move, PointerEventType.Enter -> ereignis.changes.firstOrNull()?.position?.let { position ->
                                findeVerbindungsTreffer(
                                    position = position,
                                    geometrien = aktuelleGeometrien,
                                    trefferRadius = trefferRadius,
                                    ausgewählteVerbindung = aktuelleAuswahl,
                                )?.id
                            }
                            else -> schwebendeVerbindung
                        }
                    }
                }
            }
    ) {
        geometrien.forEach { eintrag ->
            val verbindung = eintrag.verbindung
            val istAusgewählt = zustand.ausgewählteVerbindung == verbindung.id
            val istSchwebend = schwebendeVerbindung == verbindung.id
            val basisBreite = when {
                istAusgewählt -> 5.dp.toPx()
                istSchwebend -> 4.dp.toPx()
                else -> 3.dp.toPx()
            }
            val mindestBreite = when {
                istAusgewählt -> 3.dp.toPx()
                istSchwebend -> 2.5.dp.toPx()
                else -> 1.5.dp.toPx()
            }
            drawPath(
                eintrag.geometrie.pfad,
                when {
                    istAusgewählt -> gewählt
                    istSchwebend -> gewählt.copy(alpha = .75f)
                    else -> standard
                },
                style = Stroke(
                    width = max(basisBreite * ansicht.zoom, mindestBreite),
                    cap = StrokeCap.Round,
                ),
            )
        }
        val startRef = zustand.verbindungsStart
        val vorschau = zustand.verbindungsVorschau
        if (startRef != null && vorschau != null) {
            val festerAnschluss = anschlussBildschirmPosition(karte, startRef, dichte.density, ansicht)
            val zeiger = weltZuBildschirm(vorschau, dichte.density, ansicht)
            val startRichtung = karte.knoten
                .firstOrNull { it.id == startRef.knotenId }
                ?.anschlüsse
                ?.firstOrNull { it.id == startRef.anschlussId }
                ?.richtung
            val endpunkte = normalisiereVerbindungsVorschauEndpunkte(
                festerAnschluss = festerAnschluss,
                zeiger = zeiger,
                startRichtung = startRichtung,
            )
            val geometrie = berechneVerbindungsGeometrie(
                start = endpunkte.quelle,
                ende = endpunkte.ziel,
                mindestKontrollAbstand = 72.dp.toPx() * ansicht.zoom,
            )
            drawPath(
                geometrie.pfad,
                gewählt.copy(alpha = .75f),
                style = Stroke(
                    width = max(3.dp.toPx() * ansicht.zoom, 2.dp.toPx()),
                    cap = StrokeCap.Round,
                ),
            )
        }
    }
}

private fun findeVerbindungsTreffer(
    position: Offset,
    geometrien: List<SichtbareVerbindungsGeometrie>,
    trefferRadius: Float,
    ausgewählteVerbindung: VerbindungsId?,
): VerbindungDaten? {
    val kandidaten = geometrien.mapNotNull { eintrag ->
        if (!eintrag.geometrie.umhüllung.erweitert(trefferRadius).enthält(position)) return@mapNotNull null
        val abstand = eintrag.geometrie.abstandZu(position)
        if (abstand <= trefferRadius) eintrag to abstand else null
    }
    val kleinsterAbstand = kandidaten.minOfOrNull { it.second } ?: return null
    val gleichNahe = kandidaten.filter { it.second <= kleinsterAbstand + .5f }
    return gleichNahe.firstOrNull { it.first.verbindung.id == ausgewählteVerbindung }?.first?.verbindung
        ?: gleichNahe.maxByOrNull { it.first.zeichnungsIndex }?.first?.verbindung
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
    magnetischesZiel: AnschlussVerweis?,
    beiMagnetischemZiel: (AnschlussVerweis?) -> Unit,
    beiKnotenKontext: (KnotenDaten) -> Unit,
    beiAnschlussKontext: (AnschlussVerweis) -> Unit,
    beiVerbindungAufHintergrund: (AnschlussVerweis, GraphPunkt) -> Unit,
    beiDoppelklick: () -> Unit,
    auswahlÄnderung: AuswahlÄnderung,
    zeigeKnotenInspektor: Boolean,
    beiInspektorÖffnen: () -> Unit,
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
                .pointerHoverIcon(PointerIcon.Hand)
                .semantics {
                    contentDescription = "Knoten ${knoten.name}, ${knoten.anschlüsse.size} Anschlüsse"
                    selected = ausgewählt
                }
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when {
                        event.key == Key.Enter -> { zustand.wähleKnoten(knoten.id, auswahlÄnderung); true }
                        event.key == Key.F10 && event.isShiftPressed -> { beiKnotenKontext(knoten); true }
                        event.isAltPressed && event.key == Key.DirectionLeft -> zustand.wähleRäumlichNächsten(GraphPunkt(-1f, 0f))
                        event.isAltPressed && event.key == Key.DirectionRight -> zustand.wähleRäumlichNächsten(GraphPunkt(1f, 0f))
                        event.isAltPressed && event.key == Key.DirectionUp -> zustand.wähleRäumlichNächsten(GraphPunkt(0f, -1f))
                        event.isAltPressed && event.key == Key.DirectionDown -> zustand.wähleRäumlichNächsten(GraphPunkt(0f, 1f))
                        else -> false
                    }
                }
                .focusable()
                .pointerInput(knoten.id, zoom) {
                    detectDragGestures(
                        onDragStart = { start ->
                            ziehbar = renderer.interaktionsModus == KnotenInteraktionsModus.GanzeFlächeZiehbar || start.y <= KOPFZEILE_HÖHE_DP * density
                            if (ziehbar) {
                                if (knoten.id !in zustand.ausgewählteKnoten) zustand.wähleKnoten(knoten.id, auswahlÄnderung)
                                zustand.beginneInteraktion()
                            }
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
                .sekundärKlick(zustand, knoten.id) {
                    if (knoten.id !in zustand.ausgewählteKnoten) zustand.wähleKnoten(knoten.id)
                    beiKnotenKontext(knoten)
                }
                .pointerInput(knoten.id) {
                    detectTapGestures(
                        onTap = { zustand.wähleKnoten(knoten.id, auswahlÄnderung) },
                        onLongPress = {
                            zustand.wähleKnoten(knoten.id)
                            beiKnotenKontext(knoten)
                        },
                        onDoubleTap = { beiDoppelklick() },
                    )
                },
            elevation = CardDefaults.cardElevation(if (ausgewählt) 8.dp else 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            renderer.Inhalt(knoten, ausgewählt, object : KnotenRendererAktionen {
                override fun eigenschaftenErsetzen(eigenschaften: Map<String, KnotenEigenschaft>) {
                    zustand.führeAus(KartenAktion.KnotenEigenschaftenErsetzen(knoten.id, eigenschaften))
                }
            })
        }

        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset(y = 22.dp)
                .fillMaxWidth()
                .height(18.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            renderer.Fußzeile(knoten, ausgewählt)
        }

        if (zeigeKnotenInspektor) {
            KnotenInspektorSchaltfläche(
                beiKlick = {
                    zustand.wähleKnoten(knoten.id)
                    beiInspektorÖffnen()
                },
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }

        knoten.anschlüsse.groupBy { it.kante }.forEach { (_, anschlüsse) ->
            anschlüsse.sortedBy { it.reihenfolge }.forEachIndexed { index, anschluss ->
                AnschlussGriff(
                    knoten = knoten,
                    anschluss = anschluss,
                    index = index,
                    anzahl = anschlüsse.size,
                    zustand = zustand,
                    farbe = farbeFürAnschluss(anschluss),
                    magnetischesZiel = magnetischesZiel,
                    beiMagnetischemZiel = beiMagnetischemZiel,
                    beiAnschlussKontext = beiAnschlussKontext,
                    beiVerbindungAufHintergrund = beiVerbindungAufHintergrund,
                )
            }
        }

        if (ausgewählt) {
            Box(
                Modifier.align(Alignment.BottomEnd).offset(6.dp, 6.dp).size(18.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .pointerHoverIcon(PointerIcon.Crosshair)
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
private fun KnotenInspektorSchaltfläche(
    beiKlick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val knotenHintergrund = MaterialTheme.colorScheme.surfaceContainerLow
    val iconFarbe = kontrastAdaptiveProfilFarbe(
        profilFarbe = MaterialTheme.colorScheme.primary,
        hintergrund = knotenHintergrund,
    )
    IconButton(
        onClick = beiKlick,
        modifier = modifier
            .size(40.dp)
            .semantics { contentDescription = "Inspektor öffnen" },
        colors = IconButtonDefaults.iconButtonColors(contentColor = iconFarbe),
    ) {
        KnotenInspektorSymbol(Modifier.size(20.dp))
    }
}

private fun bildschirmZuWelt(position: Offset, ansicht: AnsichtsFenster, dichte: Float): GraphPunkt = GraphPunkt(
    (position.x - ansicht.verschiebung.x) / ansicht.zoom / dichte.coerceAtLeast(.0001f),
    (position.y - ansicht.verschiebung.y) / ansicht.zoom / dichte.coerceAtLeast(.0001f),
)

private fun Modifier.sekundärKlick(zustand: KartenEditorZustand, key: Any?, aktion: (Offset) -> Unit): Modifier =
    pointerInput(zustand, key) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (down.type != PointerType.Mouse || !currentEvent.buttons.isSecondaryPressed) return@awaitEachGesture
            down.consume()
            aktion(down.position)
            do {
                val ereignis = awaitPointerEvent()
                ereignis.changes.forEach { it.consume() }
            } while (ereignis.changes.any { it.pressed })
        }
    }

internal fun farbKontrastVerhältnis(vordergrund: Color, hintergrund: Color): Float {
    val vordergrundLuminanz = vordergrund.copy(alpha = 1f).luminance()
    val hintergrundLuminanz = hintergrund.copy(alpha = 1f).luminance()
    val heller = max(vordergrundLuminanz, hintergrundLuminanz)
    val dunkler = min(vordergrundLuminanz, hintergrundLuminanz)
    return (heller + 0.05f) / (dunkler + 0.05f)
}

/**
 * Bewahrt die Profilfarbe, solange sie auf dem Knoten ausreichend kontrastiert.
 * Andernfalls wird sie nur so weit in Richtung Schwarz oder Weiß verschoben,
 * wie für die Erkennbarkeit erforderlich ist. Ein zusätzlicher Icon-Hintergrund
 * ist dadurch weder nötig noch erlaubt.
 */
internal fun kontrastAdaptiveProfilFarbe(
    profilFarbe: Color,
    hintergrund: Color,
    mindestKontrast: Float = 4.5f,
): Color {
    val deckendeProfilFarbe = profilFarbe.copy(alpha = 1f)
    if (farbKontrastVerhältnis(deckendeProfilFarbe, hintergrund) >= mindestKontrast) {
        return deckendeProfilFarbe
    }
    val ziel = listOf(Color.Black, Color.White)
        .maxBy { farbKontrastVerhältnis(it, hintergrund) }
    var unten = 0f
    var oben = 1f
    repeat(18) {
        val mitte = (unten + oben) / 2f
        if (farbKontrastVerhältnis(lerp(deckendeProfilFarbe, ziel, mitte), hintergrund) >= mindestKontrast) {
            oben = mitte
        } else {
            unten = mitte
        }
    }
    return lerp(deckendeProfilFarbe, ziel, oben).copy(alpha = 1f)
}

@Composable
private fun BoxScope.AnschlussGriff(
    knoten: KnotenDaten,
    anschluss: AnschlussDaten,
    index: Int,
    anzahl: Int,
    zustand: KartenEditorZustand,
    farbe: Color,
    magnetischesZiel: AnschlussVerweis?,
    beiMagnetischemZiel: (AnschlussVerweis?) -> Unit,
    beiAnschlussKontext: (AnschlussVerweis) -> Unit,
    beiVerbindungAufHintergrund: (AnschlussVerweis, GraphPunkt) -> Unit,
) {
    val anteil = (index + 1f) / (anzahl + 1f)
    val zoom = zustand.karte.ansicht.zoom.coerceAtLeast(.0001f)
    val interaktionsGröße = ANSCHLUSS_TREFFER_GRÖSSE_DP / zoom
    val sichtbareGröße = ANSCHLUSS_SICHTBAR_GRÖSSE_DP / zoom
    val interaktionsHalbe = interaktionsGröße / 2f
    val ausrichtung = Alignment.TopStart
    val x = when (anschluss.kante) {
        AnschlussKante.Links -> (-interaktionsHalbe).dp
        AnschlussKante.Rechts -> (knoten.größe.breite - interaktionsHalbe).dp
        AnschlussKante.Oben, AnschlussKante.Unten -> (knoten.größe.breite * anteil - interaktionsHalbe).dp
    }
    val y = when (anschluss.kante) {
        AnschlussKante.Oben -> (-interaktionsHalbe).dp
        AnschlussKante.Unten -> (knoten.größe.höhe - interaktionsHalbe).dp
        AnschlussKante.Links, AnschlussKante.Rechts -> (knoten.größe.höhe * anteil - interaktionsHalbe).dp
    }
    val ref = AnschlussVerweis(knoten.id, anschluss.id)
    val kompatibel = zustand.kompatibelMitStart(ref)
    val eingerastet = magnetischesZiel == ref
    val aktuellesZielSetzen by rememberUpdatedState(beiMagnetischemZiel)
    var zugPosition by remember(knoten.id, anschluss.id) { mutableStateOf<GraphPunkt?>(null) }
    var zugZiel by remember(knoten.id, anschluss.id) { mutableStateOf<AnschlussVerweis?>(null) }
    val startWelt = anschlussPositionWelt(knoten, anschluss)
    val interaktionsObenLinks = startWelt - GraphPunkt(interaktionsHalbe, interaktionsHalbe)
    Box(
        Modifier.align(ausrichtung).offset(x, y).size(interaktionsGröße.dp)
            .pointerHoverIcon(PointerIcon.Crosshair)
            .semantics {
                contentDescription = "${knoten.name}, Anschluss ${anschluss.name}, ${anschluss.richtung}, ${anschluss.art.wert}"
            }
            .sekundärKlick(zustand, ref) {
                aktuellesZielSetzen(null)
                zustand.brecheVerbindungsVorschauAb()
                beiAnschlussKontext(ref)
            }
            .combinedClickable(
                enabled = kompatibel,
                onClick = { zustand.anschlussAngeklickt(ref) },
                onLongClick = {
                    aktuellesZielSetzen(null)
                    zustand.brecheVerbindungsVorschauAb()
                    beiAnschlussKontext(ref)
                },
            )
            .pointerInput(ref, kompatibel, zoom) {
                if (!kompatibel) return@pointerInput
                detectDragGestures(
                    onDragStart = {
                        zugPosition = startWelt
                        zugZiel = null
                        aktuellesZielSetzen(null)
                        zustand.beginneVerbindung(ref, startWelt)
                    },
                    onDrag = { änderung, _ ->
                        änderung.consume()
                        val zeigerWelt = interaktionsObenLinks + GraphPunkt(
                            // Absolute lokale Pointerposition statt Delta-Akkumulation:
                            // dadurch werden Touch-Slop und Overslop nicht doppelt addiert.
                            änderung.position.x / density,
                            änderung.position.y / density,
                        )
                        zugPosition = zeigerWelt
                        val ziel = nächsterKompatiblerAnschluss(
                            zustand = zustand,
                            start = ref,
                            ende = zeigerWelt,
                            bisherigesZiel = zugZiel,
                        )
                        zugZiel = ziel
                        aktuellesZielSetzen(ziel)
                        val vorschau = ziel?.let { anschlussPositionWelt(zustand.karte, it) } ?: zeigerWelt
                        zustand.aktualisiereVerbindungsVorschau(vorschau)
                    },
                    onDragEnd = {
                        val ende = zugPosition ?: startWelt
                        val ziel = zugZiel
                        zugZiel = null
                        aktuellesZielSetzen(null)
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
                        zugZiel = null
                        aktuellesZielSetzen(null)
                        zustand.brecheVerbindungsVorschauAb()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(sichtbareGröße.dp)
                .background(if (kompatibel) farbe else farbe.copy(alpha = .2f), CircleShape)
                .border((2f / zoom).dp, MaterialTheme.colorScheme.surface, CircleShape)
        )
        if (eingerastet) {
            Box(
                Modifier.size((24f / zoom).dp)
                    .border((3f / zoom).dp, MaterialTheme.colorScheme.tertiary, CircleShape)
            )
        }
    }
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
    bisherigesZiel: AnschlussVerweis?,
): AnschlussVerweis? {
    val zoom = zustand.karte.ansicht.zoom.coerceAtLeast(.0001f)
    fun abstandZu(ref: AnschlussVerweis): Float? {
        if (ref == start || !zustand.kompatibelMitStart(ref)) return null
        val position = anschlussPositionWelt(zustand.karte, ref) ?: return null
        return hypot((position.x - ende.x).toDouble(), (position.y - ende.y).toDouble()).toFloat()
    }

    if (bisherigesZiel != null) {
        val abstand = abstandZu(bisherigesZiel)
        if (abstand != null && abstand <= SNAP_AUSTRITT_DP / zoom) return bisherigesZiel
    }

    return zustand.karte.knoten.asSequence().flatMap { knoten ->
        knoten.anschlüsse.asSequence().map { anschluss -> AnschlussVerweis(knoten.id, anschluss.id) }
    }.mapNotNull { ref -> abstandZu(ref)?.let { abstand -> ref to abstand } }
        .filter { it.second <= SNAP_EINTRITT_DP / zoom }
        .minByOrNull { it.second }
        ?.first
}

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

/** Zeichnung und Viewport-Culling verwenden dieselbe kubische Verbindungsgeometrie. */
private fun VerbindungDaten.istImBereich(
    karte: KartenDaten,
    bereich: Rect,
    puffer: Float,
): Boolean {
    val start = anschlussPositionWelt(karte, von) ?: return false
    val ende = anschlussPositionWelt(karte, zu) ?: return false
    val geometrie = berechneVerbindungsGeometrie(
        start = Offset(start.x, start.y),
        ende = Offset(ende.x, ende.y),
        mindestKontrollAbstand = 72f,
    )
    return geometrie.umhüllung.erweitert(puffer).überschneidet(bereich)
}

private fun anschlussPositionWelt(karte: KartenDaten, ref: AnschlussVerweis): GraphPunkt? {
    val knoten = karte.knoten.firstOrNull { it.id == ref.knotenId } ?: return null
    val anschluss = knoten.anschlüsse.firstOrNull { it.id == ref.anschlussId } ?: return null
    return anschlussPositionWelt(knoten, anschluss)
}

private fun Rect.enthält(punkt: Offset): Boolean =
    punkt.x in left..right && punkt.y in top..bottom

private fun Rect.erweitert(puffer: Float): Rect = Rect(
    left = left - puffer,
    top = top - puffer,
    right = right + puffer,
    bottom = bottom + puffer,
)

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