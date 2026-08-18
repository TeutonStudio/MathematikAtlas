package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

object MathematikAnschlussArten {
    /** Domänenneutraler Oberanschluss für alle Atlas-Laufzeitwerte. */
    val AtlasWert = AnschlussArt(
        id = AnschlussArtId("atlas.wert"),
        name = "Atlas-Wert",
        beschreibung = "Beliebiger Laufzeitwert des Atlas ohne mathematische Semantik.",
    )
    val Objekt = AnschlussArt(
        id = AnschlussArtId("mathematik.objekt"),
        name = "Mathematisches Objekt",
        elternArt = AtlasWert.id,
        beschreibung = "Beliebiges mathematisches Objekt.",
    )
    val Zahl = AnschlussArt(
        id = AnschlussArtId("mathematik.zahl"),
        name = "Zahl",
        elternArt = Objekt.id,
        beschreibung = "Ein skalarer numerischer Wert.",
    )
    val Aussage = AnschlussArt(
        id = AnschlussArtId("mathematik.aussage"),
        name = "Aussage",
        elternArt = Objekt.id,
        beschreibung = "Eine mathematische Aussage mit einem Wahrheitswert.",
    )
    val Menge = AnschlussArt(
        id = AnschlussArtId("mathematik.menge"),
        name = "Menge",
        elternArt = Objekt.id,
        beschreibung = "Eine Menge mathematischer Objekte.",
    )
    val Topologie = AnschlussArt(
        id = AnschlussArtId("mathematik.topologie"),
        name = "Topologie",
        elternArt = Objekt.id,
        beschreibung = "Eine Topologie τ auf einer expliziten Trägermenge.",
    )
    val TopologischerRaum = AnschlussArt(
        id = AnschlussArtId("mathematik.raum.topologisch"),
        name = "Topologischer Raum",
        elternArt = Objekt.id,
        beschreibung = "Eine Trägermenge zusammen mit einer expliziten Topologie (X,τ).",
    )
    val MetrischerRaum = AnschlussArt(
        id = AnschlussArtId("mathematik.raum.metrisch"),
        name = "Metrischer Raum",
        elternArt = TopologischerRaum.id,
        beschreibung = "Ein metrischer Raum (X,d), der über τ_d zugleich eine topologische Struktur trägt.",
    )
    val Mass = AnschlussArt(
        id = AnschlussArtId("mathematik.mass"),
        name = "Maß",
        elternArt = Objekt.id,
        beschreibung = "Ein Maß, das messbaren Mengen nichtnegative Größenwerte zuordnet.",
    )
    val Vektor = AnschlussArt(
        id = AnschlussArtId("mathematik.vektor"),
        name = "Vektor",
        elternArt = Objekt.id,
        beschreibung = "Ein Vektor ohne festgelegte Zeilen- oder Spaltenorientierung.",
    )
    val SpaltenVektor = AnschlussArt(
        id = AnschlussArtId("mathematik.vektor.spalte"),
        name = "Spaltenvektor",
        elternArt = Vektor.id,
        beschreibung = "Ein als Spalte orientierter Vektor.",
    )
    val ZeilenVektor = AnschlussArt(
        id = AnschlussArtId("mathematik.vektor.zeile"),
        name = "Zeilenvektor",
        elternArt = Vektor.id,
        beschreibung = "Ein als Zeile orientierter Vektor.",
    )
    val Matrix = AnschlussArt(
        id = AnschlussArtId("mathematik.matrix"),
        name = "Matrix",
        elternArt = Objekt.id,
        beschreibung = "Eine zweidimensionale rechteckige Anordnung mathematischer Einträge.",
    )
    val Tensor = AnschlussArt(
        id = AnschlussArtId("mathematik.tensor"),
        name = "Tensor",
        elternArt = Objekt.id,
        beschreibung = "Ein mehrdimensionales, indiziertes mathematisches Objekt.",
    )
    val Tupel = AnschlussArt(
        id = AnschlussArtId("mathematik.tupel"),
        name = "Tupel",
        elternArt = Objekt.id,
        beschreibung = "Eine geordnete endliche Folge mathematischer Objekte.",
    )

