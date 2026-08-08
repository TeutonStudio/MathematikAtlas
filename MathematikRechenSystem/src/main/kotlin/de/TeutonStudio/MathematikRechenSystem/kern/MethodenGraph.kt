package de.TeutonStudio.MathematikRechenSystem.kern

/**
 * Der Graph einer Methode als eigenständiger symbolischer Mengenausdruck.
 *
 * Die Methode bleibt die einzige Quelle der Wahrheit für Definitions- und Zielraum.
 * Restriktionen oder Erweiterungen wirken deshalb automatisch über die aktuelle
 * Methodensignatur auf den Graphen.
 */
data class MethodenGraphMenge(
    val methode: Methode,
) : MengenAusdruck {
    override fun zuLatex(): String = "\\operatorname{Graph}\\left(${methode.name}\\right)"
}

/**
 * Kanonischer Argumentraum einer Methode.
 *
 * Ein einzelnes Argument behält seinen tatsächlichen Wertevorrat W. Mehrere
 * Argumente bilden einen geordneten Tupelraum W1×...×Wn. Ein expliziter effektiver
 * Wertevorrat, etwa nach einer Restriktion auf eine nicht-kartesische Teilmenge,
 * hat Vorrang vor der komponentenweisen Ableitung.
 *
 * Die Signatur kann von Aufrufern vorab berechnet werden. Der optionale Parameter
 * hält ältere, dateilokale Hilfsfunktionen mit demselben Kurzname konfliktfrei,
 * bis diese schrittweise auf den zentralen Methodenvertrag umgestellt werden.
 */
fun Methode.argumentRaum(signatur: MethodenSignatur = methodenSignatur()): MengenAusdruck =
    signatur.effektiverWerteVorrat ?: when (signatur.argumente.size) {
        0 -> LeereMenge
        1 -> signatur.argumente.single().werteVorrat
        else -> Tupelraum(signatur.argumente.map { it.werteVorrat })
    }

/** Umgebender Produktraum Graph(f) ⊆ W×Z. */
fun Methode.graphRaum(): MengenAusdruck {
    val signatur = methodenSignatur()
    return KartesischesProdukt(listOf(argumentRaum(signatur), signatur.zielMenge))
}

/** Erzeugt die symbolische Graphmenge ohne den Methodenvertrag zu duplizieren. */
fun Methode.graphMenge(): MethodenGraphMenge = MethodenGraphMenge(this)
