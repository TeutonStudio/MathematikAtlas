package de.TeutonStudio.KnotenKartenVerwalter.daten.verbindung

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.Anschluss

data class IDEhe(
    val knotenIdMann: String,
    val knotenIdWeib: String,
    val anschlussIdMann: String,
    val anschlussIdWeib: String,
) {
    constructor(
        anschlussMann: Anschluss<out AnschlussDaten>,
        anschlussWeib: Anschluss<out AnschlussDaten>,
    ): this(
        anschlussMann.besitzer.daten.id,
        anschlussWeib.besitzer.daten.id,
        anschlussMann.daten.id,
        anschlussWeib.daten.id,
    )
}