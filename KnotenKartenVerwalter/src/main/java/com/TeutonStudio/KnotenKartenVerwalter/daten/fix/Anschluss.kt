package com.TeutonStudio.KnotenKartenVerwalter.daten.fix

import com.TeutonStudio.KnotenKartenVerwalter.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussRichtung
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.EingangDaten
import com.TeutonStudio.KnotenKartenVerwalter.daten.fix.RichtungsAnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BasisAnschluss
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BasisAusgang
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.BasisEingang

/**
 * Gemeinsame Basisdaten eines Anschlusses.
 *
 * Anschlüsse entsprechen ReactFlow-Handles: Sie besitzen eine stabile ID, ein
 * sichtbares Label.
 */
sealed class AnschlussDaten(
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

/**
 * Gemeinsame Basisdaten eines gerichteten Anschlusses.
 *
 * Anschlüsse entsprechen ReactFlow-Handles: Sie besitzen eine stabile ID, ein
 * sichtbares Label und eine feste Richtung.
 */
sealed class RichtungsAnschlussDaten(
    override val id: String,
    open val richtung: AnschlussRichtung,
    override val kante: AnschlussKante,
): AnschlussDaten(id,kante) {

    constructor(
        id: String,
        richtung: AnschlussRichtung,
        kante: AnschlussKante,
        label: String = "",
    ): this(id,richtung,kante) {
        this.label = label
    }
}

/**
 * Eingangsanschluss eines Knotens.
 */
open class EingangDaten(
    override val id: String,
    override val kante: AnschlussKante = AnschlussKante.Links,
) : RichtungsAnschlussDaten(id,AnschlussRichtung.Eingang,kante) {
    override val klasse: String? = BasisEingang.ANSCHLUSS_ART

    constructor(
        id: String,
        kante: AnschlussKante,
        label: String = "",
    ): this(id,kante) {
        this.label = label
    }

}

/**
 * Ausgangsanschluss eines Knotens.
 */
open class AusgangDaten(
    override val id: String,
    override val kante: AnschlussKante = AnschlussKante.Rechts,
) : RichtungsAnschlussDaten(id,AnschlussRichtung.Ausgang,kante) {
    override val klasse: String? = BasisAusgang.ANSCHLUSS_ART

    constructor(
        id: String,
        kante: AnschlussKante,
        label: String = "",
    ): this(id,kante) {
        this.label = label
    }

}
