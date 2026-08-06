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
        val annahmen: Set<Aussage>,
    ) : IterationsOrdnung {
        init { require(annahmen.isNotEmpty()) { "Eine symbolische Ordnung benötigt den sichtbaren Nachweis n∈ℕ₀." } }
        override fun zuLatex(): String = ausdruck.zuLatex()
    }
}

sealed interface IterationsOrdnungsPruefung {
    data class Gueltig(val ordnung: IterationsOrdnung) : IterationsOrdnungsPruefung
    data class Ungueltig(val nachricht: String) : IterationsOrdnungsPruefung
}

fun pruefeIterationsOrdnung(
    objekt: MathematischesObjekt,
    annahmen: Set<Aussage> = emptySet(),
): IterationsOrdnungsPruefung = when (objekt) {
    is RationaleZahl -> when {
        objekt.nenner != BigInteger.ONE -> IterationsOrdnungsPruefung.Ungueltig(
            "Iterationsordnungen müssen ganzzahlig sein.",
        )
        objekt.zähler.signum() < 0 -> IterationsOrdnungsPruefung.Ungueltig(
            "Negative Iterationsordnungen sind nicht Teil dieses Vertrags.",
        )
        else -> IterationsOrdnungsPruefung.Gueltig(IterationsOrdnung.Konkret(objekt.zähler))
    }
    is ZahlAusdruck -> {
        val natuerlichkeitsAussage = UnentscheidbareAussage(
            bezeichnung = "${objekt.zuLatex()}\\in\\mathbb N_0",
            system = "Iterationsordnung",
        )
        IterationsOrdnungsPruefung.Gueltig(
            IterationsOrdnung.Symbolisch(objekt, annahmen + natuerlichkeitsAussage),
        )
    }
    else -> IterationsOrdnungsPruefung.Ungueltig(
        "Die Iterationsordnung muss ein Zahlterm sein.",
    )
}

enum class IterationsArt(val operatorId: String) {
    MULTIPLIKATION("iteration.multiplikation"),
    DIFFERENTIATION("iteration.differentiation"),
    SELBSTKOMPOSITION("iteration.selbstkomposition"),
}

data class MethodenEinschraenkung(
    val ursprungsMethode: Methode,
    val menge: MengenAusdruck,
    val voraussetzungen: Set<Aussage> = emptySet(),
    val nachgewiesenesBild: MengenAusdruck? = null,
) : Ausdruck {
    val operatorId: String = "methode.einschraenkung"

    init {
        require(ursprungsMethode.parameter.size == 1) {
            "Die erste Restriktionsumsetzung erwartet eine einstellige Methode."
        }
    }

    val ursprungsWerteVorrat: MengenAusdruck
        get() {
            val parameter = ursprungsMethode.parameter.single()
            return ursprungsMethode.werteVorräte[parameter.name]
                ?: FehlendeObermenge("restriktion.${ursprungsMethode.name}.${parameter.name}")
        }

    val eingeschraenkteMethode: Methode
        get() {
            val parameter = ursprungsMethode.parameter.single()
            return ursprungsMethode.copy(
                name = zuLatex(),
                werteVorräte = ursprungsMethode.werteVorräte + (parameter.name to menge),
            )
        }

    override fun zuLatex(): String = "${ursprungsMethode.name}\\vert_{${menge.zuLatex()}}"
}

fun schraenkeMethodeEin(
    methode: Methode,
    menge: MengenAusdruck,
    kontext: RechenKontext = RechenKontext(),
    nachgewiesenesBild: MengenAusdruck? = null,
): MethodenEinschraenkung {
    require(methode.parameter.size == 1) {
        "Die erste Restriktionsumsetzung erwartet eine einstellige Methode."
    }
    val parameter = methode.parameter.single()
    val ursprungsBereich = methode.werteVorräte[parameter.name]
        ?: FehlendeObermenge("restriktion.${methode.name}.${parameter.name}")
    val beziehung = TeilmengenBeziehung(menge, ursprungsBereich)
    val ergebnis = beziehung.entscheide(kontext)
    require(ergebnis.wahrheitswert != Wahrheitswert.Lüge) {
        "${menge.zuLatex()} ist nachweislich keine Teilmenge von ${ursprungsBereich.zuLatex()}."
    }
    return MethodenEinschraenkung(
        ursprungsMethode = methode,
        menge = menge,
        voraussetzungen = if (ergebnis.wahrheitswert == Wahrheitswert.Wahr) emptySet() else setOf(beziehung),
        nachgewiesenesBild = nachgewiesenesBild,
    )
}

