package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.foundation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.*
import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.KnotenKartenVerwalter.logik.KartenAktion
import de.TeutonStudio.KnotenKartenVerwalter.logik.ansichtFürInhalt
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
    auswertungsDauerFür: (KnotenId) -> Long? = { null },
    interaktionGesperrt: Boolean = false,
    anschlussInfoFür: (KartenDaten, AnschlussVerweis) -> AnschlussInfo? = { k, r -> anschlussBasisInfo(k, r) },
    beiVerbindungsEinfügeAnfrage: ((VerbindungsEinfügeKontext, GraphPunkt) -> Unit)? = null,
) {
    val einfügeAnimation = zustand.einfügeLayoutAnimation
    LaunchedEffect(einfügeAnimation) {
        if (einfügeAnimation != null) {
            val fortschritt = Animatable(0f)
            fortschritt.animateTo(1f, tween(200)) {
                zustand.aktualisiereEinfügeAnimation(einfügeAnimation, value)
            }
        }
    }
    DisposableEffect(zustand) { onDispose { zustand.beendeEinfügeAnimation() } }
    zustand.projektionImHintergrund = true
    DisposableEffect(zustand) { onDispose { zustand.projektionImHintergrund = false } }
    val gruppenStruktur = remember(zustand.karte.visuelleGruppen) { zustand.karte.visuelleGruppen.map { it.copy(ansicht = AnsichtsFenster.Standard, ansichtInitialisiert = false) } }
    LaunchedEffect(zustand.karte.id, zustand.karte.knoten, zustand.karte.verbindungen, gruppenStruktur, zustand.geöffneteGruppe, zustand.knotenFußzeilenHöhe) {
        if (zustand.projektionWirdBerechnet) zustand.berechneProjektionImHintergrund()
    }
    val dichte = LocalDensity.current
    val karte = zustand.darstellungsKarte
    val ansicht = karte.ansicht
    val gruppenÜbergang = erinnereGruppenÜbergang(zustand)
    val aktuelleKarte by rememberUpdatedState(karte)
    val aktuelleAnsicht by rememberUpdatedState(ansicht)
    val aktuelleDichte by rememberUpdatedState(dichte.density)
    val aktuellGesperrt by rememberUpdatedState(interaktionGesperrt)
    var anzeigeGröße by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(zustand.geöffneteGruppe, anzeigeGröße, zustand.projektionWirdBerechnet) {
        val id = zustand.geöffneteGruppe
        val gruppe = zustand.karte.visuelleGruppen.firstOrNull { it.id == id }
        if (gruppe != null && !gruppe.ansichtInitialisiert && !zustand.projektionWirdBerechnet && anzeigeGröße.width > 0 && anzeigeGröße.height > 0) {
            val angepasst = zustand.darstellungsKarte.ansichtFürInhalt(anzeigeGröße.width / dichte.density, anzeigeGröße.height / dichte.density, pufferDp = 60f)
                ?.let { it.copy(verschiebung = GraphPunkt(it.verschiebung.x * dichte.density, it.verschiebung.y * dichte.density)) }
                ?: AnsichtsFenster(GraphPunkt(80f-gruppe.position.x*dichte.density,80f-gruppe.position.y*dichte.density),1f)
            zustand.führeAus(KartenAktion.AnsichtÄndern(angepasst),mitHistorie=false)
        }
    }
    val anschlussInfos = remember(zustand, karte.id, zustand.geöffneteGruppe) { AnschlussInfoInteraktion() }
    val aktuelleAnschlussInfos by rememberUpdatedState(anschlussInfos)
    var editorUrsprungImRoot by remember { mutableStateOf(Offset.Zero) }
    var magnetischesZiel by remember(karte.id) { mutableStateOf<AnschlussVerweis?>(null) }
    var auswahlRechteckBildschirm by remember(karte.id) { mutableStateOf<Rect?>(null) }
    var aktuelleAuswahlÄnderung by remember { mutableStateOf(AuswahlÄnderung.Ersetzen) }
    var leertasteGedrückt by remember { mutableStateOf(false) }
    var umschaltGedrückt by remember { mutableStateOf(false) }
    var primärModifierGedrückt by remember { mutableStateOf(false) }
    var altGedrückt by remember { mutableStateOf(false) }
    LaunchedEffect(zustand.verbindungsStart) {
        if (zustand.verbindungsStart == null) magnetischesZiel = null
    }
    val sichtbarerWeltBereich = sichtbarerWeltBereich(ansicht, anzeigeGröße, dichte.density)
    var renderAuswahl by remember(karte.id, karte.knoten, karte.verbindungen, zustand.knotenAnhänge, anzeigeGröße, dichte.density) {
        mutableStateOf<RenderAuswahl?>(null)
    }
    val neueRenderAuswahl = sichtbarerWeltBereich?.let { bereich ->
        renderAuswahl?.takeIf { it.deckt(bereich) } ?: renderAuswahlFür(karte, bereich, zustand.knotenAnhänge)
    } ?: RenderAuswahl.alle(karte)
    if (neueRenderAuswahl != renderAuswahl) renderAuswahl = neueRenderAuswahl
    val sichtbareKnoten = neueRenderAuswahl.knoten
    val sichtbareVerbindungen = neueRenderAuswahl.verbindungen

    Box(
        modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .clipToBounds()
            .onSizeChanged { anzeigeGröße = it }
            .onGloballyPositioned { editorUrsprungImRoot = it.positionInRoot() }
            .onPreviewKeyEvent { event ->
                if (interaktionGesperrt) return@onPreviewKeyEvent true
                umschaltGedrückt = event.isShiftPressed
                altGedrückt = event.isAltPressed
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
                        if (event.type == PointerEventType.Press || event.type == PointerEventType.Scroll) aktuelleAnschlussInfos.schließenHover()
                        if (aktuellGesperrt) { event.changes.forEach { it.consume() }; continue }
                        val änderung = event.changes.firstOrNull() ?: continue
                        if (event.type == PointerEventType.Press) altGedrückt = event.keyboardModifiers.isAltPressed
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
                if (!aktuellGesperrt && !trifftKnoten(pos, aktuelleKarte, aktuelleAnsicht, aktuelleDichte, zustand.ausgewählteDarstellungsKnoten)) {
                    zustand.wähleKnoten(null)
                    beiHintergrundKontext(bildschirmZuWelt(pos, aktuelleAnsicht, aktuelleDichte))
                }
            }
            .pointerInput(zustand) {
                awaitEachGesture {
                    var gesamterPan = Offset.Zero
                    var gesamterZoom = 1f
                    var hintergrundGesteAktiv = false
                    // awaitFirstDown akzeptiert auf Desktop nur die primäre Maustaste.
                    // Die Kartennavigation benötigt auch den Druck der mittleren Taste.
                    var startEreignis: PointerEvent
                    do {
                        startEreignis = awaitPointerEvent()
                    } while (startEreignis.changes.none { it.changedToDownIgnoreConsumed() })
                    val ersterDruck = startEreignis.changes.first { it.changedToDownIgnoreConsumed() }
                    // Der parallele Tap-Handler konsumiert bereits den Erstkontakt.
                    // Erst konsumierte Bewegungen schließen die Kamerageste aus.
                    if (aktuellGesperrt) return@awaitEachGesture
                    val maus = ersterDruck.type == PointerType.Mouse
                    val mittlereTaste = maus && currentEvent.buttons.isTertiaryPressed
                    if (maus && !mittlereTaste && !currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
                    val auswahlGeste = maus && currentEvent.buttons.isPrimaryPressed && !leertasteGedrückt
                    if (trifftKnoten(ersterDruck.position, aktuelleKarte, aktuelleAnsicht, aktuelleDichte, zustand.ausgewählteDarstellungsKnoten) && !mittlereTaste) {
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
            .pointerInput(karte.id, ansicht, interaktionGesperrt) {
                if (interaktionGesperrt) return@pointerInput
                detectTapGestures(
                    onTap = { punkt ->
                        if (!trifftKnoten(punkt, aktuelleKarte, aktuelleAnsicht, aktuelleDichte, zustand.ausgewählteDarstellungsKnoten)) zustand.wähleKnoten(null)
                    },
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
        GruppenÜbergangsRahmen(gruppenÜbergang, zustand.projektion, ansicht)
        GruppenNavigation(zustand)
        GruppenInspektor(zustand)
        if (zustand.projektionWirdBerechnet) LinearProgressIndicator(Modifier.align(Alignment.TopCenter).fillMaxWidth().zIndex(4f))
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
                    beiVerbindungKontext = { v -> zustand.karte.verbindungen.firstOrNull { it.id == v.id }?.let(beiVerbindungKontext) },
                    gruppenÜbergang = gruppenÜbergang,
                )
            }
            GruppenRahmen(zustand, auswertungsDauerFür)
            KnotenMeldungsEbene(sichtbareKnoten, zustand, ansicht, dichte.density, gruppenÜbergang, vordergrund = false)
            GruppenÜbergangsInhalt(gruppenÜbergang, karte, sichtbarerWeltBereich, rendererFür)
            for (neueEbene in listOf(false,true)) {
                val inhalt = sichtbareKnoten.filter {
                    val neu = gruppenÜbergang.vorher != null && it.id !in gruppenÜbergang.vorherigeKnotenIds
                    neu == neueEbene
                }
                Box(Modifier.fillMaxSize().graphicsLayer {
                    if (neueEbene) alpha = gruppenÜbergang.fortschritt.value
                }) {
                    inhalt.forEach { knoten ->
                        key(knoten.id) {
                            KnotenDarstellung(
                                knoten = knoten,
                                ausgewählt = knoten.id in zustand.ausgewählteKnoten || zustand.projektion.blöcke[knoten.id] == zustand.ausgewählteGruppe && zustand.ausgewählteGruppe != null,
                                zustand = zustand,
                                ansicht = ansicht,
                                dichte = dichte.density,
                                renderer = if (knoten.art in setOf(GRUPPEN_BLOCK_ART, GRUPPEN_EINGANG_ART, GRUPPEN_AUSGANG_ART)) GruppenKnotenRenderer else rendererFür(knoten),
                                farbeFürAnschluss = farbeFürAnschluss,
                                magnetischesZiel = magnetischesZiel,
                                beiMagnetischemZiel = { magnetischesZiel = it },
                                beiKnotenKontext = beiKnotenKontext,
                                beiAnschlussKontext = beiAnschlussKontext,
                                beiVerbindungAufHintergrund = beiVerbindungAufHintergrund,
                                beiVerbindungsEinfügeAnfrage = beiVerbindungsEinfügeAnfrage?.let { callback -> { kontext, punkt ->
                                    if (sichtbarerWeltBereich?.contains(Offset(punkt.x, punkt.y)) != false) callback(kontext, punkt)
                                } },
                                zeigeMeldung = false,
                                beiDoppelklick = {
                                    val gruppe = zustand.projektion.blöcke[knoten.id]
                                    if (gruppe != null) zustand.öffneGruppe(gruppe) else if (knoten.id !in zustand.projektion.portale) beiKnotenDoppelklick(knoten)
                                },
                                auswahlÄnderung = aktuelleAuswahlÄnderung,
                                beimZiehenLösen = { altGedrückt },
                                zeigeKnotenInspektor = zeigeKnotenInspektor,
                                beiInspektorÖffnen = {
                                    val gruppe = zustand.projektion.blöcke[knoten.id]
                                    if (gruppe != null) zustand.wähleGruppe(gruppe, inspektor = true) else {
                                        val original = knoten.anschlüsse.firstOrNull()?.let { zustand.projektion.original(AnschlussVerweis(knoten.id,it.id)).knotenId }
                                        val daten = zustand.karte.knoten.firstOrNull { it.id == original } ?: knoten
                                        zustand.wähleKnoten(daten.id)
                                        beiKnotenInspektor(daten)
                                    }
                                },
                                editorUrsprungImRoot = editorUrsprungImRoot,
                                sichtbarerBereich = sichtbarerWeltBereich,
                                anschlussInfos = anschlussInfos,
                            )
                        }
                    }
                }
            }
            KnotenMeldungsEbene(sichtbareKnoten, zustand, ansicht, dichte.density, gruppenÜbergang, vordergrund = true)
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
        zustand.ausgewählterKnoten?.let { id ->
            if (zustand.karte.verbindungen.any { it.von.knotenId == id || it.zu.knotenId == id }) {
                OutlinedButton(
                    onClick = { zustand.merkeKnotenZumHerausziehen(id) },
                    modifier = Modifier.align(Alignment.TopCenter).padding(8.dp),
                ) { Text("Aus Verbindung herausziehen") }
            }
        }
        if (interaktionGesperrt) Box(Modifier.matchParentSize().zIndex(100f).pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false).consume()
                do {
                    val event = awaitPointerEvent()
                    event.changes.forEach { it.consume() }
                } while (event.changes.any { it.pressed })
            }
        })
        AnschlussInfoFenster(zustand, anschlussInfos, interaktionGesperrt, anschlussInfoFür)
    }
    VerbindungsAuswahlDialog(zustand)
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
    val inhalt = remember(karte.knoten) { karte.inhaltsGrenzen(puffer = 80f) }
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

/**
 * Eine Auswahl bleibt über kleine Kamerabewegungen stabil. Erst außerhalb der
 * Sicherheitszone wird sie anhand eines größeren Weltfensters neu berechnet.
 */
internal data class RenderAuswahl(
    val fenster: Rect?,
    val sicherheitsZone: Rect?,
    val knoten: List<KnotenDaten>,
    val verbindungen: List<VerbindungDaten>,
) {
    fun deckt(bereich: Rect): Boolean = sicherheitsZone?.enthält(bereich) == true

    companion object {
        fun alle(karte: KartenDaten) = RenderAuswahl(null, null, karte.knoten, karte.verbindungen)
    }
}

internal fun renderAuswahlFür(
    karte: KartenDaten, sichtbar: Rect, anhänge: Map<KnotenId, KnotenAnhangMaße> = emptyMap(),
): RenderAuswahl {
    val fenster = sichtbar.erweitert(RENDER_OVERSCAN)
    val sicherheitsZone = sichtbar.erweitert(RENDER_HYSTERESE)
    val positionen = karte.knoten.flatMap { k -> k.anschlüsse.map { a -> AnschlussVerweis(k.id,a.id) to anschlussPositionWelt(k,a) } }.toMap()
    return RenderAuswahl(
        fenster = fenster,
        sicherheitsZone = sicherheitsZone,
        knoten = karte.knoten.filter { it.istImBereich(fenster, KNOTEN_VIEWPORT_PUFFER, anhänge[it.id] ?: KnotenAnhangMaße()) },
        verbindungen = karte.verbindungen.filter { v ->
            val a = positionen[v.von]; val b = positionen[v.zu]
            a != null && b != null && berechneVerbindungsGeometrie(Offset(a.x,a.y),Offset(b.x,b.y),72f)
                .umhüllung.erweitert(VERBINDUNG_VIEWPORT_PUFFER).überschneidet(fenster)
        },
    )
}

private fun Rect.enthält(anderer: Rect): Boolean =
    anderer.left >= left && anderer.top >= top && anderer.right <= right && anderer.bottom <= bottom

private const val RENDER_OVERSCAN = 540f
private const val RENDER_HYSTERESE = 270f

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
    gruppenÜbergang: GruppenÜbergang,
) {
    val dichte = LocalDensity.current
    val standard = MaterialTheme.colorScheme.outline
    val gewählt = MaterialTheme.colorScheme.primary
    var schwebendeVerbindung by remember { mutableStateOf<VerbindungsId?>(null) }
    val geometrien = remember(verbindungen, karte.knoten, ansicht, dichte.density) {
        val anschlussPositionen = karte.knoten.flatMap { k -> k.anschlüsse.map { a -> AnschlussVerweis(k.id, a.id) to anschlussPositionWelt(k, a) } }.toMap()
        verbindungen.mapIndexedNotNull { index, verbindung ->
            val start = anschlussPositionen[verbindung.von] ?: return@mapIndexedNotNull null
            val ende = anschlussPositionen[verbindung.zu] ?: return@mapIndexedNotNull null
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
    val übergangsEndpunkte = remember(verbindungen, karte.knoten, gruppenÜbergang) {
        val positionen = karte.knoten.flatMap { k -> k.anschlüsse.map { a -> AnschlussVerweis(k.id,a.id) to anschlussPositionWelt(k,a) } }.toMap()
        verbindungen.associate { v ->
            val von = positionen[v.von]
            val zu = positionen[v.zu]
            v.id to listOf(gruppenÜbergang.position(zustand.projektion.original(v.von)) ?: von, gruppenÜbergang.position(zustand.projektion.original(v.zu)) ?: zu, von, zu)
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
            val istAusgewählt = zustand.ausgewählteVerbindung == verbindung.id || zustand.einfügeVerbindungsZiel == verbindung.id
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
            val punkte = übergangsEndpunkte[verbindung.id]
            val t = gruppenÜbergang.fortschritt.value
            val pfad = if (t < 1f && punkte != null && punkte.all { it != null }) {
                fun punkt(i: Int, j: Int): Offset {
                    val a = punkte[i]!!; val b = punkte[j]!!
                    return weltZuBildschirm(GraphPunkt(a.x+(b.x-a.x)*t,a.y+(b.y-a.y)*t),dichte.density,ansicht)
                }
                berechneVerbindungsGeometrie(punkt(0,2),punkt(1,3),72f*dichte.density*ansicht.zoom).pfad
            } else eintrag.geometrie.pfad
            drawPath(
                pfad,
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
        val startRef = zustand.verbindungsStart?.let(zustand.projektion::sichtbar)
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
private fun KnotenMeldungsEbene(knoten: List<KnotenDaten>, zustand: KartenEditorZustand,
    ansicht: AnsichtsFenster, dichte: Float, übergang: GruppenÜbergang, vordergrund: Boolean) {
    val gewählt = zustand.ausgewählteDarstellungsKnoten
    knoten.filter { (it.id in gewählt) == vordergrund }
        .sortedBy { it.id == zustand.ausgewählterKnoten }.forEach { k -> key(k.id) {
            val p = weltZuBildschirm(k.position, dichte, ansicht)
            Box(Modifier.offset { IntOffset(p.x.roundToInt(), p.y.roundToInt()) }
                .graphicsLayer {
                    scaleX = ansicht.zoom; scaleY = ansicht.zoom; transformOrigin = TransformOrigin(0f, 0f)
                    if (übergang.vorher != null && k.id !in übergang.vorherigeKnotenIds) alpha = übergang.fortschritt.value
                }.wrapContentSize(Alignment.TopStart, unbounded = true).requiredSize(k.größe.breite.dp, k.größe.höhe.dp)) {
                KnotenMeldungsAnhang(k, zustand)
            }
        } }
}

@Composable
private fun BoxScope.KnotenMeldungsAnhang(knoten: KnotenDaten, zustand: KartenEditorZustand) {
    val meldungen = zustand.meldungenFür(knoten.id)
    val lokaleAnhangDichte = LocalDensity.current.density
    val (kopfLinks, kopfRechts) = berechneAnhangEinzüge(knoten.größe.breite.dp, knoten.größe.höhe.dp,
        knotenKörperForm(knoten), LocalDensity.current, androidx.compose.ui.platform.LocalLayoutDirection.current, oben = true)
        if (meldungen.isNotEmpty()) {
            Layout(
                content = {
                    Box(Modifier.onSizeChanged {
                        zustand.meldeKnotenAnhangHöhe(knoten.id, oben = true, höhe = it.height / lokaleAnhangDichte)
                    }) { KnotenMeldungsKopfzeile(meldungen) }
                },
                modifier = Modifier.align(Alignment.TopStart).offset(x = kopfLinks)
                    .width((knoten.größe.breite.dp - kopfLinks - kopfRechts).coerceAtLeast(0.dp)),
            ) { messbare, constraints ->
                val kopf = messbare.single().measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
                layout(kopf.width, 0) { kopf.placeRelative(0, -kopf.height) }
            }
        }

}

@Composable
private fun KnotenDarstellung(
    modifier: Modifier = Modifier,
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
    beimZiehenLösen: () -> Boolean,
    zeigeKnotenInspektor: Boolean,
    beiInspektorÖffnen: () -> Unit,
    editorUrsprungImRoot: Offset,
    sichtbarerBereich: Rect?,
    anschlussInfos: AnschlussInfoInteraktion? = null,
    beiVerbindungsEinfügeAnfrage: ((VerbindungsEinfügeKontext, GraphPunkt) -> Unit)? = null,
    zeigeMeldung: Boolean = true,
) {
    val zoom = zustand.darstellungsKarte.ansicht.zoom
    val fußzeilenHöhe = with(LocalDensity.current) { MaterialTheme.typography.labelSmall.lineHeight.toDp() }
        .plus(16.dp).coerceAtLeast(18.dp)
    var viewportMinimum by remember(knoten.id) { mutableStateOf(GraphGröße(0f, 0f)) }
    var ziehbar by remember(knoten.id) { mutableStateOf(false) }
    val aktuellLösen by rememberUpdatedState(beimZiehenLösen)
    val hauptkörperHöhe = knoten.größe.höhe.dp
    val knotenForm = knotenKörperForm(knoten)
    val inhaltsRahmen = knoten.inhaltsRahmen(renderer.misstNatürlichenInhalt)
    val hintergrund = (knoten.eigenschaften[KNOTEN_HINTERGRUND_EIGENSCHAFT] as? KnotenEigenschaft.Farbe)?.argb?.let { Color(it) }
        ?: MaterialTheme.colorScheme.surfaceContainerLow
    val rahmen = (knoten.eigenschaften[KNOTEN_RAHMEN_EIGENSCHAFT] as? KnotenEigenschaft.Farbe)?.argb?.let { Color(it) }
        ?: MaterialTheme.colorScheme.outlineVariant
    // Die Oberkante der Fußzeile liegt exakt auf der Unterkante des Hauptkörpers.
    // Ihre Ecken liegen auf dessen beiden unteren Übergängen zur Rundung.
    val fußzeilenGeometrie = berechneZeitfußzeilenGeometrie(
        knoten.größe.breite.dp, knoten.größe.höhe.dp,
        knotenForm, LocalDensity.current, androidx.compose.ui.platform.LocalLayoutDirection.current,
    )
    val lokaleAnhangDichte = LocalDensity.current.density
    val bildschirmPosition = weltZuBildschirm(knoten.position, dichte, ansicht)
    Box(
        modifier.offset { IntOffset(bildschirmPosition.x.roundToInt(), bildschirmPosition.y.roundToInt()) }
            .graphicsLayer {
                scaleX = ansicht.zoom
                scaleY = ansicht.zoom
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .wrapContentSize(Alignment.TopStart, unbounded = true)
            .requiredSize(knoten.größe.breite.dp, knoten.größe.höhe.dp)
    ) {
        Card(
            Modifier.fillMaxWidth().height(hauptkörperHöhe)
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
                .pointerInput(zustand, knoten.id, zoom, knoten.größe, knoten.darstellungsForm) {
                    detectDragGestures(
                        orientationLock = null,
                        onDragStart = { down, _, _ ->
                            val start = down.position
                            ziehbar = knoten.enthältRelativenPunkt(GraphPunkt(start.x / density, start.y / density)) && (renderer.interaktionsModus == KnotenInteraktionsModus.GanzeFlächeZiehbar || start.y <= KOPFZEILE_HÖHE_DP * density)
                            if (knoten.id in zustand.projektion.portale) ziehbar = false
                            if (ziehbar) {
                                if (knoten.id !in zustand.ausgewählteKnoten) zustand.wähleKnoten(knoten.id, auswahlÄnderung)
                                zustand.beginneKnotenZiehen(knoten.id, aktuellLösen())
                            }
                        },
                        onDragEnd = { _ -> if (ziehbar) zustand.beendeKnotenZiehen(); ziehbar = false },
                        onDragCancel = { if (ziehbar) zustand.verwerfeKnotenZiehen(); ziehbar = false },
                        onDrag = { änderung, delta ->
                            if (!ziehbar) return@detectDragGestures
                            änderung.consume()
                            val gruppe = zustand.projektion.blöcke[knoten.id]
                            if (gruppe != null) {
                                zustand.führeAus(KartenAktion.VisuelleGruppeVerschieben(gruppe, GraphPunkt(delta.x / density, delta.y / density)), mitHistorie = false)
                            }
                            val aktuell = zustand.karte.knoten.firstOrNull { it.id == knoten.id }
                            if (aktuell != null) {
                                // Lokale Layerkoordinaten sind bereits entzoomt.
                                zustand.aktualisiereKnotenZiehen(aktuell.position + GraphPunkt(delta.x / density, delta.y / density))
                            }
                        },
                    )
                }
                .sekundärKlick(zustand, listOf(knoten.id, knoten.größe, knoten.darstellungsForm),
                    treffer = { punkt -> knoten.enthältRelativenPunkt(GraphPunkt(punkt.x / dichte, punkt.y / dichte)) }) {
                    if (knoten.id !in zustand.ausgewählteKnoten) zustand.wähleKnoten(knoten.id)
                    val gruppe = zustand.projektion.blöcke[knoten.id]
                    if (gruppe != null) zustand.wähleGruppe(gruppe, true) else if (knoten.id !in zustand.projektion.portale) beiKnotenKontext(knoten)
                }
                .pointerInput(zustand, knoten.id, knoten.größe, knoten.darstellungsForm) {
                    detectTapGestures(
                        onTap = { punkt ->
                            if (knoten.enthältRelativenPunkt(GraphPunkt(punkt.x / density, punkt.y / density)))
                                zustand.wähleKnoten(knoten.id, auswahlÄnderung)
                        },
                        onLongPress = { punkt ->
                            if (!knoten.enthältRelativenPunkt(GraphPunkt(punkt.x / density, punkt.y / density))) return@detectTapGestures
                            zustand.wähleKnoten(knoten.id)
                            val gruppe = zustand.projektion.blöcke[knoten.id]
                            if (gruppe != null) zustand.wähleGruppe(gruppe, true) else if (knoten.id !in zustand.projektion.portale) beiKnotenKontext(knoten)
                        },
                        onDoubleTap = { punkt ->
                            if (knoten.enthältRelativenPunkt(GraphPunkt(punkt.x / density, punkt.y / density))) beiDoppelklick()
                        },
                    )
                },
            shape = knotenForm,
            border = BorderStroke(if (ausgewählt) 3.dp else 1.dp,
                if (ausgewählt) kontrastAdaptiveProfilFarbe(MaterialTheme.colorScheme.primary, hintergrund) else rahmen),
            elevation = CardDefaults.cardElevation(if (ausgewählt) 8.dp else 2.dp),
            colors = CardDefaults.cardColors(containerColor = hintergrund, contentColor =
                if ((knoten.eigenschaften[KNOTEN_HINTERGRUND_EIGENSCHAFT] as? KnotenEigenschaft.Farbe) == null) MaterialTheme.colorScheme.onSurface
                else if (hintergrund.luminance() > .179f) Color.Black else Color.White),
        ) {
            val minimum = renderer.Mindestgröße(knoten)
            val kantenMinimum = GraphGröße(
                maxOf(minimum.breite, 48f + 30f * knoten.anschlüsse.count { it.kante == AnschlussKante.Oben },
                    48f + 30f * knoten.anschlüsse.count { it.kante == AnschlussKante.Unten }),
                maxOf(minimum.höhe, 48f + 30f * knoten.anschlüsse.count { it.kante == AnschlussKante.Links },
                    48f + 30f * knoten.anschlüsse.count { it.kante == AnschlussKante.Rechts }),
            )
            val lokaleDichte = LocalDensity.current
            val kapsel = knoten.darstellungsForm == KnotenDarstellungsForm.Adapter && renderer.misstNatürlichenInhalt
            var gemessen by remember(knoten.id, lokaleDichte) { mutableStateOf(IntSize.Zero) }
            val breite = if (renderer.misstNatürlichenInhalt) gemessen.width / lokaleDichte.density else minimum.breite
            val höhe = if (renderer.misstNatürlichenInhalt) gemessen.height / lokaleDichte.density else minimum.höhe
            SideEffect {
                zustand.meldeInhaltsminimum(knoten.id, GraphGröße(
                    ceil(maxOf(kantenMinimum.breite, viewportMinimum.breite, breite) / inhaltsRahmen.breite),
                    ceil(maxOf(kantenMinimum.höhe, höhe, viewportMinimum.höhe) / inhaltsRahmen.höhe),
                ))
            }
            Layout(content = {
                Box(Modifier.onSizeChanged { gemessen = it }) {
                    renderer.Inhalt(knoten, ausgewählt, object : KnotenRendererAktionen {
                        override fun meldungenMelden(meldungen: List<KnotenMeldung>) {
                            zustand.meldeKnotenMeldungen(knoten.id, meldungen)
                        }
                        override fun eigenschaftenErsetzen(eigenschaften: Map<String, KnotenEigenschaft>) {
                            zustand.führeAus(KartenAktion.KnotenEigenschaftenErsetzen(knoten.id, eigenschaften))
                        }
                        override fun inhaltsminimumMelden(minimum: GraphGröße) {
                            if (minimum.breite.isFinite() && minimum.höhe.isFinite() && minimum.breite > 0f && minimum.höhe > 0f)
                                viewportMinimum = minimum
                        }
                    })
                }
            }, modifier = Modifier.fillMaxSize()) { messbare, constraints ->
                val messConstraints = if (renderer.misstNatürlichenInhalt) Constraints() else Constraints(
                    maxWidth = (constraints.maxWidth * inhaltsRahmen.breite).toInt(),
                    maxHeight = (constraints.maxHeight * inhaltsRahmen.höhe).toInt(),
                )
                val inhalt = messbare.single().measure(messConstraints)
                layout(constraints.maxWidth, constraints.maxHeight) {
                    inhalt.placeRelative(
                        if (kapsel) ((constraints.maxWidth - inhalt.width) / 2).coerceAtLeast(0) else (constraints.maxWidth * inhaltsRahmen.x).toInt(),
                        if (kapsel) ((constraints.maxHeight - inhalt.height) / 2).coerceAtLeast(0) else (constraints.maxHeight * inhaltsRahmen.y).toInt())
                }
            }
        }

        // Anhänge liegen im selben Transformationslayer, außerhalb der Körpergröße.
        Box(
            Modifier.align(Alignment.TopStart)
                .offset(x = fußzeilenGeometrie.links, y = hauptkörperHöhe)
                .width(fußzeilenGeometrie.breite)
                .heightIn(max = fußzeilenHöhe)
                .onSizeChanged { zustand.meldeKnotenAnhangHöhe(knoten.id, oben = false, höhe = it.height / lokaleAnhangDichte) },
            contentAlignment = Alignment.CenterStart,
        ) {
            renderer.Fußzeile(knoten, ausgewählt)
        }
        if (zeigeMeldung) KnotenMeldungsAnhang(knoten, zustand)

        if (zeigeKnotenInspektor) {
            KnotenInspektorSchaltfläche(
                hintergrund = hintergrund,
                beiKlick = {
                    zustand.wähleKnoten(knoten.id)
                    beiInspektorÖffnen()
                },
                modifier = Modifier.align(Alignment.TopEnd).then(
                    if (knoten.darstellungsForm == KnotenDarstellungsForm.Adapter)
                        Modifier.offset(x = (-12).dp, y = 8.dp)
                    else Modifier
                ),
            )
        }

        knoten.anschlüsse.filter { it.sammelDarstellung && it.sammelAnschluss != null }
            .groupBy { it.sammelAnschluss }.values.forEach { gruppe ->
                val punkte = gruppe.map { knoten.anschlussPositionRelativ(it) }
                val oben = punkte.minOf { it.y }
                val unten = punkte.maxOf { it.y }
                val breite = ANSCHLUSS_SICHTBAR_GRÖSSE_DP / zoom + 6f / zoom
                Box(Modifier.offset((punkte.first().x - breite / 2f).dp, (oben - breite / 2f).dp)
                    .width(breite.dp).height((unten - oben + breite).dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, androidx.compose.foundation.shape.RoundedCornerShape(50))
                    .border((2f / zoom).dp, MaterialTheme.colorScheme.outline, androidx.compose.foundation.shape.RoundedCornerShape(50)))
            }

        knoten.anschlüsse.groupBy { it.kante }.forEach { (_, anschlüsse) ->
            anschlüsse.sortedBy { it.reihenfolge }.forEachIndexed { index, anschluss ->
                val welt = knoten.position + knoten.anschlussPositionRelativ(anschluss)
                val puffer = ANSCHLUSS_TREFFER_GRÖSSE_DP / zoom
                if (sichtbarerBereich != null && !sichtbarerBereich.erweitert(puffer).enthält(Offset(welt.x,welt.y))) return@forEachIndexed
                val farbArten = if (anschluss.zulässigeArten.isNotEmpty()) {
                    anschluss.zulässigeArten.sortedBy { it.wert }
                } else {
                    listOf(anschluss.art)
                }
                val farben = farbArten.map { art ->
                    farbeFürAnschluss(anschluss.copy(art = art, zulässigeArten = emptySet()))
                }
                AnschlussGriff(
                    knoten = knoten,
                    anschluss = zustand.karte.symbolAnschluss(knoten.id, anschluss),
                    index = index,
                    anzahl = anschlüsse.size,
                    zustand = zustand,
                    farben = farben,
                    farbeFürAnschluss = farbeFürAnschluss,
                    magnetischesZiel = magnetischesZiel,
                    beiMagnetischemZiel = beiMagnetischemZiel,
                    beiAnschlussKontext = beiAnschlussKontext,
                    beiVerbindungAufHintergrund = beiVerbindungAufHintergrund,
                    beiVerbindungsEinfügeAnfrage = beiVerbindungsEinfügeAnfrage,
                    editorUrsprungImRoot = editorUrsprungImRoot,
                    anschlussInfos = anschlussInfos,
                )
            }
        }

        if (ausgewählt) {
            Box(
                Modifier.align(Alignment.BottomEnd).offset(6.dp, 6.dp).size(18.dp)
                    .semantics { contentDescription = "Knoten ${knoten.name} vergrößern" }
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
    hintergrund: Color = MaterialTheme.colorScheme.surfaceContainerLow,
) {
    val iconFarbe = kontrastAdaptiveProfilFarbe(
        profilFarbe = MaterialTheme.colorScheme.primary,
        hintergrund = hintergrund,
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

internal fun bildschirmZuWelt(position: Offset, ansicht: AnsichtsFenster, dichte: Float): GraphPunkt = GraphPunkt(
    (position.x - ansicht.verschiebung.x) / ansicht.zoom / dichte.coerceAtLeast(.0001f),
    (position.y - ansicht.verschiebung.y) / ansicht.zoom / dichte.coerceAtLeast(.0001f),
)

private fun Modifier.sekundärKlick(zustand: KartenEditorZustand, key: Any?, treffer: (Offset) -> Boolean = { true }, aktion: (Offset) -> Unit): Modifier =
    pointerInput(zustand, key) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val down = event.changes.firstOrNull() ?: continue
                // Sekundärklicks explizit am Press-Ereignis erkennen und nur
                // innerhalb des aktuellen Trefferbereichs konsumieren.
                if (event.type != PointerEventType.Press || down.type != PointerType.Mouse ||
                    !event.buttons.isSecondaryPressed || down.isConsumed || !treffer(down.position)) continue
                down.consume()
                aktion(down.position)
                do {
                    val ereignis = awaitPointerEvent()
                    ereignis.changes.forEach { it.consume() }
                } while (ereignis.changes.any { it.pressed })
            }
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
    farben: List<Color>,
    farbeFürAnschluss: @Composable (AnschlussDaten) -> Color,
    magnetischesZiel: AnschlussVerweis?,
    beiMagnetischemZiel: (AnschlussVerweis?) -> Unit,
    beiAnschlussKontext: (AnschlussVerweis) -> Unit,
    beiVerbindungAufHintergrund: (AnschlussVerweis, GraphPunkt) -> Unit,
    editorUrsprungImRoot: Offset,
    anschlussInfos: AnschlussInfoInteraktion?,
    beiVerbindungsEinfügeAnfrage: ((VerbindungsEinfügeKontext, GraphPunkt) -> Unit)?,
) {
    val anteil = (index + 1f) / (anzahl + 1f)
    val zoom = zustand.darstellungsKarte.ansicht.zoom.coerceAtLeast(.0001f)
    val interaktionsGröße = ANSCHLUSS_TREFFER_GRÖSSE_DP / zoom
    val sichtbareGröße = ANSCHLUSS_SICHTBAR_GRÖSSE_DP / zoom
    val interaktionsHalbe = interaktionsGröße / 2f
    val ausrichtung = Alignment.TopStart
    val relativ = knoten.anschlussPositionRelativ(anschluss)
    val x = (relativ.x - interaktionsHalbe).dp
    val y = (relativ.y - interaktionsHalbe).dp
    val ref = AnschlussVerweis(knoten.id, anschluss.id)
    val kompatibel = zustand.kompatibelMitStart(ref)
    val eingerastet = magnetischesZiel == ref
    val aktuellesZielSetzen by rememberUpdatedState(beiMagnetischemZiel)
    val aktuelleInfos by rememberUpdatedState(anschlussInfos)
    val aktuellerEditorUrsprung by rememberUpdatedState(editorUrsprungImRoot)
    var griffKoordinaten by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var zugPosition by remember(knoten.id, anschluss.id) { mutableStateOf<GraphPunkt?>(null) }
    var zugZiel by remember(knoten.id, anschluss.id) { mutableStateOf<AnschlussVerweis?>(null) }
    var pieBewaffnet by remember(knoten.id, anschluss.id) { mutableStateOf(false) }
    val aktuelleEinfügeAnfrage by rememberUpdatedState(beiVerbindungsEinfügeAnfrage)
    val startWelt = anschlussPositionWelt(knoten, anschluss)
    val interaktionsObenLinks = startWelt - GraphPunkt(interaktionsHalbe, interaktionsHalbe)
    val typenText = if (anschluss.zulässigeArten.isNotEmpty()) {
        anschluss.zulässigeArten.sortedBy { it.wert }.joinToString(" oder ") { it.wert }
    } else {
        anschluss.art.wert
    }
    val signaturHinweis = if (
        zustand.verbindungsStart != null &&
        zustand.verbindungsStart != ref &&
        kompatibel
    ) {
        VerbindungsDragZielHinweis.textFür(zustand, zustand.projektion.original(ref))
    } else null

    Box(
        Modifier.align(ausrichtung).offset(x, y).size(interaktionsGröße.dp)
            .onGloballyPositioned { griffKoordinaten = it }
            .pointerHoverIcon(PointerIcon.Crosshair)
            .semantics {
                contentDescription = "${knoten.name}, Anschluss ${anschluss.name}, ${anschluss.richtung}, zulässige Typen $typenText"
            }
            .sekundärKlick(zustand, ref) {
                aktuellesZielSetzen(null)
                zustand.brecheVerbindungsVorschauAb()
                beiAnschlussKontext(zustand.projektion.original(ref))
            }
            .pointerInput(ref, anschlussInfos) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val zeiger = event.changes.firstOrNull()
                        when (event.type) {
                            PointerEventType.Enter, PointerEventType.Move -> {
                                if (zeiger != null && !zeiger.pressed &&
                                    (zeiger.type == PointerType.Mouse || zeiger.type == PointerType.Stylus) &&
                                    zustand.verbindungsStart == null && aktuelleInfos?.dialog == null) {
                                    val anker = griffKoordinaten?.let {
                                        it.localToRoot(Offset(it.size.width / 2f, it.size.height / 2f)) - aktuellerEditorUrsprung
                                    }
                                    if (anker != null) aktuelleInfos?.hover = AnschlussHover(zustand.infoZiel(ref), anker)
                                } else aktuelleInfos?.schließenHover()
                            }
                            PointerEventType.Exit -> {
                                if (aktuelleInfos?.hover?.ziel == zustand.infoZiel(ref)) aktuelleInfos?.schließenHover()
                            }
                            PointerEventType.Press, PointerEventType.Scroll -> aktuelleInfos?.schließenHover()
                        }
                    }
                }
            }
            .combinedClickable(
                onClick = { aktuelleInfos?.öffnen(zustand.infoZiel(ref)) },
                onLongClick = {
                    aktuelleInfos?.schließenHover()
                    aktuellesZielSetzen(null)
                    zustand.brecheVerbindungsVorschauAb()
                    beiAnschlussKontext(zustand.projektion.original(ref))
                },
            )
            .pointerInput(ref) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    pieBewaffnet = false
                    do {
                        val event = currentEvent
                        if (down.type == PointerType.Stylus &&
                            (event.buttons.isPrimaryPressed || event.buttons.isSecondaryPressed)) pieBewaffnet = true
                        if (event.changes.none { it.pressed }) break
                        awaitPointerEvent(PointerEventPass.Initial)
                    } while (true)
                }
            }
            .pointerInput(ref, kompatibel, zoom, editorUrsprungImRoot) {
                if (!kompatibel) return@pointerInput
                detectDragGestures(
                    onDragStart = {
                        aktuelleInfos?.schließenHover()
                        zugPosition = startWelt
                        zugZiel = null
                        aktuellesZielSetzen(null)
                        zustand.beginneVerbindung(ref, startWelt)
                    },
                    onDrag = { änderung, _ ->
                        änderung.consume()
                        val zeigerWelt = griffKoordinaten?.localToRoot(änderung.position)?.let { imRoot ->
                            bildschirmZuWelt(imRoot - editorUrsprungImRoot, zustand.darstellungsKarte.ansicht, density)
                        } ?: (interaktionsObenLinks + GraphPunkt(
                            änderung.position.x / density,
                            änderung.position.y / density,
                        ))
                        zugPosition = zeigerWelt
                        val ziel = nächsterKompatiblerAnschluss(
                            zustand = zustand,
                            start = ref,
                            ende = zeigerWelt,
                            bisherigesZiel = zugZiel,
                        )
                        zugZiel = ziel
                        aktuellesZielSetzen(ziel)
                        val vorschau = ziel?.let { anschlussPositionWelt(zustand.darstellungsKarte, it) } ?: zeigerWelt
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
                            val anfrage = aktuelleEinfügeAnfrage
                            val kontext = if (pieBewaffnet && anfrage != null) zustand.übergibVerbindungZurEinfügung() else null
                            if (kontext != null) anfrage?.invoke(kontext, ende)
                            val ablage = if (kontext == null) zustand.beendeVerbindungsVorschau(startBeibehalten = false)
                                else VerbindungsAblageErgebnis.StartBeibehalten
                            if (ablage == VerbindungsAblageErgebnis.NeueVerbindungAufHintergrund) {
                                beiVerbindungAufHintergrund(zustand.projektion.original(ref), ende)
                            }
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
        signaturHinweis?.let { text ->
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-34f / zoom).dp)
                    .zIndex(20f),
                shape = MaterialTheme.shapes.small,
                tonalElevation = 6.dp,
                shadowElevation = 4.dp,
                border = BorderStroke((1f / zoom).dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Text(
                    text = text,
                    modifier = Modifier.padding(horizontal = (8f / zoom).dp, vertical = (4f / zoom).dp),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
        AnschlussSymbol(
            anschluss = anschluss,
            fallbackFarben = farben,
            größe = sichtbareGröße,
            zoom = zoom,
            aktiviert = kompatibel,
            farbeFürAnschluss = farbeFürAnschluss,
        )
        if (eingerastet) {
            Box(
                Modifier.size((24f / zoom).dp)
                    .border((3f / zoom).dp, MaterialTheme.colorScheme.tertiary, CircleShape)
            )
        }
    }
}

@Composable
private fun MehrfarbenAnschluss(
    farben: List<Color>,
    größe: Float,
    zoom: Float,
) {
    val sichereFarben = farben.ifEmpty { listOf(MaterialTheme.colorScheme.primary) }
    Box(Modifier.size(größe.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().clip(CircleShape)) {
            if (sichereFarben.size == 1) {
                drawRect(sichereFarben.single())
            } else {
                val streifenBreite = (size.minDimension / sichereFarben.size.coerceAtLeast(2)).coerceAtLeast(2f)
                rotate(-45f) {
                    var x = -size.width * 1.5f
                    var index = 0
                    while (x < size.width * 2.5f) {
                        drawRect(
                            color = sichereFarben[index % sichereFarben.size],
                            topLeft = Offset(x, -size.height),
                            size = Size(streifenBreite, size.height * 3f),
                        )
                        x += streifenBreite
                        index += 1
                    }
                }
            }
        }
        Box(
            Modifier.fillMaxSize()
                .border((2f / zoom).dp, MaterialTheme.colorScheme.surface, CircleShape),
        )
    }
}

private fun anschlussPositionWelt(knoten: KnotenDaten, anschluss: AnschlussDaten): GraphPunkt =
    knoten.position + knoten.anschlussPositionRelativ(anschluss)

/** Entspricht auch für synthetische Gruppenblöcke dem sichtbaren Auswahlzustand. */
private val KartenEditorZustand.ausgewählteDarstellungsKnoten: Set<KnotenId>
    get() = if (ausgewählteGruppe == null) ausgewählteKnoten else
        ausgewählteKnoten + projektion.blöcke.filterValues { it == ausgewählteGruppe }.keys

internal fun trifftKnoten(
    position: Offset, karte: KartenDaten, ansicht: AnsichtsFenster, dichte: Float,
    ausgewählteKnoten: Set<KnotenId> = emptySet(),
): Boolean {
    val faktor = (dichte * ansicht.zoom).coerceAtLeast(.0001f)
    return karte.knoten.any { knoten ->
        val relativ = GraphPunkt(
            (position.x - ansicht.verschiebung.x) / faktor - knoten.position.x,
            (position.y - ansicht.verschiebung.y) / faktor - knoten.position.y,
        )
        val halberGriff = ANSCHLUSS_TREFFER_GRÖSSE_DP / ansicht.zoom / 2f
        knoten.enthältRelativenPunkt(relativ) || knoten.anschlüsse.any { anschluss ->
            val griff = knoten.anschlussPositionRelativ(anschluss)
            abs(relativ.x - griff.x) <= halberGriff && abs(relativ.y - griff.y) <= halberGriff
        } || knoten.id in ausgewählteKnoten &&
            relativ.x in (knoten.größe.breite - 12f)..(knoten.größe.breite + 6f) &&
            relativ.y in (knoten.größe.höhe - 12f)..(knoten.größe.höhe + 6f)
    }
}

private fun nächsterKompatiblerAnschluss(
    zustand: KartenEditorZustand,
    start: AnschlussVerweis,
    ende: GraphPunkt,
    bisherigesZiel: AnschlussVerweis?,
): AnschlussVerweis? {
    val zoom = zustand.darstellungsKarte.ansicht.zoom.coerceAtLeast(.0001f)
    fun abstandZu(ref: AnschlussVerweis): Float? {
        if (ref == start) return null
        val position = zustand.projektion.anschlussPositionen[ref] ?: return null
        val abstand = hypot((position.x - ende.x).toDouble(), (position.y - ende.y).toDouble()).toFloat()
        if (abstand > SNAP_AUSTRITT_DP / zoom || !zustand.kompatibelMitStart(ref)) return null
        return abstand
    }

    if (bisherigesZiel != null) {
        val abstand = abstandZu(bisherigesZiel)
        if (abstand != null && abstand <= SNAP_AUSTRITT_DP / zoom) return bisherigesZiel
    }

    return zustand.darstellungsKarte.knoten.asSequence().flatMap { knoten ->
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

internal fun KnotenDaten.istImBereich(
    bereich: Rect, puffer: Float, anhang: KnotenAnhangMaße = KnotenAnhangMaße(),
): Boolean = Rect(
    left = position.x - puffer,
    top = position.y - anhang.oben - puffer,
    right = position.x + größe.breite + puffer,
    bottom = position.y + größe.höhe + anhang.unten + puffer,
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


/** Kompakte graphneutrale Auswahl für Anschlüsse und bereits geprüfte Wege. */
@Composable
fun VerbindungsEinfügeAuswahlDialog(titel: String, optionen: List<Pair<String, String>>,
    abbrechen: () -> Unit, übernehmen: (Int) -> Unit) {
    var gewählt by remember(titel, optionen) { mutableStateOf<Int?>(null) }
    AtlasBestätigungsDialog(
        onDismissRequest = abbrechen,
        // Bei großer Systemschrift bleibt der kurze Aufgabentitel vollständig
        // im gemeinsamen kompakten Kopf; die Optionen besitzen den Scrollbereich.
        title = { Text(titel, style = if (LocalDensity.current.fontScale >= 1.5f)
            MaterialTheme.typography.titleSmall else MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AtlasDialogAbstände.beschreibung)) {
                optionen.forEachIndexed { index, (name, detail) ->
                    Surface(onClick = { gewählt = index }, shape = MaterialTheme.shapes.medium,
                        tonalElevation = if (gewählt == index) 3.dp else 0.dp) {
                        Row(Modifier.fillMaxWidth().padding(AtlasDialogAbstände.beschreibung)) {
                            RadioButton(selected = gewählt == index, onClick = { gewählt = index })
                            Column(Modifier.weight(1f)) {
                                Text(name, style = MaterialTheme.typography.titleSmall)
                                if (detail.isNotEmpty()) Text(detail, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(enabled = gewählt != null, onClick = {
                val index = gewählt ?: return@Button
                übernehmen(index)
            }) { Text("Verbinden") }
        },
        dismissButton = { TextButton(onClick = abbrechen) { Text("Abbrechen") } },
    )
}

@Composable
private fun VerbindungsAuswahlDialog(zustand: KartenEditorZustand) {
    val auswahl = zustand.verbindungsAuswahl ?: return
    AtlasBestätigungsDialog(
        onDismissRequest = zustand::verwerfeVerbindungsAuswahl,
        title = { Text("Verbindungsweg auswählen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mehrere passende Wege sind möglich. Wähle die gewünschte Verbindung.")
                auswahl.vorschläge.forEachIndexed { index, vorschlag ->
                    OutlinedButton(onClick = { zustand.wähleVerbindungsVorschlag(index) }, modifier = Modifier.fillMaxWidth()) {
                        Text(vorschlag.titel)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = zustand::verwerfeVerbindungsAuswahl) { Text("Abbrechen") } },
    )
}

/** Weltkoordinaten verwenden: die Auswahl bleibt unabhängig von Zoom und Pixeldichte. */
internal fun findeEinfügeVerbindung(karte: KartenDaten, knotenId: KnotenId): VerbindungDaten? {
    val knoten = karte.knoten.firstOrNull { it.id == knotenId } ?: return null
    val mitte = Offset(knoten.position.x + knoten.größe.breite / 2f, knoten.position.y + knoten.größe.höhe / 2f)
    return karte.verbindungen.asSequence()
        .filterNot { it.von.knotenId == knotenId || it.zu.knotenId == knotenId }
        .mapNotNull { verbindung ->
            val a = anschlussPositionWelt(karte, verbindung.von) ?: return@mapNotNull null
            val b = anschlussPositionWelt(karte, verbindung.zu) ?: return@mapNotNull null
            val geometrie = berechneVerbindungsGeometrie(Offset(a.x, a.y), Offset(b.x, b.y), 72f)
            val abstand = geometrie.abstandZu(mitte)
            if (abstand <= minOf(knoten.größe.höhe / 2f, 40f)) verbindung to abstand else null
        }.minByOrNull { it.second }?.first
}

/** Passive Körper-/Anschlussdarstellung aus demselben Pfad wie der Karteneditor. */
@Composable
fun KnotenVorschau(
    knoten: KnotenDaten,
    renderer: KnotenRenderer,
    zoom: Float,
    farbeFürAnschluss: @Composable (AnschlussDaten) -> Color,
    modifier: Modifier = Modifier,
    beiGröße: (GraphGröße) -> Unit = {},
) {
    val vorschau = remember(knoten.id) {
        KartenEditorZustand(KartenDaten(name = "Vorschau", knoten = listOf(knoten.copy(position = GraphPunkt.Zero))),
            de.TeutonStudio.KnotenKartenVerwalter.logik.GraphPrüfung(de.TeutonStudio.KnotenKartenVerwalter.logik.AnschlussArtRegister()))
    }
    SideEffect { vorschau.führeAus(KartenAktion.AnsichtÄndern(AnsichtsFenster(zoom = zoom)), mitHistorie = false) }
    val gezeigt = vorschau.darstellungsKarte.knoten.single()
    val aktuellBeiGröße by rememberUpdatedState(beiGröße)
    LaunchedEffect(gezeigt.größe) { aktuellBeiGröße(gezeigt.größe) }
    val dichte = LocalDensity.current.density
    Box(modifier.requiredSize((gezeigt.größe.breite * zoom).dp, (gezeigt.größe.höhe * zoom).dp)) {
        KnotenDarstellung(
            knoten = gezeigt, ausgewählt = false, zustand = vorschau,
            ansicht = AnsichtsFenster(zoom = zoom), dichte = dichte, renderer = renderer,
            farbeFürAnschluss = farbeFürAnschluss, magnetischesZiel = null, beiMagnetischemZiel = {},
            beiKnotenKontext = {}, beiAnschlussKontext = {}, beiVerbindungAufHintergrund = { _, _ -> },
            beiDoppelklick = {}, auswahlÄnderung = AuswahlÄnderung.Ersetzen, beimZiehenLösen = { false },
            zeigeKnotenInspektor = false, beiInspektorÖffnen = {}, editorUrsprungImRoot = Offset.Zero,
            sichtbarerBereich = null,
        )
        // Der Trefferpfad endet hier: kein Renderer, Anschluss oder Textfeld empfängt Eingaben.
        Box(Modifier.matchParentSize().pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false).consume()
                do { val event = awaitPointerEvent(); event.changes.forEach { it.consume() } }
                while (event.changes.any { it.pressed })
            }
        })
    }
}
