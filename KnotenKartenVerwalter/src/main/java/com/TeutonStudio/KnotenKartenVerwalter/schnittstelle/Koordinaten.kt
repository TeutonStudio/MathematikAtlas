package com.TeutonStudio.KnotenKartenVerwalter.schnittstelle


/**
 * Zentrale Umrechnungen zwischen Weltkoordinaten und Bildschirmkoordinaten.
 *
 * Diese Datei dient als beschrifteter Zielort fuer die Koordinatenlogik. Die aktuelle Hauptkarte
 * besitzt noch interne Hilfsfunktionen; diese koennen schrittweise hierher verschoben werden.
 */
/*object KoordinatenUmrechnung {
    *//** Rechnet eine Weltposition in eine Bildschirmposition um. *//*
    fun weltZuBildschirm(position: Offset, zustand: KarteZustand): Offset {
        val zoom = zustand.zoom.takeIf { it > 0f } ?: 1f
        return Offset(
            x = position.x * zoom + zustand.verschiebung.x,
            y = position.y * zoom + zustand.verschiebung.y,
        )
    }

    *//** Rechnet eine Bildschirmposition in eine Weltposition um. *//*
    fun bildschirmZuWelt(position: Offset, zustand: KarteZustand): Offset {
        val zoom = zustand.zoom.takeIf { it > 0f } ?: 1f
        return Offset(
            x = (position.x - zustand.verschiebung.x) / zoom,
            y = (position.y - zustand.verschiebung.y) / zoom,
        )
    }

    *//** Rechnet eine Bildschirmbewegung in eine Bewegung in Weltkoordinaten um. *//*
    fun deltaZuWelt(delta: Offset, zustand: KarteZustand): Offset {
        val zoom = zustand.zoom.takeIf { it > 0f } ?: 1f
        return Offset(delta.x / zoom, delta.y / zoom)
    }
}*/
