package de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.vordefiniert

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl

object imaginäreKonstante: Zahl {
    override val dimension = 2
    override fun negiert(): Zahl {
        TODO("Not yet implemented")
    }

    override fun konjugiert(): Zahl = negiert()
    override fun realteil(): Zahl = addititvNeutral
    override fun imaginärteil(): Zahl = multiplikativNeutral

    override fun zuLatex(): String {
        TODO("Not yet implemented")
    }

    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }
}