package de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.Aussage
import de.TeutonStudio.AndroidMathematikRechenSystem.LaTeXOperator
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischerOperator
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt

// logisches oder
class disjunktion(vararg argumente: Aussage): Prädikat {
    val aussagen = argumente.toSet()

    override fun zuLatex(): String {
        TODO("Not yet implemented")
    }

    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }

    override fun auswertung(): Aussage {
        if (aussagen.isEmpty()) return Aussage.LÜGE
        return if (aussagen.any { it.istWahr() }) Aussage.WAHR else Aussage.LÜGE
    }

    public companion object: LaTeXOperator {
        override val BINÄR_OPERATOR = "\\wedge"
        override val OPERATOR = "\\bigwedge"
    }
}
