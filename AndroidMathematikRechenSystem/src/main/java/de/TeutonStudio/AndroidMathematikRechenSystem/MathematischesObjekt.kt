package de.TeutonStudio.AndroidMathematikRechenSystem

interface MathematischesObjekt {
    public fun zuLatex(): String
    public fun vereinfacht(): MathematischesObjekt

    public companion object {

    }
}