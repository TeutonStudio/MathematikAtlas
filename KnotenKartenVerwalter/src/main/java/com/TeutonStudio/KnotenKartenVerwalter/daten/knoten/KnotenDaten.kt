package com.TeutonStudio.KnotenKartenVerwalter.daten.knoten

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import com.TeutonStudio.KnotenKartenVerwalter.KnotenAnschlüsse
import com.TeutonStudio.KnotenKartenVerwalter.Rechteck
import com.TeutonStudio.KnotenKartenVerwalter.daten.AuswahlDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.GraphDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.BasisKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenArt

open class KnotenDaten(
    override val id: String,
    open val name: String = "",
): GraphDaten(id) {
    override val klasse: KnotenArt? = BasisKnoten.KNOTEN_ART
    open val dimension: Rechteck get() = Rect(position,position + Offset(breite,tiefe))
    open var breite: Float = 180f
    open var tiefe: Float = 96f

    open var beweglich: Boolean = true
    open var position: KartenPosition by mutableStateOf(KartenPosition.Zero)
    open val anschlüsse: KnotenAnschlüsse = mutableMapOf()
    open val data: MutableMap<String, Any> = mutableMapOf()

    constructor(
        id: String,
        name: String,
        position: KartenPosition? = null,
        breite: Float? = null,
        tiefe: Float? = null,
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
        this.beweglich = beweglich ?: this.beweglich
        this.anschlüsse.clear()
        this.anschlüsse.putAll(anschlüsse ?: this.anschlüsse)
        this.data.clear()
        this.data.putAll(data ?: this.data)
    }

    public fun zuAuswahl(): AuswahlDaten = AuswahlDaten(knotenIds = setOf(id))
}