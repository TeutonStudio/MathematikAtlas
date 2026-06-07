package com.TeutonStudio.KnotenKartenVerwalter.daten.fix

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import com.TeutonStudio.KnotenKartenVerwalter.KnotenArt
import com.TeutonStudio.KnotenKartenVerwalter.Rechteck
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.AusgabeKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BasisKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.EingabeKnoten


open class KnotenZustand(

)

/**
 * Rechnet ein Bildschirmdelta mit dem aktuellen Zoom in ein Weltdelta um.
 */
/*public operator fun Offset.div(other: KarteZustand): Offset {
    val zoom = other.zoom.takeIf { it > 0f } ?: 1f
    return Offset(this.x / zoom, this.y / zoom)
}*/


/**
 * Fachlicher Zustand eines Knotens.
 *
 * Die `position` beschreibt die linke obere Ecke in Weltkoordinaten. Die
 * Anschlüsse werden getrennt nach Eingängen und Ausgängen gespeichert, damit die
 * UI sie links und rechts am Knotenrahmen anordnen kann.
 */
open class KnotenDaten(
    override val id: String,
    override val klasse: KnotenArt? = BasisKnoten.KNOTEN_ART,
    open val name: String,
    open var position: KartenPosition = Offset(0f, 0f), // Mitte des Knotens
    open val dimension: Rechteck = IntSize(180, 96),
//    open val art: String = "default",
    open var ausgewaehlt: Boolean = false,
    open val beweglich: Boolean = true,
    open val data: Map<String, Any> = emptyMap(),
): GraphDaten {
    constructor(
        daten: KnotenDaten,
        id: String? = null,
        klasse: KnotenArt? = null,
        name: String? = null,
        position: KartenPosition? = null,
        dimension: Rechteck? = null,
//        art: String? = null,
        ausgewaehlt: Boolean? = null,
        beweglich: Boolean? = null,
        data: Map<String, Any>? = null,
    ): this(
        id ?: daten.id,
        klasse ?: daten.klasse,
        name ?: daten.name,
        position ?: daten.position,
        dimension ?: daten.dimension,
//        art ?: daten.art,
        ausgewaehlt ?: daten.ausgewaehlt,
        beweglich ?: daten.beweglich,
        data ?: daten.data,
    )

    fun copy(
        id: String = this.id,
        klasse: KnotenArt? = this.klasse,
        name: String = this.name,
        position: KartenPosition = this.position,
        dimension: Rechteck = this.dimension,
//        art: String = this.art,
        ausgewaehlt: Boolean = this.ausgewaehlt,
        beweglich: Boolean = this.beweglich,
        data: Map<String, Any> = this.data,
    ): KnotenDaten = KnotenDaten(
        id = id,
        klasse = klasse,
        name = name,
        position = position,
        dimension = dimension,
//        art = art,
//        ausgewaehlt = ausgewaehlt,
        beweglich = beweglich,
        data = data,
    )

/*    fun save(betrachtungsModell: LiveKnoten) = copy(
        dimension = betrachtungsModell.dimension,
        position = betrachtungsModell.position,
        ausgewaehlt = betrachtungsModell.ausgewaehlt,
        name = betrachtungsModell.name,
        data = betrachtungsModell.data
    )*/
}

/**
 *
 */
open class EingabeDaten(
    override val id: String,
    override val klasse: KnotenArt? = EingabeKnoten.KNOTEN_ART,
    override val name: String,
    override var position: KartenPosition = Offset(0f, 0f),
    override val dimension: Rechteck = IntSize(180, 96),
//    override val art: String = "default",
    override var ausgewaehlt: Boolean = false,
    override val beweglich: Boolean = true,
    override val data: Map<String, Any> = emptyMap(),
    val anschlussLabel: List<String> = listOf("anschluss"),
): KnotenDaten(
    id,klasse,name,position,dimension,ausgewaehlt,beweglich,data
) {

    fun copy(
        id: String = this.id,
        klasse: KnotenArt? = this.klasse,
        name: String = this.name,
        position: KartenPosition = this.position,
        fläche: Rechteck = this.dimension,
//        art: String = this.art,
        ausgewaehlt: Boolean = this.ausgewaehlt,
        beweglich: Boolean = this.beweglich,
        data: Map<String, Any> = this.data,
        anschlussLabel: List<String> = this.anschlussLabel,
    ): EingabeDaten = EingabeDaten(
        id = id,
        klasse = klasse,
        name = name,
        position = position,
        dimension = fläche,
//        art = art,
        ausgewaehlt = ausgewaehlt,
        beweglich = beweglich,
        data = data,
        anschlussLabel = anschlussLabel,
    )
}

/**
 *
 */
open class AusgabeDaten(
    override val id: String,
    override val klasse: KnotenArt? = AusgabeKnoten.KNOTEN_ART,
    override val name: String,
    override var position: KartenPosition = Offset(0f, 0f),
    override val dimension: Rechteck = IntSize(180, 96),
//    override val art: String = "default",
    override var ausgewaehlt: Boolean = false,
    override val beweglich: Boolean = true,
    override val data: Map<String, Any> = emptyMap(),
    val anschlussLabel: List<String> = listOf("anschluss"),
): KnotenDaten(
    id,klasse,name,position,dimension,ausgewaehlt,beweglich,data
) {

    fun copy(
        id: String = this.id,
        klasse: KnotenArt? = this.klasse,
        name: String = this.name,
        position: KartenPosition = this.position,
        fläche: Rechteck = this.dimension,
//        art: String = this.art,
        ausgewaehlt: Boolean = this.ausgewaehlt,
        beweglich: Boolean = this.beweglich,
        data: Map<String, Any> = this.data,
        anschlussLabel: List<String> = this.anschlussLabel,
    ): AusgabeDaten = AusgabeDaten(
        id = id,
        klasse = klasse,
        name = name,
        position = position,
        dimension = fläche,
//        art = art,
        ausgewaehlt = ausgewaehlt,
        beweglich = beweglich,
        data = data,
        anschlussLabel = anschlussLabel,
    )
}
