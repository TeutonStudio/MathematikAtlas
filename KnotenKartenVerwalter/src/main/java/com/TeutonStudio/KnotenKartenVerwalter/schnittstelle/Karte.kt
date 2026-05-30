package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import com.TeutonStudio.KnotenKartenVerwalter.daten.AuswahlDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.KarteZustand
import com.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungDaten
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Callback für eine geänderte Knotenposition in Weltkoordinaten.
 */
typealias KartenAktualisierung = (knotenId: String, position: Offset) -> Unit

/**
 * Callback, wenn durch Anschluss-Drag eine neue Verbindung entstanden ist.
 */
typealias VerbindungErstellen = (verbindung: VerbindungDaten) -> Unit

/**
 * Callback für Aktionen aus dem Kontextmenü der Karte.
 */
typealias KontextAktionAusführen = (aktion: KartenKontextAktion) -> Unit

/**
 * Callback fuer kontrollierte Auswahl von Knoten und Verbindungen.
 */
typealias AuswahlÄndern = (auswahl: AuswahlDaten) -> Unit

/**
 * Ergebnis eines Hit-Tests auf der Karte.
 *
 * Die Reihenfolge der Prüfung ist fachlich wichtig: Anschlüsse liegen auf
 * Knoten, Knoten liegen über Verbindungen und der Hintergrund ist der Fallback.
 */
sealed class KartenTreffer {
    data object Hintergrund : KartenTreffer()
    data class Knoten(val knotenId: String) : KartenTreffer()
    data class Anschluss(
        val knotenId: String,
        val anschlussId: String,
        val richtung: AnschlussRichtung,
    ) : KartenTreffer()
    data class Verbindung(val verbindungId: String) : KartenTreffer()
}

/**
 * Beschreibt eine vom Kontextmenü ausgewählte Aktion.
 */
data class KartenKontextAktion(
    val ziel: KartenTreffer,
    val weltPosition: Offset,
    val aktion: String,
)

sealed interface Karte: GraphObjekt {
    public val daten: KarteDaten
    public val zustand: KarteZustand
    public val knotenArten: KnotenArten
    public val aktualisierung: KartenAktualisierung
    public val onVerbindungErstellen: VerbindungErstellen
    public val onKontextAktion: KontextAktionAusführen
    public val onAuswahlÄndern: AuswahlÄndern
}

open class BasisKarte(
    override val daten: KarteDaten,
    override val zustand: KarteZustand = KarteZustand(),
    override val knotenArten: KnotenArten = KnotenArten.Standard,
    override val aktualisierung: KartenAktualisierung,
    override val onVerbindungErstellen: VerbindungErstellen = {},
    override val onKontextAktion: KontextAktionAusführen = {},
    override val onAuswahlÄndern: AuswahlÄndern = {},
): Karte {
    @Composable
    override fun zuComposable(modifier: Modifier) {
        Inhalt(modifier)
    }

    @Composable
    protected open fun Inhalt(modifier: Modifier) {
        KartenOberfläche(
            daten = daten,
            zustand = zustand,
            knotenArten = knotenArten,
            modifier = modifier,
            aktualisierung = aktualisierung,
            onVerbindungErstellen = onVerbindungErstellen,
            onKontextAktion = onKontextAktion,
            onAuswahlÄndern = onAuswahlÄndern,
        )
    }
}

/**
 * Aufgelöste Anschlussgeometrie für Rendering, Verbindungserstellung und
 * Hit-Testing.
 */
private data class AnschlussReferenz(
    val knotenId: String,
    val anschlussId: String,
    val richtung: AnschlussRichtung,
    val kante: AnschlussKante,
    val position: Offset,
)

/**
 * Temporärer Zustand während eine neue Verbindung gezogen wird.
 */
private data class VerbindungsDrag(
    val start: AnschlussReferenz,
    val startPosition: Offset,
    val aktuellePosition: Offset,
)

/**
 * Position und Ziel des aktuell geöffneten Kontextmenüs.
 */
private data class KontextMenüZustand(
    val position: Offset,
    val ziel: KartenTreffer,
    val weltPosition: Offset,
)

/**
 * Rendert eine Karte mit kompatibler Minimal-API.
 */
@Composable
public fun KarteDaten.zuComposable(
    modifier: Modifier = Modifier,
    zustand: KarteZustand = KarteZustand(),
    knotenArten: KnotenArten = KnotenArten.Standard,
    aktualisierung: KartenAktualisierung,
) = BasisKarte(this, zustand, knotenArten, aktualisierung).zuComposable(modifier)

