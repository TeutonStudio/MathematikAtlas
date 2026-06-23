package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Menge
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert.LeereMenge

class Vereinigung<M: Menge<out Element>>(): polyMengenOperator<M> {
    override var argumente: List<M> = emptyList()
    override val argument: (Element) -> M = { LeereMenge() as M }
    override val idxMenge: Menge<out Element> = LeereMenge()

    constructor(links: M, rechts: M): this() {
        argumente = listOf(links, rechts)
    }

    override fun zuLatex(): String =
        argumente.joinToString(" \\cup ") { it.zuLatex() }.ifBlank { "\\emptyset" }.let { "\\left($it\\right)" }

    override fun vereinfacht(): MathematischesObjekt = this

    override fun enthält(element: Element): Boolean? {
        argumente.forEach {
            if (it.enthält(element) == true) return true
        }

        return null
    }
}
