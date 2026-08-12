package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Zentraler Übergangsadapter für ältere Mathematikanalysen, die eine statisch als
 * [Methode] typisierte mathematische Methode auf freie Variablen untersuchen.
 * Neue generische Methodencodes dürfen diesen Adapter nicht verwenden.
 */
@Deprecated("Mathematische Variablenanalyse verlangt eine explizite mathematische Methode.")
fun Methode.enthalteneVariablen(): Set<Variable> {
    val objekt: MathematischesObjekt = alsMathematischeMethode("mathematische Variablenanalyse")
    return objekt.enthalteneVariablen()
}
