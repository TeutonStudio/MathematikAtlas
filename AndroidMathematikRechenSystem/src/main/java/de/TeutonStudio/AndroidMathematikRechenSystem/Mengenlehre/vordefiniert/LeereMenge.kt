package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Menge

class LeereMenge: Menge<Element> {
    override fun enthält(element: Element): Boolean = false
    override fun zuLatex(): String = "\\emptyset"

    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }
}