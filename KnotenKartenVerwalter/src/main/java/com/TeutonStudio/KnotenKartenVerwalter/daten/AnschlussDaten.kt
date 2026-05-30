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
 * Gemeinsame Basisdaten eines Anschlusses.
 *
 * Anschlüsse entsprechen ReactFlow-Handles: Sie besitzen eine stabile ID, ein
 * sichtbares Label und eine feste Richtung.
 */
sealed class AnschlussDaten(
    open val id: String,
    open val label: String,
    open val richtung: AnschlussRichtung,
)

/**
 * Eingangsanschluss eines Knotens.
 */
data class EingangDaten(
    override val id: String,
    override val label: String,
) : AnschlussDaten(
    id = id,
    label = label,
    richtung = AnschlussRichtung.Eingang,
)

/**
 * Ausgangsanschluss eines Knotens.
 */
data class AusgangDaten(
    override val id: String,
    override val label: String,
) : AnschlussDaten(
    id = id,
    label = label,
    richtung = AnschlussRichtung.Ausgang,
)
