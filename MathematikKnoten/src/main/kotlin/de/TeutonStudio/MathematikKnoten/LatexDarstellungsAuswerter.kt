package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.*

/** Darstellung konfiguriert ausschließlich die Anzeige eines unveränderten Wertes. */
internal fun MathematikAuswerterRegister.registriereLatexDarstellung() {
    registriere("mathematik.darstellungsoptimierung") { k ->
        val wert = k.eingänge["wert"] ?: error("Ein Wert muss verbunden sein.")
        val latex = k.knoten.parameter["latex"].orEmpty().trim()
        KnotenAuswertungsErgebnis(
            mapOf("wert" to wert.copy(latexDarstellung = latex.ifBlank { wert.latexDarstellung })),
            eingänge = k.eingänge,
            bezeugteAusgaben = setOf("wert"),
        )
    }
}
