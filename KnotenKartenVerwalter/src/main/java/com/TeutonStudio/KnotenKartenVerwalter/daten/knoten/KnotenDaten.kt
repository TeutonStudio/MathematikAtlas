package com.TeutonStudio.KnotenKartenVerwalter.daten.knoten

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import com.TeutonStudio.KnotenKartenVerwalter.Rechteck
import com.TeutonStudio.KnotenKartenVerwalter.daten.GraphDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.BasisKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenArt



open class KnotenDaten<D: AnschlussDaten>(
    override val id: String,
    override val name: String = "",
): KnotenAnschlussDaten<D> {
    override val klasse: KnotenArt? = BasisKnoten.KNOTEN_ART
    override val dimension: Rechteck get() = Rect(position,position + Offset(breite,tiefe))
    override var breite: Float = 180f
    override var tiefe: Float = 96f

    override var beweglich: Boolean = true
    override var position: KartenPosition by mutableStateOf(KartenPosition.Zero)
    override val anschlüsse = mutableStateListOf<D>()
    override val anschlussIdx = mutableStateMapOf<String, Int>()
    override val data: MutableMap<String, Any> = mutableMapOf()

    constructor(
        id: String,
        name: String,
        anschlüsse: MutableMap<D,Int>? = null,
        position: KartenPosition? = null,
        breite: Float? = null,
        tiefe: Float? = null,
        beweglich: Boolean? = null,
        data: MutableMap<String, Any>? = null,
    ): this(id,name) {
        this.position = position ?: this.position
        this.breite = breite ?: this.breite
        this.tiefe = tiefe ?: this.tiefe
        this.beweglich = beweglich ?: this.beweglich
        this.anschlüsse.addAll(anschlüsse?.keys ?: this.anschlüsse)
        this.data.clear()
        this.data.putAll(data ?: this.data)
    }
}