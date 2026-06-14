package de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischerOperator
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Menge
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert.NatürlicheZahlen
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert.ZahlenMenge
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl

// minus rechnen
class subtraktion(
    val minuend: Zahl,
    val subtrahend: Zahl,
): Zahl, Element, Rechnung {
    override fun istAsoziativ(): Boolean = false
    override fun istKommutativ(): Boolean = false

    override fun kleinsteOberMenge(): Menge<out Element> {
        if (minuend.kleinsteOberMenge() is ReelleZahlen && subtrahend.kleinsteOberMenge() is ReelleZahlen) {
            if (RelleZahlen.aufsteigend(arg: Pair<Zahl,Zahl>) || RelleZahlen.gleich(ararg: Pair<Zahl,Zahl>)) return NatürlicheZahlen
            else return GanzeZahlen
        }
        TODO("Not yet implemented")
    }

    override fun zuLatex(): String {
        return listOf(minuend,subtrahend).joinToString("-") { it.zuLatex() }
    }

    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }

}