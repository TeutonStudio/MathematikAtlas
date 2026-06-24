package de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.vordefiniert

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.multiplikation

class natürlicheZahl(val wert: Int): Zahl {
    override val dimension = 1
    override fun realteil(): Zahl = this
    override fun imaginärteil(): Zahl = addititvNeutral
    override fun negiert(): Zahl = ganzeZahl(-wert)

    override fun zuLatex(): String {
        TODO("Not yet implemented")
    }

    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }

}