package de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss

import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.BasisEingang

open class EingangDaten(
    override val id: String,
    override val kante: AnschlussKante = AnschlussKante.Links,
) : RichtungsAnschlussDaten(id,kante,
    AnschlussRichtung.Eingang) {
    override var klasse: String? = BasisEingang.ANSCHLUSS_ART

    constructor(
        id: String,
        kante: AnschlussKante,
        label: String = "",
    ): this(id,kante) {
        this.label = label
    }
}