package com.TeutonStudio.KnotenKartenVerwalter.daten.knoten

import com.TeutonStudio.KnotenKartenVerwalter.KartenPosition
import com.TeutonStudio.KnotenKartenVerwalter.KnotenAnschlüsse
import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.EingangDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.AusgabeKnoten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.knoten.KnotenArt
import com.TeutonStudio.KnotenKartenVerwalter.toMutableMap
import kotlin.collections.putAll
import kotlin.text.clear

open class AusgabeDaten(
    override val id: String,
    override val name: String,
): RichtungsDaten(id,name) {
    override val klasse: KnotenArt? = AusgabeKnoten.KNOTEN_ART
    override val anschlüsse: KnotenAnschlüsse
        get() = anschlussLabel.map { EingangDaten(
            id(this.id, it.value.second),
            it.key,
            it.value.first
        ) to it.value.second }.toMutableMap()

    constructor(
        id: String,
        name: String,
        position: KartenPosition? = null,
        breite: Float? = null,
        tiefe: Float? = null,
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