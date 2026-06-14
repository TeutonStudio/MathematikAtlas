package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.operator

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Menge

class Vereinigung<E: Element>(
    val links: Menge<E>,
    val rechts: Menge<E>,
): MengenOperator<E> {
    override fun zuLatex(): String {
        TODO("Not yet implemented")
    }

    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }

    override fun enthält(element: E): Boolean {
        TODO("Not yet implemented")
    }

    override fun kleinsteOberMenge(): Menge<out Element> {
        TODO("Not yet implemented")
    }
}