package de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.vordefiniert

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl

class ganzeZahl(val wert: Int): Zahl {
    override val dimension = 1
    override fun negiert(): Zahl {
        if (wert == -1) return multiplikativNeutral
        return natürlicheZahl(-wert)
    }

    override fun kehrwert(): Zahl {
        if (wert == -1) return this
        return super.kehrwert()
    }

    override fun realteil(): Zahl = this
    override fun imaginärteil(): Zahl = addititvNeutral
    override fun konjugiert(): Zahl = this

    override fun zuLatex(): String {
        TODO("Not yet implemented")
    }

    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }
}