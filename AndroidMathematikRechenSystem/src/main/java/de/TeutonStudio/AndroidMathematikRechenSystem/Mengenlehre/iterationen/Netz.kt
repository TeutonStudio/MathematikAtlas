package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.iterationen

import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Menge
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.geordneteMenge
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert.GanzeZahlen
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert.ReelleZahlen
import de.TeutonStudio.AndroidMathematikRechenSystem.Relationen.Ordnung
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.operatoren.Interval

class Netz<idxM: geordneteMenge<out Element, out Ordnung>,O: MathematischesObjekt>: ElementOperator  {
    override fun zuLatex(): String {
        TODO("Not yet implemented")
    }

    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }
}