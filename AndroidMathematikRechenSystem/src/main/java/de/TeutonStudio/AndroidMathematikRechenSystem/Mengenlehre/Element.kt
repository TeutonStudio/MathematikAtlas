package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischerOperator
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt

interface Element: MathematischesObjekt {
    // TODELETE
    public fun kleinsteOberMenge(): Menge<out Element>
}