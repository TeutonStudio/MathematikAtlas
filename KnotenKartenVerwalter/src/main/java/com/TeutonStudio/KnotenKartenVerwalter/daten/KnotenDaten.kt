package com.TeutonStudio.KnotenKartenVerwalter.daten

import androidx.compose.runtime.retain.retain
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/**
 * Rechnet die Weltposition eines Knotens in eine Bildschirmposition um.
 *
 * Diese Hilfsfunktion wird von älterem UI-Code verwendet. Neue Kartenlogik nutzt
 * zusätzlich die Transformationsfunktionen in `schnittstelle/Karte.kt`.
 */
public fun KnotenDaten.zuIntOffset(zustand: KarteZustand): IntOffset = IntOffset(
    x = (this.position.waagrecht * zustand.zoom + zustand.verschiebung.x).roundToInt(),
    y = (this.position.senkrecht * zustand.zoom + zustand.verschiebung.y).roundToInt(),
)

/**
 * Kurzform für eine reine Verschiebung ohne expliziten Zoom.
 */
public fun KnotenDaten.zuIntOffset(verschiebung: Offset): IntOffset = zuIntOffset(
    KarteZustand(verschiebung = verschiebung),
)

/**
 * Rechnet ein Bildschirmdelta mit dem aktuellen Zoom in ein Weltdelta um.
 */
public operator fun Offset.div(other: KarteZustand): PositionDaten {
    val zoom = other.zoom.takeIf { it > 0f } ?: 1f
    return PositionDaten(this.x / zoom, this.y / zoom)
}

/**
 * Position in Weltkoordinaten.
 */
data class PositionDaten(
    val waagrecht: Float,
    val senkrecht: Float,
) {
    /** Addiert zwei Weltpositionen komponentenweise. */
    operator fun plus(other: PositionDaten) = PositionDaten(waagrecht+other.waagrecht,senkrecht+other.senkrecht)

    /** Subtrahiert zwei Weltpositionen komponentenweise. */
    operator fun minus(other: PositionDaten) = PositionDaten(waagrecht-other.waagrecht,senkrecht-other.senkrecht)
}

/**
 * Größe eines Knotens in Weltkoordinaten.
 */
data class FlächeDaten(
    val waagrecht: Float,
    val senkrecht: Float,
) {

}

/**
 * Fachlicher Zustand eines Knotens.
 *
 * Die `position` beschreibt die linke obere Ecke in Weltkoordinaten. Die
 * Anschlüsse werden getrennt nach Eingängen und Ausgängen gespeichert, damit die
 * UI sie links und rechts am Knotenrahmen anordnen kann.
 */
data class KnotenDaten(
    val id: String,
    val name: String,
    val position: PositionDaten = PositionDaten(0f, 0f),
    val fläche: FlächeDaten = FlächeDaten(180f, 96f),
    val typ: String = "default",
    val eingänge: List<EingangDaten> = emptyList(),
    val ausgänge: List<AusgangDaten> = emptyList(),
    val ausgewaehlt: Boolean = false,
    val beweglich: Boolean = true,
//    val bewegt: Boolean = false, // Eher für schnittstelle relevant.
) {
    /**
     * Gibt die Anschlüsse passend zur gewünschten Richtung zurück.
     */
    public fun erhalteAnschlüsseGeordnet(richtung: AnschlussRichtung): List<AnschlussDaten> = when(richtung) {
        AnschlussRichtung.Eingang -> eingängeGeordnet
        AnschlussRichtung.Ausgang -> ausgängeGeordnet
    }

    /**
     * Sortierte Eingänge. Aktuell entspricht die Render-Reihenfolge der
     * gespeicherten Listen-Reihenfolge.
     */
    val eingängeGeordnet: List<EingangDaten>
        get() = eingänge

    /**
     * Sortierte Ausgänge. Aktuell entspricht die Render-Reihenfolge der
     * gespeicherten Listen-Reihenfolge.
     */
    val ausgängeGeordnet: List<AusgangDaten>
        get() = ausgänge
}
