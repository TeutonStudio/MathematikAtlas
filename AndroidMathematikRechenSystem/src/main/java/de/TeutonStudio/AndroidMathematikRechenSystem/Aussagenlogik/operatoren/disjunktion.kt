package de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.Aussage
import de.TeutonStudio.AndroidMathematikRechenSystem.LaTeXOperator
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischerOperator
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt

// logisches oder
class disjunktion(vararg argumente: Aussage): Prädikat {
    override val istAssoziativ get() = true
    override val istKommutativ get() = true

    val aussagen = argumente.toSet()

    override fun zuLatex(): String =
        aussagen.joinToString(" \\lor ") { it.zuLatex() }.ifBlank { "\\bot" }.let { "\\left($it\\right)" }

    override fun vereinfacht(): MathematischesObjekt = auswertung()

    override fun auswertung(): Aussage {
        if (aussagen.isEmpty()) return Aussage.LÜGE
        return if (aussagen.any { it.istWahr }) Aussage.WAHR else Aussage.LÜGE
    }

    public companion object: LaTeXOperator {
        override val BINÄR_OPERATOR = "\\lor"
        override val OPERATOR = "\\bigvee"
    }
}
