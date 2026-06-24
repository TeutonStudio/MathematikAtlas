package de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.vordefiniert

import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.Aussage
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt

object Lüge: Aussage {
    override val istWahr = false
    override val istLüge = true

    override fun zuLatex(): String = "\\bot"

    override fun vereinfacht(): MathematischesObjekt = this
}
