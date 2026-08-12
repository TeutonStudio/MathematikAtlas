package de.TeutonStudio.MathematikRechenSystem.kern

sealed interface MathematischesObjekt {
    fun zuLatex(): String
}

/**
 * Nicht rein mathematische, aber über den bestehenden Atlas-Wertkanal transportierbare
 * Darstellungswerte. Der Übergangsvertrag hält Grafikwerte vom mathematischen Ausdrucksbaum
 * getrennt, bis der allgemeine Laufzeitwertkanal vollständig aus dem Mathematikadapter gelöst ist.
 */
interface DarstellungsWert : MathematischesObjekt

/** Gemeinsamer Obervertrag für strukturierte Grafikformate wie SVG und später TikZ. */
interface Grafik : DarstellungsWert

/** Ein benannter, bei einer [Methode] bindbarer Parameter. */
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
