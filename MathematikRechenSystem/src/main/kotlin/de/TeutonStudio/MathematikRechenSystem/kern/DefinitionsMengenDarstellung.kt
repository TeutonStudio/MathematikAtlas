package de.TeutonStudio.MathematikRechenSystem.kern

/** Mengenschreibweise mit Doppelpunkt für Definitionskarten und Lehrdarstellungen. */
fun DefinierteMenge.zuDoppelpunktLatex(): String {
    val linkeSeite = if (variablen.size == 1) {
        variablen.single().variable.zuLatex()
    } else {
        variablen.joinToString(prefix = "\\left(", postfix = "\\right)") { it.variable.zuLatex() }
    }
    val gleicheGrundmenge = variablen.map { it.grundMenge }.distinct().singleOrNull()
    val grundmenge = when {
        gleicheGrundmenge != null && variablen.size == 1 -> gleicheGrundmenge.zuLatex()
        gleicheGrundmenge != null -> "${gleicheGrundmenge.zuLatex()}^${variablen.size}"
        else -> KartesischesProdukt(variablen.map { it.grundMenge }).zuLatex()
    }
    return "\\left\\{$linkeSeite\\in$grundmenge:${bedingung.zuLatex()}\\right\\}"
}
