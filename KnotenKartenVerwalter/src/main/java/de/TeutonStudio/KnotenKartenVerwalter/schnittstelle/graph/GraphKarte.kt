package de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteZustand
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.karten.Karte

/**
 * Ergänzt den Graph um eine Übersichtsdarstellung der aktuellen [Karte].
 */
interface GraphKarte {

    /**
     * Erstellt die interaktive Minimap der Karte.
     * Sie wird vom Graphen als Übersichtsebene neben der Kartensteuerung eingebunden.
     *
     * @receiver Karte, deren Inhalt zusammengefasst dargestellt wird
     * @param modifier äußerer Modifier der Übersicht
     */
    @Composable
    public fun Karte.zuÜbersicht(modifier: Modifier = Modifier) {
        if (!zustand.zeigeÜbersicht) return
        println(zustand.sichtbarerWeltBereich() != null)

        val sichtGrenzen = zustand.sichtbarerWeltBereich() ?: return

        /*
         * Der sichtbare Bereich wird immer einbezogen. Dadurch bleibt der
         * Viewport-Rahmen auch sichtbar, wenn die Ansicht weit vom Inhalt
         * wegbewegt wurde.
         */
        val miniGrenzen = (
                inhaltsGrenzen(puffer = 80f) ?: sichtGrenzen
                ).vereinigtMit(sichtGrenzen)

        var miniFläche by remember(daten.id) {
            mutableStateOf(IntSize.Zero)
        }

        /*
         * Der Pointer-Handler soll während einer laufenden Geste nicht
         * neu erzeugt werden, aber trotzdem die aktuellen Grenzen verwenden.
         */
        val aktuelleGrenzen by rememberUpdatedState(miniGrenzen)
        val aktuelleMiniFläche by rememberUpdatedState(miniFläche)

        val hintergrundFarbe = MaterialTheme.colorScheme.surface
        val knotenFarbe = MaterialTheme.colorScheme.outline
        val ausgewähltFarbe = MaterialTheme.colorScheme.primary
        val viewportFarbe =
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        val rahmenFarbe = MaterialTheme.colorScheme.outlineVariant

        Card(
            modifier = modifier.size(
                width = 180.dp,
                height = 120.dp,
            ),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged {
                        miniFläche = it
                    }
                    .pointerInput(daten.id) {
                        fun zentriereAufMiniMapPosition(position: Offset) {
                            val fläche = aktuelleMiniFläche

                            if (
                                fläche.width <= 0 ||
                                fläche.height <= 0
                            ) {
                                return
                            }

                            val projektion = MiniMapProjektion(
                                grenzen = aktuelleGrenzen,
                                größe = Size(
                                    width = fläche.width.toFloat(),
                                    height = fläche.height.toFloat(),
                                ),
                            )

                            zustand.zentriereAuf(
                                projektion.zuWelt(position),
                            )
                        }

                        detectDragGestures(
                            onDragStart = {
                                zentriereAufMiniMapPosition(it)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                zentriereAufMiniMapPosition(
                                    change.position,
                                )
                            },
                        )
                    },
            ) {
                val projektion = MiniMapProjektion(
                    grenzen = miniGrenzen,
                    größe = size,
                )

                drawRect(
                    color = hintergrundFarbe,
                )

                /*
                 * Die neue Struktur speichert die Auswahl zentral im
                 * KarteZustand. Deshalb wird nicht mehr ein Feld auf den
                 * Knotendaten geprüft.
                 */
                knoten.forEach { knoten ->
                    val rechteck = knoten.daten.dimension
                    val linksOben = projektion.zuMiniMap(
                        Offset(
                            x = rechteck.left,
                            y = rechteck.top,
                        ),
                    )

                    drawRect(
                        color = if (

                            zustand.auswahl.value.enthält(knoten)
                        ) {
                            ausgewähltFarbe
                        } else {
                            knotenFarbe
                        },
                        topLeft = linksOben,
                        size = Size(
                            width = (
                                    rechteck.width *
                                            projektion.skalierung
                                    ).coerceAtLeast(3f),
                            height = (
                                    rechteck.height *
                                            projektion.skalierung
                                    ).coerceAtLeast(3f),
                        ),
                    )
                }

                val viewportLinksOben = projektion.zuMiniMap(
                    Offset(
                        x = sichtGrenzen.left,
                        y = sichtGrenzen.top,
                    ),
                )

                val viewportRechtsUnten = projektion.zuMiniMap(
                    Offset(
                        x = sichtGrenzen.right,
                        y = sichtGrenzen.bottom,
                    ),
                )

                val viewportGröße = Size(
                    width = (
                            viewportRechtsUnten.x -
                                    viewportLinksOben.x
                            ).coerceAtLeast(0f),
                    height = (
                            viewportRechtsUnten.y -
                                    viewportLinksOben.y
                            ).coerceAtLeast(0f),
                )

                clipRect(
                    left = 0f,
                    top = 0f,
                    right = size.width,
                    bottom = size.height,
                ) {
                    drawRect(
                        color = viewportFarbe,
                        topLeft = viewportLinksOben,
                        size = viewportGröße,
                    )

                    drawRect(
                        color = ausgewähltFarbe,
                        topLeft = viewportLinksOben,
                        size = viewportGröße,
                        style = Stroke(
                            width = 2.dp.toPx(),
                        ),
                    )
                }

                drawRect(
                    color = rahmenFarbe,
                    style = Stroke(
                        width = 1.dp.toPx(),
                    ),
                )
            }
        }
    }
}

