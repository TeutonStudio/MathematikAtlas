package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.iterationen

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.geordneteMenge
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.relationen.Ordnung

class Netz<idxM: geordneteMenge<out Element, out Ordnung>,O: MathematischesObjekt>: ElementOperator  {
    override fun zuLatex(): String = "\\left( x_i \\right)_{i\\in I}"

    override fun vereinfacht(): MathematischesObjekt = this
}
