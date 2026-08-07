package de.TeutonStudio.MathematikRechenSystem.kern

sealed interface DivisionsMigrationErgebnis {
    data class Erfolg(
        val ausdruck: ZahlAusdruck,
        val seite: DivisionsSeite?,
        val kommutativNormalisiert: Boolean,
    ) : DivisionsMigrationErgebnis

    data class SeitenAuswahlErforderlich(
        val dividend: ZahlAusdruck,
        val divisor: ZahlAusdruck,
        val grund: String,
        val erlaubteSeiten: List<DivisionsSeite> = DivisionsSeite.entries,
    ) : DivisionsMigrationErgebnis
}

/**
 * Migriert historische Quotienten, ohne aus der Operandenreihenfolge eine
 * Divisionsseite abzuleiten.
 */
fun migriereHistorischeDivision(
    dividend: ZahlAusdruck,
    divisor: ZahlAusdruck,
    kommutativitaet: KommutativitaetsStatus,
    gespeicherteSeite: DivisionsSeite? = null,
    struktur: ZahlbereichsId? = null,
): DivisionsMigrationErgebnis = when {
    divisor == RationaleZahl.Null -> DivisionsMigrationErgebnis.SeitenAuswahlErforderlich(
        dividend = dividend,
        divisor = divisor,
        grund = "Der historische Divisor ist null und nicht invertierbar.",
        erlaubteSeiten = emptyList(),
    )

    kommutativitaet == KommutativitaetsStatus.NACHGEWIESEN -> DivisionsMigrationErgebnis.Erfolg(
        ausdruck = Division(dividend, divisor),
        seite = gespeicherteSeite,
        kommutativNormalisiert = true,
    )

    gespeicherteSeite != null -> DivisionsMigrationErgebnis.Erfolg(
        ausdruck = StrukturierteDivision(
            dividend = dividend,
            divisor = divisor,
            seite = gespeicherteSeite,
            kommutativitaet = kommutativitaet,
            struktur = struktur,
        ),
        seite = gespeicherteSeite,
        kommutativNormalisiert = false,
    )

    else -> DivisionsMigrationErgebnis.SeitenAuswahlErforderlich(
        dividend = dividend,
        divisor = divisor,
        grund = "Für die historische Division fehlt die semantische Divisionsseite.",
    )
}
