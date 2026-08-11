package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage

/**
 * Vollständige Vorlagenquelle für Definitionsdialoge und Konzeptkarten.
 *
 * Der Erstellen-Dialog verwendet [alleMathematikKnotenVorlagen] und zeigt je
 * Rechnerfamilie nur einen konfigurierbaren Knoten. Definitionsoberflächen
 * benötigen dagegen weiterhin jede operatorabhängige Anschluss- und
 * Regelvariante des Zahlenrechners sowie die vorkonfigurierten Axiom-Prädikate.
 */
fun alleMathematikDefinitionsVorlagen(): List<KnotenVorlage> =
    (
        alleMathematikKnotenVorlagen().filterNot { it.art == ZAHLENRECHNER_ART } +
            ZahlenRechnerKnotenVorlagen.alle +
            AxiomKnotenVorlagen.alle
    ).distinctBy { vorlage ->
        vorlage.art to listOf(
            vorlage.standardParameter[ZAHLENRECHNER_OPERATOR].orEmpty(),
            vorlage.standardParameter["operator"].orEmpty(),
            vorlage.standardParameter["eingabeModus"].orEmpty(),
            vorlage.name,
        ).joinToString("|")
    }
