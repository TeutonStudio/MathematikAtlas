package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischerOperator
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Menge
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.Rechnung

interface ZahlenMenge: MathematischesObjekt, Menge<Zahl> {
    override fun enthält(element: Element): Boolean {
        if (element !is Zahl) return false
        // TODO
        return RationaleZahlen.enthält(element)
    }
//    public fun istKommutativ(arg: Rechnung): Boolean
//    public fun istAssoziativ(arg: Rechnung): Boolean
    // frü a,b,c gilt äußere(a.innere(b,c)) = innere(äußere(a,b), äußere(a,c))
//    public fun istDistributiv(äußere: Rechnung, innere: Rechnung): Boolean
//    // frü a,b,c gilt äußere(innere(a,b),c) = innere(äußere(a,c), äußere(b,c))
//    public fun istDistributivInvers(äußere: Rechnung, innere: Rechnung): Boolean
}