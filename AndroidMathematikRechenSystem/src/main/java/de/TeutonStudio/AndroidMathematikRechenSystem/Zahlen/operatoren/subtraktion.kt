package de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischerOperator
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Menge
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert.NatürlicheZahlen
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert.ZahlenMenge
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl
import kotlin.math.max

// minus rechnen
class subtraktion private constructor(
    val minuend: Zahl,
    val subtrahend: Zahl,
): Rechnung {
    companion object {
        operator fun invoke(minuend: Zahl, subtrahend: Zahl, ): subtraktion {
            return when (subtrahend) {
                is subtraktion -> invoke(
                    minuend = addition(minuend, subtrahend.subtrahend),
                    subtrahend = subtrahend.minuend,
                )

                else -> subtraktion(minuend, subtrahend)
            }
        }
    }

    override val dimension get() = max(minuend.dimension,subtrahend.dimension)
    override val istAssoziativ get() = false
    override val istKommutativ get() = false


    override fun realteil(): Zahl = subtraktion(minuend.realteil(),subtrahend.realteil())
    override fun imaginärteil(): Zahl = subtraktion(minuend.imaginärteil(),subtrahend.imaginärteil())


    override fun zuLatex(): String {
        return listOf(minuend,subtrahend).joinToString("-") { it.zuLatex() }
    }

    override fun vereinfacht(): MathematischesObjekt = this
}
