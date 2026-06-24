package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.Rechnung
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.vordefiniert.ganzeZahl

object GanzeZahlen: ZahlenMenge {
    override fun zuLatex(): String = "\\mathbb{Z}"

    override fun enthält(element: Element): Boolean {
        if (element !is Zahl) return false
        if (element is ganzeZahl) return true
        return NatürlicheZahlen.enthält(element) || NatürlicheZahlen.enthält(element.negiert())
    }

    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }

}
