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

/** Kanonischer mathematischer Argumentraum. Allgemeine Methoden besitzen keinen Mengenraum. */
fun Methode.argumentRaum(@Suppress("UNUSED_PARAMETER") kanonisch: Boolean = true): MengenAusdruck =
    mathematischeMethodenSignatur().definitionsRaum

/** Expliziter Alias für Aufrufer, die die Mathematikgrenze im Namen sichtbar machen möchten. */
fun Methode.mathematischerArgumentRaum(): MengenAusdruck = argumentRaum()

/** Umgebender Produktraum Graph(f) ⊆ D_f × Z_f einer symbolischen mathematischen Methode. */
fun Methode.graphRaum(): MengenAusdruck {
    val mathematisch = alsMathematischeMethode("einen mathematischen Funktionsgraphen")
    val signatur = mathematisch.mathematischeMethodenSignatur()
    return Tupelraum(listOf(signatur.definitionsRaum, signatur.zielRaum))
}

/** Erzeugt die symbolische Graphmenge ohne den Methodenvertrag zu duplizieren. */
fun Methode.graphMenge(): MethodenGraphMenge =
    MethodenGraphMenge(alsMathematischeMethode("einen mathematischen Funktionsgraphen"))
