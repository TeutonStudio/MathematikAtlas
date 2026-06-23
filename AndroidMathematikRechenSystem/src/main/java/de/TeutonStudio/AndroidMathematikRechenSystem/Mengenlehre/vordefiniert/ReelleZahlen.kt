package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.Rechnung

object ReelleZahlen: ZahlenMenge {
    override fun zuLatex(): String = "\\mathbb{R}"
    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }

//    override fun istKommutativ(arg: Rechnung): Boolean = true
//
//    override fun istAssoziativ(arg: Rechnung): Boolean = true
//
//    override fun istDistributiv(
//        äußere: Rechnung,
//        innere: Rechnung
//    ): Boolean = super.istDistributiv(äußere, innere)
//
//    override fun istDistributivInvers(
//        äußere: Rechnung,
//        innere: Rechnung
//    ): Boolean = super.istDistributivInvers(äußere, innere)
}
