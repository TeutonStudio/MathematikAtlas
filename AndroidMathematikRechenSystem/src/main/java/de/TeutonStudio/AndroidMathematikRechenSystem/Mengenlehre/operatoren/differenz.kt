package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Menge

class differenz(
    val links: Menge<*>,
    val rechts: Menge<*>,
): MengenOperator<Element> {
    override fun zuLatex(): String {
        TODO("Not yet implemented")
    }

    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }

    override fun enthält(element: Element): Boolean? {
        return links.enthält(element)?.let { l -> rechts.enthält(element)?.let { r -> l && !r } }
    }
}