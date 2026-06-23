package de.TeutonStudio.AndroidMathematikRechenSystem.Relationen

import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.Aussage
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischerOperator

interface Relation: MathematischerOperator, Aussage {
    override val istAssoziativ: Boolean get() = false
    override val istKommutativ: Boolean get() = false
}
