package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.Methode
import de.TeutonStudio.MathematikRechenSystem.kern.RationaleZahl
import de.TeutonStudio.MathematikRechenSystem.kern.Tupel

/**
 * Ergänzt den historischen Tupel-Auswerter ausschließlich um den Grenzfall n = 0.
 * Positive Dimensionen und der Elementmodus bleiben vollständig beim bestehenden
 * Auswerter, damit deren etablierte Platzhalter-, Annahmen- und Fehlerssemantik
 * unverändert erhalten bleibt.
 */
fun MathematikAuswerterRegister.registriereTupelNullDimension() {
    val bisher = finde("mathematik.tupel")
        ?: error("Die 0-Dimensions-Verfeinerung benötigt den bestehenden Tupel-Auswerter.")

    registriere("mathematik.tupel") { kontext ->
        if (tupelKonfiguration(kontext.knoten).erzeugungsArt != TUPEL_METHODE) {
            return@registriere bisher.auswerten(kontext)
        }

        val dimension = kontext.eingänge["dimension"]?.objekt as? RationaleZahl
        val istNull = dimension?.let {
            it.nenner == java.math.BigInteger.ONE && it.zähler.signum() == 0
        } == true
        if (!istNull) return@registriere bisher.auswerten(kontext)

        kontext.eingänge["methode"]?.objekt as? Methode ?: error("Tupelmethode fehlt.")
        val annahmen = kontext.eingänge.values.flatMapTo(linkedSetOf()) { it.annahmen }
        KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "tupel" to BedingterWert(
                    objekt = Tupel(emptyList()),
                    annahmen = annahmen,
                ),
            ),
        )
    }
}
