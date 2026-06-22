package de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.Aussage
import de.TeutonStudio.AndroidMathematikRechenSystem.LaTeXOperator
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt

class kontrajunktion(
    val entweder: Aussage,
    val oder: Aussage,
): Prädikat {
    override val istAssoziativ get() = true
    override val istKommutativ get() = true

//    val aussagen = argumente.toSet()

    override fun zuLatex(): String {
        TODO("Not yet implemented")
    }

    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }

    override fun auswertung(): Aussage {
        if (entweder.istWahr() && oder.istWahr() || entweder.istLüge() && oder.istLüge()) return Aussage.LÜGE
        if (entweder.istWahr() && oder.istLüge() || entweder.istLüge() && oder.istWahr()) return Aussage.WAHR

        return this
    }

    public companion object: LaTeXOperator {
        override val BINÄR_OPERATOR = "\\wedge"
        override val OPERATOR = "\\bigwedge"
    }
}
