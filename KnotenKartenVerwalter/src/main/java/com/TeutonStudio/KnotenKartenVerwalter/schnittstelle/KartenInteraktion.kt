package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.ui.geometry.Offset
import com.TeutonStudio.KnotenKartenVerwalter.daten.KarteZustand

/**
 * Vorbereitete Datenstrukturen fuer Pointer-, Drag- und Zoom-Interaktionen der Karte.
 *
 * Die konkrete Gestenerkennung bleibt zunaechst in der bestehenden Kartenoberflaeche. Diese Datei
 * beschreibt die spaetere Trennung zwischen Gestenerkennung, internem Drag-Zustand und gemeldeten
 * Ereignissen.
 */
internal data class KnotenDragZustand(
    /** ID des Knotens, der gerade gezogen wird. */
    val knotenId: String,

    /** Weltposition des Knotens beim Start der Geste. */
    val startPosition: Offset,

    /** Bildschirmposition des Zeigers beim Start der Geste. */
    val startPointer: Offset,
)

/**
 * Berechnet einen neuen Viewport fuer einen Zoom um einen konkreten Bildschirmpunkt.
 *
 * Der Weltpunkt unter dem Zeiger bleibt dabei stabil. Das ist das Verhalten, das Benutzer von
 * Graph-Editoren und von React Flow erwarten.
 */
internal fun KarteZustand.zoomeUmPunkt(
    bildschirmPunkt: Offset,
    zoomFaktor: Float,
    minZoom: Float,
    maxZoom: Float,
): KarteZustand {
    val alterZoom = zoom.takeIf { it > 0f } ?: 1f
    val neuerZoom = (alterZoom * zoomFaktor).coerceIn(minZoom, maxZoom)
    val weltX = (bildschirmPunkt.x - verschiebung.x) / alterZoom
    val weltY = (bildschirmPunkt.y - verschiebung.y) / alterZoom
    return copy(
        zoom = neuerZoom,
        verschiebung = Offset(
            x = bildschirmPunkt.x - weltX * neuerZoom,
            y = bildschirmPunkt.y - weltY * neuerZoom,
        ),
    )
}
