package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Domänenneutraler Laufzeitwert des Atlas.
 *
 * Der Vertrag besitzt absichtlich keinerlei Mathematik-, LaTeX-, Mengen- oder
 * Auswertungssemantik. Script-, Engine- und Darstellungswerte können ihn deshalb
 * implementieren, ohne künstlich zu mathematischen Objekten zu werden.
 */
interface AtlasWert

/** Optionale Darstellungscapability. Sie ist ausdrücklich kein Bestandteil von [AtlasWert]. */
interface LatexDarstellbar {
    fun zuLatex(): String
}

interface MathematischesObjekt : AtlasWert, LatexDarstellbar

/** Nichtmathematischer, über den allgemeinen Atlas-Wertkanal transportierbarer Darstellungswert. */
interface DarstellungsWert : AtlasWert, LatexDarstellbar

/** Gemeinsamer Obervertrag für strukturierte Grafikformate wie SVG und später TikZ. */
interface Grafik : DarstellungsWert

/** Ein benannter, bei einer mathematischen [Methode] bindbarer Parameter. */
sealed interface MethodenParameter : MathematischesObjekt {
    val name: String
}

sealed interface Ausdruck : MathematischesObjekt
sealed interface ZahlAusdruck : Ausdruck
sealed interface MengenAusdruck : Ausdruck

/**
 * Intrinsisches geometrisches Objekt in einem euklidischen Raum.
 *
 * Geometrische Objekte sind ausdrücklich keine [MengenAusdruck]-Instanzen. Ihre
 * Trägermenge, ihr Koordinatenbild und ihre Zellstruktur entstehen nur durch
 * benannte Konvertierungen.
 */
interface GeometrischerAusdruck : Ausdruck {
    val raum: EuklidischerRaum
}

interface MathematischesKonzept<T : MathematischesObjekt> {
    val artId: String
    fun passt(objekt: MathematischesObjekt): Boolean
    fun normalisiere(objekt: T, kontext: RechenKontext = RechenKontext()): T
}

data class RechenKontext(
    val annahmen: Set<Aussage> = emptySet(),
    val dezimalstellen: Int = 34,
)
