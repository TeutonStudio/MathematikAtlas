package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import com.TeutonStudio.KnotenKartenVerwalter.daten.AusgangDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.EingangDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.KarteZustand
import com.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.PositionDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungDaten
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt

typealias KartenAktualisierung = (knotenId: String, position: PositionDaten) -> Unit
typealias VerbindungErstellen = (verbindung: VerbindungDaten) -> Unit
typealias KontextAktionAusführen = (aktion: KartenKontextAktion) -> Unit

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

data class KartenKontextAktion(
    val ziel: KartenTreffer,
    val weltPosition: PositionDaten,
    val aktion: String,
)

private data class AnschlussReferenz(
    val knotenId: String,
    val anschlussId: String,
    val richtung: AnschlussRichtung,
    val position: Offset,
)

private data class VerbindungsDrag(
    val start: AnschlussReferenz,
    val startPosition: Offset,
    val aktuellePosition: Offset,
)

private data class KontextMenüZustand(
    val position: Offset,
    val ziel: KartenTreffer,
    val weltPosition: PositionDaten,
)

@Composable
public fun KarteDaten.zuComposable(
    modifier: Modifier = Modifier,
    zustand: KarteZustand = KarteZustand(),
    aktualisierung: KartenAktualisierung,
) = Karte(this, zustand, modifier, aktualisierung)

@Composable
public fun KarteDaten.zuComposable(
    modifier: Modifier = Modifier,
    zustand: KarteZustand = KarteZustand(),
    aktualisierung: KartenAktualisierung,
    onVerbindungErstellen: VerbindungErstellen = {},
    onKontextAktion: KontextAktionAusführen = {},
) = Karte(this, zustand, modifier, aktualisierung, onVerbindungErstellen, onKontextAktion)

