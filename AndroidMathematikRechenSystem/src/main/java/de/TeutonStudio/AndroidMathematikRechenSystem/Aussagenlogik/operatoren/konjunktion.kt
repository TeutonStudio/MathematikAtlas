package de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.Aussage
import de.TeutonStudio.AndroidMathematikRechenSystem.LaTeXOperator
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischerOperator
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt

// logisches Und
class konjunktion(vararg argumente: Aussage): Prädikat {
    override val istAssoziativ get() = true
    override val istKommutativ get() = true

    val aussagen = argumente.toSet()

    override fun zuLatex(): String =
        aussagen.joinToString(" \\land ") { it.zuLatex() }.ifBlank { "\\top" }.let { "\\left($it\\right)" }

    override fun vereinfacht(): MathematischesObjekt = auswertung()

    override fun auswertung(): Aussage {
        if (aussagen.isEmpty()) return Aussage.WAHR
        return if (aussagen.any { it.istLüge }) Aussage.LÜGE else Aussage.WAHR
    }

    public companion object: LaTeXOperator {
        override val BINÄR_OPERATOR = "\\land"
        override val OPERATOR = "\\bigwedge"
    }
}
