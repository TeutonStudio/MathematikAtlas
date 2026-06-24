package de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert.NatürlicheZahlen
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.addition
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.multiplikation
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.potenz
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.vordefiniert.addititvNeutral
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.vordefiniert.ganzeZahl
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.vordefiniert.imaginäreKonstante
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.vordefiniert.multiplikativNeutral
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.vordefiniert.natürlicheZahl
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.vordefiniert.rationaleZahl

// TODO eine Zahl
interface Zahl: MathematischesObjekt, Element {
    val dimension: Int

    fun negiert(): Zahl = multiplikation(this, multiplikativNeutral.negiert())
    fun kehrwert(): Zahl = potenz(this, multiplikativNeutral.negiert())
    public fun realteil(): Zahl
    public fun imaginärteil(): Zahl

    public fun konjugiert(): Zahl = addition(realteil(), multiplikation(imaginärteil(), imaginäreKonstante))

    public companion object {
        public fun erzeuge(wert: Int): Zahl {
            if (wert == 0) return addititvNeutral
            if (wert == 1) return multiplikativNeutral
            if (wert < 0) return ganzeZahl(wert)
            return natürlicheZahl(wert)
        }
        public fun erzeuge(wert: Float): Zahl = if (wert == wert.toInt().toFloat()) erzeuge(wert.toInt()) else rationaleZahl(wert)

        private fun istAuswertbar(z: Zahl) = z is addititvNeutral || z is multiplikativNeutral || z is natürlicheZahl || z is ganzeZahl || z is rationaleZahl
        public fun Array<out Zahl>.filterAuswertbar() = filter { istAuswertbar(it) }
        public fun Array<out Zahl>.filterNichtAuswertbar() = filter { !istAuswertbar(it) }
    }
}