@Composable
private fun Karte(
    daten: KarteDaten,
    zustand: KarteZustand = KarteZustand(),
    modifier: Modifier = Modifier,
    aktualisierung: KartenAktualisierung,
    onVerbindungErstellen: VerbindungErstellen = {},
    onKontextAktion: KontextAktionAusführen = {},
) {
    var fläche by remember { mutableStateOf(IntSize.Zero) }
    var ansicht by remember(daten.id) {
        mutableStateOf(
            KarteZustand(
                verschiebung = Offset(daten.ansichtsfenster.x, daten.ansichtsfenster.y),
                zoom = daten.ansichtsfenster.zoom.takeIf { it > 0f } ?: 1f,
                zeigeÜbersicht = zustand.zeigeÜbersicht,
                zeigeKontrollLeiste = zustand.zeigeKontrollLeiste,
            ),
        )
    }
    var gezogeneKnoten by remember(daten.id) {
        mutableStateOf(emptyMap<String, PositionDaten>())
    }
    var verbindungsDrag by remember { mutableStateOf<VerbindungsDrag?>(null) }
    var kontextMenü by remember { mutableStateOf<KontextMenüZustand?>(null) }

    val sichtbareKnoten = daten.knoten.map { knoten ->
        gezogeneKnoten[knoten.id]?.let { knoten.copy(position = it) } ?: knoten
    }
    val sichtbareDaten = daten.copy(knoten = sichtbareKnoten)
    val knotenNachId = sichtbareKnoten.associateBy { it.id }
    val sichtbarerZustand = ansicht.copy(
        zeigeÜbersicht = zustand.zeigeÜbersicht,
        zeigeKontrollLeiste = zustand.zeigeKontrollLeiste,
    )
    val anschlüsse = sichtbareKnoten.flatMap { it.anschlussReferenzen(sichtbarerZustand) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { fläche = it }
            .background(Color(0xFFF8FAFC))
            .pointerInput(daten.id, fläche, sichtbarerZustand) {
                detectTransformGestures { zentrum, pan, zoomÄnderung, _ ->
                    kontextMenü = null
                    ansicht = sichtbarerZustand.transformiereUm(zentrum, pan, zoomÄnderung)
                }
            }
            .pointerInput(daten.id, sichtbarerZustand, fläche, anschlüsse, daten.verbindungen) {
                awaitPointerEventScope {
                    while (true) {
                        val ereignis = awaitPointerEvent()
                        if (
                            ereignis.type == PointerEventType.Press &&
                            ereignis.buttons.isSecondaryPressed
                        ) {
                            val position = ereignis.changes.first().position
                            val ziel = position.treffer(sichtbareKnoten, daten.verbindungen, anschlüsse, sichtbarerZustand)
                            kontextMenü = KontextMenüZustand(
                                position = position,
                                ziel = ziel,
                                weltPosition = position.zuWeltPosition(sichtbarerZustand).zuPositionDaten(),
                            )
                            ereignis.changes.forEach { it.consume() }
                        }
                    }
                }
            },
    ) {
        sichtbareDaten.verbindungen.zuComposable(
            { it.startOffset(knotenNachId, sichtbarerZustand) },
            { it.endeOffset(knotenNachId, sichtbarerZustand) },
            Modifier.fillMaxSize(),
        )

        verbindungsDrag?.let { drag ->
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
                    drag.startPosition,
                    drag.aktuellePosition,
                ),
            ).zuComposable(Modifier.fillMaxSize())
        }

        sichtbareKnoten.forEach { knoten ->
            var startPosition = knoten.position
            var gesamtDrag = Offset.Zero
            var knotenModifier = Modifier
                .offset { knoten.position.zuBildschirmIntOffset(sichtbarerZustand) }
                .size(
                    with(density) { (knoten.fläche.waagrecht * sichtbarerZustand.zoom).toDp() },
                    with(density) { (knoten.fläche.senkrecht * sichtbarerZustand.zoom).toDp() },
                )
            if (knoten.beweglich) {
                knotenModifier = knotenModifier.pointerInput(daten.id, knoten.id, sichtbarerZustand.zoom) {
                    detectDragGestures(
                        onDragStart = {
                            kontextMenü = null
                            startPosition = knoten.position
                            gesamtDrag = Offset.Zero
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            gesamtDrag += dragAmount
                            val neuePosition = startPosition + (gesamtDrag / sichtbarerZustand.zoomSicher())
                            gezogeneKnoten = gezogeneKnoten + (knoten.id to neuePosition)
                            aktualisierung(knoten.id, neuePosition)
                        },
                    )
                }
            }

            knoten.zuComposable(
                modifierKnoten = knotenModifier,
                modifierAnschluss = { richtung, index ->
                    val referenz = knoten.anschlussReferenz(richtung, index, sichtbarerZustand)
                    AnschlussModifier.pointerInput(daten.id, referenz, anschlüsse, sichtbarerZustand) {
                        detectDragGestures(
                            onDragStart = {
                                kontextMenü = null
                                verbindungsDrag = VerbindungsDrag(
                                    start = referenz,
                                    startPosition = referenz.position,
                                    aktuellePosition = referenz.position,
                                )
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val alt = verbindungsDrag ?: return@detectDragGestures
                                verbindungsDrag = alt.copy(aktuellePosition = alt.aktuellePosition + dragAmount)
                            },
                            onDragEnd = {
                                val drag = verbindungsDrag
                                val ziel = drag?.aktuellePosition?.nächsterAnschluss(anschlüsse, maxAbstand = 28f)
                                if (drag != null && ziel != null && drag.start.istKompatibelMit(ziel)) {
                                    onVerbindungErstellen(drag.start.zuVerbindung(ziel))
                                }
                                verbindungsDrag = null
                            },
                            onDragCancel = {
                                verbindungsDrag = null
                            },
                        )
                    }
                },
            )
        }

        if (sichtbarerZustand.zeigeÜbersicht) {
            sichtbareDaten.zuComposable(
                modifier = Modifier,
                zustand = sichtbarerZustand,
                fläche = fläche,
                onAnsichtÄndern = { neueAnsicht -> ansicht = neueAnsicht },
            )
        }

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

internal fun KarteZustand.zoomUm(faktor: Float, fläche: IntSize): KarteZustand {
    val mittelpunkt = Offset(fläche.width / 2f, fläche.height / 2f)
    return transformiereUm(mittelpunkt, Offset.Zero, faktor)
}

