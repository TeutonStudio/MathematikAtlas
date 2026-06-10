package com.TeutonStudio.KnotenKartenVerwalter.daten.knoten

import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.AnschlussKante

open class RichtungsDaten(
    override val id: String,
    override val name: String = "",
): KnotenDaten(id,name) {
    val anschlussLabel: MutableMap<AnschlussKante,Pair<String, Int>> = mutableMapOf()
}