/**
 * Rendert eine interaktive Knotenkarte.
 *
 * Der fachliche Zustand bleibt beim Aufrufer. Dieses Composable verwaltet nur
 * temporäre UI-Zustände wie Drag-Vorschauen, Kontextmenü und den sichtbaren
 * Viewport.
 */
@Composable
public fun KarteDaten.zuComposable(
    modifier: Modifier = Modifier,
    zustand: KarteZustand = KarteZustand(),
    knotenArten: KnotenArten = KnotenArten.Standard,
    aktualisierung: KartenAktualisierung,
    onVerbindungErstellen: VerbindungErstellen = {},
    onKontextAktion: KontextAktionAusführen = {},
    onAuswahlÄndern: AuswahlÄndern = {},
) = BasisKarte(
    daten = this,
    zustand = zustand,
    knotenArten = knotenArten,
    aktualisierung = aktualisierung,
    onVerbindungErstellen = onVerbindungErstellen,
    onKontextAktion = onKontextAktion,
    onAuswahlÄndern = onAuswahlÄndern,
).zuComposable(modifier)

/**
 * Zentrale Kartenoberfläche.
 *
 * Hier werden alle Weltkoordinaten in Bildschirmkoordinaten transformiert und
 * die Interaktionen für Pan, Zoom, Knoten-Drag, Verbindungs-Drag und
 * Kontextmenü koordiniert.
 */
