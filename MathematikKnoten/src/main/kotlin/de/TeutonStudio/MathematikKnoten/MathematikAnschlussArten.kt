package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

object MathematikAnschlussArten {
    val Objekt = AnschlussArt(
        id = AnschlussArtId("mathematik.objekt"),
        name = "Mathematisches Objekt",
        beschreibung = "Beliebiges mathematisches Objekt; Oberart der allgemeinen Atlas-Datentypen.",
    )
    val Zahl = AnschlussArt(
        id = AnschlussArtId("mathematik.zahl"),
        name = "Zahl",
        elternArt = Objekt.id,
        beschreibung = "Ein skalarer numerischer Wert.",
    )
    val Angulus = AnschlussArt(
        id = AnschlussArtId("mathematik.angulus"),
        name = "Angulus",
        elternArt = Objekt.id,
        beschreibung = "Ein Winkel mit expliziter Einheit (Radian, Grad oder Gon) und Dimensionsbezug.",
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
    val KartesischesTupel = AnschlussArt(
        id = AnschlussArtId("mathematik.tupel.kartesisch"),
        name = "Kartesisches Tupel",
        elternArt = Tupel.id,
        beschreibung = "Ein Tupel, dessen Komponenten ausschließlich Zahlen sind.",
    )
    val PolarTupel = AnschlussArt(
        id = AnschlussArtId("mathematik.tupel.polar"),
        name = "PolarTupel",
        elternArt = Tupel.id,
        beschreibung = "Ein Tupel aus Radius (Zahl) und mindestens einem Angulus.",
    )

    /** Einzige produktive Anschlussart für sämtliche Methoden. */
    val Methode = AnschlussArt(
        id = AnschlussArtId("mathematik.methode"),
        name = "Methode",
        elternArt = Objekt.id,
        beschreibung = "Eine aufrufbare Methode bzw. Abbildung mit Argumenten und Ergebnis.",
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
        Objekt,
        Zahl,
        Angulus,
        Aussage,
        Menge,
        Mass,
        Vektor,
        SpaltenVektor,
        ZeilenVektor,
        Matrix,
        Tensor,
        Tupel,
        KartesischesTupel,
        PolarTupel,
        Methode,
        LegacyMethode,
        LegacyAussageMethode,
        LegacyZahlMethode,
        LegacyMengenMethode,
        LegacySpaltenVektorMethode,
        LegacyZeilenVektorMethode,
    ) + GeometrieAnschlussArten.alle
}
