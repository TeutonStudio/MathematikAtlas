package de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.vordefiniert

import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.Aussage
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt

class Lüge: Aussage {
    override fun istWahr(): Boolean = false
    override fun istLüge(): Boolean = true

    override fun zuLatex(): String = "\\bot"

    override fun vereinfacht(): MathematischesObjekt = this
}
