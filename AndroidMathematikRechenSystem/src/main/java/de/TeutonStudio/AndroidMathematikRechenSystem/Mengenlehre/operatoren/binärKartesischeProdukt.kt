package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Menge

class binärKartesischeProdukt<EL: Element, ER: Element, ML: Menge<EL>, MR: Menge<ER>>(
    val links: ML,
    val rechts: MR,
): binärMengenOperator<EL,ER,ML,MR> {
    override fun vereinfacht(): MathematischesObjekt = this

    override fun enthält(element: Element): Boolean? = null

    override fun zuLatex(): String = "\\left(${links.zuLatex()} \\times ${rechts.zuLatex()}\\right)"
}
