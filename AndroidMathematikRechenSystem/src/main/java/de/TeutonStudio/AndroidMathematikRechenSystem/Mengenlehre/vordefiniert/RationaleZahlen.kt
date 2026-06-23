package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Relationen.Gleichheit
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.Rechnung
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.addition
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.distanz
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.division
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.multiplikation
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.signum
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.subtraktion

object RationaleZahlen: ZahlenMenge {
    override fun zuLatex(): String = "\\mathbb{Q}"
    override fun enthält(element: Element): Boolean {
        if (element !is Zahl) return false
        return GanzeZahlen.enthält(element) || when {
            element is addition -> false
            element is subtraktion -> RationaleZahlen.enthält(element.subtrahend) && RationaleZahlen.enthält(element.minuend)
            element is multiplikation -> false
            element is division -> if (element.divident.konjugiert() == element.divident) RationaleZahlen.enthält(multiplikation(signum(element.divident),element.divisor)) && RationaleZahlen.enthält(distanz(element.divident)) else false
            else -> false
        }
    }
    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }

//    override fun istKommutativ(arg: Rechnung): Boolean {
//        TODO("Not yet implemented")
//    }
//
//    override fun istAssoziativ(arg: Rechnung): Boolean {
//        TODO("Not yet implemented")
//    }

//    override fun istDistributiv(
//        äußere: Rechnung,
//        innere: Rechnung
//    ): Boolean {
//        TODO("Not yet implemented")
//    }
//
//    override fun istDistributivInvers(
//        äußere: Rechnung,
//        innere: Rechnung
//    ): Boolean {
//        TODO("Not yet implemented")
//    }

}
