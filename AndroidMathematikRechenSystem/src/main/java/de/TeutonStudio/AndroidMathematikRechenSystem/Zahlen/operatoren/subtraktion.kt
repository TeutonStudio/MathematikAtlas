package de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischerOperator
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Menge
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert.NatürlicheZahlen
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert.ZahlenMenge
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl

// minus rechnen
class subtraktion(
    val minuend: Zahl,
    val subtrahend: Zahl,
): Zahl, Element, Rechnung {
    override val istAssoziativ get() = false
    override val istKommutativ get() = false

    override fun zuLatex(): String {
        return listOf(minuend,subtrahend).joinToString("-") { it.zuLatex() }
    }

    override fun vereinfacht(): MathematischesObjekt = this

}
