package de.TeutonStudio.MathematikRechenSystem.kern

/** Die positiven rationalen Primzahlen innerhalb der natürlichen Zahlen. */
data object Primzahlen : MengenAusdruck {
    override fun zuLatex() = "\\mathbb{P}"
}

/** Der Ring der gaußschen ganzen Zahlen Z[i]. */
data object GaußscheGanzeZahlen : MengenAusdruck {
    override fun zuLatex() = "\\mathbb{Z}[i]"
}

/** Die Primelemente des Rings der gaußschen ganzen Zahlen. */
data object GaußschePrimzahlen : MengenAusdruck {
    override fun zuLatex() = "\\mathbb{P}_{\\mathbb{Z}[i]}"
}

/** Die Menge aller Teilmengen einer Grundmenge. */
data class Potenzmenge(val grundMenge: MengenAusdruck) : MengenAusdruck {
    override fun zuLatex() = "\\mathcal{P}\\left(${grundMenge.zuLatex()}\\right)"
}

/**
 * Die Menge aller Abbildungen von [definitionsMenge] nach [zielMenge].
 * Entsprechend der üblichen Konvention bezeichnet A^B die Abbildungen B -> A.
 */
data class Abbildungsmenge(
    val zielMenge: MengenAusdruck,
    val definitionsMenge: MengenAusdruck,
) : MengenAusdruck {
    override fun zuLatex() = "${zielMenge.zuLatex()}^{${definitionsMenge.zuLatex()}}"
}

/**
 * Raum endlichdimensionaler Felder über einer Elementmenge.
 *
 * Die Achsengrößen bleiben als Zahl-Ausdrücke erhalten. Dadurch verwenden konkrete
 * und symbolische positive natürliche Dimensionen denselben Kernvertrag. Ob ein
 * symbolischer Ausdruck tatsächlich natürlich und strikt positiv ist, wird dort
 * geprüft, wo sein Wertebereich und die zugehörigen Annahmen verfügbar sind.
 */
data class Tensorraum(
    val elementMenge: MengenAusdruck,
    val dimensionen: List<ZahlAusdruck>,
) : MengenAusdruck {
    init {
        require(dimensionen.isNotEmpty()) { "Ein Tensorraum benötigt mindestens eine Dimension." }
        dimensionen.forEachIndexed { index, dimension ->
            if (dimension is RationaleZahl) {
                require(dimension.nenner == java.math.BigInteger.ONE && dimension.zähler.signum() > 0) {
                    "Tensorraumdimension ${index + 1} muss eine positive natürliche Zahl sein."
                }
            }
        }
    }

    override fun zuLatex(): String {
        val exponent = dimensionen.joinToString("\\times") { it.zuLatex() }
        return "${elementMenge.zuLatex()}^{$exponent}"
    }
}

/** Quellkompatible Fabrik für die zahlreichen bereits konkreten Tensorformen. */
@Suppress("FunctionName")
fun Tensorraum(elementMenge: MengenAusdruck, dimensionen: List<Int>): Tensorraum = Tensorraum(
    elementMenge = elementMenge,
    dimensionen = dimensionen.map { RationaleZahl.von(it.toLong()) },
)

/** Der Restklassenring der ganzen Zahlen modulo n. */
data class ModuloZahlenraum(val modul: Int) : MengenAusdruck {
    init { require(modul >= 2) { "Der Modul muss mindestens 2 sein." } }
    override fun zuLatex() = "\\mathbb{Z}/${modul}\\mathbb{Z}"
}
