package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Menge
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert.LeereMenge

class Schnitt<M: Menge<out Element>>(): polyMengenOperator<M> {
    override val argumente: List<M> = emptyList()
    override val argument: (Element) -> M = { LeereMenge() as M }
    override val idxMenge: Menge<out Element> = LeereMenge()

    constructor(
        links: M,
        rechts: M,
    ): this()

    constructor(
        arg: (Element) -> M,
        idxMenge: Menge<out Element>,
    ): this()

    override fun zuLatex(): String {
        TODO("Not yet implemented")
    }

    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }

    override fun enthält(element: Element): Boolean? {
        argumente.forEach {
            if (it.enthält(element) == false) return false
        }


        TODO("Not yet implemented")
    }
}