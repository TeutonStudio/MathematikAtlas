package de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.Aussage
import de.TeutonStudio.AndroidMathematikRechenSystem.LaTeXOperator
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischerOperator
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt

// logisches Und
class konjunktion(vararg argumente: Aussage): Prädikat {
    val aussagen = argumente.toSet()

    override fun zuLatex(): String {
        TODO("Not yet implemented")
    }

    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }

    override fun auswertung(): Aussage {
        if (aussagen.isEmpty()) return Aussage.WAHR
        if (aussagen.filterIsInstance<Prädikat>().isEmpty()) {
            aussagen.forEach {
                if (it.istLüge()) return Aussage.LÜGE
            }; return Aussage.WAHR
        }

        TODO("Not yet implemented")
    }

    public companion object: LaTeXOperator {
        override val BINÄR_OPERATOR = "\\vee"
        override val OPERATOR = "\\bigvee"
    }
}