package de.TeutonStudio.MathematikAtlas.anschlüsse

import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.AnschlussFabrik
import de.TeutonStudio.KnotenKartenVerwalter.schnittstelle.vordefiniert.AnschlussKonstruktor


@Suppress("UNCHECKED_CAST")
val MatheAnschlussFabrik: AnschlussFabrik = mapOf(
    AussageObjektAnschluss.ANSCHLUSS_ART to ::AussageObjektAnschluss as AnschlussKonstruktor,
    AussageObjektEingang.ANSCHLUSS_ART to ::AussageObjektAnschluss as AnschlussKonstruktor,
    AussageObjektAusgang.ANSCHLUSS_ART to ::AussageObjektAnschluss as AnschlussKonstruktor,
    ZahlenObjektAnschluss.ANSCHLUSS_ART to ::ZahlenObjektAnschluss as AnschlussKonstruktor,
    ZahlenObjektAnschluss.EINGANG_ART to ::ZahlenObjektAnschluss as AnschlussKonstruktor,
    ZahlenObjektAnschluss.AUSGANG_ART to ::ZahlenObjektAnschluss as AnschlussKonstruktor,
    MengenObjektAnschluss.ANSCHLUSS_ART to ::MengenObjektAnschluss as AnschlussKonstruktor,
    MengenObjektAnschluss.EINGANG_ART to ::MengenObjektAnschluss as AnschlussKonstruktor,
    MengenObjektAnschluss.AUSGANG_ART to ::MengenObjektAnschluss as AnschlussKonstruktor,
)
