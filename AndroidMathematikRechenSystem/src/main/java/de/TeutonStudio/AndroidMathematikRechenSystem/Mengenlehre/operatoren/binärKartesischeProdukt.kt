package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Menge

class binärKartesischeProdukt<EL: Element, ER: Element, ML: Menge<EL>, MR: Menge<ER>>(
    val links: ML,
    val rechts: MR,
): binärMengenOperator<EL,ER,ML,MR> {
    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }

    override fun enthält(element: Element): Boolean {
//        super.enthält(element)
        TODO("Not yet implemented")
    }

    override fun zuLatex(): String {
        TODO("Not yet implemented")
    }
}