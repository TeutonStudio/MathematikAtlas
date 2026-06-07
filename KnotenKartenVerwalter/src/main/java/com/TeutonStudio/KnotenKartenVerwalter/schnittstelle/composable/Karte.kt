package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.composable

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import com.TeutonStudio.KnotenKartenVerwalter.AuswahlÄndern
import com.TeutonStudio.KnotenKartenVerwalter.BildschirmPosition
import com.TeutonStudio.KnotenKartenVerwalter.KartenAktualisierung
import com.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import com.TeutonStudio.KnotenKartenVerwalter.KontextAktionAusführen
import com.TeutonStudio.KnotenKartenVerwalter.VerbindungErstellen
import com.TeutonStudio.KnotenKartenVerwalter.daten.AuswahlDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KarteZustand
import com.TeutonStudio.KnotenKartenVerwalter.erhalteNachBildPos
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.KartenTreffer
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Knoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.Verbindung
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.VerbindungsDrag
import com.TeutonStudio.KnotenKartenVerwalter.transformiere
import com.TeutonStudio.KnotenKartenVerwalter.verschiebe
import com.TeutonStudio.KnotenKartenVerwalter.zuComposable
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
    knoten: Iterable<Knoten>,
    verbindungen: Iterable<Verbindung>,
    zustand: KarteZustand = KarteZustand(),
    modifier: Modifier = Modifier,
    aktualisierung: KartenAktualisierung,
    onVerbindungErstellen: VerbindungErstellen = {},
    onKontextAktion: KontextAktionAusführen = {},
    onAuswahlÄndern: AuswahlÄndern = {},
) {
    val ctxPos = remember { mutableStateOf(IntOffset(0,0)) }
    var ctxKarte by remember { mutableStateOf(false) }
    var ctxKnoten by remember { mutableStateOf<Knoten?>(null) }
    var ctxVerbindung by remember { mutableStateOf<Verbindung?>(null) }
    val onDoupleTap = { position: Offset ->
        /* TODO
            Zoom und Verschiebung auf Inhalt.
        */
    }
    val onLongTap = { position: Offset ->
        ctxPos.value = position.round()
        val ziel = zustand.erhalteNachBildPos(ctxPos.value, knoten, verbindungen)
        if (ziel == null) ctxKarte = true; ctxKnoten = null; ctxVerbindung = null
        if (ziel is Knoten) ctxKnoten = ziel
        if (ziel is Verbindung) ctxVerbindung = ziel
        /* TODO
            Kontextfenster aufrufen
         */
    }
    val onTap = { position: Offset ->
        val ziel = zustand.erhalteNachBildPos(position.round(), knoten, verbindungen)
        if (ziel is Knoten) {
            onAuswahlÄndern(AuswahlDaten(knotenIds = setOf(ziel.daten.id)))
        }
        if (ziel is Verbindung) {
            onAuswahlÄndern(AuswahlDaten(verbindungIds = setOf(ziel.daten.id)))
        }
    }

    Box(modifier = modifier
        .pointerInput(daten.id) {
            detectTapGestures(
                onDoubleTap = onDoupleTap,
                onLongPress = onLongTap,
                onTap = onTap,
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
            detectTransformGestures { zentrum, pan, zoomDelta, rot ->
                zustand.transformiere(pan,zoomDelta)
/*                        if (hintergrundGestenBlockiert) return@detectTransformGestures
                        kontextMenü = null*/
//                    ansicht = aktuelleAnsicht.transformiereUm(zentrum, pan, zoomÄnderung) TODO
            }
        }
        .pointerInput(daten.id) { // TODO noch notwendig, da nun onLongPress
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
        },) {
        // TODO Hintergrund zeichnen
        verbindungen.zuComposable({ d -> Modifier.fillMaxSize() })
        knoten.zuComposable({ d -> Modifier})

        if (ctxKarte) {
            // TODO Karten kontextfenster
        } else if (ctxKnoten != null) {
            // TODO Knoten kontextfenstzer
        } else if (ctxVerbindung != null) {
            // TODO Verbindung kontextfenster
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
            text = "Schließen",
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