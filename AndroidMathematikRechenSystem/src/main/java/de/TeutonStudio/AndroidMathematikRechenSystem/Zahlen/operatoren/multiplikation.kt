package de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Menge
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.vordefiniert.multiplikativNeutral

open class multiplikation(
    vararg argumente: Zahl,
): Rechnung {
    override val dimension get() = faktoren.maxBy { it.dimension }.dimension
    override val istAssoziativ get() = true
    override val istKommutativ get() = true
    lateinit var faktoren: List<Zahl>
    init {
        faktoren = argumente.flatMap {
            if (it is multiplikation) return@flatMap it.faktoren
            if (it is division) return@flatMap listOf(it.divisor,it.divident.kehrwert())
            return@flatMap listOf(it)
        }
    }

    override fun realteil(): Zahl {
        TODO("Not yet implemented")
    }

    override fun imaginärteil(): Zahl {
        TODO("Not yet implemented")
    }

    override fun konjugiert(): Zahl = multiplikation().apply { faktoren = this@multiplikation.faktoren.map { it.konjugiert() } }

    override fun zuLatex(): String =
        faktoren.joinToString(" \\cdot ") { it.zuLatex() }.ifBlank { "1" }.let { "\\left($it\\right)" }

    override fun vereinfacht(): MathematischesObjekt = this
}
