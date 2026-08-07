package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.MathematikRechenSystem.kern.DivisionsSeite
import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator

/**
 * Schreibt den Standard rechts explizit, bevor eine neu erzeugte Karte erstmals
 * persistiert wird. Historisch offene Divisionen besitzen bereits den Offen-Marker
 * und werden ausdrücklich nicht normalisiert.
 */
fun KartenDaten.normalisiereStrukturierteDivisionVorSpeichern(): KartenDaten = copy(
    knoten = knoten.map { knoten ->
        val istDivision = knoten.art == ZAHLENRECHNER_ART &&
            UniversellerZahlenOperator.vonIdOderNull(
                knoten.parameter[ZAHLENRECHNER_OPERATOR],
            ) == UniversellerZahlenOperator.DIVISION
        val seiteFehlt = knoten.parameter[ZAHLENRECHNER_DIVISIONSSEITE].isNullOrBlank()
        val historischOffen = knoten.parameter[ZAHLENRECHNER_DIVISIONSSEITE_FEHLT] == "true"
        if (istDivision && seiteFehlt && !historischOffen) {
            konfiguriereDivisionsSeite(knoten, DivisionsSeite.RECHTS)
        } else {
            knoten
        }
    },
)
