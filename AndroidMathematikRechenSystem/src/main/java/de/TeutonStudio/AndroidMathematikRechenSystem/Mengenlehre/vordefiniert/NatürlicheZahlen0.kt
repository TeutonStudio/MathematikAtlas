package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.addition
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.multiplikation
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.subtraktion
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.relationen.kleiner
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.vordefiniert.addititvNeutral
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.vordefiniert.multiplikativNeutral
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.vordefiniert.natürlicheZahl

object NatürlicheZahlen0: ZahlenMenge {
    override fun zuLatex(): String = "\\mathbb{N}"
    override fun enthält(element: Element): Boolean {
        if (element !is Zahl) return false
        if (element is addititvNeutral) return true
        if (element is multiplikativNeutral) return true
        if (element is natürlicheZahl) return true
        if (element is addition) return !element.summanden.map { enthält(it) }.contains(false)
        if (element is subtraktion) return kleiner(element.subtrahend, element.minuend).istWahr
        if (element is multiplikation) return !element.faktoren.map { enthält(it) }.contains(false)

        return true // TODO erkennung ob NatürlicheZahl
    }

    override fun vereinfacht(): MathematischesObjekt = this
}
