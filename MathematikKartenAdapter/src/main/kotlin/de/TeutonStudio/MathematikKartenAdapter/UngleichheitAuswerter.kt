package de.TeutonStudio.MathematikKartenAdapter

import de.TeutonStudio.MathematikRechenSystem.kern.Ungleichheit

/** Wertet den eigenständig erzeugbaren Ungleichheitsknoten analog zur Gleichheit aus. */
internal object UngleichheitAuswerter : MathematikKnotenAuswerter {
    override fun auswerten(kontext: KnotenAuswertungsKontext): KnotenAuswertungsErgebnis {
        val links = kontext.eingänge["links"]?.objekt ?: error("Linke Seite fehlt.")
        val rechts = kontext.eingänge["rechts"]?.objekt ?: error("Rechte Seite fehlt.")

        return KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "aussage" to BedingterWert(
                    objekt = Ungleichheit(links, rechts),
                    annahmen = kontext.eingänge.values.flatMap { it.annahmen }.toSet(),
                ),
            ),
        )
    }
}
