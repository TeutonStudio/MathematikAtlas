package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Menge
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert.LeereMenge

class schnitt<M: Menge<*>>(): polyMengenOperator<M> {
    override var argumente: List<M> = emptyList()
    override val argument: (Element) -> M = { LeereMenge as M }
    override val idxMenge: Menge<*> = LeereMenge

    constructor(
        links: M,
        rechts: M,
    ): this() {
        argumente = listOf(links, rechts)
    }

    constructor(
        arg: (Element) -> M,
        idxMenge: Menge<*>,
    ): this()

    override fun zuLatex(): String =
        argumente.joinToString(" \\cap ") { it.zuLatex() }.ifBlank { "\\emptyset" }.let { "\\left($it\\right)" }

    override fun vereinfacht(): MathematischesObjekt = this

    override fun enthält(element: Element): Boolean? {
        if (argumente.contains(LeereMenge as M)) return false
        argumente.forEach {
            if (it.enthält(element) == false) return false
        }

        return null
    }
}
