package com.TeutonStudio.KnotenKartenVerwalter.daten.knoten

import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.RichtungsAnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.AnschlussKante

open class RichtungsAnschlussDaten<D: RichtungsAnschlussDaten>(
    override val id: String,
    override val name: String = "",
): KnotenAnschlussDaten<D>(id,name) {
    val anschlussLabel: MutableMap<AnschlussKante,Pair<String, Int>> = mutableMapOf()
}