package de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.Aussage
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt

class implikation(
    val bedingung: Aussage,
    val behauptung: Aussage,
): Prädikat {
    override fun auswertung(): Aussage {
        if (bedingung !is Prädikat) {
            if (bedingung.istLüge()) return Aussage.WAHR
            if (behauptung !is Prädikat) {
                if (bedingung.istWahr() && behauptung.istWahr()) return Aussage.WAHR
                if (bedingung.istWahr() && behauptung.istLüge()) return Aussage.LÜGE
            }
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