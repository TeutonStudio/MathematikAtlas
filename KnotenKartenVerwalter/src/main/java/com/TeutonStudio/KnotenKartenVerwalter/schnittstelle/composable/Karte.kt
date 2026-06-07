package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.composable

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import com.TeutonStudio.KnotenKartenVerwalter.AuswahlÄndern
import com.TeutonStudio.KnotenKartenVerwalter.KartenAktualisierung
import com.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import com.TeutonStudio.KnotenKartenVerwalter.KnotenFabrik
import com.TeutonStudio.KnotenKartenVerwalter.KnotenPosition
import com.TeutonStudio.KnotenKartenVerwalter.KontextAktionAusführen
import com.TeutonStudio.KnotenKartenVerwalter.VerbindungErstellen
import com.TeutonStudio.KnotenKartenVerwalter.VerbindungFabrik
import com.TeutonStudio.KnotenKartenVerwalter.daten.AuswahlDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KarteZustand
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.erhalteNachBildPos
import com.TeutonStudio.KnotenKartenVerwalter.erzeugeKnoten
import com.TeutonStudio.KnotenKartenVerwalter.erzeugeVerbindung
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.KartenTreffer
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Knoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Verbindung
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.VerbindungsDrag
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.zuComposable
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.zuComposable
import kotlin.math.roundToInt

/**
 * Position und Ziel des aktuell geoeffneten Kontextmenues.
 */
private data class KontextMenüZustand(
    val position: Offset,
    val ziel: KartenTreffer,
    val weltPosition: Offset,
)

/**
 * Zentrale Kartenoberflaeche.
 *
 * Diese Datei enthaelt nur Compose:
 * - Container
 * - Pointer-Events
 * - Knoten-/Verbindungs-Rendering
 * - Minimap
 * - Kontrollleiste
 * - Kontextmenue
 *
 * Graph-, Hit-Test- und Verbindungslogik liegen in KartenGraph.kt und
 * VerbindungsZiehen.kt. Eine Karte, die nicht alles selbst macht. Skandal.
 */
