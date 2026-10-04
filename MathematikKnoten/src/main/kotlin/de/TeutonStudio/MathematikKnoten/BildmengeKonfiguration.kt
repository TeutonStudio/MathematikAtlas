package de.TeutonStudio.MathematikKnoten

const val BILDMENGE_ARGUMENT_MODUS = "argumentModus"
const val BILDMENGE_MODUS_PRODUKT = "produkt"
const val BILDMENGE_MODUS_EINZELMENGEN = "einzelmengen"

fun bildmengeArgumentName(index: Int): String {
    require(index >= 0) { "Der Bildmengen-Argumentindex darf nicht negativ sein." }
    return "menge-$index"
}