internal fun KarteZustand.transformiereUm(zentrum: Offset, pan: Offset, zoomÄnderung: Float): KarteZustand {
    val alterZoom = zoomSicher()
    val neuerZoom = (alterZoom * zoomÄnderung).coerceIn(0.25f, 3f)
    val weltZentrum = (zentrum - verschiebung) / alterZoom
    return copy(
        zoom = neuerZoom,
        verschiebung = zentrum - weltZentrum * neuerZoom + pan,
    )
}

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
    return aktuellerZustand.copy(zoom = neuerZoom, verschiebung = verschiebung)
}

internal data class KartenGrenzen(
    val links: Float,
    val oben: Float,
    val rechts: Float,
    val unten: Float,
)

internal fun List<KnotenDaten>.grenzen(padding: Float = 0f): KartenGrenzen? {
    if (isEmpty()) return null
    val grenzen = fold<KnotenDaten, KartenGrenzen?>(null) { grenzen, knoten ->
        val links = knoten.position.waagrecht
        val oben = knoten.position.senkrecht
        val rechts = links + knoten.fläche.waagrecht
        val unten = oben + knoten.fläche.senkrecht
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

internal fun Offset.zuWeltPosition(zustand: KarteZustand): Offset {
    val zoom = zustand.zoomSicher()
    return (this - zustand.verschiebung) / zoom
}

internal fun PositionDaten.zuBildschirmOffset(zustand: KarteZustand): Offset = Offset(
    x = waagrecht * zustand.zoomSicher() + zustand.verschiebung.x,
    y = senkrecht * zustand.zoomSicher() + zustand.verschiebung.y,
)

internal fun PositionDaten.zuBildschirmIntOffset(zustand: KarteZustand): IntOffset {
    val offset = zuBildschirmOffset(zustand)
    return IntOffset(offset.x.roundToInt(), offset.y.roundToInt())
}

internal fun KarteZustand.zoomSicher(): Float = zoom.takeIf { it > 0f } ?: 1f

internal operator fun Offset.div(wert: Float): Offset = Offset(x / wert, y / wert)

internal operator fun Offset.times(wert: Float): Offset = Offset(x * wert, y * wert)

private operator fun PositionDaten.plus(other: Offset): PositionDaten = PositionDaten(
    waagrecht = waagrecht + other.x,
    senkrecht = senkrecht + other.y,
)

private fun Offset.zuPositionDaten(): PositionDaten = PositionDaten(x, y)

private fun KnotenDaten.anschlussReferenzen(zustand: KarteZustand): List<AnschlussReferenz> =
    eingängeGeordnet.mapIndexed { index, anschluss ->
        anschlussReferenz(AnschlussRichtung.Eingang, index, zustand).copy(anschlussId = anschluss.id)
    } + ausgängeGeordnet.mapIndexed { index, anschluss ->
        anschlussReferenz(AnschlussRichtung.Ausgang, index, zustand).copy(anschlussId = anschluss.id)
    }

private fun KnotenDaten.anschlussReferenz(
    richtung: AnschlussRichtung,
    index: Int,
    zustand: KarteZustand,
): AnschlussReferenz {
    val anzahl = when (richtung) {
        AnschlussRichtung.Eingang -> eingängeGeordnet.size
        AnschlussRichtung.Ausgang -> ausgängeGeordnet.size
    }.coerceAtLeast(1)
    val anschlussId = when (richtung) {
        AnschlussRichtung.Eingang -> eingängeGeordnet.getOrNull(index)?.id
        AnschlussRichtung.Ausgang -> ausgängeGeordnet.getOrNull(index)?.id
    }.orEmpty()
    val weltPosition = PositionDaten(
        waagrecht = when (richtung) {
            AnschlussRichtung.Eingang -> position.waagrecht
            AnschlussRichtung.Ausgang -> position.waagrecht + fläche.waagrecht
        },
        senkrecht = position.senkrecht + fläche.senkrecht * (index + 1) / (anzahl + 1),
    )
    return AnschlussReferenz(
        knotenId = id,
        anschlussId = anschlussId,
        richtung = richtung,
        position = weltPosition.zuBildschirmOffset(zustand),
    )
}

private fun VerbindungDaten.startOffset(
    knotenNachId: Map<String, KnotenDaten>,
    zustand: KarteZustand,
): Offset? {
    val knoten = knotenNachId[quellKnotenId] ?: return null
    val index = knoten.ausgängeGeordnet.indexOfFirst { it.id == quellAnschlussId }.coerceAtLeast(0)
    return knoten.anschlussReferenz(AnschlussRichtung.Ausgang, index, zustand).position
}

private fun VerbindungDaten.endeOffset(
    knotenNachId: Map<String, KnotenDaten>,
    zustand: KarteZustand,
): Offset? {
    val knoten = knotenNachId[zielKnotenId] ?: return null
    val index = knoten.eingängeGeordnet.indexOfFirst { it.id == zielAnschlussId }.coerceAtLeast(0)
    return knoten.anschlussReferenz(AnschlussRichtung.Eingang, index, zustand).position
}

private fun AnschlussReferenz.istKompatibelMit(ziel: AnschlussReferenz): Boolean =
    knotenId != ziel.knotenId && richtung != ziel.richtung

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

private fun Offset.nächsterAnschluss(
    anschlüsse: List<AnschlussReferenz>,
    maxAbstand: Float,
): AnschlussReferenz? = anschlüsse
    .map { it to hypot(x - it.position.x, y - it.position.y) }
    .filter { it.second <= maxAbstand }
    .minByOrNull { it.second }
    ?.first

private fun Offset.treffer(
    knoten: List<KnotenDaten>,
    verbindungen: List<VerbindungDaten>,
    anschlüsse: List<AnschlussReferenz>,
    zustand: KarteZustand,
): KartenTreffer {
    nächsterAnschluss(anschlüsse, maxAbstand = 16f)?.let {
        return KartenTreffer.Anschluss(it.knotenId, it.anschlussId, it.richtung)
    }

    knoten.firstOrNull { enthältBildschirmPunkt(it, zustand) }?.let {
        return KartenTreffer.Knoten(it.id)
    }

    val knotenNachId = knoten.associateBy { it.id }
    verbindungen.firstOrNull { verbindung ->
        val start = verbindung.startOffset(knotenNachId, zustand)
        val ende = verbindung.endeOffset(knotenNachId, zustand)
        start != null && ende != null && abstandZuBezier(start, ende) <= 8f
    }?.let {
        return KartenTreffer.Verbindung(it.id)
    }

    return KartenTreffer.Hintergrund
}

private fun Offset.enthältBildschirmPunkt(knoten: KnotenDaten, zustand: KarteZustand): Boolean {
    val linksOben = knoten.position.zuBildschirmOffset(zustand)
    val zoom = zustand.zoomSicher()
    return x in linksOben.x..(linksOben.x + knoten.fläche.waagrecht * zoom) &&
        y in linksOben.y..(linksOben.y + knoten.fläche.senkrecht * zoom)
}

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

private fun kubisch(p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float): Offset {
    val u = 1f - t
    return p0 * u.pow(3) + p1 * (3f * u.pow(2) * t) + p2 * (3f * u * t.pow(2)) + p3 * t.pow(3)
}

private fun Offset.abstandZuSegment(a: Offset, b: Offset): Float {
    val dx = b.x - a.x
    val dy = b.y - a.y
    if (dx == 0f && dy == 0f) return hypot(x - a.x, y - a.y)
    val t = (((x - a.x) * dx + (y - a.y) * dy) / (dx * dx + dy * dy)).coerceIn(0f, 1f)
    val projektion = Offset(a.x + t * dx, a.y + t * dy)
    return hypot(x - projektion.x, y - projektion.y)
}

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
                position = PositionDaten(40f, 80f),
                ausgänge = listOf(AusgangDaten("out", "Weiter")),
            ),
            KnotenDaten(
                id = "satz",
                name = "Satz",
                position = PositionDaten(300f, 360f),
                eingänge = listOf(EingangDaten("in", "Vorher")),
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
