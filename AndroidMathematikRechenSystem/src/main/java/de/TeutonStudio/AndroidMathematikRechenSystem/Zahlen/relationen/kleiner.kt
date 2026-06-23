package de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.relationen

import de.TeutonStudio.AndroidMathematikRechenSystem.Aussagenlogik.Aussage
import de.TeutonStudio.AndroidMathematikRechenSystem.MathematischesObjekt
import de.TeutonStudio.AndroidMathematikRechenSystem.Relationen.Relation
import de.TeutonStudio.AndroidMathematikRechenSystem.Zahlen.Zahl

class kleiner(
    val links: Zahl,
    val rechts: Zahl,
): Relation {
    override fun istWahr(): Boolean =
        links.zuLatex().toDoubleOrNull()?.let { l ->
            rechts.zuLatex().toDoubleOrNull()?.let { r -> l < r }
        } ?: false

    override fun istLüge(): Boolean = !istWahr()

    override fun zuLatex(): String = "${links.zuLatex()} < ${rechts.zuLatex()}"

    override fun vereinfacht(): MathematischesObjekt =
        if (istWahr()) Aussage.WAHR else Aussage.LÜGE
}
