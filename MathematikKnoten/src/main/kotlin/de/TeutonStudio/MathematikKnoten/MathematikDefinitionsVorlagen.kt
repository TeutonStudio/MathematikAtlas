package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage

/**
 * Vollständige Vorlagenquelle für Definitionsdialoge und Konzeptkarten.
 *
 * Der Erstellen-Dialog verwendet [alleMathematikKnotenVorlagen] und zeigt je
 * Rechnerfamilie nur einen konfigurierbaren Knoten. Definitionsoberflächen
 * benötigen dagegen weiterhin jede operatorabhängige Anschluss- und
 * Regelvariante des Zahlenrechners.
 */
fun alleMathematikDefinitionsVorlagen(): List<KnotenVorlage> =
    (
        alleMathematikKnotenVorlagen().filterNot { it.art == ZAHLENRECHNER_ART } +
            ZahlenRechnerKnotenVorlagen.alle
    ).distinctBy { vorlage ->
        vorlage.art to listOf(
            vorlage.standardParameter[ZAHLENRECHNER_OPERATOR].orEmpty(),
            vorlage.standardParameter["eingabeModus"].orEmpty(),
            vorlage.name,
        ).joinToString("|")
    }
