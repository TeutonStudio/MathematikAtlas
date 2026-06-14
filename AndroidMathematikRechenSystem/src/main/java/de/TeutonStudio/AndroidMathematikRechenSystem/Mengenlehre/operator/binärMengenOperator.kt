package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.operator

import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Menge
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.iterationen.Dupel

interface binärMengenOperator<EL: Element, ER: Element, ML: Menge<EL>, MR: Menge<ER>>: MengenOperator<Dupel<EL, ER>> {
    override fun zuLatex(): String
}