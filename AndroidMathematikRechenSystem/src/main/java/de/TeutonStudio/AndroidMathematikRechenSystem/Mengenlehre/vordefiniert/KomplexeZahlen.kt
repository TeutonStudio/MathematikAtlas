package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.Rechnung

object KomplexeZahlen: ZahlenMenge {
//    override fun istKommutativ(arg: Rechnung): Boolean {
//        TODO("Not yet implemented")
//    }
//
//    override fun istAssoziativ(arg: Rechnung): Boolean {
//        TODO("Not yet implemented")
//    }
//
    override fun enthält(element: Element): Boolean {
        if (element !is Zahl) return false
        return ReelleZahlen.enthält(element.realteil()) && ReelleZahlen.enthält(element.imaginärteil())
    }

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

    override fun zuLatex(): String {
        TODO("Not yet implemented")
    }

    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }
}