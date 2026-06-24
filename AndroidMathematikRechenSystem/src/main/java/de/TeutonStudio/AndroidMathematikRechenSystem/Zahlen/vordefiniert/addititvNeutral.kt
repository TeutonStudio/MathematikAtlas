package de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.vordefiniert

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl

// Die 0
object addititvNeutral: Zahl {
    override val dimension = 1

    override fun negiert(): Zahl = this
    override fun kehrwert(): Zahl {
        TODO(" / 0 nicht möglich")
        return super.kehrwert()
    }
    override fun konjugiert(): Zahl = this
    override fun realteil(): Zahl = this
    override fun imaginärteil(): Zahl = this

    override fun zuLatex(): String {
        TODO("Not yet implemented")
    }

    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }
}