@Composable
private fun KartenOberfläche(
    daten: KarteDaten,
    zustand: KarteZustand = KarteZustand(),
    knotenArten: KnotenArten = KnotenArten.Standard,
    modifier: Modifier = Modifier,
    aktualisierung: KartenAktualisierung,
    onVerbindungErstellen: VerbindungErstellen = {},
    onKontextAktion: KontextAktionAusführen = {},
    onAuswahlÄndern: AuswahlÄndern = {},
) {
    // Größe des sichtbaren Kartencontainers. Sie ist notwendig für Fit-to-View
    // und Minimap-Viewport-Berechnungen.
    var fläche by remember { mutableStateOf(IntSize.Zero) }

    // Laufzeit-Viewport. Beim Wechsel der Karte wird aus den gespeicherten
    // Ansichtsfensterdaten neu initialisiert.
    var ansicht by remember(daten.id) {
        mutableStateOf(
            KarteZustand(
                verschiebung = Offset(daten.ansichtsfenster.verschiebung.x, daten.ansichtsfenster.verschiebung.y),
                zoom = daten.ansichtsfenster.zoom.takeIf { it > 0f } ?: 1f,
                zeigeÜbersicht = zustand.zeigeÜbersicht,
                zeigeKontrollLeiste = zustand.zeigeKontrollLeiste,
            ),
        )
    }
    var gezogeneKnoten by remember(daten.id) {
        mutableStateOf(emptyMap<String, Offset>())
    }

    // Diese Zustände sind rein visuell und werden nicht in KarteDaten
    // persistiert.
    var verbindungsDrag by remember { mutableStateOf<VerbindungsDrag?>(null) }
    var kontextMenü by remember { mutableStateOf<KontextMenüZustand?>(null) }
    var blockiereHintergrundGesten by remember { mutableStateOf(false) }
    var ziehtAnschluss by remember { mutableStateOf(false) }

    // Während eines Drags kann die sichtbare Position bereits von den
    // übergebenen Daten abweichen. Diese Map hält die unmittelbare UI-Reaktion
    // stabil, bis der Aufrufer den neuen State zurückgibt.
    val sichtbareKnotenDaten = daten.knoten.map { knoten ->
        val position = gezogeneKnoten[knoten.id]
        KnotenDaten(
            knoten,
            position = position ?: knoten.position,
            ausgewaehlt = knoten.id in zustand.auswahl.knotenIds || knoten.ausgewaehlt,
        )
    }
    val sichtbareKnoten = sichtbareKnotenDaten.map { knotenArten.erstelle(it) }
    val sichtbareVerbindungen = daten.verbindungen.map { verbindung ->
        verbindung.copy(ausgewaehlt = verbindung.id in zustand.auswahl.verbindungIds || verbindung.ausgewaehlt)
    }
    val sichtbareDaten = KarteDaten(daten, knoten = sichtbareKnotenDaten, verbindungen = sichtbareVerbindungen)
    val knotenNachId = sichtbareKnoten.associateBy { it.daten.id }
    val sichtbarerZustand = KarteZustand(
        ansicht,
        zeigeÜbersicht = zustand.zeigeÜbersicht,
        zeigeKontrollLeiste = zustand.zeigeKontrollLeiste,
        auswahl = zustand.auswahl,
    )
    val anschlüsse = sichtbareKnoten.flatMap { it.anschlussReferenzen(sichtbarerZustand) }
    val density = LocalDensity.current

    // Pointer-Handler laufen über mehrere Frames. rememberUpdatedState sorgt
    // dafür, dass sie aktuelle Daten sehen, ohne bei jeder Zustandsänderung neu
    // gestartet zu werden.
    val aktuelleAnsicht by rememberUpdatedState(sichtbarerZustand)
    val aktuelleKnoten by rememberUpdatedState(sichtbareKnoten)
    val aktuelleVerbindungen by rememberUpdatedState(daten.verbindungen)
    val aktuelleAnschlüsse by rememberUpdatedState(anschlüsse)
    val hintergrundGestenBlockiert by rememberUpdatedState(blockiereHintergrundGesten)

    // Beim Laden einer Karte wird der Inhalt sichtbar eingepasst.
    LaunchedEffect(daten.id, fläche) {
        if (fläche.width > 0 && fläche.height > 0 && daten.knoten.isNotEmpty()) {
            ansicht = daten.zoomAufInhalt(fläche, ansicht)
        }
    }

    /**
     * Öffnet das Kontextmenü an einer Bildschirmposition und speichert zusätzlich
     * die entsprechende Weltposition für Aktionen wie "Knoten erstellen".
     */
    fun öffneKontextMenü(position: Offset) {
        val ziel = position.treffer(aktuelleKnoten, aktuelleVerbindungen, aktuelleAnschlüsse, aktuelleAnsicht)
        kontextMenü = KontextMenüZustand(
            position = position,
            ziel = ziel,
            weltPosition = position.zuWeltPosition(aktuelleAnsicht).zuWeltOffset(),
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { fläche = it }
            .background(Color(0xFFF8FAFC))
            .pointerInput(daten.id) {
                detectTapGestures(
                    onTap = { position ->
                        val ziel = position.treffer(
                            aktuelleKnoten,
                            aktuelleVerbindungen,
                            aktuelleAnschlüsse,
                            aktuelleAnsicht
                        )
                        onAuswahlÄndern(ziel.zuAuswahl())
                    },
                )
            }
            // Android liefert Rechtsklicks je nach Eingabegerät zuverlässiger
            // über MotionEvent-Buttonzustände als über reine Compose-Events.
            .pointerInteropFilter { ereignis ->
                val sekundär = ereignis.buttonState and MotionEvent.BUTTON_SECONDARY != 0
                if (
                    sekundär &&
                    (ereignis.actionMasked == MotionEvent.ACTION_DOWN ||
                            ereignis.actionMasked == MotionEvent.ACTION_BUTTON_PRESS)
                ) {
                    öffneKontextMenü(Offset(ereignis.x, ereignis.y))
                    true
                } else {
                    false
                }
            }
            // Pan und Zwei-Finger-Zoom teilen sich dieselbe Transformationslogik.
            .pointerInput(daten.id) {
                detectTransformGestures { zentrum, pan, zoomÄnderung, _ ->
                    if (hintergrundGestenBlockiert) return@detectTransformGestures
                    kontextMenü = null
                    ansicht = aktuelleAnsicht.transformiereUm(zentrum, pan, zoomÄnderung)
                }
            }
            // Zweiter Pfad für sekundäre Pointer-Events innerhalb der Compose
            // Pointer-API. Dadurch bleiben Desktop-/Mausvarianten abgedeckt.
            .pointerInput(daten.id) {
                awaitPointerEventScope {
                    while (true) {
                        val ereignis = awaitPointerEvent()
                        if (
                            ereignis.type == PointerEventType.Press &&
                            ereignis.buttons.isSecondaryPressed
                        ) {
                            öffneKontextMenü(ereignis.changes.first().position)
                            ereignis.changes.forEach { it.consume() }
                        }
                    }
                }
            },
    ) {
        // Verbindungen liegen hinter den Knoten.
        sichtbareDaten.verbindungen.zuComposable(
            { it.startOffset(knotenNachId, sichtbarerZustand) },
            { it.endeOffset(knotenNachId, sichtbarerZustand) },
            Modifier.fillMaxSize(),
        )

        // Während des Ziehens eines Anschlusses wird eine temporäre Verbindung
        // angezeigt. Startet der Drag an einem Eingang, wird die Bezier-Kurve
        // visuell so gedreht, dass die Tangentenrichtung korrekt bleibt.
        verbindungsDrag?.let { drag ->
            val start = if (drag.start.richtung == AnschlussRichtung.Eingang) {
                drag.aktuellePosition
            } else {
                drag.startPosition
            }
            val ende = if (drag.start.richtung == AnschlussRichtung.Eingang) {
                drag.startPosition
            } else {
                drag.aktuellePosition
            }
            listOf(
                Triple(
                    VerbindungDaten(
                        id = "temporaer",
                        quellKnotenId = drag.start.knotenId,
                        quellAnschlussId = drag.start.anschlussId,
                        zielKnotenId = drag.start.knotenId,
                        zielAnschlussId = drag.start.anschlussId,
                        ausgewaehlt = true,
                    ),
                    start,
                    ende,
                ),
            ).zuComposable(Modifier.fillMaxSize())
        }

        // Jeder Knoten bekommt seine eigene Drag-Interaktion. Das Delta wird
        // durch den Zoom geteilt, weil Knotenpositionen in Weltkoordinaten
        // gespeichert werden.
        sichtbareKnoten.forEach { knotenObjekt ->
            val knoten = knotenObjekt.daten
            val aktuellerKnoten by rememberUpdatedState(knoten)
            var knotenModifier = Modifier
                .offset { knoten.position.zuBildschirmIntOffset(sichtbarerZustand) }
                .size(
                    with(density) { (knoten.fläche.x * sichtbarerZustand.zoom).toDp() },
                    with(density) { (knoten.fläche.y * sichtbarerZustand.zoom).toDp() },
                )
            if (knoten.beweglich) {
                knotenModifier = knotenModifier.pointerInput(daten.id, knoten.id, sichtbarerZustand.zoom) {
                    var startPosition = Offset.Zero
                    var gesamtDrag = Offset.Zero
                    detectDragGestures(
                        onDragStart = { if (!ziehtAnschluss) {
                            val aktuellerKnotenId = aktuellerKnoten.id
                            kontextMenü = null
                            blockiereHintergrundGesten = true
                            startPosition = aktuellerKnoten.position
                            gesamtDrag = Offset.Zero
                            onAuswahlÄndern(AuswahlDaten(knotenIds = setOf(aktuellerKnotenId)))
                        } },
                        onDrag = { change, dragAmount -> if (!ziehtAnschluss) {
                            change.consume()
                            gesamtDrag += dragAmount
                            val neuePosition = startPosition + (gesamtDrag / sichtbarerZustand.zoomSicher())
                            val aktuellerKnotenId = aktuellerKnoten.id
                            gezogeneKnoten = gezogeneKnoten + (aktuellerKnotenId to neuePosition)
                            aktualisierung(aktuellerKnotenId, neuePosition)
                        } },
                        onDragEnd = {
                            blockiereHintergrundGesten = false
                        },
                        onDragCancel = {
                            blockiereHintergrundGesten = false
                        },
                    )
                }
            }

            knotenObjekt.zuComposable(
                modifierKnoten = knotenModifier,
                modifierAnschluss = { richtung, index ->
                    val referenz = knotenObjekt.anschlussReferenz(richtung, index, sichtbarerZustand)
                    // Anschlüsse sind Drag-Startpunkte für neue Verbindungen.
                    AnschlussModifier.pointerInput(daten.id, referenz) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            try {
                                ziehtAnschluss = true
                                kontextMenü = null
                                blockiereHintergrundGesten = true
                                verbindungsDrag = VerbindungsDrag(
                                    start = referenz,
                                    startPosition = referenz.position,
                                    aktuellePosition = referenz.position,
                                )

                                down.consume()
                                drag(down.id) { change ->
                                    change.consume()
                                    val alt = verbindungsDrag ?: return@drag
                                    verbindungsDrag = alt.copy(
                                        aktuellePosition = alt.aktuellePosition + (change.position - change.previousPosition),
                                    )
                                }

                                val drag = verbindungsDrag
                                val ziel = drag?.aktuellePosition?.nächsterAnschluss(aktuelleAnschlüsse, maxAbstand = 28f)
                                if (drag != null && ziel != null && drag.start.istKompatibelMit(ziel)) {
                                    onVerbindungErstellen(drag.start.zuVerbindung(ziel))
                                }
                            } finally {
                                ziehtAnschluss = false
                                verbindungsDrag = null
                                blockiereHintergrundGesten = false
                            }
                        }
                    }
                },
            )
        }

        // Optionale Minimap.
        if (sichtbarerZustand.zeigeÜbersicht) {
            sichtbareDaten.zuComposable(
                modifier = Modifier,
                zustand = sichtbarerZustand,
                fläche = fläche,
                onAnsichtÄndern = { neueAnsicht -> ansicht = neueAnsicht },
            )
        }

        // Optionale Kontrollleiste für Zoom und Fit-to-View.
        if (sichtbarerZustand.zeigeKontrollLeiste) {
            sichtbarerZustand.zuComposable(
                daten = sichtbareDaten,
                onZoomRein = {
                    ansicht = ansicht.zoomUm(1.25f, fläche)
                },
                onZoomRaus = {
                    ansicht = ansicht.zoomUm(0.8f, fläche)
                },
                onZoomAufInhalt = {
                    ansicht = sichtbareDaten.zoomAufInhalt(fläche, sichtbarerZustand)
                },
            )
        }

        // Kontextmenüs werden über allen anderen Karteninhalten gerendert.
        kontextMenü?.let { menü ->
            KontextMenü(
                zustand = menü,
                onAktion = { aktion ->
                    onKontextAktion(KartenKontextAktion(menü.ziel, menü.weltPosition, aktion))
                    kontextMenü = null
                },
                onSchließen = { kontextMenü = null },
            )
        }
    }
}

