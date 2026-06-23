package de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Menge
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl

class multiplikation(
    vararg val argumente: Zahl,
): Rechnung {
    override val dimension: Int get() = argumente.maxBy { it.dimension }.dimension
    override fun negiert(): Zahl {
        TODO("Not yet implemented")
    }

    override fun konjugiert(): Zahl {
        TODO("Not yet implemented")
    }

    override fun realteil(): Zahl {
        TODO("Not yet implemented")
    }

    override fun imaginärteil(): Zahl {
        TODO("Not yet implemented")
    }

    override val istAssoziativ get() = true

    override val istKommutativ get() = true

    override fun zuLatex(): String =
        argumente.joinToString(" \\cdot ") { it.zuLatex() }.ifBlank { "1" }.let { "\\left($it\\right)" }

    override fun vereinfacht(): MathematischesObjekt = this
}
