package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.relationen

import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.Aussage
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Menge
import de.TeutonStudio.AndroidMathematikRechenSystem.Relationen.binärRelation

interface Ordnung: binärRelation<Menge<out Element>, Menge<out Element>> {
    override fun auswerten(): Aussage =
        if (istWahr) Aussage.WAHR else Aussage.LÜGE

    override fun zuLatex(): String = "${links.zuLatex()} \\le ${rechts.zuLatex()}"

    override fun vereinfacht(): MathematischesObjekt = auswerten()
}