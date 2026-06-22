package de.TeutonStudio.MathematikAtlas.anschlüsse

import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.AnschlussFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.AnschlussKonstruktor


@Suppress("UNCHECKED_CAST")
val MatheAnschlussFabrik: AnschlussFabrik = mapOf(
    AussageObjektAnschluss.ANSCHLUSS_ART to ::AussageObjektAnschluss as AnschlussKonstruktor,
    AussageObjektEingang.ANSCHLUSS_ART to ::AussageObjektAnschluss as AnschlussKonstruktor,
    AussageObjektAusgang.ANSCHLUSS_ART to ::AussageObjektAnschluss as AnschlussKonstruktor,
)
