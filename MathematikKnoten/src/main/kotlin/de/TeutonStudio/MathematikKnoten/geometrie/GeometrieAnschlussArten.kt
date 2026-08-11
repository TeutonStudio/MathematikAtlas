package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArt
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussArtId

/** Hierarchische Anschlussarten der eigenständigen Geometriedomäne. */
object GeometrieAnschlussArten {
    val Raum = AnschlussArt(
        id = AnschlussArtId("mathematik.geometrie.raum"),
        name = "Euklidischer Raum",
        elternArt = MathematikAnschlussArten.Objekt.id,
        beschreibung = "Ein euklidischer Raum, in dem geometrische Objekte liegen.",
    )
    val Koordinatensystem = AnschlussArt(
        id = AnschlussArtId("mathematik.geometrie.koordinatensystem"),
        name = "Koordinatensystem",
        elternArt = MathematikAnschlussArten.Objekt.id,
        beschreibung = "Ein Koordinatensystem zur Darstellung von Punkten und Objekten in einem Raum.",
    )
    val Objekt = AnschlussArt(
        id = AnschlussArtId("mathematik.geometrie.objekt"),
        name = "Geometrisches Objekt",
        elternArt = MathematikAnschlussArten.Objekt.id,
        beschreibung = "Ein allgemeines geometrisches Objekt.",
    )
    val Punkt = AnschlussArt(
        id = AnschlussArtId("mathematik.geometrie.punkt"),
        name = "Punkt",
        elternArt = Objekt.id,
        beschreibung = "Ein punktförmiges geometrisches Objekt.",
    )
    val Gerade = AnschlussArt(
        id = AnschlussArtId("mathematik.geometrie.gerade"),
        name = "Gerade",
        elternArt = Objekt.id,
        beschreibung = "Eine in beide Richtungen unbeschränkte Gerade.",
    )
    val Ebene = AnschlussArt(
        id = AnschlussArtId("mathematik.geometrie.ebene"),
        name = "Ebene",
        elternArt = Objekt.id,
        beschreibung = "Eine zweidimensionale affine Ebene.",
    )
    val Strecke = AnschlussArt(
        id = AnschlussArtId("mathematik.geometrie.strecke"),
        name = "Strecke",
        elternArt = Objekt.id,
        beschreibung = "Der von zwei Endpunkten begrenzte Teil einer Geraden.",
    )
    val Strahl = AnschlussArt(
        id = AnschlussArtId("mathematik.geometrie.strahl"),
        name = "Strahl",
        elternArt = Objekt.id,
        beschreibung = "Eine an einem Punkt beginnende und in eine Richtung unbeschränkte Halbgerade.",
    )
    val Winkel = AnschlussArt(
        id = AnschlussArtId("mathematik.geometrie.winkel"),
        name = "Winkel",
        elternArt = Objekt.id,
        beschreibung = "Ein geometrischer Winkel zwischen zwei Richtungen oder Strahlen.",
    )
    val Kreislinie = AnschlussArt(
        id = AnschlussArtId("mathematik.geometrie.kreislinie"),
        name = "Kreislinie",
        elternArt = Objekt.id,
        beschreibung = "Die Menge aller Punkte mit festem Abstand zu einem Mittelpunkt.",
    )
    val Polygon = AnschlussArt(
        id = AnschlussArtId("mathematik.geometrie.polygon"),
        name = "Polygon",
        elternArt = Objekt.id,
        beschreibung = "Ein durch endlich viele geradlinige Seiten begrenztes geometrisches Objekt.",
    )
    val Dreieck = AnschlussArt(
        id = AnschlussArtId("mathematik.geometrie.dreieck"),
        name = "Dreieck",
        elternArt = Objekt.id,
        beschreibung = "Ein Polygon mit drei Ecken und drei Seiten.",
    )
    val Gruppe = AnschlussArt(
        id = AnschlussArtId("mathematik.geometrie.gruppe"),
        name = "Geometriegruppe",
        elternArt = Objekt.id,
        beschreibung = "Eine Zusammenfassung mehrerer geometrischer Objekte.",
    )
    val Struktur = AnschlussArt(
        id = AnschlussArtId("mathematik.geometrie.struktur"),
        name = "Geometriestruktur",
        elternArt = MathematikAnschlussArten.Objekt.id,
        beschreibung = "Eine geometrische Struktur mit zusammengehörigen Objekten und Beziehungen.",
    )
    val Transformation = AnschlussArt(
        id = AnschlussArtId("mathematik.geometrie.transformation"),
        name = "Geometrische Transformation",
        elternArt = MathematikAnschlussArten.Objekt.id,
        beschreibung = "Eine Abbildung, die geometrische Objekte oder Koordinaten transformiert.",
    )
    val KoordinatenTupel = AnschlussArt(
        id = AnschlussArtId("mathematik.geometrie.koordinaten"),
        name = "Koordinatentupel",
        elternArt = MathematikAnschlussArten.Tupel.id,
        beschreibung = "Ein Tupel von Koordinaten, das eine Position oder geometrische Größe beschreibt.",
    )

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
