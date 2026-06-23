package de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.vordefiniert.addititvNeutral

class distanz(val argument: Zahl): Rechnung {
    override val dimension = 1
    override fun zuLatex(): String {
        TODO("Not yet implemented")
    }

    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }

    override fun negiert(): Zahl {
        TODO("Not yet implemented")
    }

    override fun konjugiert(): Zahl = this
    override fun realteil(): Zahl {
        TODO("Not yet implemented")
    }

    override fun imaginärteil(): Zahl = addititvNeutral
}