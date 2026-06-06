package com.TeutonStudio.KnotenKartenVerwalter.daten.fix

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import com.TeutonStudio.KnotenKartenVerwalter.daten.KarteZustand
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.GraphDaten
import kotlin.math.roundToInt
import kotlin.times



open class KnotenZustand(

)

typealias KnotenPosition = Offset
typealias KnotenRechteck = Offset

/**
 * Rechnet ein Bildschirmdelta mit dem aktuellen Zoom in ein Weltdelta um.
 */
public operator fun Offset.div(other: KarteZustand): Offset {
    val zoom = other.zoom.takeIf { it > 0f } ?: 1f
    return Offset(this.x / zoom, this.y / zoom)
}

/**
 * Rechnet die Weltposition eines Knotens in eine Bildschirmposition um.
 *
 * Diese Hilfsfunktion wird von älterem UI-Code verwendet. Neue Kartenlogik nutzt
 * zusätzlich die Transformationsfunktionen in `schnittstelle/Karte.kt`.
 */
public fun KnotenPosition.zuIntOffset(zustand: KarteZustand): IntOffset = IntOffset(
    x = (this.x * zustand.zoom + zustand.verschiebung.x).roundToInt(),
    y = (this.y * zustand.zoom + zustand.verschiebung.y).roundToInt(),
)

/**
 * Kurzform für eine reine Verschiebung ohne expliziten Zoom.
 */
public fun KnotenPosition.zuIntOffset(verschiebung: Offset): IntOffset = zuIntOffset(
    KarteZustand(verschiebung = verschiebung),
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
    override val klasse: String?,
    open val name: String,
    open val position: KnotenPosition = Offset(0f, 0f),
    open val fläche: KnotenRechteck = Offset(180f, 96f),
    open val art: String = "default",
    open val ausgewaehlt: Boolean = false,
    open val beweglich: Boolean = true,
    open val data: Map<String, Any> = emptyMap(),
): GraphDaten {
    constructor(
        daten: KnotenDaten,
        id: String? = null,
        klasse: String? = null,
        name: String? = null,
        position: KnotenPosition? = null,
        fläche: KnotenRechteck? = null,
        art: String? = null,
        ausgewaehlt: Boolean? = null,
        beweglich: Boolean? = null,
        data: Map<String, Any>? = null,
    ): this(
        id ?: daten.id,
        klasse ?: daten.klasse,
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
        klasse: String? = this.klasse,
        name: String = this.name,
        position: KnotenPosition = this.position,
        fläche: KnotenRechteck = this.fläche,
        art: String = this.art,
        ausgewaehlt: Boolean = this.ausgewaehlt,
        beweglich: Boolean = this.beweglich,
        data: Map<String, Any> = this.data,
    ): KnotenDaten = KnotenDaten(
        id = id,
        klasse = klasse,
        name = name,
        position = position,
        fläche = fläche,
        art = art,
        ausgewaehlt = ausgewaehlt,
        beweglich = beweglich,
        data = data,
    )
}

open class EingabeDaten(
    override val id: String,
    override val klasse: String?,
    override val name: String,
    override val position: KnotenPosition = Offset(0f, 0f),
    override val fläche: KnotenRechteck = Offset(180f, 96f),
    override val art: String = "default",
    override val ausgewaehlt: Boolean = false,
    override val beweglich: Boolean = true,
    override val data: Map<String, Any> = emptyMap(),
    val anschlussLabel: String = "anschluss",
): KnotenDaten(
    id,klasse,name,position,fläche,art,ausgewaehlt,beweglich,data
) {

    fun copy(
        id: String = this.id,
        klasse: String? = this.klasse,
        name: String = this.name,
        position: KnotenPosition = this.position,
        fläche: KnotenRechteck = this.fläche,
        art: String = this.art,
        ausgewaehlt: Boolean = this.ausgewaehlt,
        beweglich: Boolean = this.beweglich,
        data: Map<String, Any> = this.data,
        anschlussLabel: String = this.anschlussLabel,
    ): EingabeDaten = EingabeDaten(
        id = id,
        klasse = klasse,
        name = name,
        position = position,
        fläche = fläche,
        art = art,
        ausgewaehlt = ausgewaehlt,
        beweglich = beweglich,
        data = data,
        anschlussLabel = anschlussLabel,
    )
}

open class AusgabeDaten(
    override val id: String,
    override val klasse: String?,
    override val name: String,
    override val position: KnotenPosition = Offset(0f, 0f),
    override val fläche: KnotenRechteck = Offset(180f, 96f),
    override val art: String = "default",
    override val ausgewaehlt: Boolean = false,
    override val beweglich: Boolean = true,
    override val data: Map<String, Any> = emptyMap(),
    val anschlussLabel: String = "anschluss",
): KnotenDaten(
    id,klasse,name,position,fläche,art,ausgewaehlt,beweglich,data
) {

    fun copy(
        id: String = this.id,
        klasse: String? = this.klasse,
        name: String = this.name,
        position: KnotenPosition = this.position,
        fläche: KnotenRechteck = this.fläche,
        art: String = this.art,
        ausgewaehlt: Boolean = this.ausgewaehlt,
        beweglich: Boolean = this.beweglich,
        data: Map<String, Any> = this.data,
        anschlussLabel: String = this.anschlussLabel,
    ): AusgabeDaten = AusgabeDaten(
        id = id,
        klasse = klasse,
        name = name,
        position = position,
        fläche = fläche,
        art = art,
        ausgewaehlt = ausgewaehlt,
        beweglich = beweglich,
        data = data,
        anschlussLabel = anschlussLabel,
    )
}
