package com.TeutonStudio.KnotenKartenVerwalter.daten

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

/**
 * Gemeinsame Basisdaten eines Anschlusses.
 *
 * Anschlüsse entsprechen ReactFlow-Handles: Sie besitzen eine stabile ID, ein
 * sichtbares Label und eine feste Richtung.
 */
sealed class AnschlussDaten(
    override val id: String,
    open val label: String,
    open val richtung: AnschlussRichtung,
    open val kante: AnschlussKante,
): GraphDaten

/**
 * Eingangsanschluss eines Knotens.
 */
data class EingangDaten(
    override val id: String,
    override val label: String,
    override val kante: AnschlussKante = AnschlussKante.Links,
) : AnschlussDaten(
    id = id,
    label = label,
    richtung = AnschlussRichtung.Eingang,
    kante = kante,
)

/**
 * Ausgangsanschluss eines Knotens.
 */
data class AusgangDaten(
    override val id: String,
    override val label: String,
    override val kante: AnschlussKante = AnschlussKante.Rechts,
) : AnschlussDaten(
    id = id,
    label = label,
    richtung = AnschlussRichtung.Ausgang,
    kante = kante,
)
