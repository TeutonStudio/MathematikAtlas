package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArt
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId

/** Hierarchische Anschlussarten der eigenständigen Geometriedomäne. */
object GeometrieAnschlussArten {
    val Raum = AnschlussArt(AnschlussArtId("mathematik.geometrie.raum"), "Euklidischer Raum", MathematikAnschlussArten.Objekt.id)
    val Koordinatensystem = AnschlussArt(AnschlussArtId("mathematik.geometrie.koordinatensystem"), "Koordinatensystem", MathematikAnschlussArten.Objekt.id)
    val Objekt = AnschlussArt(AnschlussArtId("mathematik.geometrie.objekt"), "Geometrisches Objekt", MathematikAnschlussArten.Objekt.id)
    val Punkt = AnschlussArt(AnschlussArtId("mathematik.geometrie.punkt"), "Punkt", Objekt.id)
    val Gerade = AnschlussArt(AnschlussArtId("mathematik.geometrie.gerade"), "Gerade", Objekt.id)
    val Ebene = AnschlussArt(AnschlussArtId("mathematik.geometrie.ebene"), "Ebene", Objekt.id)
    val Strecke = AnschlussArt(AnschlussArtId("mathematik.geometrie.strecke"), "Strecke", Objekt.id)
    val Strahl = AnschlussArt(AnschlussArtId("mathematik.geometrie.strahl"), "Strahl", Objekt.id)
    val Winkel = AnschlussArt(AnschlussArtId("mathematik.geometrie.winkel"), "Winkel", Objekt.id)
    val Kreislinie = AnschlussArt(AnschlussArtId("mathematik.geometrie.kreislinie"), "Kreislinie", Objekt.id)
    val Polygon = AnschlussArt(AnschlussArtId("mathematik.geometrie.polygon"), "Polygon", Objekt.id)
    val Dreieck = AnschlussArt(AnschlussArtId("mathematik.geometrie.dreieck"), "Dreieck", Objekt.id)
    val Gruppe = AnschlussArt(AnschlussArtId("mathematik.geometrie.gruppe"), "Geometriegruppe", Objekt.id)
    val Struktur = AnschlussArt(AnschlussArtId("mathematik.geometrie.struktur"), "Geometriestruktur", MathematikAnschlussArten.Objekt.id)
    val Transformation = AnschlussArt(AnschlussArtId("mathematik.geometrie.transformation"), "Geometrische Transformation", MathematikAnschlussArten.Objekt.id)
    val KoordinatenTupel = AnschlussArt(AnschlussArtId("mathematik.geometrie.koordinaten"), "Koordinatentupel", MathematikAnschlussArten.Tupel.id)

    val alle = listOf(
        Raum,
        Koordinatensystem,
        Objekt,
        Punkt,
        Gerade,
        Ebene,
        Strecke,
        Strahl,
        Winkel,
        Kreislinie,
        Polygon,
        Dreieck,
        Gruppe,
        Struktur,
        Transformation,
        KoordinatenTupel,
    )
}
