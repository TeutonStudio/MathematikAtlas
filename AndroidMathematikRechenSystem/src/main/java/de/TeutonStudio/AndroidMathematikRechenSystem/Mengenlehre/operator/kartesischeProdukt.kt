package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.operator

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Menge
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.iterationen.Dupel

class binärKartesischeProdukt<EL: Element, ER: Element, ML: Menge<EL>, MR: Menge<ER>>(
    val links: ML,
    val rechts: MR,
): binärMengenOperator<EL,ER,ML,MR> {
    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }

    override fun enthält(element: Dupel<EL, ER>): Boolean {
        TODO("Not yet implemented")
    }

    override fun kleinsteOberMenge(): Menge<out Element> = this
    override fun zuLatex(): String {
        TODO("Not yet implemented")
    }
}