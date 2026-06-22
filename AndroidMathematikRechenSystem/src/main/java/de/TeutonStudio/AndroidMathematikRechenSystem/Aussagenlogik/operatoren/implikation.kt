package de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.Aussage
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt

class implikation(
    val bedingung: Aussage,
    val behauptung: Aussage,
): Prädikat {
    override val istAssoziativ get() = false
    override val istKommutativ get() = false

    override fun auswertung(): Aussage {
        return if (bedingung.istLüge() || behauptung.istWahr()) {
            Aussage.WAHR
        } else {
            Aussage.LÜGE
        }
    }

    override fun zuLatex(): String {
        TODO("Not yet implemented")
    }

    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }

}
