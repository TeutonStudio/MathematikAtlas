package de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Menge
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.iterationen.ElementOperator
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert.ZahlenMenge
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.relationen.Ordnung
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl

class Interval<T: Menge<Zahl>, O: Ordnung>: ElementOperator, ZahlenMenge {
    var enthältLinks: Boolean = true
    var enthältRechts: Boolean = true

    override fun zuLatex(): String = "\\left[\\,?\\,,\\,?\\,\\right]"

    override fun vereinfacht(): MathematischesObjekt = this

    override fun enthält(element: Element): Boolean = false
}
