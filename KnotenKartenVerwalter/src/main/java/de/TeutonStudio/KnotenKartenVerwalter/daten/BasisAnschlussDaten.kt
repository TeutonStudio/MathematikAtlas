package de.TeutonStudio.KnotenKartenVerwalter.daten

import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.BasisAnschluss

open class BasisAnschlussDaten(
    override val id: GraphDatenId,
    override val kante: Kante,
): GraphDatenAnschluss {
    override var klasse: String? = BasisAnschluss.ANSCHLUSS_ART
    override var label: String = ""

    constructor(
        id: String,
        kante: Kante,
        label: String = "",
    ): this(id,kante) {
        this.label = label
    }

}