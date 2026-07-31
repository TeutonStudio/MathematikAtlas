package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.MathematikRechenSystem.kern.Ungleichheit

/** Wertet den eigenständig erzeugbaren Ungleichheitsknoten analog zur Gleichheit aus. */
internal object UngleichheitAuswerter : MathematikKnotenAuswerter {
    override fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        val links = kontext.eingänge["links"] ?: error("Linke Seite fehlt.")
        val rechts = kontext.eingänge["rechts"] ?: error("Rechte Seite fehlt.")

        return KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "aussage" to BedingterWert(
                    objekt = Ungleichheit(links.objekt, rechts.objekt),
                    annahmen = links.annahmen + rechts.annahmen,
                    latexDarstellung = "${links.anzeigeLatex()} \\ne ${rechts.anzeigeLatex()}",
                ),
            ),
        )
    }
}
