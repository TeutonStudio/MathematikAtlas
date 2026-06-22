package de.TeutonStudio.AndroidMathematikRechenSystem

interface MathematischerOperator: MathematischesObjekt {
    public val istAssoziativ: Boolean
        get() = false
    public val istKommutativ: Boolean
        get() = false

}
