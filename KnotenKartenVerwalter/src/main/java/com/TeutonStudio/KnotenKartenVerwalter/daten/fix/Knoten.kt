package com.TeutonStudio.KnotenKartenVerwalter.daten.fix

import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
    open val dimension: Rechteck get() = Rect(position,position + Offset(breite,tiefe))
    open var breite: Float = 180f
    open var tiefe: Float = 96f

    open var beweglich: Boolean = true
    open var position: KartenPosition by mutableStateOf(KartenPosition.Zero)
//    open var ausgewaehlt by mutableStateOf(false)
    open val anschlüsse: KnotenAnschlüsse = mutableMapOf()
    open val data: MutableMap<String, Any> = mutableMapOf()

    constructor(
        id: String,
        name: String,
        position: KartenPosition? = null,
        breite: Float? = null,
        tiefe: Float? = null,
//        ausgewaehlt: Boolean? = null,
        beweglich: Boolean? = null,
        anschlüsse: KnotenAnschlüsse? = null,
        data: MutableMap<String, Any>? = null,
    ): this(
        id,
        name,
    ) {
        this.position = position ?: this.position
        this.breite = breite ?: this.breite
        this.tiefe = tiefe ?: this.tiefe
//        this.ausgewaehlt = ausgewaehlt ?: this.ausgewaehlt
        this.beweglich = beweglich ?: this.beweglich
        this.anschlüsse.clear()
        this.anschlüsse.putAll(anschlüsse ?: this.anschlüsse)
        this.data.clear()
        this.data.putAll(data ?: this.data)
    }
}

/**
 *
 */
open class RichtungsDaten(
    override val id: String,
    override val name: String = "",
): KnotenDaten(id,name) {
    val anschlussLabel: MutableMap<AnschlussKante,Pair<String, Int>> = mutableMapOf()
}

/**
 *
 */
open class EingabeDaten(
    override val id: String,
    override val name: String,
): RichtungsDaten(id,name) {
    override val klasse: KnotenArt? = EingabeKnoten.KNOTEN_ART
    override val anschlüsse: KnotenAnschlüsse
        get() = anschlussLabel.map { AusgangDaten(id(this.id,it.value.second),it.key,it.value.first) to it.value.second }.toMutableMap()

    constructor(
        id: String,
        name: String,
        position: KartenPosition? = null,
        breite: Float? = null,
        tiefe: Float? = null,
//        ausgewaehlt: Boolean? = null,
        beweglich: Boolean? = null,
        anschlussLabel: MutableMap<AnschlussKante,Pair<String, Int>>? = null,
        data: MutableMap<String, Any>? = null,
    ): this(
        id,
        name,
    ) {
        this.position = position ?: this.position
        this.breite = breite ?: this.breite
        this.tiefe = tiefe ?: this.tiefe
//        this.ausgewaehlt = ausgewaehlt ?: this.ausgewaehlt
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
): RichtungsDaten(id,name) {
    override val klasse: KnotenArt? = AusgabeKnoten.KNOTEN_ART
    override val anschlüsse: KnotenAnschlüsse
        get() = anschlussLabel.map { EingangDaten(id(this.id, it.value.second),it.key,it.value.first) to it.value.second }.toMutableMap()

    constructor(
        id: String,
        name: String,
        position: KartenPosition? = null,
        breite: Float? = null,
        tiefe: Float? = null,
//        ausgewaehlt: Boolean? = null,
        beweglich: Boolean? = null,
        anschlussLabel: MutableMap<AnschlussKante,Pair<String, Int>>? = null,
        data: MutableMap<String, Any>? = null,
    ): this(
        id,
        name,
    ) {
        this.position = position ?: this.position
        this.breite = breite ?: this.breite
        this.tiefe = tiefe ?: this.tiefe
//        this.ausgewaehlt = ausgewaehlt ?: this.ausgewaehlt
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
