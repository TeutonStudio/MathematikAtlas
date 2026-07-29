package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

/** Registriert die arithmetische Subtraktion als eigene Knotensemantik. */
internal fun MathematikAuswerterRegister.registriereSubtraktion() {
    registriere("mathematik.subtraktion") { k ->
        val minuendWert = k.eingänge["minuend"] ?: error("Minuend fehlt.")
        val subtrahendWert = k.eingänge["subtrahend"] ?: error("Subtrahend fehlt.")
        val minuend = minuendWert.objekt as? ZahlAusdruck ?: error("Der Minuend muss eine Zahl sein.")
        val subtrahend = subtrahendWert.objekt as? ZahlAusdruck ?: error("Der Subtrahend muss eine Zahl sein.")
        val eingänge = listOf(minuendWert, subtrahendWert)
        val annahmen = eingänge.flatMap { it.annahmen }.toSet()
        val kontext = k.rechenKontext.copy(annahmen = k.rechenKontext.annahmen + annahmen)
        val differenz = vereinfache(subtraktion(minuend, subtrahend), kontext)
        KnotenAuswertungsErgebnis(mapOf(
            "wert" to BedingterWert(
                objekt = differenz,
                annahmen = annahmen,
                reelleVariablen = reelleVariablen(eingänge),
                variablenQuellen = eingänge.flatMap { it.variablenQuellen }.distinctBy {
                    Pair(Triple(it.knotenId, it.name, it.werteVorrat), it.alsMethodenParameter)
                },
                latexDarstellung = "${minuendWert.anzeigeLatex()} - ${subtrahendWert.anzeigeLatex()}",
            ),
        ))
    }
}
