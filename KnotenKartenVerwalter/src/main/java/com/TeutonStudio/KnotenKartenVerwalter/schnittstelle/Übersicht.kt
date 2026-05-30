package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.TeutonStudio.KnotenKartenVerwalter.daten.KarteDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.KarteZustand

/**
 * Rendert nur die Minimap einer Karte.
 */
@Composable
public fun KarteDaten.zuComposable(
    modifier: Modifier = Modifier,
    zustand: KarteZustand = KarteZustand(),
) = Übersicht(this, zustand, modifier)

/**
 * Rendert die interaktive Minimap mit Zugriff auf die Größe der Hauptkarte.
 */
@Composable
public fun KarteDaten.zuComposable(
    modifier: Modifier = Modifier,
    zustand: KarteZustand = KarteZustand(),
    fläche: IntSize,
    onAnsichtÄndern: (KarteZustand) -> Unit,
) = Übersicht(this, zustand, modifier, fläche, onAnsichtÄndern)

/**
 * Minimap der Knotenkarte.
 *
 * Sie stellt den gesamten Graphen und den aktuell sichtbaren Weltbereich dar.
 * Drag auf der Minimap verschiebt den Hauptviewport.
 */
@Composable
private fun Übersicht(
    daten: KarteDaten,
    zustand: KarteZustand,
    modifier: Modifier = Modifier,
    fläche: IntSize = IntSize.Zero,
    onAnsichtÄndern: ((KarteZustand) -> Unit)? = null,
) {
    // Die Minimap berücksichtigt sowohl Graphgrenzen als auch den sichtbaren
    // Bereich, damit der blaue Viewport-Rahmen immer im Minimap-Koordinatensystem liegt.
    val graphGrenzen = daten.knoten.grenzen(padding = 80f) ?: return
    val sichtGrenzen = zustand.sichtbarerWeltBereich(fläche)
    val miniGrenzen = graphGrenzen.vereinigtMit(sichtGrenzen)
    var miniFläche by remember { mutableStateOf(IntSize.Zero) }

    // Der Drag-Handler soll während einer laufenden Geste nicht neu starten,
    // aber trotzdem den aktuellen Viewport verwenden.
    val aktuellerZustand by rememberUpdatedState(zustand)
    val aktuelleGrenzen by rememberUpdatedState(miniGrenzen)
    val aktuelleFläche by rememberUpdatedState(fläche)

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Canvas(
            modifier = Modifier
                .size(180.dp, 120.dp)
                .background(Color.White, RoundedCornerShape(8.dp))
                .onSizeChanged { miniFläche = it }
                .pointerInput(daten.id, miniFläche) {
                    // DragStart setzt den Viewport sofort, Drag hält ihn während
                    // gedrückter Maustaste oder Fingerbewegung kontinuierlich nach.
                    detectDragGestures(
                        onDragStart = { position ->
                            onAnsichtÄndern?.invoke(
                                aktuellerZustand.ansichtFürMiniMapPosition(
                                    position,
                                    aktuelleGrenzen,
                                    miniFläche,
                                    aktuelleFläche,
                                ),
                            )
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            onAnsichtÄndern?.invoke(
                                aktuellerZustand.ansichtFürMiniMapPosition(
                                    change.position,
                                    aktuelleGrenzen,
                                    miniFläche,
                                    aktuelleFläche,
                                ),
                            )
                        },
                    )
                },
        ) {
            val projektion = MiniMapProjektion(miniGrenzen, size)

            // Hintergrund der Minimap.
            drawRect(
                color = Color(0xFFF8FAFC),
                topLeft = Offset.Zero,
                size = size,
            )

            // Knoten werden als stark vereinfachte Rechtecke dargestellt.
            daten.knoten.forEach { knoten ->
                val linksOben = projektion.zuMiniMap(knoten.position.waagrecht, knoten.position.senkrecht)
                drawRect(
                    color = if (knoten.ausgewaehlt) Color(0xFF2563EB) else Color(0xFF94A3B8),
                    topLeft = linksOben,
                    size = Size(
                        width = (knoten.fläche.waagrecht * projektion.skalierung).coerceAtLeast(3f),
                        height = (knoten.fläche.senkrecht * projektion.skalierung).coerceAtLeast(3f),
                    ),
                )
            }

            // Das blaue Rechteck beschreibt den sichtbaren Bereich der Hauptkarte.
            // clipRect verhindert, dass Rahmenanteile außerhalb der Minimap sichtbar werden.
            if (sichtGrenzen != null) {
                val linksOben = projektion.zuMiniMap(sichtGrenzen.links, sichtGrenzen.oben)
                val rechtsUnten = projektion.zuMiniMap(sichtGrenzen.rechts, sichtGrenzen.unten)
                val viewportGröße = Size(
                    width = rechtsUnten.x - linksOben.x,
                    height = rechtsUnten.y - linksOben.y,
                )
                clipRect(0f, 0f, size.width, size.height) {
                    drawRect(
                        color = Color(0x552563EB),
                        topLeft = linksOben,
                        size = viewportGröße,
                    )
                    drawRect(
                        color = Color(0xFF2563EB),
                        topLeft = linksOben,
                        size = viewportGröße,
                        style = Stroke(width = 2f),
                    )
                }
            }

            // Dünner Abschlussrahmen der Minimap.
            drawRect(
                color = Color(0xFFE2E8F0),
                topLeft = Offset.Zero,
                size = size,
                style = Stroke(width = 1f),
            )
        }
    }
}

