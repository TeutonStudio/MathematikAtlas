package de.TeutonStudio.MathematikRechenSystem.kern

import java.math.BigInteger

sealed interface IterationsOrdnung {
    fun zuLatex(): String

    data class Konkret(val wert: BigInteger) : IterationsOrdnung {
        init { require(wert.signum() >= 0) { "Iterationsordnungen müssen in ℕ₀ liegen." } }
        constructor(wert: Long) : this(BigInteger.valueOf(wert))
        override fun zuLatex(): String = wert.toString()
    }

    data class Symbolisch(
        val ausdruck: ZahlAusdruck,
        val annahmen: Set<Aussage> = emptySet(),
    ) : IterationsOrdnung {
        override fun zuLatex(): String = ausdruck.zuLatex()
    }
}

enum class IterationsArt(val operatorId: String) {
    MULTIPLIKATION("iteration.multiplikation"),
    DIFFERENTIATION("iteration.differentiation"),
    SELBSTKOMPOSITION("iteration.selbstkomposition"),
}

data class MethodenEinschraenkung(
    val methode: Methode,
    val menge: MengenAusdruck,
    val teilMengenStatus: Boolean? = null,
) : MathematischesObjekt {
    val operatorId: String = "methode.einschraenkung"
    val voraussetzungen: Set<String> = if (teilMengenStatus == null) {
        setOf("${menge.zuLatex()}\\subseteq D_{${methode.name}}")
    } else {
        emptySet()
    }

    init {
        require(teilMengenStatus != false) {
            "Eine nachweislich fremde Menge kann nicht als Wertevorrat der Methode eingeschränkt werden."
        }
    }

    override fun zuLatex(): String = "${methode.name}\\vert_{${menge.zuLatex()}}"
}

data class EingeschraenkteIdentitaet(
    val menge: MengenAusdruck,
) : MathematischesObjekt {
    val operatorId: String = "methode.identitaet.eingeschraenkt"
    override fun zuLatex(): String = "\\operatorname{id}\\vert_{${menge.zuLatex()}}"
}

data class IterierterAusdruck(
    val basis: MathematischesObjekt,
    val art: IterationsArt,
    val ordnung: IterationsOrdnung,
) : MathematischesObjekt {
    val operatorId: String = art.operatorId

    override fun zuLatex(): String = when (art) {
        IterationsArt.MULTIPLIKATION ->
            "{${basisLatex()}}^{${ordnung.zuLatex()}}"
        IterationsArt.DIFFERENTIATION -> differenzierungsLatex()
        IterationsArt.SELBSTKOMPOSITION ->
            "{${basisLatex()}}^{\\langle ${ordnung.zuLatex()}\\rangle}"
    }

    private fun basisLatex(): String = when (basis) {
        is Methode -> basis.name
        else -> basis.zuLatex()
    }

    private fun differenzierungsLatex(): String = when (ordnung) {
        is IterationsOrdnung.Symbolisch ->
            "{${basisLatex()}}^{(${ordnung.zuLatex()})}"
        is IterationsOrdnung.Konkret -> when {
            ordnung.wert == BigInteger.ZERO -> basisLatex()
            else -> roemischeZahlOderNull(ordnung.wert)?.let { roemisch ->
                "{${basisLatex()}}^{\\mathrm{$roemisch}}"
            } ?: "{${basisLatex()}}^{(${ordnung.wert})}"
        }
    }
}

sealed interface IterationsNullfall {
    data class MultiplikativNeutral(val element: MathematischesObjekt) : IterationsNullfall
    data class UrspruenglicherAusdruck(val ausdruck: MathematischesObjekt) : IterationsNullfall
    data class Identitaet(val identitaet: EingeschraenkteIdentitaet) : IterationsNullfall
}

fun bestimmeIterationsNullfall(
    art: IterationsArt,
    basis: MathematischesObjekt,
    neutralesElement: MathematischesObjekt? = null,
    werteVorrat: MengenAusdruck? = null,
): IterationsNullfall = when (art) {
    IterationsArt.MULTIPLIKATION -> IterationsNullfall.MultiplikativNeutral(
        requireNotNull(neutralesElement) {
            "Die nullte Multiplikationsiteration benötigt das neutrale Element ihrer Struktur."
        },
    )
    IterationsArt.DIFFERENTIATION -> IterationsNullfall.UrspruenglicherAusdruck(basis)
    IterationsArt.SELBSTKOMPOSITION -> IterationsNullfall.Identitaet(
        EingeschraenkteIdentitaet(
            requireNotNull(werteVorrat) {
                "Die nullte Selbstkomposition benötigt den Wertevorrat der Methode."
            },
        ),
    )
}

fun roemischeZahlOderNull(wert: BigInteger): String? {
    if (wert < BigInteger.ONE || wert > BigInteger.valueOf(3999)) return null
    var rest = wert.toInt()
    val teile = listOf(
        1000 to "M",
        900 to "CM",
        500 to "D",
        400 to "CD",
        100 to "C",
        90 to "XC",
        50 to "L",
        40 to "XL",
        10 to "X",
        9 to "IX",
        5 to "V",
        4 to "IV",
        1 to "I",
    )
    return buildString {
        for ((zahl, zeichen) in teile) {
            while (rest >= zahl) {
                append(zeichen)
                rest -= zahl
            }
        }
    }
}
