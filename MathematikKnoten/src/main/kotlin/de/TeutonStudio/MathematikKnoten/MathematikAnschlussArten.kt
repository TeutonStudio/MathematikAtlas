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
    val Tupel = AnschlussArt(AnschlussArtId("mathematik.tupel"), "Tupel", Objekt.id)
    val Funktion = AnschlussArt(AnschlussArtId("mathematik.funktion"), "Funktion", Objekt.id)
    val AussageFunktion = AnschlussArt(AnschlussArtId("mathematik.funktion.aussage"), "Aussagenabbildung", Funktion.id)
    val ZahlFunktion = AnschlussArt(AnschlussArtId("mathematik.funktion.zahl"), "Zahlfunktion", Funktion.id)
    val MengenFunktion = AnschlussArt(AnschlussArtId("mathematik.funktion.menge"), "Mengenfunktion", Funktion.id)
    val SpaltenVektorFunktion = AnschlussArt(AnschlussArtId("mathematik.funktion.vektor.spalte"), "Spaltenvektorfunktion", Funktion.id)
    val ZeilenVektorFunktion = AnschlussArt(AnschlussArtId("mathematik.funktion.vektor.zeile"), "Zeilenvektorfunktion", Funktion.id)

    // V2_2_MERGE_NOTE: Geometrieanschlüsse werden in einem eigenen Register definiert und hier nur additiv eingebunden.
    // Beim Rebase auf den letzten v2.1.x-Stand bestehende Anschlussarten und deren Reihenfolge nicht durch eine ältere Gesamtliste ersetzen.
    val alle = listOf(Objekt, Zahl, Aussage, Menge, Vektor, SpaltenVektor, ZeilenVektor, Matrix, Tupel, Funktion, AussageFunktion, ZahlFunktion, MengenFunktion, SpaltenVektorFunktion, ZeilenVektorFunktion)
}
