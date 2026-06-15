package de.TeutonStudio.AndroidMathematikRechenSystem.Relationen

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Menge

class Gleichheit(
    override val links: Element,
    override val rechts: Element,
): binärRelation<Menge<out Element>,Menge<out Element>> {
    override fun zuLatex(): String {
        TODO("Not yet implemented")
    }

    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }

    override fun auswerten(): Boolean? {
        TODO("Not yet implemented")
    }
}