@Composable
internal fun KartenOberfläche(
    daten: KarteDaten,
    zustand: KarteZustand = KarteZustand(),
    knotenFabrik: KnotenFabrik,
    verbindungFabrik: VerbindungFabrik,
    modifier: Modifier = Modifier,
    aktualisierung: KartenAktualisierung,
    onVerbindungErstellen: VerbindungErstellen = {},
    onKontextAktion: KontextAktionAusführen = {},
    onAuswahlÄndern: AuswahlÄndern = {},
) {
    var fläche by remember { mutableStateOf(daten.größe) }
    var ansicht by remember(daten.id) {
        mutableStateOf(
            KarteZustand(
                ansicht = daten.ansicht,
                zeigeÜbersicht = zustand.zeigeÜbersicht,
                zeigeKontrollLeiste = zustand.zeigeKontrollLeiste,
            ),
        )
    }
    var gezogeneKnoten by remember(daten.id) { mutableStateOf(emptyMap<String, KartenPosition>()) }
    var verbindungsDrag by remember { mutableStateOf<VerbindungsDrag?>(null) }
    var kontextMenü by remember { mutableStateOf<KontextMenüZustand?>(null) }
    var blockiereHintergrundGesten by remember { mutableStateOf(false) }
    var ziehtAnschluss by remember { mutableStateOf(false) }

/*    val sichtbareKnotenDaten = daten.knoten.map { knoten ->
        knoten.
        knoten.copy(
        position = gezogeneKnoten[knoten.id] ?: knoten.position,
        ausgewaehlt = knoten.id in zustand.auswahl.knotenIds || knoten.ausgewaehlt,

    ) }*/
    val sichtbareKnoten = daten.knoten.mapNotNull { knotenFabrik.erzeugeKnoten(it) }

/*    val sichtbareVerbindungenDaten = daten.verbindungen.map { verbindung -> verbindung.copy(
        ausgewaehlt = verbindung.id in zustand.auswahl.verbindungIds || verbindung.ausgewaehlt,
    ) }*/
    val sichtbareVerbindungen = daten.verbindungen.mapNotNull { verbindungFabrik.erzeugeVerbindung(it) }

/*    val sichtbareDaten = KarteDaten(
        daten,
        knoten = sichtbareKnotenDaten,
        verbindungen = sichtbareVerbindungenDaten,
    )*/

    val sichtbarerZustand = KarteZustand(
        ansicht.ansicht,
        zeigeÜbersicht = zustand.zeigeÜbersicht,
        zeigeKontrollLeiste = zustand.zeigeKontrollLeiste,
        auswahl = zustand.auswahl,
    )

    val density = LocalDensity.current

    val aktuelleAnsicht by rememberUpdatedState(sichtbarerZustand)
    val aktuellerDrag by rememberUpdatedState(verbindungsDrag)
    val hintergrundGestenBlockiert by rememberUpdatedState(blockiereHintergrundGesten)

/*    LaunchedEffect(daten.id, fläche) { TODO
        if (fläche.width > 0 && fläche.height > 0 && daten.knoten.isNotEmpty()) {
            ansicht = daten.zoomAufInhalt(fläche, ansicht)
        }
    }*/

/*    fun öffneKontextMenü(position: Offset) { TODO
        val ziel = aktuellerGraph.treffer(position)
        kontextMenü = KontextMenüZustand(
            position = position,
            ziel = ziel,
            weltPosition = position.zuWeltPosition(aktuelleAnsicht).zuWeltOffset(),
        )
    }*/

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { fläche = it }
            .background(Color(0xFFF8FAFC))
            .pointerInput(daten.id) {
                detectTapGestures(
                    onDoubleTap = { position ->
                        /* TODO
                            Zoom und Verschiebugn auf Inhalt.
                        */
                    },
                    onLongPress = { position ->
                        /* TODO
                            Kontextfenster aufrufen
                         */
                    },
                    onTap = { position ->
                        val ziel = sichtbarerZustand.erhalteNachBildPos(
                            position.round(),
                            sichtbareKnoten,
                            sichtbareVerbindungen
                        )
                        if (ziel is Knoten) {
                            onAuswahlÄndern(AuswahlDaten(knotenIds = setOf(ziel.daten.id)))
                        }
                        if (ziel is Verbindung) {
                            onAuswahlÄndern(AuswahlDaten(verbindungIds = setOf(ziel.daten.id)))
                        }
                    },
                )
            }
            .pointerInteropFilter { ereignis -> // TODO verstehen warum existent
                val sekundär = ereignis.buttonState and MotionEvent.BUTTON_SECONDARY != 0
                if (
                    sekundär &&
                    (
                            ereignis.actionMasked == MotionEvent.ACTION_DOWN ||
                                    ereignis.actionMasked == MotionEvent.ACTION_BUTTON_PRESS
                            )
                ) {
//                    öffneKontextMenü(Offset(ereignis.x, ereignis.y)) TODO
                    true
                } else {
                    false
                }
            }
            .pointerInput(daten.id) {
                detectTransformGestures { zentrum, pan, zoomÄnderung, _ ->
                    if (hintergrundGestenBlockiert) return@detectTransformGestures
                    kontextMenü = null
//                    ansicht = aktuelleAnsicht.transformiereUm(zentrum, pan, zoomÄnderung) TODO
                }
            }
            .pointerInput(daten.id) {
                awaitPointerEventScope {
                    while (true) {
                        val ereignis = awaitPointerEvent()
                        if (
                            ereignis.type == PointerEventType.Press &&
                            ereignis.buttons.isSecondaryPressed
                        ) {
//                            öffneKontextMenü(ereignis.changes.first().position) TODO
                            ereignis.changes.forEach { it.consume() }
                        }
                    }
                }
            },
    ) {
        sichtbareVerbindungen.zuComposable(Modifier.fillMaxSize())
        sichtbareKnoten.zuComposable(
            { d -> Modifier},
            { d -> { a,idx -> Modifier }},
        )

/*        aktuellerDrag?.let { drag ->
            listOf(drag.zuVorschau()).zuComposable(Modifier.fillMaxSize())
        }*/

        /*sichtbareKnoten.forEach { knotenObjekt ->
            val knoten = knotenObjekt.daten
            val aktuellerKnoten by rememberUpdatedState(knoten)

            var knotenModifier = Modifier
                .offset { knoten.position.zuBildschirmIntOffset(sichtbarerZustand) }
                .size(
                    with(density) { (knoten.fläche.x * sichtbarerZustand.zoom).toDp() },
                    with(density) { (knoten.fläche.y * sichtbarerZustand.zoom).toDp() },
                )

            if (knoten.beweglich) {
                knotenModifier = knotenModifier.pointerInput(
                    daten.id,
                    knoten.id,
                    sichtbarerZustand.zoom,
                ) {
                    var startPosition = Offset.Zero
                    var gesamtDrag = Offset.Zero

                    detectDragGestures(
                        onDragStart = {
                            if (!ziehtAnschluss) {
                                val aktuellerKnotenId = aktuellerKnoten.id
                                kontextMenü = null
                                blockiereHintergrundGesten = true
                                startPosition = aktuellerKnoten.position
                                gesamtDrag = Offset.Zero
                                onAuswahlÄndern(
                                    AuswahlDaten(knotenIds = setOf(aktuellerKnotenId)),
                                )
                            }
                        },
                        onDrag = { change, dragAmount ->
                            if (!ziehtAnschluss) {
                                change.consume()
                                gesamtDrag += dragAmount

                                val neuePosition = startPosition +
                                        (gesamtDrag / sichtbarerZustand.zoomSicher())

                                val aktuellerKnotenId = aktuellerKnoten.id
                                gezogeneKnoten = gezogeneKnoten +
                                        (aktuellerKnotenId to neuePosition)

                                aktualisierung(aktuellerKnotenId, neuePosition)
                            }
                        },
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
                inhaltSkalierung = sichtbarerZustand.zoomSicher(),
                modifierAnschluss = { anschlussDaten, _ ->
                    val referenz = aktuellerGraph.anschlussReferenz(
                        knotenId = knoten.id,
                        anschlussId = anschlussDaten.id,
                    )

                    Modifier
                        .anschlussModifierSkaliert(sichtbarerZustand.zoomSicher())
                        .then(
                            if (referenz != null) {
                                Modifier.verbindungsZiehen(
                                    start = referenz,
                                    graph = { aktuellerGraph },
                                    onDragAendern = { verbindungsDrag = it },
                                    onZiehtAnschlussAendern = { ziehtAnschluss = it },
                                    onBlockiereHintergrundGestenAendern = {
                                        blockiereHintergrundGesten = it
                                    },
                                    onVerbindungErstellen = onVerbindungErstellen,
                                )
                            } else {
                                Modifier
                            },
                        )
                },
            )
        }*/

/*        if (sichtbarerZustand.zeigeÜbersicht) {
            sichtbareDaten.zuComposable(
                modifier = Modifier,
                zustand = sichtbarerZustand,
                fläche = fläche,
                onAnsichtÄndern = { neueAnsicht ->
                    ansicht = neueAnsicht
                },
            )
        }*/

/*        if (sichtbarerZustand.zeigeKontrollLeiste) {
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
        }*/

/*        kontextMenü?.let { menü ->
            KontextMenü(
                zustand = menü,
                onAktion = { aktion ->
                    onKontextAktion(
                        KartenKontextAktion(
                            ziel = menü.ziel,
                            weltPosition = menü.weltPosition,
                            aktion = aktion,
                        ),
                    )
                    kontextMenü = null
                },
                onSchließen = {
                    kontextMenü = null
                },
            )
        }*/
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
        is KartenTreffer.Knoten -> listOf(
            "Knoten auswaehlen",
            "Knoten duplizieren",
            "Knoten loeschen",
        )

        is KartenTreffer.Anschluss -> listOf("Verbindung starten", "Anschluss auswaehlen")
        is KartenTreffer.Verbindung -> listOf("Verbindung auswaehlen", "Verbindung loeschen")
    }

    Column(
        modifier = Modifier
            .offset {
                IntOffset(
                    zustand.position.x.roundToInt(),
                    zustand.position.y.roundToInt(),
                )
            }
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
            style = TextStyle(
                color = Color(0xFF6B7280),
                fontSize = 13.sp,
            ),
        )
    }
}

private fun Offset.zuWeltOffset(): Offset =
    Offset(x, y)

@Preview
@Composable
private fun KartePreview() {
/*    val daten = KarteDaten(
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
    )*/

/*    daten.zuComposable(
        aktualisierung = { _, _ -> },
    )*/
}