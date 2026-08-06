package de.TeutonStudio.MathematikRechenSystem.kern

enum class DivisionsSeite(val latexIndex: String) {
    LINKS("L"),
    RECHTS("R"),
}

enum class KommutativitaetsStatus {
    NACHGEWIESEN,
    NICHT_KOMMUTATIV,
    UNBEKANNT,
}

data class InversesElement(
    val argument: ZahlAusdruck,
) : ZahlAusdruck {
    override fun zuLatex(): String = "{${argument.zuLatex()}}^{-1}"
}

data class StrukturierteDivision(
    val dividend: ZahlAusdruck,
    val divisor: ZahlAusdruck,
    val seite: DivisionsSeite,
    val kommutativitaet: KommutativitaetsStatus = KommutativitaetsStatus.UNBEKANNT,
    val struktur: ZahlbereichsId? = null,
    val voraussetzungen: Set<String> = emptySet(),
) : ZahlAusdruck {
    val operatorId: String = "algebra.division.${seite.name.lowercase()}"

    override fun zuLatex(): String = when (kommutativitaet) {
        KommutativitaetsStatus.NACHGEWIESEN ->
            "\\frac{${dividend.zuLatex()}}{${divisor.zuLatex()}}"
        KommutativitaetsStatus.NICHT_KOMMUTATIV,
        KommutativitaetsStatus.UNBEKANNT,
        -> "${klammereFallsNoetig(dividend)}\\div_${seite.latexIndex}${klammereFallsNoetig(divisor)}"
    }

    fun alsGeordnetesProdukt(): Multiplikation {
        val inverses = InversesElement(divisor)
        val faktoren = when (seite) {
            DivisionsSeite.RECHTS -> listOf(dividend, inverses)
            DivisionsSeite.LINKS -> listOf(inverses, dividend)
        }
        return Multiplikation.roh(faktoren)
    }

    fun normalisiereKommutativ(): ZahlAusdruck = when (kommutativitaet) {
        KommutativitaetsStatus.NACHGEWIESEN -> Division(dividend, divisor)
        KommutativitaetsStatus.NICHT_KOMMUTATIV,
        KommutativitaetsStatus.UNBEKANNT,
        -> this
    }

    private fun klammereFallsNoetig(ausdruck: ZahlAusdruck): String = when (ausdruck) {
        is Addition -> "\\left(${ausdruck.zuLatex()}\\right)"
        else -> ausdruck.zuLatex()
    }
}

fun strukturierteDivision(
    dividend: ZahlAusdruck,
    divisor: ZahlAusdruck,
    seite: DivisionsSeite,
    kommutativitaet: KommutativitaetsStatus,
    struktur: ZahlbereichsId? = null,
    voraussetzungen: Set<String> = emptySet(),
): ZahlAusdruck = StrukturierteDivision(
    dividend = dividend,
    divisor = divisor,
    seite = seite,
    kommutativitaet = kommutativitaet,
    struktur = struktur,
    voraussetzungen = voraussetzungen,
).normalisiereKommutativ()
