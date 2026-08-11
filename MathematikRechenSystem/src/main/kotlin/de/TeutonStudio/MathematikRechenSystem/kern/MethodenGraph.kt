package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Der Graph einer symbolischen mathematischen Methode als eigenständiger Mengenausdruck.
 * Engine- oder Scriptmethoden besitzen nicht allein aufgrund einer Signatur einen
 * mathematischen Graphen und werden deshalb an der Erzeugungsgrenze ausgeschlossen.
 */
data class MethodenGraphMenge(
    val methode: MathematischeMethode,
) : MengenAusdruck {
    override fun zuLatex(): String = "\\operatorname{Graph}\\left(${methode.name}\\right)"
}

/**
 * Kanonischer Argumentraum einer signaturtragenden Methode.
 *
 * Diese reine Signaturprojektion ist bewusst allgemeiner als der mathematische Graph:
 * G0.2 kann sie auf den allgemeinen Typkern heben, ohne Scriptmethoden zu mathematischen
 * Abbildungen umzudeuten.
 */
fun Methode.argumentRaum(signatur: MethodenSignatur = methodenSignatur()): MengenAusdruck =
    signatur.werteVorrat

/** Umgebender Produktraum Graph(f) ⊆ W×Z einer symbolischen mathematischen Methode. */
fun Methode.graphRaum(): MengenAusdruck {
    val mathematisch = alsMathematischeMethode("einen mathematischen Funktionsgraphen")
    val signatur = mathematisch.methodenSignatur()
    return KartesischesProdukt(listOf(mathematisch.argumentRaum(signatur), signatur.zielMenge))
}

/** Erzeugt die symbolische Graphmenge ohne den Methodenvertrag zu duplizieren. */
fun Methode.graphMenge(): MethodenGraphMenge =
    MethodenGraphMenge(alsMathematischeMethode("einen mathematischen Funktionsgraphen"))