@Composable
private fun KontextMenü(
    zustand: KontextMenüZustand,
    onAktion: (String) -> Unit,
    onSchließen: () -> Unit,
) {
    // Die Menüeinträge hängen vom Trefferziel ab. Der aufrufende Code
    // entscheidet später, welche Aktionen tatsächlich fachlich umgesetzt werden.
    val einträge = when (zustand.ziel) {
        KartenTreffer.Hintergrund -> listOf("Knoten erstellen", "Ansicht zentrieren")
        is KartenTreffer.Knoten -> listOf("Knoten auswaehlen", "Knoten duplizieren", "Knoten loeschen")
        is KartenTreffer.Anschluss -> listOf("Verbindung starten", "Anschluss auswaehlen")
        is KartenTreffer.Verbindung -> listOf("Verbindung auswaehlen", "Verbindung loeschen")
    }
    Column(
        modifier = Modifier
            .offset { IntOffset(zustand.position.x.roundToInt(), zustand.position.y.roundToInt()) }
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(8.dp))
            .padding(vertical = 4.dp),
    ) {
        einträge.forEach { eintrag ->
            BasicText(
                text = eintrag,
                modifier = Modifier
                    .clickable { onAktion(eintrag) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                style = TextStyle(
                    color = Color(0xFF111827),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
        BasicText(
            text = "Schliessen",
            modifier = Modifier
                .clickable(onClick = onSchließen)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            style = TextStyle(color = Color(0xFF6B7280), fontSize = 13.sp),
        )
    }
}

/**
 * Zoomt um die Mitte des sichtbaren Kartencontainers.
 */
internal fun KarteZustand.zoomUm(faktor: Float, fläche: IntSize): KarteZustand {
    val mittelpunkt = Offset(fläche.width / 2f, fläche.height / 2f)
    return transformiereUm(mittelpunkt, Offset.Zero, faktor)
}

/**
 * Transformiert den Viewport um einen Bildschirm-Mittelpunkt.
 *
 * Der Weltpunkt unter `zentrum` bleibt auch nach dem Zoom unter derselben
 * Bildschirmposition. Das verhindert visuelles Springen beim Pinch-Zoom.
 */
internal fun KarteZustand.transformiereUm(zentrum: Offset, pan: Offset, zoomÄnderung: Float): KarteZustand {
    val alterZoom = zoomSicher()
    val neuerZoom = (alterZoom * zoomÄnderung).coerceIn(0.25f, 3f)
    val weltZentrum = (zentrum - verschiebung) / alterZoom
    return KarteZustand(
        this,
        zoom = neuerZoom,
        verschiebung = zentrum - weltZentrum * neuerZoom + pan,
    )
}

/**
 * Berechnet einen Viewport, der alle Knoten sichtbar in den Container einpasst.
 */
internal fun KarteDaten.zoomAufInhalt(fläche: IntSize, aktuellerZustand: KarteZustand): KarteZustand {
    val grenzen = knoten.grenzen() ?: return aktuellerZustand
    if (fläche.width <= 0 || fläche.height <= 0) return aktuellerZustand

    val padding = 48f
    val breite = (grenzen.rechts - grenzen.links).coerceAtLeast(1f)
    val höhe = (grenzen.unten - grenzen.oben).coerceAtLeast(1f)
    val neuerZoom = minOf(
        (fläche.width - padding * 2f) / breite,
        (fläche.height - padding * 2f) / höhe,
    ).coerceIn(0.25f, 3f)

    val verschiebung = Offset(
        x = (fläche.width - breite * neuerZoom) / 2f - grenzen.links * neuerZoom,
        y = (fläche.height - höhe * neuerZoom) / 2f - grenzen.oben * neuerZoom,
    )
    return KarteZustand(aktuellerZustand, zoom = neuerZoom, verschiebung = verschiebung)
}

/**
 * Rechteckige Begrenzung in Weltkoordinaten.
 */
internal data class KartenGrenzen(
    val links: Float,
    val oben: Float,
    val rechts: Float,
    val unten: Float,
)

/**
 * Berechnet die Gesamtgrenzen einer Knotenliste inklusive optionalem Padding.
 */
internal fun List<KnotenDaten>.grenzen(padding: Float = 0f): KartenGrenzen? {
    if (isEmpty()) return null
    val grenzen = fold<KnotenDaten, KartenGrenzen?>(null) { grenzen, knoten ->
        val links = knoten.position.x
        val oben = knoten.position.y
        val rechts = links + knoten.fläche.x
        val unten = oben + knoten.fläche.y
        if (grenzen == null) {
            KartenGrenzen(links, oben, rechts, unten)
        } else {
            KartenGrenzen(
                links = minOf(grenzen.links, links),
                oben = minOf(grenzen.oben, oben),
                rechts = maxOf(grenzen.rechts, rechts),
                unten = maxOf(grenzen.unten, unten),
            )
        }
    } ?: return null
    return KartenGrenzen(
        links = grenzen.links - padding,
        oben = grenzen.oben - padding,
        rechts = grenzen.rechts + padding,
        unten = grenzen.unten + padding,
    )
}

/**
 * Rechnet eine Bildschirmposition in Weltkoordinaten um.
 */
internal fun Offset.zuWeltPosition(zustand: KarteZustand): Offset {
    return KoordinatenUmrechnung.bildschirmZuWelt(this, zustand)
}

/**
 * Rechnet eine Weltposition in Bildschirmkoordinaten um.
 */
internal fun Offset.zuBildschirmOffset(zustand: KarteZustand): Offset =
    KoordinatenUmrechnung.weltZuBildschirm(this, zustand)

/**
 * Rechnet eine Weltposition in eine ganzzahlige Bildschirmposition für Modifier.offset um.
 */
internal fun Offset.zuBildschirmIntOffset(zustand: KarteZustand): IntOffset {
    val offset = zuBildschirmOffset(zustand)
    return IntOffset(offset.x.roundToInt(), offset.y.roundToInt())
}

/**
 * Liefert einen robusten Zoomfaktor, auch wenn fehlerhafte Daten `0` oder einen
 * negativen Wert enthalten.
 */
internal fun KarteZustand.zoomSicher(): Float = zoom.takeIf { it > 0f } ?: 1f

/** Teilt einen Bildschirm-Offset komponentenweise durch einen Faktor. */
internal operator fun Offset.div(wert: Float): Offset = Offset(x / wert, y / wert)

/** Multipliziert einen Bildschirm-Offset komponentenweise mit einem Faktor. */
internal operator fun Offset.times(wert: Float): Offset = Offset(x * wert, y * wert)

/**
 * Konvertiert einen Offset in eine fachliche Weltposition.
 */
private fun Offset.zuWeltOffset(): Offset = Offset(x, y)

/**
 * Löst alle Anschlüsse eines Knotens in Bildschirmpositionen auf.
 */
private fun Knoten.anschlussReferenzen(zustand: KarteZustand): List<AnschlussReferenz> =
    erhalteAnschlüsseGeordnet(AnschlussRichtung.Eingang).mapIndexed { index, anschluss ->
        anschlussReferenz(AnschlussRichtung.Eingang, index, zustand)
    } + erhalteAnschlüsseGeordnet(AnschlussRichtung.Ausgang).mapIndexed { index, anschluss ->
        anschlussReferenz(AnschlussRichtung.Ausgang, index, zustand)
    }

/**
 * Berechnet die Bildschirmposition eines einzelnen Anschlusses.
 */
private fun Knoten.anschlussReferenz(
    richtung: AnschlussRichtung,
    index: Int,
    zustand: KarteZustand,
): AnschlussReferenz {
    val daten = daten
    val anschlüsse = erhalteAnschlüsseGeordnet(richtung)
    val anschluss = anschlüsse.getOrNull(index)
    val kante = anschluss?.kante ?: when (richtung) {
        AnschlussRichtung.Eingang -> AnschlussKante.Links
        AnschlussRichtung.Ausgang -> AnschlussKante.Rechts
    }
    val anschlüsseAnKante = erhalteAnschlüsseGeordnet().filter { it.kante == kante }
    val indexAnKante = anschlüsseAnKante.indexOfFirst { it.richtung == richtung && it.id == anschluss?.id }
        .coerceAtLeast(0)
    val anzahlAnKante = anschlüsseAnKante.size.coerceAtLeast(1)
    val anteil = (indexAnKante + 1f) / (anzahlAnKante + 1f)
    val weltPosition = Offset(
        x = when (richtung) {
            AnschlussRichtung.Eingang -> when (kante) {
                AnschlussKante.Links -> daten.position.x
                AnschlussKante.Rechts -> daten.position.x + daten.fläche.x
                AnschlussKante.Oben, AnschlussKante.Unten -> daten.position.x + daten.fläche.x * anteil
            }
            AnschlussRichtung.Ausgang -> when (kante) {
                AnschlussKante.Links -> daten.position.x
                AnschlussKante.Rechts -> daten.position.x + daten.fläche.x
                AnschlussKante.Oben, AnschlussKante.Unten -> daten.position.x + daten.fläche.x * anteil
            }
        },
        y = when (kante) {
            AnschlussKante.Links, AnschlussKante.Rechts -> daten.position.y + daten.fläche.y * anteil
            AnschlussKante.Oben -> daten.position.y
            AnschlussKante.Unten -> daten.position.y + daten.fläche.y
        },
    )
    return AnschlussReferenz(
        knotenId = daten.id,
        anschlussId = anschluss?.id.orEmpty(),
        richtung = richtung,
        kante = kante,
        position = weltPosition.zuBildschirmOffset(zustand),
    )
}

/**
 * Liefert den Bildschirm-Startpunkt einer Verbindung.
 */
private fun VerbindungDaten.startOffset(
    knotenNachId: Map<String, Knoten>,
    zustand: KarteZustand,
): Offset? {
    val knoten = knotenNachId[quellKnotenId] ?: return null
    val index = knoten.erhalteAnschlüsseGeordnet(AnschlussRichtung.Ausgang)
        .indexOfFirst { it.id == quellAnschlussId }
        .coerceAtLeast(0)
    return knoten.anschlussReferenz(AnschlussRichtung.Ausgang, index, zustand).position
}

/**
 * Liefert den Bildschirm-Endpunkt einer Verbindung.
 */
private fun VerbindungDaten.endeOffset(
    knotenNachId: Map<String, Knoten>,
    zustand: KarteZustand,
): Offset? {
    val knoten = knotenNachId[zielKnotenId] ?: return null
    val index = knoten.erhalteAnschlüsseGeordnet(AnschlussRichtung.Eingang)
        .indexOfFirst { it.id == zielAnschlussId }
        .coerceAtLeast(0)
    return knoten.anschlussReferenz(AnschlussRichtung.Eingang, index, zustand).position
}

/**
 * Prüft, ob zwei Anschlüsse verbunden werden dürfen.
 */
private fun AnschlussReferenz.istKompatibelMit(ziel: AnschlussReferenz): Boolean =
    knotenId != ziel.knotenId && richtung != ziel.richtung

/**
 * Erzeugt aus zwei kompatiblen Anschlüssen eine fachliche Verbindung.
 */
private fun AnschlussReferenz.zuVerbindung(ziel: AnschlussReferenz): VerbindungDaten {
    val quelle = if (richtung == AnschlussRichtung.Ausgang) this else ziel
    val ende = if (richtung == AnschlussRichtung.Eingang) this else ziel
    return VerbindungDaten(
        id = "verbindung-${quelle.knotenId}-${quelle.anschlussId}-${ende.knotenId}-${ende.anschlussId}",
        quellKnotenId = quelle.knotenId,
        quellAnschlussId = quelle.anschlussId,
        zielKnotenId = ende.knotenId,
        zielAnschlussId = ende.anschlussId,
    )
}

/**
 * Sucht den nächsten Anschluss zu einer Bildschirmposition.
 */
private fun Offset.nächsterAnschluss(
    anschlüsse: List<AnschlussReferenz>,
    maxAbstand: Float,
): AnschlussReferenz? = anschlüsse
    .map { it to hypot(x - it.position.x, y - it.position.y) }
    .filter { it.second <= maxAbstand }
    .minByOrNull { it.second }
    ?.first

/**
 * Führt den Hit-Test für Kontextmenüs aus.
 */
private fun Offset.treffer(
    knoten: List<Knoten>,
    verbindungen: List<VerbindungDaten>,
    anschlüsse: List<AnschlussReferenz>,
    zustand: KarteZustand,
): KartenTreffer {
    nächsterAnschluss(anschlüsse, maxAbstand = 16f)?.let {
        return KartenTreffer.Anschluss(it.knotenId, it.anschlussId, it.richtung)
    }

    knoten.firstOrNull { enthältBildschirmPunkt(it.daten, zustand) }?.let {
        return KartenTreffer.Knoten(it.daten.id)
    }

    val knotenNachId = knoten.associateBy { it.daten.id }
    verbindungen.firstOrNull { verbindung ->
        val start = verbindung.startOffset(knotenNachId, zustand)
        val ende = verbindung.endeOffset(knotenNachId, zustand)
        start != null && ende != null && abstandZuBezier(start, ende) <= 8f
    }?.let {
        return KartenTreffer.Verbindung(it.id)
    }

    return KartenTreffer.Hintergrund
}

private fun KartenTreffer.zuAuswahl(): AuswahlDaten = when (this) {
    KartenTreffer.Hintergrund -> AuswahlDaten()
    is KartenTreffer.Knoten -> AuswahlDaten(knotenIds = setOf(knotenId))
    is KartenTreffer.Anschluss -> AuswahlDaten(knotenIds = setOf(knotenId))
    is KartenTreffer.Verbindung -> AuswahlDaten(verbindungIds = setOf(verbindungId))
}

/**
 * Prüft, ob eine Bildschirmposition innerhalb des sichtbaren Knotenrechtecks liegt.
 */
private fun Offset.enthältBildschirmPunkt(knoten: KnotenDaten, zustand: KarteZustand): Boolean {
    val linksOben = knoten.position.zuBildschirmOffset(zustand)
    val zoom = zustand.zoomSicher()
    return x in linksOben.x..(linksOben.x + knoten.fläche.x * zoom) &&
        y in linksOben.y..(linksOben.y + knoten.fläche.y * zoom)
}

/**
 * Approximiert den Abstand zu einer Bezier-Verbindung durch kurze Liniensegmente.
 */
private fun Offset.abstandZuBezier(start: Offset, ende: Offset): Float {
    val kontrollAbstand = maxOf(48f, abs(ende.x - start.x) / 2f)
    val p1 = Offset(start.x + kontrollAbstand, start.y)
    val p2 = Offset(ende.x - kontrollAbstand, ende.y)
    var besterAbstand = Float.MAX_VALUE
    var vorher = start
    for (schritt in 1..24) {
        val t = schritt / 24f
        val punkt = kubisch(start, p1, p2, ende, t)
        besterAbstand = minOf(besterAbstand, abstandZuSegment(vorher, punkt))
        vorher = punkt
    }
    return besterAbstand
}

/**
 * Berechnet einen Punkt auf einer kubischen Bezier-Kurve.
 */
private fun kubisch(p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float): Offset {
    val u = 1f - t
    return p0 * u.pow(3) + p1 * (3f * u.pow(2) * t) + p2 * (3f * u * t.pow(2)) + p3 * t.pow(3)
}

/**
 * Berechnet den kürzesten Abstand zu einem Liniensegment.
 */
private fun Offset.abstandZuSegment(a: Offset, b: Offset): Float {
    val dx = b.x - a.x
    val dy = b.y - a.y
    if (dx == 0f && dy == 0f) return hypot(x - a.x, y - a.y)
    val t = (((x - a.x) * dx + (y - a.y) * dy) / (dx * dx + dy * dy)).coerceIn(0f, 1f)
    val projektion = Offset(a.x + t * dx, a.y + t * dy)
    return hypot(x - projektion.x, y - projektion.y)
}

/**
 * Vorschau einer kleinen Beispielkarte.
 */
@Preview
@Composable
private fun KartePreview() {
    val daten = KarteDaten(
        id = "karte-1",
        name = "Mathematik",
        knoten = listOf(
            KnotenDaten(
                id = "definition",
                name = "Definition",
                position = Offset(40f, 80f),
                art = EingabeKnoten.KNOTEN_ART,
            ),
            KnotenDaten(
                id = "satz",
                name = "Satz",
                position = Offset(300f, 360f),
                art = AusgabeKnoten.KNOTEN_ART,
            ),
        ),
        verbindungen = listOf(
            VerbindungDaten(
                id = "kante-1",
                quellKnotenId = "definition",
                quellAnschlussId = "out",
                zielKnotenId = "satz",
                zielAnschlussId = "in",
            ),
        ),
    )
    daten.zuComposable { _, _ -> }
}
