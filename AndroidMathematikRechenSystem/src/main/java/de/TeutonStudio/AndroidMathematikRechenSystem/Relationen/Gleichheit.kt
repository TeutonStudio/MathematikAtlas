package de.TeutonStudio.AndroidMathematikRechenSystem.Relationen

import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.Aussage
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

    override fun auswerten(): Aussage {
        TODO("Not yet implemented")
    }

    override fun istWahr(): Boolean {
        TODO("Not yet implemented")
    }

    override fun istLüge(): Boolean {
        TODO("Not yet implemented")
    }
}