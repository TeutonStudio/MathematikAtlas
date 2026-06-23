package de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischerOperator
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Menge
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl

/**
 * Verarbeitet iterative Additionen.
 */
class addition: Rechnung, Zahl, Element, MathematischerOperator {
    val argumente: List<Zahl>

    constructor( // endliche Argumente
        vararg arguments: Zahl,
    ) {
        argumente = arguments.toList()
    }

    constructor(
        vararg arguments: List<Zahl>,
    ) {
        argumente = arguments.toList().flatten()
    }

    constructor( // indexierte Argumente
        argument: (arg: Any) -> Zahl,
        argMenge:Any,
    ) {
        argumente = emptyList()
    }
    val cache: SnapshotStateMap<String,Any> = mutableStateMapOf<String,Any>()

    override fun zuLatex(): String =
        argumente.joinToString(" + ") { it.zuLatex() }.ifBlank { "0" }.let { "\\left($it\\right)" }

    override fun vereinfacht(): MathematischesObjekt = this

    override val istAssoziativ: Boolean get() = true

    override val istKommutativ: Boolean get() = true

    public companion object {
        val Neutrales_Objekt = 0
    }
}
