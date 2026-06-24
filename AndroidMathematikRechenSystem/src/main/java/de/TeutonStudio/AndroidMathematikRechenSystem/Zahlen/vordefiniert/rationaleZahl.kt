package de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.vordefiniert

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl

class rationaleZahl(val wert: Float): Zahl {
    override val dimension = 1
    override fun realteil(): Zahl = this
    override fun imaginärteil(): Zahl = addititvNeutral
    override fun negiert(): Zahl = rationaleZahl(-wert)
    override fun kehrwert(): Zahl {
        // kleinster zähler und nenner für float herausfinden
        return super.kehrwert()
    }

    override fun zuLatex(): String {
        TODO("Not yet implemented")
    }

    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }
}