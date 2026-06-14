package de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Menge
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.operator.ElementOperator
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert.ZahlenMenge
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl

class Interval<T: Menge<Zahl>>: ElementOperator, ZahlenMenge {
    override fun zuLatex(): String {
        TODO("Not yet implemented")
    }

    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }

    override fun kleinsteOberMenge(): Menge<out Element> {
        TODO("Not yet implemented")
    }

    override fun istKommutativ(arg: Rechnung): Boolean {
        TODO("Not yet implemented")
    }

    override fun istAssoziativ(arg: Rechnung): Boolean {
        TODO("Not yet implemented")
    }

    override fun istDistributiv(
        äußere: Rechnung,
        innere: Rechnung
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun istDistributivInvers(
        äußere: Rechnung,
        innere: Rechnung
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun enthält(element: Zahl): Boolean {
        TODO("Not yet implemented")
    }
}