/**
 * Projektionsdaten zwischen Weltkoordinaten und Minimap-Koordinaten.
 */
private data class MiniMapProjektion(
    val grenzen: KartenGrenzen,
    val größe: Size,
) {
    private val padding = 10f
    private val breite = (grenzen.rechts - grenzen.links).coerceAtLeast(1f)
    private val höhe = (grenzen.unten - grenzen.oben).coerceAtLeast(1f)
    val skalierung = minOf(
        (größe.width - padding * 2f) / breite,
        (größe.height - padding * 2f) / höhe,
    ).coerceAtLeast(0.01f)
    private val ursprung = Offset(
        x = (größe.width - breite * skalierung) / 2f,
        y = (größe.height - höhe * skalierung) / 2f,
    )

    /**
     * Rechnet eine Weltposition in eine Position innerhalb der Minimap um.
     */
    fun zuMiniMap(x: Float, y: Float): Offset = Offset(
        x = ursprung.x + (x - grenzen.links) * skalierung,
        y = ursprung.y + (y - grenzen.oben) * skalierung,
    )

    /**
     * Rechnet eine Minimap-Position zurück in Weltkoordinaten.
     */
    fun zuWelt(position: Offset): Offset = Offset(
        x = grenzen.links + (position.x - ursprung.x) / skalierung,
        y = grenzen.oben + (position.y - ursprung.y) / skalierung,
    )
}

/**
 * Berechnet den in der Hauptkarte sichtbaren Weltbereich.
 */
private fun KarteZustand.sichtbarerWeltBereich(fläche: IntSize): KartenGrenzen? {
    if (fläche.width <= 0 || fläche.height <= 0) return null
    val linksOben = Offset.Zero.zuWeltPosition(this)
    val rechtsUnten = Offset(fläche.width.toFloat(), fläche.height.toFloat()).zuWeltPosition(this)
    return KartenGrenzen(
        links = minOf(linksOben.x, rechtsUnten.x),
        oben = minOf(linksOben.y, rechtsUnten.y),
        rechts = maxOf(linksOben.x, rechtsUnten.x),
        unten = maxOf(linksOben.y, rechtsUnten.y),
    )
}

/**
 * Vereinigt zwei Welt-Rechtecke.
 */
private fun KartenGrenzen.vereinigtMit(andere: KartenGrenzen?): KartenGrenzen {
    if (andere == null) return this
    return KartenGrenzen(
        links = minOf(links, andere.links),
        oben = minOf(oben, andere.oben),
        rechts = maxOf(rechts, andere.rechts),
        unten = maxOf(unten, andere.unten),
    )
}

/**
 * Erzeugt einen neuen Hauptviewport aus einer Position innerhalb der Minimap.
 */
private fun KarteZustand.ansichtFürMiniMapPosition(
    position: Offset,
    grenzen: KartenGrenzen,
    miniFläche: IntSize,
    fläche: IntSize,
): KarteZustand {
    if (miniFläche.width <= 0 || miniFläche.height <= 0 || fläche.width <= 0 || fläche.height <= 0) return this

    val weltPosition = MiniMapProjektion(
        grenzen = grenzen,
        größe = Size(miniFläche.width.toFloat(), miniFläche.height.toFloat()),
    ).zuWelt(position)

    return copy(
        verschiebung = Offset(
            x = fläche.width / 2f - weltPosition.x * zoomSicher(),
            y = fläche.height / 2f - weltPosition.y * zoomSicher(),
        ),
    )
}
