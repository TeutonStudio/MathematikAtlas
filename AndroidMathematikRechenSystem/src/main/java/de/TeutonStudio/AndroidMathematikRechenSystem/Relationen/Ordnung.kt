package de.TeutonStudio.AndroidMathematikRechenSystem.Relationen

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Menge

interface Ordnung: binärRelation<Menge<out Element>,Menge<out Element>> {
    override fun auswerten(): Boolean? {
        TODO("Not yet implemented")
    }

    override fun zuLatex(): String {
        TODO("Not yet implemented")
    }

    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }
}