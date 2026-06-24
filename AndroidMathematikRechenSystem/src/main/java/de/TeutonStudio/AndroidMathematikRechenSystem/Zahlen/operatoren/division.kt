package de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl
import kotlin.math.max

class division private constructor(
    val divident: Zahl,
    val divisor: Zahl,
) : Rechnung {
    companion object {
        operator fun invoke(
            divident: Zahl,
            divisor: Zahl,
        ): division {
            return when (divisor) {
                is division -> invoke(
                    divident = multiplikation(divident, divisor.divisor),
                    divisor = divisor.divident,
                )

                else -> division(divident, divisor)
            }
        }
    }

    override val dimension get() = max(divident.dimension, divisor.dimension)

    override fun zuLatex(): String = "\\frac{${divident.zuLatex()}}{${divisor.zuLatex()}}"

    override fun vereinfacht(): MathematischesObjekt = this

    override fun negiert(): Zahl = division(divident.negiert(), divisor)

    override fun realteil(): Zahl {
        val divis = subtraktion(quadrat(divisor.realteil()),quadrat(divisor.imaginärteil()))
        return division(subtraktion(
            multiplikation(divident.realteil(),divisor.realteil()),
            multiplikation(divident.imaginärteil(),divisor.imaginärteil())
        ),divis)
    }

    override fun imaginärteil(): Zahl {
        val divis = subtraktion(quadrat(divisor.realteil()),quadrat(divisor.imaginärteil()))
        return division(addition(multiplikation(
            divident.realteil(),
            divisor.imaginärteil()
        ), multiplikation(
            divident.imaginärteil(),
            divisor.realteil()
        ) ),divis)
    }

}