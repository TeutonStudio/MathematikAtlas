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
    constructor( // endliche Argumente
        vararg arguments: List<Zahl>
    ) // TODO iteration auflösen, die unnötig ist, solange alle Zahl.kleinsteOberMenge eine additiv komutativ Menge ist.
    constructor( // indexierte Argumente
        argument: (arg: Any) -> Zahl,
        argMenge:Any,
    )
    val cache: SnapshotStateMap<String,Any> = mutableStateMapOf<String,Any>()

    override fun zuLatex(): String {
        TODO("Not yet implemented")
    }

    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }

    override val istAssoziativ: Boolean
        get() {
        TODO("Not yet implemented")
        }

    override val istKommutativ: Boolean
        get() {
        TODO("Not yet implemented")
        }

    public companion object {
        val Neutrales_Objekt = 0
    }
}
