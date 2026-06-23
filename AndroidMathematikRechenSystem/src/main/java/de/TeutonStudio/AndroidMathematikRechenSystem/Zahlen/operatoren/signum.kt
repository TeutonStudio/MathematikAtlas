package de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert.ReelleZahlen
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.vordefiniert.addititvNeutral
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.vordefiniert.multiplikativNeutral

class signum(val argument: Zahl): Rechnung {
    override val dimension = 1

    override fun zuLatex(): String {
        TODO("Not yet implemented")
    }

    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }

    override fun negiert(): Zahl = multiplikation(multiplikativNeutral.negiert(),this)

    override fun konjugiert(): Zahl = this
    override fun realteil(): Zahl = this
    override fun imaginärteil(): Zahl = addititvNeutral
}