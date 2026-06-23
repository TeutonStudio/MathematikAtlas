package de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.vordefiniert

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl

// Die 1
object multiplikativNeutral: Zahl {
    override val dimension = 1
    override fun negiert(): Zahl {
        TODO("Keine -1 implementiert")
    }

    override fun konjugiert(): Zahl = this
    override fun realteil(): Zahl = this
    override fun imaginärteil(): Zahl = addititvNeutral

    override fun zuLatex(): String {
        TODO("Not yet implemented")
    }

    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }
}