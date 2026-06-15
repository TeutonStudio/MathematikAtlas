package de.TeutonStudio.AndroidMathematikRechenSystem.Relationen

import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Menge

interface binärRelation<L: Menge<out Element>, R: Menge<out Element>>: Relation {
    val links: Element
    val rechts: Element

    public fun auswerten(): Boolean?
}