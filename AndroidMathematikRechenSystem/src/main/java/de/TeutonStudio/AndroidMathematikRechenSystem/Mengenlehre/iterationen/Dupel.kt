package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.iterationen

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element

class Dupel<EL: Element, ER: Element>(
    val links: EL,
    val rechts: ER,
): ElementOperator {
    override fun zuLatex(): String = "\\left(${links.zuLatex()}, ${rechts.zuLatex()}\\right)"

    override fun vereinfacht(): MathematischesObjekt = this
}
