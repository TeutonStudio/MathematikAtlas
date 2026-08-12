package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Zentraler Übergangsadapter für ältere Mathematikanalysen, die eine statisch als
 * [Methode] typisierte mathematische Methode wie ein mathematisches Objekt analysieren.
 * Neue generische Methodencodes dürfen diesen Adapter nicht verwenden.
 */
@Deprecated("Mathematische Parameteranalyse verlangt eine explizite mathematische Methode.")
fun Methode.enthalteneMethodenParameter(): Set<MethodenParameter> =
    (alsMathematischeMethode("mathematische Parameteranalyse") as MathematischesObjekt)
        .enthalteneMethodenParameter()

@Deprecated("Mathematische Variablenanalyse verlangt eine explizite mathematische Methode.")
fun Methode.enthalteneVariablen(): Set<Variable> =
    (alsMathematischeMethode("mathematische Variablenanalyse") as MathematischesObjekt)
        .enthalteneVariablen()
