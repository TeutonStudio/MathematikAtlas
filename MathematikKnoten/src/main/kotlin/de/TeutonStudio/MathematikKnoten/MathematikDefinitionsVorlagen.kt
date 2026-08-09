package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator

/**
 * Vollständige Vorlagenquelle für Definitionsdialoge und Konzeptkarten.
 *
 * Der Erstellen-Dialog verwendet [alleMathematikKnotenVorlagen] und zeigt je
 * Rechnerfamilie nur einen konfigurierbaren Knoten. Definitionsoberflächen
 * benötigen dagegen weiterhin jede kanonische operatorabhängige Anschluss- und
 * Regelvariante des Zahlenrechners. Historische Aliasoperatoren bleiben ladbar,
 * erhalten aber keine zweite sichtbare Definitionsvariante.
 */
fun alleMathematikDefinitionsVorlagen(): List<KnotenVorlage> =
    (
        alleMathematikKnotenVorlagen().filterNot { it.art == ZAHLENRECHNER_ART } +
            ZahlenRechnerKnotenVorlagen.alle.filterNot { vorlage ->
                vorlage.standardParameter[ZAHLENRECHNER_OPERATOR] ==
                    UniversellerZahlenOperator.KOMPLEXER_RADIUS.stabileId
            }
    ).distinctBy { vorlage ->
        vorlage.art to listOf(
            vorlage.standardParameter[ZAHLENRECHNER_OPERATOR].orEmpty(),
            vorlage.standardParameter["eingabeModus"].orEmpty(),
            vorlage.name,
        ).joinToString("|")
    }