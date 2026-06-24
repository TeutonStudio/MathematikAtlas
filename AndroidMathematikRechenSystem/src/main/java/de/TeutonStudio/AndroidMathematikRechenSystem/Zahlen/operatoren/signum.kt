package de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert.ReelleZahlen
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.relationen.kleiner
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.vordefiniert.addititvNeutral
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.vordefiniert.multiplikativNeutral

class signum(val argument: Zahl): Rechnung {
    override val dimension = 1
    lateinit var ergebniss: Zahl
    init {
        if (argument is addititvNeutral) ergebniss = addititvNeutral
        if (argument is subtraktion) ergebniss = if (kleiner(argument.subtrahend,argument.minuend).istWahr) multiplikativNeutral else multiplikativNeutral.negiert()
        if (argument is division) ergebniss = if (kleiner(addititvNeutral, argument.divident).istWahr) multiplikativNeutral else multiplikativNeutral.negiert()
    }

    override fun negiert(): Zahl = multiplikation(multiplikativNeutral.negiert(),this)
    override fun kehrwert(): Zahl = this

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