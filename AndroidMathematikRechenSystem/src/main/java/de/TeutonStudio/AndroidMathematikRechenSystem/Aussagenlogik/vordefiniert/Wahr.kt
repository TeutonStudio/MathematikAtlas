package de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.vordefiniert

import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.Aussage
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt

object Wahr: Aussage {
    override val istWahr = true
    override val istLüge = false

    override fun zuLatex(): String = "\\top"

    override fun vereinfacht(): MathematischesObjekt = this
}
