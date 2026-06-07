package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KarteZustand

/**
 * Reine Hilfsfunktionen fuer die spaetere Kontrollleiste.
 *
 * `KontrollLeiste.kt` rendert die Buttons. Diese Datei sammelt die Berechnungen, die hinter
 * Aktionen wie Zoom rein, Zoom raus und FitView stehen sollen.
 */
/*
object KartenControlsRechner {
    */
/** Berechnet einen Viewport, der den uebergebenen Weltbereich sichtbar macht. *//*

    fun fitView(
        grenzen: KartenGrenzenDaten,
        flaeche: IntSize,
        aktuellerZustand: KarteZustand,
        padding: Float = 48f,
        minZoom: Float = 0.25f,
        maxZoom: Float = 3f,
    ): KarteZustand {
        if (flaeche.width <= 0 || flaeche.height <= 0) return aktuellerZustand
        val breite = grenzen.breite.coerceAtLeast(1f)
        val hoehe = grenzen.hoehe.coerceAtLeast(1f)
        val zoom = minOf(
            (flaeche.width - padding * 2f) / breite,
            (flaeche.height - padding * 2f) / hoehe,
        ).coerceIn(minZoom, maxZoom)
        return aktuellerZustand.copy(
            zoom = zoom,
            verschiebung = Offset(
                x = (flaeche.width - breite * zoom) / 2f - grenzen.links * zoom,
                y = (flaeche.height - hoehe * zoom) / 2f - grenzen.oben * zoom,
            ),
        )
    }
}
*/
