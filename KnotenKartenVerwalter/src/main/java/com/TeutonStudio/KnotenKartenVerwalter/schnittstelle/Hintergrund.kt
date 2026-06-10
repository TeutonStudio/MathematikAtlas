package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.TeutonStudio.KnotenKartenVerwalter.daten.karte.KarteZustand

/**
 * Hintergrund-Layer der Karte.
 *
 * Diese Datei ist fuer Raster, Punktehintergrund und spaetere Snap-to-Grid-Anzeige vorgesehen.
 * Interaktionen wie Pan oder Kontextklick bleiben im Karten-Root, damit die Layer nicht
 * gegeneinander konkurrieren.
 */
@Composable
internal fun KartenHintergrund(
    zustand: KarteZustand,
    rasterGroesse: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        // Platzhalter: Der Parameterzugriff verhindert, dass das Geruest unbenutzte Werte versteckt.
        val zoom = 1f // zustand.zoom.takeIf { it > 0f } ?: 1f
        val abstand = (rasterGroesse * zoom).coerceAtLeast(8f)

        // Ein sehr dezenter Rahmen macht den Layer im Debugging sichtbar, ohne schon ein fertiges
        // Rasterdesign festzulegen.
        drawRect(color = Color.Transparent)

        // Die eigentlichen Rasterpunkte oder Rasterlinien werden spaeter aus sichtbarem Weltbereich,
        // Viewport-Verschiebung und `abstand` berechnet.
        @Suppress("UNUSED_VARIABLE")
        val geplanterRasterAbstand = abstand
    }
}
