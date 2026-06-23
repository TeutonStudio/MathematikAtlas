package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Menge
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert.LeereMenge

class Schnitt<M: Menge<out Element>>(): polyMengenOperator<M> {
    override var argumente: List<M> = emptyList()
    override val argument: (Element) -> M = { LeereMenge() as M }
    override val idxMenge: Menge<out Element> = LeereMenge()

    constructor(
        links: M,
        rechts: M,
    ): this() {
        argumente = listOf(links, rechts)
    }

    constructor(
        arg: (Element) -> M,
        idxMenge: Menge<out Element>,
    ): this()

    override fun zuLatex(): String =
        argumente.joinToString(" \\cap ") { it.zuLatex() }.ifBlank { "\\emptyset" }.let { "\\left($it\\right)" }

    override fun vereinfacht(): MathematischesObjekt = this

    override fun enthält(element: Element): Boolean? {
        argumente.forEach {
            if (it.enthält(element) == false) return false
        }


        return null
    }
}
