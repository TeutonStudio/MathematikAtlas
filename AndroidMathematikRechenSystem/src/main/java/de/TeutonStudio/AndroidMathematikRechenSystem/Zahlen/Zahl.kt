package de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.addition
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.multiplikation
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.potenz
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.vordefiniert.imaginäreKonstante
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.vordefiniert.multiplikativNeutral

// TODO eine Zahl
interface Zahl: MathematischesObjekt, Element {
    val dimension: Int

    fun negiert(): Zahl = multiplikation(this, multiplikativNeutral.negiert())
    fun kehrwert(): Zahl = potenz(this, multiplikativNeutral.negiert())
    public fun realteil(): Zahl
    public fun imaginärteil(): Zahl

    public fun konjugiert(): Zahl = addition(realteil(), multiplikation(imaginärteil(), imaginäreKonstante))
}