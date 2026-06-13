package de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.BasisAnschluss

open class AnschlussDaten(
    override val id: String,
    open val kante: de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussKante,
): de.TeutonStudio.KnotenKartenVerwalter.daten.GraphDaten {
    override val klasse: String? = BasisAnschluss.ANSCHLUSS_ART
    open var label: String = ""

    constructor(
        id: String,
        kante: de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussKante,
        label: String = "",
    ): this(id,kante) {
        this.label = label
    }

}