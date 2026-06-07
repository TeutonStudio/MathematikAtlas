package com.TeutonStudio.KnotenKartenVerwalter.daten.fix

import com.TeutonStudio.KnotenKartenVerwalter.AnschlussKante
import com.TeutonStudio.KnotenKartenVerwalter.AnschlussRichtung

/**
 * Gemeinsame Basisdaten eines Anschlusses.
 *
 * Anschlüsse entsprechen ReactFlow-Handles: Sie besitzen eine stabile ID, ein
 * sichtbares Label.
 */
sealed class AnschlussDaten(
    override val id: String,
    override val klasse: String? = null,
    open val label: String,
//    open val richtung: AnschlussRichtung,
    open val kante: AnschlussKante,
//    open val zahlenTyp: ZahlenTyp? = null,
): GraphDaten

/**
 * Gemeinsame Basisdaten eines gerichteten Anschlusses.
 *
 * Anschlüsse entsprechen ReactFlow-Handles: Sie besitzen eine stabile ID, ein
 * sichtbares Label und eine feste Richtung.
 */
sealed class RichtungsAnschlussDaten(
    override val id: String,
    override val label: String,
    open val richtung: AnschlussRichtung,
    override val kante: AnschlussKante,
//    override val zahlenTyp: ZahlenTyp? = null,
): AnschlussDaten(id,null,label,kante)

/**
 * Eingangsanschluss eines Knotens.
 */
open class EingangDaten(
    override val id: String,
    override val label: String,
    override val kante: AnschlussKante = AnschlussKante.Links,
//    override val zahlenTyp: ZahlenTyp? = null,
) : RichtungsAnschlussDaten(
    id = id,
    label = label,
    richtung = AnschlussRichtung.Eingang,
    kante = kante,
//    zahlenTyp = zahlenTyp,
)

/**
 * Ausgangsanschluss eines Knotens.
 */
open class AusgangDaten(
    override val id: String,
    override val label: String,
    override val kante: AnschlussKante = AnschlussKante.Rechts,
//    override val zahlenTyp: ZahlenTyp? = null,
) : RichtungsAnschlussDaten(
    id = id,
    label = label,
    richtung = AnschlussRichtung.Ausgang,
    kante = kante,
//    zahlenTyp = zahlenTyp,
)
