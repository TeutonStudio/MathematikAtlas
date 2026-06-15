package de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss

import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.BasisAusgang

open class AusgangDaten(
    override val id: String,
    override val kante: AnschlussKante = AnschlussKante.Rechts,
) : RichtungsAnschlussDaten(id,kante,
    AnschlussRichtung.Ausgang) {
    override var klasse: String? = BasisAusgang.ANSCHLUSS_ART

    constructor(
        id: String,
        kante: AnschlussKante,
        label: String = "",
    ): this(id,kante) {
        this.label = label
    }

}