/**
 * Projektion zwischen Karten- und Minimap-Koordinaten.
 */
private data class MiniMapProjektion(
    val grenzen: Rect,
    val größe: Size,
) {
    private val padding = 10f

    private val breite = grenzen.width.coerceAtLeast(1f)
    private val höhe = grenzen.height.coerceAtLeast(1f)

    val skalierung: Float = minOf(
        (größe.width - padding * 2f).coerceAtLeast(1f) / breite,
        (größe.height - padding * 2f).coerceAtLeast(1f) / höhe,
    ).coerceAtLeast(0.0001f)

    private val ursprung = Offset(
        x = (größe.width - breite * skalierung) / 2f,
        y = (größe.height - höhe * skalierung) / 2f,
    )

    public fun zuMiniMap(
        weltPosition: Offset,
    ): Offset = Offset(
        x = ursprung.x + (weltPosition.x - grenzen.left) * skalierung,
        y = ursprung.y + (weltPosition.y - grenzen.top) * skalierung,
    )

    public fun zuWelt(
        miniMapPosition: Offset,
    ): Offset = Offset(
        x = grenzen.left +
                (
                        miniMapPosition.x -
                                ursprung.x
                        ) / skalierung,
        y = grenzen.top +
                (
                        miniMapPosition.y -
                                ursprung.y
                        ) / skalierung,
    )
}

/**
 * Ermittelt den sichtbaren Ausschnitt in Kartenkoordinaten.
 */
/*private fun KarteZustand.sichtbarerWeltBereich(): Rect? {
    if (
        dimension.width <= 0 ||
        dimension.height <= 0
    ) {
        return null
    }

    val sichererZoom = zoom.coerceAtLeast(0.0001f)

    return Rect(
        left = -pos.x / sichererZoom,
        top = -pos.y / sichererZoom,
        right = (
                dimension.width - pos.x
                ) / sichererZoom,
        bottom = (
                dimension.height - pos.y
                ) / sichererZoom,
    )
}*/

private fun Rect.vereinigtMit(
    anderes: Rect,
): Rect = Rect(
    left = minOf(left, anderes.left),
    top = minOf(top, anderes.top),
    right = maxOf(right, anderes.right),
    bottom = maxOf(bottom, anderes.bottom),
)
