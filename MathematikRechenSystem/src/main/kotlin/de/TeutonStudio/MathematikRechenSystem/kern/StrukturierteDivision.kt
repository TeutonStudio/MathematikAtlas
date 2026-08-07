package de.TeutonStudio.MathematikRechenSystem.kern

enum class DivisionsSeite(val latexIndex: String, val persistenzWert: String) {
    LINKS("L", "links"),
    RECHTS("R", "rechts");

    companion object {
        fun ausPersistenzOderNull(wert: String?): DivisionsSeite? = entries.firstOrNull {
            wert.equals(it.persistenzWert, ignoreCase = true) ||
                wert.equals(it.name, ignoreCase = true) ||
                wert.equals(it.latexIndex, ignoreCase = true)
        }
    }
}

enum class KommutativitaetsStatus {
    NACHGEWIESEN,
    NICHT_KOMMUTATIV,
    UNBEKANNT,
}

enum class InvertierbarkeitsStatus {
    NACHGEWIESEN,
    WIDERLEGT,
    UNBEKANNT,
}

enum class DivisionsVoraussetzungsArt {
    DIVISOR_IN_STRUKTUR,
    DIVISOR_INVERTIERBAR,
    BEREICHE_KOMPATIBEL,
}

data class DivisionsVoraussetzung(
    val art: DivisionsVoraussetzungsArt,
    val beschreibung: String,
    val latex: String? = null,
) {
    init { require(beschreibung.isNotBlank()) }
}

data class InversesElement(val argument: ZahlAusdruck) : ZahlAusdruck {
    override fun zuLatex(): String = "{${argument.zuLatex()}}^{-1}"
}

data class StrukturierteDivision(
    val dividend: ZahlAusdruck,
    val divisor: ZahlAusdruck,
    val seite: DivisionsSeite,
    val kommutativitaet: KommutativitaetsStatus = KommutativitaetsStatus.UNBEKANNT,
    val invertierbarkeit: InvertierbarkeitsStatus = standardInvertierbarkeit(divisor),
    val struktur: ZahlbereichsId? = null,
    val voraussetzungen: Set<DivisionsVoraussetzung> = emptySet(),
) : ZahlAusdruck {
    val operatorId: String = "algebra.division"
    val persistenzParameter: Map<String, String> = mapOf("divisionsSeite" to seite.persistenzWert)

    val effektiveVoraussetzungen: Set<DivisionsVoraussetzung> = buildSet {
        addAll(voraussetzungen)
        if (struktur == null) {
            add(
                DivisionsVoraussetzung(
                    DivisionsVoraussetzungsArt.DIVISOR_IN_STRUKTUR,
                    "Die algebraische Struktur ist noch nicht bestimmt.",
                ),
            )
        }
        if (invertierbarkeit == InvertierbarkeitsStatus.UNBEKANNT) {
            add(
                DivisionsVoraussetzung(
                    DivisionsVoraussetzungsArt.DIVISOR_INVERTIERBAR,
                    "Die Invertierbarkeit des Divisors muss nachgewiesen werden.",
                    "${divisor.zuLatex()}^{-1}",
                ),
            )
        }
    }

    val istNachweislichUngueltig: Boolean
        get() = divisor == RationaleZahl.Null || invertierbarkeit == InvertierbarkeitsStatus.WIDERLEGT

    override fun zuLatex(): String = when (kommutativitaet) {
        KommutativitaetsStatus.NACHGEWIESEN -> "\\frac{${dividend.zuLatex()}}{${divisor.zuLatex()}}"
        KommutativitaetsStatus.NICHT_KOMMUTATIV,
        KommutativitaetsStatus.UNBEKANNT,
        -> "${klammere(dividend)}\\div_{${seite.latexIndex}}\\,${klammere(divisor)}"
    }

    fun alsGeordnetesProdukt(): Multiplikation {
        require(!istNachweislichUngueltig) { "Der Divisor ist nicht invertierbar." }
        val inverses = InversesElement(divisor)
        return Multiplikation.roh(
            when (seite) {
                DivisionsSeite.RECHTS -> listOf(dividend, inverses)
                DivisionsSeite.LINKS -> listOf(inverses, dividend)
            },
        )
    }

    fun normalisiereKommutativ(): ZahlAusdruck = if (
        kommutativitaet == KommutativitaetsStatus.NACHGEWIESEN && !istNachweislichUngueltig
    ) Division(dividend, divisor) else this

    private fun klammere(ausdruck: ZahlAusdruck): String = when (ausdruck) {
        is Addition -> "\\left(${ausdruck.zuLatex()}\\right)"
        else -> ausdruck.zuLatex()
    }
}

fun standardInvertierbarkeit(divisor: ZahlAusdruck): InvertierbarkeitsStatus = when (divisor) {
    RationaleZahl.Null -> InvertierbarkeitsStatus.WIDERLEGT
    is RationaleZahl -> InvertierbarkeitsStatus.NACHGEWIESEN
    else -> InvertierbarkeitsStatus.UNBEKANNT
}

fun strukturierteDivision(
    dividend: ZahlAusdruck,
    divisor: ZahlAusdruck,
    seite: DivisionsSeite,
    kommutativitaet: KommutativitaetsStatus,
    invertierbarkeit: InvertierbarkeitsStatus = standardInvertierbarkeit(divisor),
    struktur: ZahlbereichsId? = null,
    voraussetzungen: Set<DivisionsVoraussetzung> = emptySet(),
): ZahlAusdruck = StrukturierteDivision(
    dividend,
    divisor,
    seite,
    kommutativitaet,
    invertierbarkeit,
    struktur,
    voraussetzungen,
).normalisiereKommutativ()
