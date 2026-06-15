package de.TeutonStudio.MathematikAtlas.anschlüsse

import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.AnschlussFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.graph.anschlüsse.AnschlussKonstruktor


@Suppress("UNCHECKED_CAST")
val MatheAnschlussFabrik: AnschlussFabrik = mapOf(
    AussageEingang.ANSCHLUSS_ART to ::AussageEingang as AnschlussKonstruktor,
    AussageAusgang.ANSCHLUSS_ART to ::AussageAusgang as AnschlussKonstruktor,
)