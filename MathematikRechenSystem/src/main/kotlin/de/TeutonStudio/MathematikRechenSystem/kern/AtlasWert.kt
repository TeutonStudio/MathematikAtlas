package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Domänenneutraler Laufzeitwert des Atlas.
 *
 * Dieser Vertrag trägt absichtlich weder mathematische Mengen-/Termsemantik noch
 * eine LaTeX-Darstellung. Mathematik, Darstellung und spätere Engine-/Scriptwerte
 * hängen sich ausschließlich über ihre eigenen Capabilities darunter.
 */
interface AtlasWert

/**
 * Neutrale UI-Projektion für beliebige Atlaswerte.
 *
 * Mathematische Werte behalten ihre strukturierte LaTeX-Darstellung. Methoden ohne
 * Mathematik-Capability werden über ihren Namen dargestellt; alle übrigen Werte
 * verwenden ihre normale Textdarstellung. Damit wird `zuLatex()` nicht zum
 * universellen Wertvertrag hochgezogen.
 */
fun AtlasWert.zuAtlasAnzeigeText(): String = when (this) {
    is MathematischesObjekt -> zuStrukturLatex()
    is Methode -> name
    else -> toString()
}
