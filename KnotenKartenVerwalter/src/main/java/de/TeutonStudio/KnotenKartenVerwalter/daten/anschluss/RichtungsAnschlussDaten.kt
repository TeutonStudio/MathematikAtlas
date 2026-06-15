package de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss

abstract class RichtungsAnschlussDaten(
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