package de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.Aussage
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt

class negation(
    val argument: Aussage
): Prädikat {
    override val istAssoziativ get() = false
    override val istKommutativ get() = false

    override fun auswertung(): Aussage {
        return if (argument.istWahr) Aussage.LÜGE else Aussage.WAHR
    }

    override fun zuLatex(): String = "\\lnot ${argument.zuLatex()}"

    override fun vereinfacht(): MathematischesObjekt = auswertung()
}
