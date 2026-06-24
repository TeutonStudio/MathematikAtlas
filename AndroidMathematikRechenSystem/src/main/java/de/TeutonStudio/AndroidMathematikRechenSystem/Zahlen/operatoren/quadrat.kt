package de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.vordefiniert.multiplikativNeutral
import kotlin.math.max

class quadrat(basis: Zahl): potenz(basis, addition(multiplikativNeutral, multiplikativNeutral)) {
    override val dimension get() = basis.dimension
    override val istAssoziativ get() = false
    override val istKommutativ get() = false

    override fun realteil(): Zahl {
        TODO("Not yet implemented")
    }

    override fun imaginärteil(): Zahl {
        TODO("Not yet implemented")
    }

    override fun zuLatex(): String {
        TODO("Not yet implemented")
    }

    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }
}