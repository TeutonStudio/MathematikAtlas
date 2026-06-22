package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischerOperator
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Menge
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.Rechnung
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.addition
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.multiplikation
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.subtraktion
import kotlinx.serialization.internal.throwMissingFieldException

open class NatürlicheZahlen: ZahlenMenge, MathematischesObjekt {
    override fun istKommutativ(arg: Rechnung): Boolean {
        if (arg is addition) return true
        if (arg is subtraktion) return false
        if (arg is multiplikation) return true

        return TODO("Kein Wert für ${arg} definiert.")
    }

    override fun istAssoziativ(arg: Rechnung): Boolean {
        if (arg is addition) return true
        if (arg is subtraktion) return false
        if (arg is multiplikation) return true

        return TODO("Kein Wert für ${arg} definiert.")
    }

    override fun istDistributiv(
        äußere: Rechnung,
        innere: Rechnung,
    ): Boolean {
        if (äußere is multiplikation && innere is addition) return true
        if (äußere is multiplikation && innere is subtraktion) return true

        if (äußere is subtraktion && innere is multiplikation) return false
        if (äußere is subtraktion && innere is multiplikation) return false
        TODO("Not yet implemented")
    }

    override fun istDistributivInvers(
        äußere: Rechnung,
        innere: Rechnung
    ): Boolean {
        if (äußere.istKommutativ) return istDistributiv(äußere,innere)
        if (äußere is multiplikation && innere is addition) return true
        if (äußere is multiplikation && innere is subtraktion) return true

        if (äußere is subtraktion && innere is multiplikation) return false
        if (äußere is subtraktion && innere is subtraktion) return false
        TODO("Not yet implemented")
    }
    override fun enthält(element: Element): Boolean? {
        TODO("Not yet implemented")
    }

    override fun zuLatex(): String {
        TODO("Not yet implemented")
    }

    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }
}
