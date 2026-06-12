package com.TeutonStudio.KnotenKartenVerwalter.daten.verbindung

import com.TeutonStudio.KnotenKartenVerwalter.daten.anschluss.AnschlussDaten
import com.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.Anschluss

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