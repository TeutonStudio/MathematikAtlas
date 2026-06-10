package com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss

import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.AnschlussRichtung
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.BasisAusgang

open class AusgangDaten(
    override val id: String,
    override val kante: AnschlussKante = AnschlussKante.Rechts,
) : RichtungsAnschlussDaten(id,kante,AnschlussRichtung.Ausgang) {
    override val klasse: String? = BasisAusgang.ANSCHLUSS_ART

    constructor(
        id: String,
        kante: AnschlussKante,
        label: String = "",
    ): this(id,kante) {
        this.label = label
    }

}