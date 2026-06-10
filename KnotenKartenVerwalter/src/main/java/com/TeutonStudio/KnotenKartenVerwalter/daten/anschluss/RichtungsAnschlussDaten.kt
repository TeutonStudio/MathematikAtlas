package com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss

import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.AnschlussRichtung

sealed class RichtungsAnschlussDaten(
    override val id: String,
    override val kante: AnschlussKante,
    open val richtung: AnschlussRichtung,
): AnschlussDaten(id,kante) {

    constructor(
        id: String,
        richtung: AnschlussRichtung,
        kante: AnschlussKante,
        label: String = "",
    ): this(id,kante,richtung) {
        this.label = label
    }
}