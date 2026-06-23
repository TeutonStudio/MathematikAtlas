package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.Rechnung

object GanzeZahlen: ZahlenMenge {
    override fun zuLatex(): String = "\\mathbb{Z}"

    override fun enthält(element: Element): Boolean {
        if (element !is Zahl) return false
        return NatürlicheZahlen.enthält(element) || NatürlicheZahlen.enthält(element.negiert())
    }

    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }

//    override fun istKommutativ(arg: Rechnung): Boolean = arg !is de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.subtraktion
//
//    override fun istAssoziativ(arg: Rechnung): Boolean = arg !is de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.subtraktion

//    override fun istDistributiv(
//        äußere: Rechnung,
//        innere: Rechnung
//    ): Boolean = äußere is de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.multiplikation &&
//            innere !is de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.multiplikation
//
//    override fun istDistributivInvers(
//        äußere: Rechnung,
//        innere: Rechnung
//    ): Boolean = istDistributiv(äußere, innere)
}
