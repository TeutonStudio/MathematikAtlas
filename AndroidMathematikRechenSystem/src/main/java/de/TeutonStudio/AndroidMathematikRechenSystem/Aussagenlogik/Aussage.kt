package de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik

import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.vordefiniert.Lüge
import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.vordefiniert.Wahr
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt

// TODO Eine Aussage die Wahr, Lüge ist
interface Aussage: MathematischesObjekt {
    public fun istWahr(): Boolean
    public fun istLüge(): Boolean

    public companion object {
        val WAHR = Wahr()
        val LÜGE = Lüge()
    }
}