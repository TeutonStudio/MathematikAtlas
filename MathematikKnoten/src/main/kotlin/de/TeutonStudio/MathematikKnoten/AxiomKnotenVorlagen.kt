package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikRechenSystem.kern.AxiomOperatorDefinition
import de.TeutonStudio.MathematikRechenSystem.kern.AxiomOperatoren
import de.TeutonStudio.MathematikRechenSystem.kern.RelationsOperatoren

/**
 * Vorkonfigurierte Axiomvarianten ausschließlich für Suche, Definitionskarten
 * und Konzeptbibliothek. Der normale Erstellen-Dialog zeigt weiterhin genau
 * einen konfigurierbaren Prädikatknoten.
 */
object AxiomKnotenVorlagen {
    fun vorlage(definition: AxiomOperatorDefinition): KnotenVorlage = KnotenVorlage(
        art = RelationsOperatoren.KNOTEN_ART,
        name = "Prädikat · ${definition.titel}",
        kategorie = "Aussagen: Prädikate: Axiome",
        beschreibung = buildString {
            append(definition.titel)
            append(" als parametrisiertes Axiom-Prädikat")
            if (definition.systeme.isNotEmpty()) {
                append(" (")
                append(definition.systeme.sorted().joinToString())
                append(")")
            }
            append('.')
        },
        standardGröße = GraphGröße(300f, 155f),
        anschlüsse = praedikatAnschluesse(definition),
        standardParameter = mapOf(
            RelationsOperatoren.OPERATOR_PARAMETER to definition.stabileId,
            PRAEDIKAT_SEITE_PARAMETER to PRAEDIKAT_SEITE_AXIOME,
        ),
    )

    val alle: List<KnotenVorlage> = AxiomOperatoren.alle.map(::vorlage)
}
