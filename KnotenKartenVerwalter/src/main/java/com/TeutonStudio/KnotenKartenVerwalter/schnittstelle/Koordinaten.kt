package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle

import androidx.compose.ui.geometry.Offset
import com.TeutonStudio.KnotenKartenVerwalter.daten.KarteZustand
import com.TeutonStudio.KnotenKartenVerwalter.daten.PositionDaten

/**
 * Zentrale Umrechnungen zwischen Weltkoordinaten und Bildschirmkoordinaten.
 *
 * Diese Datei dient als beschrifteter Zielort fuer die Koordinatenlogik. Die aktuelle Hauptkarte
 * besitzt noch interne Hilfsfunktionen; diese koennen schrittweise hierher verschoben werden.
 */
object KoordinatenUmrechnung {
    /** Rechnet eine Weltposition in eine Bildschirmposition um. */
    fun weltZuBildschirm(position: PositionDaten, zustand: KarteZustand): Offset {
        val zoom = zustand.zoom.takeIf { it > 0f } ?: 1f
        return Offset(
            x = position.waagrecht * zoom + zustand.verschiebung.x,
            y = position.senkrecht * zoom + zustand.verschiebung.y,
        )
    }

    /** Rechnet eine Bildschirmposition in eine Weltposition um. */
    fun bildschirmZuWelt(position: Offset, zustand: KarteZustand): PositionDaten {
        val zoom = zustand.zoom.takeIf { it > 0f } ?: 1f
        return PositionDaten(
            waagrecht = (position.x - zustand.verschiebung.x) / zoom,
            senkrecht = (position.y - zustand.verschiebung.y) / zoom,
        )
    }

    /** Rechnet eine Bildschirmbewegung in eine Bewegung in Weltkoordinaten um. */
    fun deltaZuWelt(delta: Offset, zustand: KarteZustand): PositionDaten {
        val zoom = zustand.zoom.takeIf { it > 0f } ?: 1f
        return PositionDaten(delta.x / zoom, delta.y / zoom)
    }
}
