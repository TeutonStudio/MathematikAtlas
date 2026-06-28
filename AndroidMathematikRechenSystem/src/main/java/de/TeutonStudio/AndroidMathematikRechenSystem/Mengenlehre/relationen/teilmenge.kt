package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.relationen

import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.Aussage
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Menge
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert.LeereMenge
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert.ZahlenMenge
import de.TeutonStudio.AndroidMathematikRechenSystem.Relationen.binärRelation

class teilmenge(
    override val links: Element,
    override val rechts: Element
) : binärRelation<Menge<*>, Menge<*>> {
    override val istWahr = false
    override val istLüge = false
    override fun auswerten(): Aussage {
        TODO("Not yet implemented")
    }

    override fun zuLatex(): String {
        TODO("Not yet implemented")
    }

    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }

}