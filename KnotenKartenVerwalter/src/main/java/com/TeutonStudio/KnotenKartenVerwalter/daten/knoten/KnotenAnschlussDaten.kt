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
import com.TeutonStudio.KnotenKartenVerwalter.daten.auswahl.AuswahlDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.GraphDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.auswahl.EinzelAuswahl
import com.TeutonStudio.KnotenKartenVerwalter.daten.auswahl.MultiAuswahl
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.BasisKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenArt

typealias KnotenDaten = KnotenAnschlussDaten<out AnschlussDaten>

open class KnotenAnschlussDaten<D: AnschlussDaten>(
    override val id: String,
    open val name: String = "",
): GraphDaten(id) {
    override val klasse: KnotenArt? = BasisKnoten.KNOTEN_ART
    open val dimension: Rechteck get() = Rect(position,position + Offset(breite,tiefe))
    open var breite: Float = 180f
    open var tiefe: Float = 96f

    open var beweglich: Boolean = true
    open var position: KartenPosition by mutableStateOf(KartenPosition.Zero)
    open val anschlüsse: SnapshotStateList<D> = mutableStateListOf()
    open val anschlussIdx = mutableStateMapOf<String, Int>()
    open val data: MutableMap<String, Any> = mutableMapOf()

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

    public fun duplizieren() = KnotenAnschlussDaten<D>(id.duplizieren(),name.duplizieren()).apply {
        position = this@KnotenAnschlussDaten.position + Offset(10f,10f)
        anschlüsse.addAll(this@KnotenAnschlussDaten.anschlüsse)
        anschlussIdx.putAll(this@KnotenAnschlussDaten.anschlussIdx)
        data.putAll(this@KnotenAnschlussDaten.data)
    }

    public companion object {
        public fun String.duplizieren(separierer: String = "#") = split(separierer).let {
            setOf(it[0],it.getOrElse(1,{ "0" }).toInt() + 1) }.joinToString(separierer)
    }
}