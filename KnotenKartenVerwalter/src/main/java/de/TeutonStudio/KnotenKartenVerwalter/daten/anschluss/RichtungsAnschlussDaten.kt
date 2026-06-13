package de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss

abstract class RichtungsAnschlussDaten(
    override val id: String,
    override val kante: de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussKante,
    open val richtung: de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussRichtung,
): de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten(id,kante) {

    constructor(
        id: String,
        richtung: de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussRichtung,
        kante: de.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussKante,
        label: String = "",
    ): this(id,kante,richtung) {
        this.label = label
    }
}