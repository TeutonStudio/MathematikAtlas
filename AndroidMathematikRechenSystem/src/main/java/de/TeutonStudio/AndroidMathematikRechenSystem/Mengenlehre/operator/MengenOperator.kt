package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.operator

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischerOperator
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Menge

interface MengenOperator<M: Menge<out Element>>: MathematischerOperator, Menge<Element>, Element {
}