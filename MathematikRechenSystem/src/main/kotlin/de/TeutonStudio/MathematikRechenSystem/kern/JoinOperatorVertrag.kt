package de.TeutonStudio.MathematikRechenSystem.kern

/** Geschlossene Operationen, deren Operandengruppe als Mehrfacheingang modelliert werden kann. */
fun istJoinRechnerOperator(id: String): Boolean = istAssoziativerRechnerOperator(id) && id !in setOf(
    "matrix.produkt", "tensor.tensorprodukt",
)

fun benoetigtJoinKommutativitaetsNachweis(id: String): Boolean =
    id == "zahl.multiplikation" || id.endsWith(".hadamard")
