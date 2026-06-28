package de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.Aussage
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischerOperator

interface Prädikat: MathematischerOperator, Aussage {
    public fun auswertung(): Aussage
    public override val istWahr get() = auswertung().istWahr
    public override val istLüge get() = auswertung().istLüge
}