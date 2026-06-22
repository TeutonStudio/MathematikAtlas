package de.TeutonStudio.MathematikAtlas.anschlüsse

import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.AnschlussFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.AnschlussKonstruktor


@Suppress("UNCHECKED_CAST")
val MatheAnschlussFabrik: AnschlussFabrik = mapOf(
    AussageObjektAnschluss.ANSCHLUSS_ART to ::AussageObjektAnschluss as AnschlussKonstruktor,
//    AussageEingang.ANSCHLUSS_ART to ::AussageEingang as AnschlussKonstruktor,
//    AussageAusgang.ANSCHLUSS_ART to ::AussageAusgang as AnschlussKonstruktor,
)