    /** Gemeinsame Grafikdomäne für SVG und spätere strukturierte Ausgabeformate. */
    val Grafik = AnschlussArt(
        id = AnschlussArtId("grafik"),
        name = "Grafik",
        elternArt = AtlasWert.id,
        beschreibung = "Eine strukturierte, nichtmathematische Grafik, die durch Grafikknoten schrittweise aufgebaut wird.",
    )
    val SvgGrafik = AnschlussArt(
        id = AnschlussArtId("grafik.svg"),
        name = "SVG-Grafik",
        elternArt = Grafik.id,
        beschreibung = "Ein unveränderlicher SVG-AST mit ViewBox, Definitionen und sichtbaren Elementen.",
    )
    val SvgStil = AnschlussArt(
        id = AnschlussArtId("grafik.svg.stil"),
        name = "SVG-Stil",
        elternArt = AtlasWert.id,
        beschreibung = "Wiederverwendbare Füll-, Kontur- und Linienattribute für SVG-Ergänzungen.",
    )

    /** Einzige produktive Anschlussart für sämtliche Methoden. */
    val Methode = AnschlussArt(
        id = AnschlussArtId("mathematik.methode"),
        name = "Methode",
        elternArt = AtlasWert.id,
        beschreibung = "Eine aufrufbare Methode mit neutraler Tupel-zu-Tupel-Signatur.",
    )

    /** Quellkompatible Namen; alle liefern dieselbe produktive Anschlussart. */
    val AussageMethode = Methode
    val ZahlMethode = Methode
    val MengenMethode = Methode
    val SpaltenVektorMethode = Methode
    val ZeilenVektorMethode = Methode

    /** Ausschließlich zum verlustfreien Laden alter Karten registrierte IDs. */
    val LegacyMethode = AnschlussArt(
        id = AnschlussArtId("mathematik.funktion"),
        name = "Methode (alt)",
        elternArt = Methode.id,
        beschreibung = "Historische Anschluss-ID für eine allgemeine Methode.",
    )
    val LegacyAussageMethode = AnschlussArt(
        id = AnschlussArtId("mathematik.funktion.aussage"),
        name = "Prädikatsmethode (alt)",
        elternArt = Methode.id,
        beschreibung = "Historische Anschluss-ID für eine Methode mit Aussage als Ergebnis.",
    )
    val LegacyZahlMethode = AnschlussArt(
        id = AnschlussArtId("mathematik.funktion.zahl"),
        name = "Zahlmethode (alt)",
        elternArt = Methode.id,
        beschreibung = "Historische Anschluss-ID für eine Methode mit Zahl als Ergebnis.",
    )
    val LegacyMengenMethode = AnschlussArt(
        id = AnschlussArtId("mathematik.funktion.menge"),
        name = "Mengenmethode (alt)",
        elternArt = Methode.id,
        beschreibung = "Historische Anschluss-ID für eine Methode mit Menge als Ergebnis.",
    )
    val LegacySpaltenVektorMethode = AnschlussArt(
        id = AnschlussArtId("mathematik.funktion.vektor.spalte"),
        name = "Spaltenvektormethode (alt)",
        elternArt = Methode.id,
        beschreibung = "Historische Anschluss-ID für eine Methode mit Spaltenvektor als Ergebnis.",
    )
    val LegacyZeilenVektorMethode = AnschlussArt(
        id = AnschlussArtId("mathematik.funktion.vektor.zeile"),
        name = "Zeilenvektormethode (alt)",
        elternArt = Methode.id,
        beschreibung = "Historische Anschluss-ID für eine Methode mit Zeilenvektor als Ergebnis.",
    )

    val historischeMethodenIds: Set<AnschlussArtId> = setOf(
        LegacyMethode.id,
        LegacyAussageMethode.id,
        LegacyZahlMethode.id,
        LegacyMengenMethode.id,
        LegacySpaltenVektorMethode.id,
        LegacyZeilenVektorMethode.id,
    )

    fun normalisiereMethodenArt(art: AnschlussArtId): AnschlussArtId =
        if (art in historischeMethodenIds) Methode.id else art

    val alle = listOf(
        AtlasWert,
        Objekt,
        Zahl,
        Aussage,
        Menge,
        Topologie,
        TopologischerRaum,
        MetrischerRaum,
        Mass,
        Vektor,
        SpaltenVektor,
        ZeilenVektor,
        Matrix,
        Tensor,
        Tupel,
        Grafik,
        SvgGrafik,
        SvgStil,
        Methode,
        LegacyMethode,
        LegacyAussageMethode,
        LegacyZahlMethode,
        LegacyMengenMethode,
        LegacySpaltenVektorMethode,
        LegacyZeilenVektorMethode,
    ) + GeometrieAnschlussArten.alle
}
