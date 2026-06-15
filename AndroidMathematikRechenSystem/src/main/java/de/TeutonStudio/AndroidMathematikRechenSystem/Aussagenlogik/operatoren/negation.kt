package de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.Aussage
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt

class negation(
    val argument: Aussage
): Prädikat {
    override fun auswertung(): Aussage {
        if (argument !is Prädikat) {
            if (argument.istWahr()) return Aussage.LÜGE
            if (argument.istLüge()) return Aussage.WAHR
        }

        TODO("Not yet implemented")
    }

    override fun zuLatex(): String {
        TODO("Not yet implemented")
    }

    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }
}