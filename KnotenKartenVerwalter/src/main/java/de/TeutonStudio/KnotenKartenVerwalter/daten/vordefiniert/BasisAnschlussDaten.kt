package de.TeutonStudio.KnotenKartenVerwalter.daten.vordefiniert

import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenAnschluss
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.GraphDatenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.graph.Kante
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.AnschlussArt
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.BasisObjektAnschluss

open class BasisAnschlussDaten(
    override val id: GraphDatenId,
    override val kante: Kante,
): GraphDatenAnschluss {
    override var klasse: AnschlussArt? = BasisObjektAnschluss.ANSCHLUSS_ART
    override var label: String = ""

    constructor(
        id: String,
        kante: Kante,
        label: String = "",
    ): this(id,kante) {
        this.label = label
    }

}