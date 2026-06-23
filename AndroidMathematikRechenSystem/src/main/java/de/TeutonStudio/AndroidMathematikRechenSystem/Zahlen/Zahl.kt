package de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.multiplikation
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.vordefiniert.multiplikativNeutral

// TODO eine Zahl
interface Zahl: MathematischesObjekt, Element {
    val dimension: Int

    fun negiert(): Zahl = multiplikation(multiplikativNeutral.negiert(),this)
    public fun konjugiert(): Zahl

    public fun realteil(): Zahl
    public fun imaginärteil(): Zahl
}