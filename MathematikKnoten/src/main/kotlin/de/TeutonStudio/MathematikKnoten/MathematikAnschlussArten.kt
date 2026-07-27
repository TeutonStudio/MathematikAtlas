package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*

object MathematikAnschlussArten {
    val Objekt = AnschlussArt(AnschlussArtId("mathematik.objekt"), "Mathematisches Objekt")
    val Zahl = AnschlussArt(AnschlussArtId("mathematik.zahl"), "Zahl", Objekt.id)
    val Aussage = AnschlussArt(AnschlussArtId("mathematik.aussage"), "Aussage", Objekt.id)
    val Menge = AnschlussArt(AnschlussArtId("mathematik.menge"), "Menge", Objekt.id)
    val Vektor = AnschlussArt(AnschlussArtId("mathematik.vektor"), "Vektor", Objekt.id)
    val Matrix = AnschlussArt(AnschlussArtId("mathematik.matrix"), "Matrix", Objekt.id)
    val Funktion = AnschlussArt(AnschlussArtId("mathematik.funktion"), "Funktion", Objekt.id)
    val ZahlFunktion = AnschlussArt(AnschlussArtId("mathematik.funktion.zahl"), "Zahlfunktion", Funktion.id)
    val MengenFunktion = AnschlussArt(AnschlussArtId("mathematik.funktion.menge"), "Mengenfunktion", Funktion.id)
    val alle = listOf(Objekt, Zahl, Aussage, Menge, Vektor, Matrix, Funktion, ZahlFunktion, MengenFunktion)
}
