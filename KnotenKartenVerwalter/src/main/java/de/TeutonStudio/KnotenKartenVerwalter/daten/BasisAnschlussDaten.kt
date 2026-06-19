package de.TeutonStudio.KnotenKartenVerwalter.daten

import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BasisObjektAnschluss

open class BasisAnschlussDaten(
    override val id: GraphDatenId,
    override val kante: Kante,
): GraphDatenAnschluss {
    override var klasse: String? = BasisObjektAnschluss.ANSCHLUSS_ART
    override var label: String = ""

    constructor(
        id: String,
        kante: Kante,
        label: String = "",
    ): this(id,kante) {
        this.label = label
    }

}