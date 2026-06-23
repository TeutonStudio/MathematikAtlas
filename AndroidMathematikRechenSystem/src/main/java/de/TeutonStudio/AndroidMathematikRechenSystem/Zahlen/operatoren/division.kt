package de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl

class division(
    val divisor: Zahl,
    val divident: Zahl,
): Rechnung {
    override val dimension = 1 // TODO
    override fun zuLatex(): String {
        TODO("Not yet implemented")
    }

    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }

    override fun negiert(): Zahl {
        TODO("Not yet implemented")
    }

    override fun konjugiert(): Zahl {
        TODO("Not yet implemented")
    }
}