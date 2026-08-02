package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

object MathematikAnschlussArten {
    val Objekt = AnschlussArt(AnschlussArtId("mathematik.objekt"), "Mathematisches Objekt")
    val Zahl = AnschlussArt(AnschlussArtId("mathematik.zahl"), "Zahl", Objekt.id)
    val Aussage = AnschlussArt(AnschlussArtId("mathematik.aussage"), "Aussage", Objekt.id)
    val Menge = AnschlussArt(AnschlussArtId("mathematik.menge"), "Menge", Objekt.id)
    val Vektor = AnschlussArt(AnschlussArtId("mathematik.vektor"), "Vektor", Objekt.id)
    val SpaltenVektor = AnschlussArt(AnschlussArtId("mathematik.vektor.spalte"), "Spaltenvektor", Vektor.id)
    val ZeilenVektor = AnschlussArt(AnschlussArtId("mathematik.vektor.zeile"), "Zeilenvektor", Vektor.id)
    val Matrix = AnschlussArt(AnschlussArtId("mathematik.matrix"), "Matrix", Objekt.id)
    val Tensor = AnschlussArt(AnschlussArtId("mathematik.tensor"), "Tensor", Objekt.id)
    val Tupel = AnschlussArt(AnschlussArtId("mathematik.tupel"), "Tupel", Objekt.id)

    /** Einzige produktive Anschlussart für sämtliche Methoden. */
    val Methode = AnschlussArt(AnschlussArtId("mathematik.methode"), "Methode", Objekt.id)

    /** Quellkompatible Namen; alle liefern dieselbe produktive Anschlussart. */
    val AussageMethode = Methode
    val ZahlMethode = Methode
    val MengenMethode = Methode
    val SpaltenVektorMethode = Methode
    val ZeilenVektorMethode = Methode

    /** Ausschließlich zum verlustfreien Laden alter Karten registrierte IDs. */
    val LegacyMethode = AnschlussArt(AnschlussArtId("mathematik.funktion"), "Methode (alt)", Methode.id)
    val LegacyAussageMethode = AnschlussArt(AnschlussArtId("mathematik.funktion.aussage"), "Prädikatsmethode (alt)", Methode.id)
    val LegacyZahlMethode = AnschlussArt(AnschlussArtId("mathematik.funktion.zahl"), "Zahlmethode (alt)", Methode.id)
    val LegacyMengenMethode = AnschlussArt(AnschlussArtId("mathematik.funktion.menge"), "Mengenmethode (alt)", Methode.id)
    val LegacySpaltenVektorMethode = AnschlussArt(AnschlussArtId("mathematik.funktion.vektor.spalte"), "Spaltenvektormethode (alt)", Methode.id)
    val LegacyZeilenVektorMethode = AnschlussArt(AnschlussArtId("mathematik.funktion.vektor.zeile"), "Zeilenvektormethode (alt)", Methode.id)

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
        Aussage,
        Menge,
        Vektor,
        SpaltenVektor,
        ZeilenVektor,
        Matrix,
        Tensor,
        Tupel,
        Methode,
        LegacyMethode,
        LegacyAussageMethode,
        LegacyZahlMethode,
        LegacyMengenMethode,
        LegacySpaltenVektorMethode,
        LegacyZeilenVektorMethode,
    ) + GeometrieAnschlussArten.alle
}
