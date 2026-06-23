package de.TeutonStudio.AndroidMathematikRechenSystem.Relationen

import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.Aussage
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Element
import de.TeutonStudio.AndroidMathematikRechenSystem.Mengenlehre.Menge

class Gleichheit(
    override val links: Element,
    override val rechts: Element,
): binärRelation<Menge<out Element>,Menge<out Element>> {
    override fun zuLatex(): String = "${links.zuLatex()} = ${rechts.zuLatex()}"

    override fun vereinfacht(): MathematischesObjekt = auswerten()

    override fun auswerten(): Aussage =
        if (istWahr()) Aussage.WAHR else Aussage.LÜGE

    override fun istWahr(): Boolean = links == rechts || links.zuLatex() == rechts.zuLatex()

    override fun istLüge(): Boolean = !istWahr()
}
