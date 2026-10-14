package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Verknüpft Tupel in ihrer gegebenen Reihenfolge. Es wird ausschließlich die
 * direkte Tupel-Ebene aufgelöst; verschachtelte Tupel bleiben Elemente.
 */
fun ergänzeTupel(tupel: List<Tupel>): Tupel {
    return Tupel(tupel.flatMap { it.elemente })
}

/**
 * Hängt mathematische Objekte an ein Basistupel an. Ein [Tupel] in [elemente]
 * bleibt dabei bewusst genau ein verschachteltes Element.
 */
fun ergänzeTupelUmElemente(
    basis: Tupel,
    elemente: List<MathematischesObjekt>,
): Tupel {
    return Tupel(basis.elemente + elemente)
}
