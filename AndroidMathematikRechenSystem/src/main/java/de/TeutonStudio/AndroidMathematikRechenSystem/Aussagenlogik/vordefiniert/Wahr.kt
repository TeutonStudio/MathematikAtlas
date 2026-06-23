package de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.vordefiniert

import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.Aussage
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt

class Wahr: Aussage {
    override fun istWahr(): Boolean = true
    override fun istLüge(): Boolean = false

    override fun zuLatex(): String = "\\top"

    override fun vereinfacht(): MathematischesObjekt = this
}
