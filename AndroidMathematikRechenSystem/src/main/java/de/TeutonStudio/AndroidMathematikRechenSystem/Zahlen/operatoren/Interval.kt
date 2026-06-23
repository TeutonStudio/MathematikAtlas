package de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Menge
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.iterationen.ElementOperator
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert.ZahlenMenge
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl

class Interval<T: Menge<Zahl>>: ElementOperator, ZahlenMenge {
    override fun zuLatex(): String = "\\left[\\,?\\,,\\,?\\,\\right]"

    override fun vereinfacht(): MathematischesObjekt = this

    override fun istKommutativ(arg: Rechnung): Boolean = false

    override fun istAssoziativ(arg: Rechnung): Boolean = false

    override fun istDistributiv(
        äußere: Rechnung,
        innere: Rechnung
    ): Boolean = false

    override fun istDistributivInvers(
        äußere: Rechnung,
        innere: Rechnung
    ): Boolean = false

    override fun enthält(element: Element): Boolean? = null
}
