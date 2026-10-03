package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Der Graph einer symbolischen mathematischen Methode als eigenständiger Mengenausdruck.
 * Engine- oder Scriptmethoden besitzen nicht allein aufgrund einer Typ-Signatur einen
 * mathematischen Graphen und werden deshalb an der Erzeugungsgrenze ausgeschlossen.
 */
data class MethodenGraphMenge(
    val methode: MathematischeMethode,
) : MengenAusdruck {
    override fun zuLatex(): String = "\\operatorname{Graph}\\left(${methode.name}\\right)"
}

/**
 * Kanonischer mathematischer Argumentraum. Der Parameter bleibt ausschließlich für
 * alte Aufrufer erhalten und bestimmt den Raum nicht mehr.
 */
@Deprecated("Mathematische Mengen kommen aus mathematischeMethodenSignatur().")
fun Methode.argumentRaum(
    @Suppress("UNUSED_PARAMETER") signatur: LegacyMathematischeMethodenSignatur = methodenSignatur(),
): MengenAusdruck = mathematischeMethodenSignatur().definitionsRaum

/** Umgebender Produktraum Graph(f) ⊆ D_f × Z_f einer symbolischen mathematischen Methode. */
fun Methode.graphRaum(): MengenAusdruck {
    val mathematisch = alsMathematischeMethode("einen mathematischen Funktionsgraphen")
    return kartesischesProdukt(
        listOf(
            mathematisch.mathematischeSignatur.definitionsRaum,
            mathematisch.mathematischeSignatur.zielRaum,
        ),
    )
}

/** Erzeugt die symbolische Graphmenge ohne den Methodenvertrag zu duplizieren. */
fun Methode.graphMenge(): MethodenGraphMenge =
    MethodenGraphMenge(alsMathematischeMethode("einen mathematischen Funktionsgraphen"))
