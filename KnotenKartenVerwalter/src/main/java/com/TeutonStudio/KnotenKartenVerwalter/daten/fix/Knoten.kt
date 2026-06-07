package com.TeutonStudio.KnotenKartenVerwalter.daten.fix

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import com.TeutonStudio.KnotenKartenVerwalter.KnotenAnschlüsse
import com.TeutonStudio.KnotenKartenVerwalter.KnotenArt
import com.TeutonStudio.KnotenKartenVerwalter.Rechteck
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.KnotenDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.AusgabeKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BasisKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.EingabeKnoten
import com.TeutonStudio.KnotenKartenVerwalter.toMutableMap


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
    open val name: String = "",

): GraphDaten {
    override val klasse: KnotenArt? = BasisKnoten.KNOTEN_ART
    open var position: KartenPosition = Offset(0f, 0f) // Mitte des Knotens
    open var dimension: Rechteck = IntSize(180, 96)
    //    open val art: String = "default",
    open var ausgewaehlt: Boolean = false
    open var beweglich: Boolean = true
    open val anschlüsse: KnotenAnschlüsse = mutableMapOf()
    open val data: MutableMap<String, Any> = mutableMapOf()

    constructor(
        id: String,
        name: String,
        position: KartenPosition? = null,
        dimension: Rechteck? = null,
        ausgewaehlt: Boolean? = null,
        beweglich: Boolean? = null,
        anschlüsse: KnotenAnschlüsse? = null,
        data: MutableMap<String, Any>? = null,
    ): this(
        id,
        name,
    ) {
        this.position = position ?: this.position
        this.dimension = dimension ?: this.dimension
        this.ausgewaehlt = ausgewaehlt ?: this.ausgewaehlt
        this.beweglich = beweglich ?: this.beweglich
        this.anschlüsse.clear()
        this.anschlüsse.putAll(anschlüsse ?: this.anschlüsse)
        this.data.clear()
        this.data.putAll(data ?: this.data)
    }

/*    fun copy(
        id: String = this.id,
        klasse: KnotenArt? = this.klasse,
        name: String = this.name,
        position: KartenPosition = this.position,
        dimension: Rechteck = this.dimension,
//        art: String = this.art,
        ausgewaehlt: Boolean = this.ausgewaehlt,
        beweglich: Boolean = this.beweglich,
        anschlüsse: KnotenAnschlüsse = this.anschlüsse,
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
        anschlüsse = anschlüsse,
        data = data,
    )*/

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
    override val name: String,
): KnotenDaten(id,name) {
    override val klasse: KnotenArt? = EingabeKnoten.KNOTEN_ART
    override var position: KartenPosition = Offset(0f, 0f)
    override var dimension: Rechteck = IntSize(180, 96)
    //    override val art: String = "default",
    override var ausgewaehlt: Boolean = false
    override var beweglich: Boolean = true
    override val anschlüsse: KnotenAnschlüsse
        get() = anschlussLabel.map { AusgangDaten(id(this.id,it.value.second),it.key,it.value.first) to it.value.second }.toMutableMap()
    override val data: MutableMap<String, Any> = mutableMapOf()
    val anschlussLabel: MutableMap<AnschlussKante,Pair<String, Int>> = mutableMapOf()

    constructor(
        id: String,
        name: String,
        position: KartenPosition? = null,
        dimension: Rechteck? = null,
        ausgewaehlt: Boolean? = null,
        beweglich: Boolean? = null,
        anschlussLabel: MutableMap<AnschlussKante,Pair<String, Int>>? = null,
        data: MutableMap<String, Any>? = null,
    ): this(
        id,
        name,
    ) {
        this.position = position ?: this.position
        this.dimension = dimension ?: this.dimension
        this.ausgewaehlt = ausgewaehlt ?: this.ausgewaehlt
        this.beweglich = beweglich ?: this.beweglich
        this.anschlussLabel.clear()
        this.anschlussLabel.putAll(anschlussLabel ?: this.anschlussLabel)
        this.data.clear()
        this.data.putAll(data ?: this.data)
    }

    public companion object {
        public fun id(id: String, idx: Int): String = "${id} out ${idx}"
    }
}

/**
 *
 */
open class AusgabeDaten(
    override val id: String,
    override val name: String,
): KnotenDaten(id,name) {
    override val klasse: KnotenArt? = AusgabeKnoten.KNOTEN_ART
    override var position: KartenPosition = Offset(0f, 0f)
    override var dimension: Rechteck = IntSize(180, 96)
    //    override val art: String = "default",
    override var ausgewaehlt: Boolean = false
    override var beweglich: Boolean = true
    override val anschlüsse: KnotenAnschlüsse
        get() = anschlussLabel.map { EingangDaten(id(this.id, it.value.second),it.key,it.value.first) to it.value.second }.toMutableMap()
    override val data: MutableMap<String, Any> = mutableMapOf()
    val anschlussLabel: MutableMap<AnschlussKante,Pair<String, Int>> = mutableMapOf()

    constructor(
        id: String,
        name: String,
        position: KartenPosition? = null,
        dimension: Rechteck? = null,
        ausgewaehlt: Boolean? = null,
        beweglich: Boolean? = null,
        anschlussLabel: MutableMap<AnschlussKante,Pair<String, Int>>? = null,
        data: MutableMap<String, Any>? = null,
    ): this(
        id,
        name,
    ) {
        this.position = position ?: this.position
        this.dimension = dimension ?: this.dimension
        this.ausgewaehlt = ausgewaehlt ?: this.ausgewaehlt
        this.beweglich = beweglich ?: this.beweglich
        this.anschlussLabel.clear()
        this.anschlussLabel.putAll(anschlussLabel ?: this.anschlussLabel)
        this.data.clear()
        this.data.putAll(data ?: this.data)
    }

    public companion object {
        public fun id(id: String, idx: Int): String = "${id} out ${idx}"
    }
}
