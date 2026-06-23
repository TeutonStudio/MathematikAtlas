package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Menge

interface polyMengenOperator<M: Menge<out Element>>: MengenOperator<M> {
    // Endliche Argumente
    val argumente: List<M>
    // Iterative Argumente
    val argument: (Element) -> M
    val idxMenge: Menge<out Element>
}