fun schraenkeMethodeEin(
    einschraenkung: MethodenEinschraenkung,
    menge: MengenAusdruck,
    kontext: RechenKontext = RechenKontext(),
    nachgewiesenesBild: MengenAusdruck? = null,
): MethodenEinschraenkung {
    val beziehung = TeilmengenBeziehung(menge, einschraenkung.menge)
    val ergebnis = beziehung.entscheide(kontext)
    require(ergebnis.wahrheitswert != Wahrheitswert.Lüge) {
        "${menge.zuLatex()} ist nachweislich keine Teilmenge der bisherigen Restriktion ${einschraenkung.menge.zuLatex()}."
    }
    return MethodenEinschraenkung(
        ursprungsMethode = einschraenkung.ursprungsMethode,
        menge = menge,
        voraussetzungen = einschraenkung.voraussetzungen +
            if (ergebnis.wahrheitswert == Wahrheitswert.Wahr) emptySet() else setOf(beziehung),
        nachgewiesenesBild = nachgewiesenesBild ?: einschraenkung.nachgewiesenesBild,
    )
}

data class EingeschraenkteIdentitaet(
    val menge: MengenAusdruck,
) : Ausdruck {
    val operatorId: String = "methode.identitaet.eingeschraenkt"
    private val variable = Variable("x")

    val alsMethode: Methode = Methode(
        name = "\\operatorname{id}\\vert_{${menge.zuLatex()}}",
        parameter = listOf(variable),
        vorschrift = variable,
        zielMenge = menge,
        werteVorräte = mapOf(variable.name to menge),
    )

    override fun zuLatex(): String = "\\operatorname{id}\\vert_{${menge.zuLatex()}}"
}

data class IterierterAusdruck(
    val basis: MathematischesObjekt,
    val art: IterationsArt,
    val ordnung: IterationsOrdnung,
) : Ausdruck {
    val operatorId: String = art.operatorId

    override fun zuLatex(): String = when (art) {
        IterationsArt.MULTIPLIKATION ->
            "{${basisLatex()}}^{${ordnung.zuLatex()}}"
        IterationsArt.DIFFERENTIATION -> differenzierungsLatex()
        IterationsArt.SELBSTKOMPOSITION ->
            "{${basisLatex()}}^{\\langle${ordnung.zuLatex()}\\rangle}"
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

data class IterationsOrdnungSnapshot(
    val art: String,
    val wert: String,
    val annahmenLatex: List<String> = emptyList(),
)

data class IterationsAusdruckSnapshot(
    val operatorId: String,
    val ordnung: IterationsOrdnungSnapshot,
)

fun IterationsOrdnung.alsSnapshot(): IterationsOrdnungSnapshot = when (this) {
    is IterationsOrdnung.Konkret -> IterationsOrdnungSnapshot(
        art = "konkret",
        wert = wert.toString(),
    )
    is IterationsOrdnung.Symbolisch -> IterationsOrdnungSnapshot(
        art = "symbolisch",
        wert = ausdruck.zuLatex(),
        annahmenLatex = annahmen.map(Aussage::zuLatex).sorted(),
    )
}

fun IterierterAusdruck.alsSnapshot(): IterationsAusdruckSnapshot = IterationsAusdruckSnapshot(
    operatorId = operatorId,
    ordnung = ordnung.alsSnapshot(),
)

fun IterationsOrdnungSnapshot.zuOrdnung(
    symbolResolver: (String) -> ZahlAusdruck = { Variable(it) },
): IterationsOrdnung = when (art) {
    "konkret" -> IterationsOrdnung.Konkret(wert.toBigInteger())
    "symbolisch" -> IterationsOrdnung.Symbolisch(
        ausdruck = symbolResolver(wert),
        annahmen = annahmenLatex.mapTo(linkedSetOf()) { latex ->
            UnentscheidbareAussage(latex, "persistierte Iterationsordnung")
        }.ifEmpty {
            linkedSetOf(
                UnentscheidbareAussage(
                    "$wert\\in\\mathbb N_0",
                    "persistierte Iterationsordnung",
                ),
            )
        },
    )
    else -> error("Unbekannte Iterationsordnungsart '$art'.")
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
