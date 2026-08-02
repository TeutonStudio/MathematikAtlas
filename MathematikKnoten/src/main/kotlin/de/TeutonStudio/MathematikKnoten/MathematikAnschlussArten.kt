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
    val Funktion = Methode
    val AussageFunktion = Methode
    val ZahlFunktion = Methode
    val MengenFunktion = Methode
    val SpaltenVektorFunktion = Methode
    val ZeilenVektorFunktion = Methode

    /** Ausschließlich zum verlustfreien Laden alter Karten registrierte IDs. */
    val LegacyFunktion = AnschlussArt(AnschlussArtId("mathematik.funktion"), "Methode (alt)", Methode.id)
    val LegacyAussageFunktion = AnschlussArt(AnschlussArtId("mathematik.funktion.aussage"), "Prädikatsmethode (alt)", Methode.id)
    val LegacyZahlFunktion = AnschlussArt(AnschlussArtId("mathematik.funktion.zahl"), "Zahlmethode (alt)", Methode.id)
    val LegacyMengenFunktion = AnschlussArt(AnschlussArtId("mathematik.funktion.menge"), "Mengenmethode (alt)", Methode.id)
    val LegacySpaltenVektorFunktion = AnschlussArt(AnschlussArtId("mathematik.funktion.vektor.spalte"), "Spaltenvektormethode (alt)", Methode.id)
    val LegacyZeilenVektorFunktion = AnschlussArt(AnschlussArtId("mathematik.funktion.vektor.zeile"), "Zeilenvektormethode (alt)", Methode.id)

    val historischeMethodenIds: Set<AnschlussArtId> = setOf(
        LegacyFunktion.id,
        LegacyAussageFunktion.id,
        LegacyZahlFunktion.id,
        LegacyMengenFunktion.id,
        LegacySpaltenVektorFunktion.id,
        LegacyZeilenVektorFunktion.id,
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
        LegacyFunktion,
        LegacyAussageFunktion,
        LegacyZahlFunktion,
        LegacyMengenFunktion,
        LegacySpaltenVektorFunktion,
        LegacyZeilenVektorFunktion,
    ) + GeometrieAnschlussArten.alle
}
