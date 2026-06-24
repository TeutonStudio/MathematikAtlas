package de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl
import kotlin.math.max

open class potenz(
    val basis: Zahl,
    val exponent: Zahl,
): Rechnung {
    override val dimension get() = max(basis.dimension, exponent.dimension)
    override val istAssoziativ get() = false
    override val istKommutativ get() = false

    override fun realteil(): Zahl {
        TODO("Not yet implemented")
    }

    override fun imaginärteil(): Zahl {
        TODO("Not yet implemented")
    }

    override fun konjugiert(): Zahl {
        TODO("Not yet implemented")
    }

    override fun zuLatex(): String {
        TODO("Not yet implemented")
    }

    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }
}