package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.iterationen

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert.GanzeZahlen
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert.NatürlicheZahlen
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.Interval

class Tupel<idxM: Interval<NatürlicheZahlen>,O: MathematischesObjekt>(
    private val def: (Int) -> O,
): ElementOperator {
    public fun erhalte(idx: Int): O = def(idx)

    override fun zuLatex(): String = "\\left(${def(0).zuLatex()}, \\ldots\\right)"

    override fun vereinfacht(): MathematischesObjekt = this
}
