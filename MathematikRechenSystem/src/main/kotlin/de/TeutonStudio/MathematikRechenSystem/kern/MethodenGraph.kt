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
 */
fun Methode.argumentRaum(): MengenAusdruck {
    val signatur = methodenSignatur()
    return signatur.effektiverWerteVorrat ?: when (signatur.argumente.size) {
        0 -> LeereMenge
        1 -> signatur.argumente.single().werteVorrat
        else -> Tupelraum(signatur.argumente.map { it.werteVorrat })
    }
}

/** Umgebender Produktraum Graph(f) ⊆ W×Z. */
fun Methode.graphRaum(): MengenAusdruck = KartesischesProdukt(
    listOf(argumentRaum(), methodenSignatur().zielMenge),
)

/** Erzeugt die symbolische Graphmenge ohne den Methodenvertrag zu duplizieren. */
fun Methode.graphMenge(): MethodenGraphMenge = MethodenGraphMenge(this)
