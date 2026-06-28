package de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.relationen

import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.Aussage
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Menge
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert.LeereMenge
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.vordefiniert.ZahlenMenge
import de.TeutonStudio.AndroidMathematikRechenSystem.Relationen.binärRelation
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl

class element(
    override val links: Element,
    override val rechts: Menge<*>,
): binärRelation<Menge<*>, Menge<*>> {
    override val istWahr get() = rechts.enthält(links) ?: false
    override val istLüge get() = if (rechts is LeereMenge) true else rechts.enthält(links)?.let { !it } ?: false
    override fun auswerten(): Aussage {
        if (rechts is LeereMenge) return Aussage.LÜGE
        if (links is Zahl && rechts is ZahlenMenge) return Aussage.ausBoolean(rechts.enthält(links))

        TODO("Not yet implemented")
    }

    override fun zuLatex(): String {
        TODO("Not yet implemented")
    }

    override fun vereinfacht(): MathematischesObjekt {
        TODO("Not yet implemented")
    }

}