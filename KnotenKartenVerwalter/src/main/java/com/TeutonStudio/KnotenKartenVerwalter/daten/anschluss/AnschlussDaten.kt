package com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss

import com.TeutonStudio.KnotenKartenVerwalter.daten.GraphDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.BasisAnschluss

open class AnschlussDaten(
    override val id: String,
    open val kante: AnschlussKante,
): GraphDaten {
    override val klasse: String? = BasisAnschluss.ANSCHLUSS_ART
    open var label: String = ""

    constructor(
        id: String,
        kante: AnschlussKante,
        label: String = "",
    ): this(id,kante) {
        this.label = label
    }

}