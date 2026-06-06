package com.TeutonStudio.KnotenKartenVerwalter.daten

import com.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante.Links
import com.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante.Oben
import com.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante.Rechts
import com.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante.Unten

/**
 * Seite eines Anschlusses am Knoten.
 *
 * Eingänge liegen links am Knotenrahmen, Ausgänge rechts. Diese Richtung wird
 * sowohl für das Rendering als auch für die Validierung neuer Verbindungen
 * verwendet.
 */
enum class AnschlussRichtung {
    Eingang,
    Ausgang,
}

/**
 * Kante eines Knotens, an der ein Anschluss liegt.
 */
enum class AnschlussKante {
    Links,
    Rechts,
    Oben,
    Unten,
}

public fun AnschlussKante.istVertikal(): Boolean = this == AnschlussKante.Links || this == AnschlussKante.Rechts
public fun AnschlussKante.istHorizontal(): Boolean = this == AnschlussKante.Oben || this == AnschlussKante.Unten

/**
 * Gemeinsame Basisdaten eines Anschlusses.
 *
 * Anschlüsse entsprechen ReactFlow-Handles: Sie besitzen eine stabile ID, ein
 * sichtbares Label.
 */
sealed class AnschlussDaten(
    override val id: String,
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
): AnschlussDaten(id,label,kante)

/**
 * Eingangsanschluss eines Knotens.
 */
data class EingangDaten(
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
data class AusgangDaten(
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
