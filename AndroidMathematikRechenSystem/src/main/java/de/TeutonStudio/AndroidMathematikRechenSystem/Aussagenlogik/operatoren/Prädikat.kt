package de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.Aussage
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischerOperator

interface Prädikat: MathematischerOperator, Aussage {
    public fun auswertung(): Aussage
    public override fun istWahr(): Boolean = auswertung().istWahr()
    public override fun istLüge(): Boolean = auswertung().istLüge()
}