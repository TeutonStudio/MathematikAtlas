package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.OrientierterVektor
import de.TeutonStudio.MathematikRechenSystem.kern.Tupel
import de.TeutonStudio.MathematikRechenSystem.kern.ZahlAusdruck

/**
 * Lokale Fehlergrenze des Polynomzustands.
 *
 * Der allgemeine Karten-Auswerter schützt zwar ebenfalls vor Exceptions, aber
 * ein Rechneroperator soll auch bei direkter Auswertung ein normales
 * Knotenergebnis liefern. Dadurch sind Vorschau, Inspector, Definitionskarte
 * und Tests unabhängig davon robust, ob sie über eine vollständige Karte laufen.
 */
fun MathematikAuswerterRegister.registrierePolynomFehlergrenze() {
    val basis = requireNotNull(finde(ZAHLENRECHNER_ART))
    registriere(ZAHLENRECHNER_ART) { kontext ->
        if (kontext.knoten.parameter[ZAHLENRECHNER_OPERATOR] != ErweiterterZahlenOperator.POLYNOM.stabileId) {
            return@registriere basis.auswerten(kontext)
        }

        val koeffizientenWert = kontext.eingänge["koeffizienten"]
            ?: return@registriere polynomFehler(kontext.eingänge, "Die Koeffizienten fehlen.")
        val argumentWert = kontext.eingänge["argument"]
            ?: return@registriere polynomFehler(kontext.eingänge, "Das Polynomargument fehlt.")

        val koeffizienten = when (val objekt = koeffizientenWert.objekt) {
            is Tupel -> objekt.elemente
            is OrientierterVektor -> objekt.werte
            else -> return@registriere polynomFehler(
                kontext.eingänge,
                "Koeffizienten müssen als Tupel oder orientierter Vektor vorliegen.",
            )
        }
        if (koeffizienten.isEmpty()) {
            return@registriere polynomFehler(kontext.eingänge, "Ein Polynom benötigt mindestens einen Koeffizienten.")
        }
        val falscherIndex = koeffizienten.indexOfFirst { it !is ZahlAusdruck }
        if (falscherIndex >= 0) {
            return@registriere polynomFehler(
                kontext.eingänge,
                "Koeffizient ${falscherIndex + 1} ist keine Zahl.",
            )
        }
        if (argumentWert.objekt !is ZahlAusdruck) {
            return@registriere polynomFehler(kontext.eingänge, "Das Polynomargument ist kein Zahlterm.")
        }

        runCatching { basis.auswerten(kontext) }.getOrElse { fehler ->
            polynomFehler(
                kontext.eingänge,
                fehler.message ?: "Der Polynomoperator konnte nicht ausgewertet werden.",
            )
        }
    }
}

private fun polynomFehler(
    eingänge: Map<String, de.TeutonStudio.MathematikKartenAdapter.BedingterWert>,
    nachricht: String,
): KnotenAuswertungsErgebnis = KnotenAuswertungsErgebnis(
    ausgaben = emptyMap(),
    eingänge = eingänge,
    fehler = nachricht,
)
