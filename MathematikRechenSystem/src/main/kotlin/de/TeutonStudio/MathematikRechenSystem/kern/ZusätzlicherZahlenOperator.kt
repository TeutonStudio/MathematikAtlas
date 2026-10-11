package de.TeutonStudio.MathematikRechenSystem.kern

/** Ergänzende stabile Operatorzustände des universellen Zahlenrechners. */
enum class ZusätzlicherZahlenOperator(
    val stabileId: String,
    val titel: String,
    val symbolLatex: String,
) {
    TANGENS("zahl.tan", "Tangens", "\\tan"),
    COTANGENS("zahl.cot", "Cotangens", "\\cot"),
    SEKANS("zahl.sec", "Sekans", "\\sec"),
    KOSEKANS("zahl.csc", "Kosekans", "\\csc"),
    ARCTANGENS("zahl.arctan", "Arcus Tangens", "\\arctan"),
    SINUS_HYPERBOLICUS("zahl.sinh", "Sinus hyperbolicus", "\\sinh"),
    COSINUS_HYPERBOLICUS("zahl.cosh", "Cosinus hyperbolicus", "\\cosh"),
    TANGENS_HYPERBOLICUS("zahl.tanh", "Tangens hyperbolicus", "\\tanh"),
    COTANGENS_HYPERBOLICUS("zahl.coth", "Cotangens hyperbolicus", "\\coth"),
    SEKANS_HYPERBOLICUS("zahl.sech", "Sekans hyperbolicus", "\\operatorname{sech}"),
    KOSEKANS_HYPERBOLICUS("zahl.csch", "Kosekans hyperbolicus", "\\operatorname{csch}"),
    ARITHMETISCHES_MITTEL("zahl.arithmetischesMittel", "Arithmetisches Mittel", "\\operatorname{AM}"),
    GEOMETRISCHES_MITTEL("zahl.geometrischesMittel", "Geometrisches Mittel", "\\operatorname{GM}"),
    STAMMFUNKTION("zahl.stammfunktion", "Stammfunktion", "\\int"),
    POLYNOM("zahl.polynom", "Polynom", "(c_k)_k\\cdot(x^k)_k"),
    ;

    companion object {
        fun vonId(id: String?): ZusätzlicherZahlenOperator? = entries.firstOrNull { operator ->
            id == operator.stabileId || id.equals(operator.name, ignoreCase = true)
        }
    }

    val hebungsArt: ZahlenOperatorHebungsArt
        get() = when (this) {
            POLYNOM,
            STAMMFUNKTION,
            -> ZahlenOperatorHebungsArt.METHODENSPEZIFISCH
            else -> ZahlenOperatorHebungsArt.PUNKTWEISE
        }
}
