package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

object GeometrieKnotenVorlagen {
    private fun eingang(name: String, art: AnschlussArtId, reihe: Int = 0) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Eingang,
        kante = AnschlussKante.Links,
        art = art,
        reihenfolge = reihe,
    )

    private fun ausgang(name: String, art: AnschlussArtId, reihe: Int = 0) = AnschlussDaten(
        name = name,
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = art,
        reihenfolge = reihe,
    )

    val Raum = KnotenVorlage(
        "mathematik.geometrie.raum",
        "Euklidischer Raum",
        "Geometrie: Räume",
        "Erzeugt einen eigenständigen euklidischen Raum nach Hilberts Axiomsystem.",
        GraphGröße(235f, 105f),
        listOf(ausgang("raum", GeometrieAnschlussArten.Raum.id)),
        mapOf("name" to "E", "dimension" to "2"),
    )

    val StandardKoordinatensystem = KnotenVorlage(
        "mathematik.geometrie.standardKoordinatensystem",
        "Standardkoordinatensystem",
        "Geometrie: Räume",
        "Erzeugt die analytische Standardrealisierung eines euklidischen Raums.",
        GraphGröße(260f, 105f),
        listOf(eingang("raum", GeometrieAnschlussArten.Raum.id), ausgang("system", GeometrieAnschlussArten.Koordinatensystem.id)),
        mapOf("name" to "K"),
    )

    val PunktAusKoordinaten = KnotenVorlage(
        "mathematik.geometrie.punktAusKoordinaten",
        "Punkt aus Koordinaten",
        "Geometrie: Grundobjekte",
        "Erzeugt einen geometrischen Punkt aus einem Koordinatentupel. Das Tupel bleibt Darstellung, nicht Identität des Punktbegriffs.",
        GraphGröße(265f, 125f),
        listOf(
            eingang("system", GeometrieAnschlussArten.Koordinatensystem.id, 0),
            eingang("koordinaten", MathematikAnschlussArten.Tupel.id, 1),
            ausgang("punkt", GeometrieAnschlussArten.Punkt.id),
        ),
        mapOf("name" to "A"),
    )

    val GeradeDurchPunkte = KnotenVorlage(
        "mathematik.geometrie.geradeDurchPunkte",
        "Gerade durch Punkte",
        "Geometrie: Konstruktionen",
        "Konstruiert die Gerade durch zwei Punkte und führt den entarteten Fall als Aussage aus.",
        GraphGröße(260f, 130f),
        listOf(
            eingang("a", GeometrieAnschlussArten.Punkt.id, 0),
            eingang("b", GeometrieAnschlussArten.Punkt.id, 1),
            ausgang("gerade", GeometrieAnschlussArten.Gerade.id),
            ausgang("punkteIdentisch", MathematikAnschlussArten.Aussage.id, 1),
        ),
    )

    val Strecke = KnotenVorlage(
        "mathematik.geometrie.strecke",
        "Strecke",
        "Geometrie: Grundobjekte",
        "Erzeugt die Strecke zwischen zwei Punkten.",
        GraphGröße(235f, 125f),
        listOf(
            eingang("anfang", GeometrieAnschlussArten.Punkt.id, 0),
            eingang("ende", GeometrieAnschlussArten.Punkt.id, 1),
            ausgang("strecke", GeometrieAnschlussArten.Strecke.id),
            ausgang("entartet", MathematikAnschlussArten.Aussage.id, 1),
        ),
    )

    val Strahl = KnotenVorlage(
        "mathematik.geometrie.strahl",
        "Strahl",
        "Geometrie: Grundobjekte",
        "Erzeugt einen Strahl aus Ursprung und Richtungspunkt.",
        GraphGröße(235f, 125f),
        listOf(
            eingang("ursprung", GeometrieAnschlussArten.Punkt.id, 0),
            eingang("richtung", GeometrieAnschlussArten.Punkt.id, 1),
            ausgang("strahl", GeometrieAnschlussArten.Strahl.id),
            ausgang("entartet", MathematikAnschlussArten.Aussage.id, 1),
        ),
    )

    val Winkel = KnotenVorlage(
        "mathematik.geometrie.winkel",
        "Geometrischer Winkel",
        "Geometrie: Grundobjekte",
        "Erzeugt einen Winkel aus drei Punkten mit dem mittleren Punkt als Scheitel.",
        GraphGröße(245f, 135f),
        listOf(
            eingang("a", GeometrieAnschlussArten.Punkt.id, 0),
            eingang("scheitel", GeometrieAnschlussArten.Punkt.id, 1),
            eingang("c", GeometrieAnschlussArten.Punkt.id, 2),
            ausgang("winkel", GeometrieAnschlussArten.Winkel.id),
        ),
        mapOf("orientiert" to "false"),
    )

    val Kreislinie = KnotenVorlage(
        "mathematik.geometrie.kreislinie",
        "Kreislinie",
        "Geometrie: Grundobjekte",
        "Erzeugt eine Kreislinie aus Mittelpunkt und Randpunkt.",
        GraphGröße(245f, 125f),
        listOf(
            eingang("mittelpunkt", GeometrieAnschlussArten.Punkt.id, 0),
            eingang("randpunkt", GeometrieAnschlussArten.Punkt.id, 1),
            ausgang("kreis", GeometrieAnschlussArten.Kreislinie.id),
            ausgang("radiusNull", MathematikAnschlussArten.Aussage.id, 1),
        ),
    )

    val Polygon = KnotenVorlage(
        "mathematik.geometrie.polygon",
        "Polygon",
        "Geometrie: Grundobjekte",
        "Erzeugt ein geordnetes Polygon aus drei Eckpunkten.",
        GraphGröße(245f, 135f),
        listOf(
            eingang("a", GeometrieAnschlussArten.Punkt.id, 0),
            eingang("b", GeometrieAnschlussArten.Punkt.id, 1),
            eingang("c", GeometrieAnschlussArten.Punkt.id, 2),
            ausgang("polygon", GeometrieAnschlussArten.Polygon.id),
        ),
    )

    val Gruppe = KnotenVorlage(
        "mathematik.geometrie.gruppe",
        "Geometriegruppe",
        "Geometrie: Struktur",
        "Fasst zwei geometrische Objekte desselben Raums geordnet zusammen, ohne eine Mengenvereinigung zu behaupten.",
        GraphGröße(260f, 120f),
        listOf(
            eingang("a", GeometrieAnschlussArten.Objekt.id, 0),
            eingang("b", GeometrieAnschlussArten.Objekt.id, 1),
            ausgang("gruppe", GeometrieAnschlussArten.Gruppe.id),
        ),
    )

    val Inzidenz = aussageVorlage(
        "mathematik.geometrie.inzidenz",
        "Inzidenz",
        "Prüft, ob ein Punkt auf oder in einem geometrischen Objekt liegt.",
        GeometrieAnschlussArten.Punkt.id,
        GeometrieAnschlussArten.Objekt.id,
    )

    val Zwischen = KnotenVorlage(
        "mathematik.geometrie.zwischen",
        "Zwischenlage",
        "Geometrie: Relationen",
        "Prüft, ob der mittlere Punkt zwischen den beiden äußeren Punkten liegt.",
        GraphGröße(245f, 130f),
        listOf(
            eingang("a", GeometrieAnschlussArten.Punkt.id, 0),
            eingang("b", GeometrieAnschlussArten.Punkt.id, 1),
            eingang("c", GeometrieAnschlussArten.Punkt.id, 2),
            ausgang("aussage", MathematikAnschlussArten.Aussage.id),
        ),
    )

    val Kollinear = KnotenVorlage(
        "mathematik.geometrie.kollinear",
        "Kollinear",
        "Geometrie: Relationen",
        "Prüft drei Punkte auf Kollinearität.",
        GraphGröße(235f, 130f),
        listOf(
            eingang("a", GeometrieAnschlussArten.Punkt.id, 0),
            eingang("b", GeometrieAnschlussArten.Punkt.id, 1),
            eingang("c", GeometrieAnschlussArten.Punkt.id, 2),
            ausgang("aussage", MathematikAnschlussArten.Aussage.id),
        ),
    )

    val Parallel = aussageVorlage(
        "mathematik.geometrie.parallel",
        "Parallel",
        "Prüft zwei Geraden auf Parallelität.",
        GeometrieAnschlussArten.Gerade.id,
        GeometrieAnschlussArten.Gerade.id,
    )

    val Orthogonal = aussageVorlage(
        "mathematik.geometrie.orthogonal",
        "Orthogonal",
        "Prüft zwei Geraden auf Orthogonalität.",
        GeometrieAnschlussArten.Gerade.id,
        GeometrieAnschlussArten.Gerade.id,
    )

    val GeometrischeGleichheit = aussageVorlage(
        "mathematik.geometrie.gleichheit",
        "Geometrische Gleichheit",
        "Prüft geometrische Koinzidenz unabhängig von der Struktur der Konstruktion.",
        GeometrieAnschlussArten.Objekt.id,
        GeometrieAnschlussArten.Objekt.id,
    )

    val StreckenKongruenz = aussageVorlage(
        "mathematik.geometrie.streckenKongruenz",
        "Streckenkongruenz",
        "Prüft zwei Strecken auf Kongruenz.",
        GeometrieAnschlussArten.Strecke.id,
        GeometrieAnschlussArten.Strecke.id,
    )

    val WinkelKongruenz = aussageVorlage(
        "mathematik.geometrie.winkelKongruenz",
        "Winkelkongruenz",
        "Prüft zwei Winkel auf Kongruenz.",
        GeometrieAnschlussArten.Winkel.id,
        GeometrieAnschlussArten.Winkel.id,
    )

    val ZuStruktur = KnotenVorlage(
        "mathematik.geometrie.zuStruktur",
        "Geometrie zu Struktur",
        "Geometrie: Struktur",
        "Zerlegt ein geometrisches Objekt in Zellstufen C₀ bis Cₙ.",
        GraphGröße(250f, 110f),
        listOf(eingang("objekt", GeometrieAnschlussArten.Objekt.id), ausgang("struktur", GeometrieAnschlussArten.Struktur.id)),
    )

    val ZuTrägermenge = KnotenVorlage(
        "mathematik.geometrie.zuTrägermenge",
        "Geometrie zu Trägermenge",
        "Geometrie: Mengen",
        "Erzeugt ausdrücklich die intrinsische Punktmenge eines geometrischen Objekts.",
        GraphGröße(270f, 110f),
        listOf(eingang("objekt", GeometrieAnschlussArten.Objekt.id), ausgang("menge", MathematikAnschlussArten.Menge.id)),
    )

    val KoordinatenBild = KnotenVorlage(
        "mathematik.geometrie.koordinatenBild",
        "Koordinatenbild",
        "Geometrie: Mengen",
        "Erzeugt die Menge der Zahlentupel eines geometrischen Objekts bezüglich eines Koordinatensystems.",
        GraphGröße(270f, 120f),
        listOf(
            eingang("objekt", GeometrieAnschlussArten.Objekt.id, 0),
            eingang("system", GeometrieAnschlussArten.Koordinatensystem.id, 1),
            ausgang("menge", MathematikAnschlussArten.Menge.id),
        ),
    )

    val SpalteZuTupel = KnotenVorlage(
        "mathematik.spalteZuTupel",
        "Spalte zu Tupel",
        "Vektoren",
        "Legt die Spaltenorientierung ausdrücklich ab und erzeugt ein Zahlentupel.",
        GraphGröße(225f, 105f),
        listOf(eingang("vektor", MathematikAnschlussArten.SpaltenVektor.id), ausgang("tupel", GeometrieAnschlussArten.KoordinatenTupel.id)),
    )

    val ZeileZuTupel = KnotenVorlage(
        "mathematik.zeileZuTupel",
        "Zeile zu Tupel",
        "Vektoren",
        "Legt die Zeilenorientierung ausdrücklich ab und erzeugt ein Zahlentupel.",
        GraphGröße(225f, 105f),
        listOf(eingang("vektor", MathematikAnschlussArten.ZeilenVektor.id), ausgang("tupel", GeometrieAnschlussArten.KoordinatenTupel.id)),
    )

    val LinearePunkttransformation = KnotenVorlage(
        "mathematik.geometrie.punktTransformationLinear",
        "Lineare Punkttransformation",
        "Geometrie: Transformationen",
        "Berechnet tupel(Matrix · spalte(tupel)).",
        GraphGröße(285f, 120f),
        listOf(
            eingang("punkt", MathematikAnschlussArten.Tupel.id, 0),
            eingang("matrix", MathematikAnschlussArten.Matrix.id, 1),
            ausgang("bild", GeometrieAnschlussArten.KoordinatenTupel.id),
        ),
    )

    val AffinePunkttransformation = KnotenVorlage(
        "mathematik.geometrie.punktTransformationAffin",
        "Affine Punkttransformation",
        "Geometrie: Transformationen",
        "Berechnet tupel(Matrix · spalte(punkt) + spalte(translation)).",
        GraphGröße(285f, 135f),
        listOf(
            eingang("punkt", MathematikAnschlussArten.Tupel.id, 0),
            eingang("matrix", MathematikAnschlussArten.Matrix.id, 1),
            eingang("translation", MathematikAnschlussArten.Tupel.id, 2),
            ausgang("bild", GeometrieAnschlussArten.KoordinatenTupel.id),
        ),
    )

    val LineareTransformation = KnotenVorlage(
        "mathematik.geometrie.lineareTransformation",
        "Lineare Geometrietransformation",
        "Geometrie: Transformationen",
        "Erzeugt eine lineare Raumabbildung aus Quellraum, Zielraum und Matrix.",
        GraphGröße(285f, 135f),
        listOf(
            eingang("quelle", GeometrieAnschlussArten.Raum.id, 0),
            eingang("ziel", GeometrieAnschlussArten.Raum.id, 1),
            eingang("matrix", MathematikAnschlussArten.Matrix.id, 2),
            ausgang("transformation", GeometrieAnschlussArten.Transformation.id),
        ),
    )

    val AffineTransformation = KnotenVorlage(
        "mathematik.geometrie.affineTransformation",
        "Affine Geometrietransformation",
        "Geometrie: Transformationen",
        "Erzeugt eine affine Raumabbildung A·x+b.",
        GraphGröße(285f, 145f),
        listOf(
            eingang("quelle", GeometrieAnschlussArten.Raum.id, 0),
            eingang("ziel", GeometrieAnschlussArten.Raum.id, 1),
            eingang("matrix", MathematikAnschlussArten.Matrix.id, 2),
            eingang("translation", MathematikAnschlussArten.Tupel.id, 3),
            ausgang("transformation", GeometrieAnschlussArten.Transformation.id),
        ),
    )

    val Transformieren = KnotenVorlage(
        "mathematik.geometrie.transformieren",
        "Geometrie transformieren",
        "Geometrie: Transformationen",
        "Wendet eine geometrische Transformation strukturerhaltend auf ein Objekt an.",
        GraphGröße(265f, 120f),
        listOf(
            eingang("objekt", GeometrieAnschlussArten.Objekt.id, 0),
            eingang("transformation", GeometrieAnschlussArten.Transformation.id, 1),
            ausgang("bild", GeometrieAnschlussArten.Objekt.id),
        ),
    )

    val Visualisierung = KnotenVorlage(
        "mathematik.geometrie.visualisierung",
        "Geometrievisualisierung",
        "Geometrie: Darstellung",
        "Stellt geometrische Räume und Objekte in 1D, 2D oder 3D dar und reicht das Objekt unverändert weiter.",
        GraphGröße(620f, 470f),
        listOf(
            eingang("objekt", GeometrieAnschlussArten.Objekt.id),
            ausgang("objekt", GeometrieAnschlussArten.Objekt.id),
        ),
    )

    val alle = listOf(
        Raum,
        StandardKoordinatensystem,
        PunktAusKoordinaten,
        GeradeDurchPunkte,
        Strecke,
        Strahl,
        Winkel,
        Kreislinie,
        Polygon,
        Gruppe,
        Inzidenz,
        Zwischen,
        Kollinear,
        Parallel,
        Orthogonal,
        GeometrischeGleichheit,
        StreckenKongruenz,
        WinkelKongruenz,
        ZuStruktur,
        ZuTrägermenge,
        KoordinatenBild,
        SpalteZuTupel,
        ZeileZuTupel,
        LinearePunkttransformation,
        AffinePunkttransformation,
        LineareTransformation,
        AffineTransformation,
        Transformieren,
        Visualisierung,
    )

    private fun aussageVorlage(
        art: String,
        name: String,
        beschreibung: String,
        links: AnschlussArtId,
        rechts: AnschlussArtId,
    ) = KnotenVorlage(
        art,
        name,
        "Geometrie: Relationen",
        beschreibung,
        GraphGröße(245f, 115f),
        listOf(eingang("links", links, 0), eingang("rechts", rechts, 1), ausgang("aussage", MathematikAnschlussArten.Aussage.id)),
    )
}
