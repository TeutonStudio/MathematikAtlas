package com.TeutonStudio.KnotenKartenVerwalter.daten

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
    x = (this.position.x * zustand.zoom + zustand.verschiebung.x).roundToInt(),
    y = (this.position.y * zustand.zoom + zustand.verschiebung.y).roundToInt(),
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
public operator fun Offset.div(other: KarteZustand): Offset {
    val zoom = other.zoom.takeIf { it > 0f } ?: 1f
    return Offset(this.x / zoom, this.y / zoom)
}

open class KnotenZustand(

)

/**
 * Fachlicher Zustand eines Knotens.
 *
 * Die `position` beschreibt die linke obere Ecke in Weltkoordinaten. Die
 * Anschlüsse werden getrennt nach Eingängen und Ausgängen gespeichert, damit die
 * UI sie links und rechts am Knotenrahmen anordnen kann.
 */
open class KnotenDaten(
    override val id: String,
    val name: String,
    val position: Offset = Offset(0f, 0f),
    val fläche: Offset = Offset(180f, 96f),
    val art: String = "default",
    val ausgewaehlt: Boolean = false,
    val beweglich: Boolean = true,
    val data: Map<String, Any> = emptyMap(),
): GraphDaten {
    constructor(
        daten: KnotenDaten,
        id: String? = null,
        name: String? = null,
        position: Offset? = null,
        fläche: Offset? = null,
        art: String? = null,
        ausgewaehlt: Boolean? = null,
        beweglich: Boolean? = null,
        data: Map<String, Any>? = null,
    ): this(
        id ?: daten.id,
        name ?: daten.name,
        position ?: daten.position,
        fläche ?: daten.fläche,
        art ?: daten.art,
        ausgewaehlt ?: daten.ausgewaehlt,
        beweglich ?: daten.beweglich,
        data ?: daten.data,
    )

    /** Kompatibilitätsname zur ReactFlow-Bezeichnung `type`. */
    val typ: String
        get() = art

    fun copy(
        id: String = this.id,
        name: String = this.name,
        position: Offset = this.position,
        fläche: Offset = this.fläche,
        art: String = this.art,
        ausgewaehlt: Boolean = this.ausgewaehlt,
        beweglich: Boolean = this.beweglich,
        data: Map<String, Any> = this.data,
    ): KnotenDaten = KnotenDaten(
        id = id,
        name = name,
        position = position,
        fläche = fläche,
        art = art,
        ausgewaehlt = ausgewaehlt,
        beweglich = beweglich,
        data = data,
    )
}
