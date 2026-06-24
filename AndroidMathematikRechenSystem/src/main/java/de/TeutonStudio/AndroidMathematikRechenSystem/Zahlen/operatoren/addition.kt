package de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl.Companion.filterAuswertbar
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl.Companion.filterNichtAuswertbar
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.vordefiniert.addititvNeutral
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.vordefiniert.ganzeZahl
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.vordefiniert.multiplikativNeutral
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.vordefiniert.natürlicheZahl
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.vordefiniert.rationaleZahl

/**
 * Verarbeitet iterative Additionen.
 */
class addition(
    vararg argumente: Zahl
): Rechnung {
    override val dimension get() = summanden.maxBy { it.dimension }.dimension
    override val istAssoziativ = true
    override val istKommutativ = true
    lateinit var summanden: List<Zahl>
    init {
        val auswertbar = Zahl.erzeuge(argumente.filterAuswertbar().mapNotNull {
            if (it is multiplikativNeutral) 1f
            if (it is addititvNeutral) null
            if (it is natürlicheZahl) it.wert.toFloat()
            if (it is ganzeZahl) it.wert.toFloat()
            if (it is rationaleZahl) it.wert
            null
        }.map { it.toFloat() }.sum())
        summanden = argumente.filterNichtAuswertbar().flatMap {
            if (it is addition) return@flatMap it.summanden
            if (it is subtraktion) {
                if(it.subtrahend is addition) return@flatMap it.subtrahend.summanden.map { s -> s.negiert() }.plus(it.minuend)
                return@flatMap listOf(it.minuend, it.subtrahend.negiert())
            }
            return@flatMap listOf(it)
        }.plus(auswertbar)
    }
    override fun negiert(): Zahl = multiplikation(multiplikativNeutral.negiert(),this)

    override fun realteil(): Zahl = addition().apply { summanden = this@addition.summanden.map { it.realteil() } }
    override fun imaginärteil(): Zahl = addition().apply { summanden = this@addition.summanden.map { it.imaginärteil() } }

    override fun konjugiert(): Zahl = addition().apply { summanden = this@addition.summanden.map { it.konjugiert() } }

    val cache: SnapshotStateMap<String,Any> = mutableStateMapOf<String,Any>()

    override fun zuLatex(): String = summanden.joinToString(" + ") { it.zuLatex() }.ifBlank { "0" }.let { "\\left($it\\right)" }

    override fun vereinfacht(): MathematischesObjekt = this


    public companion object {
        val Neutrales_Objekt = 0
    }
}
