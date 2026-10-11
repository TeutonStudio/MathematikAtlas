package de.TeutonStudio.MathematikKnoten.katalog

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenVorlage
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

/**
 * UI-neutraler Suchindex für OperatorKnoten.
 *
 * Ein Treffer enthält bereits den gewählten Operator. Der Benutzer muss nach der
 * Suche nicht noch einmal denselben Auswahldialog bedienen.
 */
object OperatorKnotenSuchindex {
    private data class Eintrag(
        val begriffe: Set<String>,
        val vorlage: KnotenVorlage,
    )

    fun suche(text: String, basisVorlagen: List<KnotenVorlage>): List<KnotenVorlage> {
        val query = normalisiere(text)
        if (query.isBlank()) return emptyList()
        return eintraege(basisVorlagen)
            .filter { eintrag -> eintrag.begriffe.any { normalisiere(it).contains(query) } }
            .map(Eintrag::vorlage)
            .distinctBy { it.art to it.standardParameter.toSortedMap().toString() }
    }

    private fun eintraege(basisVorlagen: List<KnotenVorlage>): List<Eintrag> = buildList {
        RechnerOperatorRegistry.alle.filterNot { it.operatorId.endsWith(".formel") }.forEach { vertrag ->
            val basis = when (vertrag.familie) {
                RechnerFamilie.ZAHL -> ZahlenRechnerKnotenVorlagen.standard
                RechnerFamilie.VEKTOR -> vektorRechnerVorlage(VektorRechnerOperator.ADDITION)
                RechnerFamilie.MATRIX -> StrukturFormelRechnerVorlagen.Matrix
                RechnerFamilie.TENSOR -> basisVorlagen.firstOrNull { it.art == TensorRechner.KNOTEN_ART }
                    ?: return@forEach
            }
            val konfiguriert = RechnerOperatorRegistry.konfiguriere(
                basis.erzeuge(GraphPunkt.Zero),
                vertrag.operatorId,
            )
            add(
                Eintrag(
                    begriffe = setOf(vertrag.titel, vertrag.operatorId, vertrag.familie.name, vertrag.symbolLatex) + vertrag.aliases,
                    vorlage = konfiguriert.alsSuchVorlage(
                        name = "${vertrag.familie.name.lowercase().replaceFirstChar(Char::uppercase)}rechner · ${vertrag.titel}",
                        kategorie = "Operatoren: ${vertrag.familie.name.lowercase().replaceFirstChar(Char::uppercase)}rechner",
                        beschreibung = "Vorkonfigurierter Operator ${vertrag.titel}.",
                    ),
                ),
            )
        }

        sichtbareMengenRechnerOperatoren().forEach { operator ->
            val konfiguriert = konfiguriereMengenRechner(
                MengenRechnerKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero),
                operator,
            )
            add(
                Eintrag(
                    begriffe = setOf(operator.titel(), operator.name, operator.stabileId) + mengenAliase(operator),
                    vorlage = konfiguriert.alsSuchVorlage(
                        name = "Mengenrechner · ${operator.titel()}",
                        kategorie = "Operatoren: Mengenrechner",
                        beschreibung = operator.definitionLatex(),
                    ),
                ),
            )
        }

        RelationsOperatoren.alle.forEach { relation ->
            add(
                Eintrag(
                    begriffe = relation.suchbegriffe + relation.titel + relation.stabileId,
                    vorlage = PraedikatKnotenVorlagen.vorlage(relation).copy(
                        name = "Prädikat · ${relation.titel}",
                        kategorie = "Operatoren: Relationen · ${relation.kategorie}",
                        beschreibung = "Vorkonfigurierte Relation ${relation.titel}.",
                    ),
                ),
            )
        }

        AxiomOperatoren.alle.forEach { axiom ->
            val basis = PraedikatKnotenVorlagen.standard.erzeuge(GraphPunkt.Zero)
            val konfiguriert = konfigurierePraedikat(basis, axiom)
            add(
                Eintrag(
                    begriffe = axiom.suchbegriffe + axiom.titel + axiom.stabileId + axiom.systeme,
                    vorlage = konfiguriert.alsSuchVorlage(
                        name = "Prädikat · ${axiom.titel}",
                        kategorie = "Operatoren: Axiome · ${axiom.kategorie}",
                        beschreibung = "Axiom-Prädikat ${axiom.titel}; Systeme: ${axiom.systeme.sorted().joinToString()}.",
                    ),
                ),
            )
        }
    }

    private fun KnotenDaten.alsSuchVorlage(
        name: String,
        kategorie: String,
        beschreibung: String,
    ) = KnotenVorlage(
        art = art,
        name = name,
        kategorie = kategorie,
        beschreibung = beschreibung,
        standardGröße = größe,
        anschlüsse = anschlüsse.map { it.copy(id = de.TeutonStudio.KnotenKartenVerwalter.daten.neueAnschlussId()) },
        standardParameter = parameter,
    )

    private fun normalisiere(text: String): String = text
        .lowercase()
        .replace('ä', 'a')
        .replace('ö', 'o')
        .replace('ü', 'u')
        .replace("ß", "ss")
        .trim()

    private fun mengenAliase(operator: MengenRechnerOperator): Set<String> = when (operator) {
        MengenRechnerOperator.VEREINIGUNG -> setOf("union", "vereinen")
        MengenRechnerOperator.SCHNITT -> setOf("intersection", "durchschnitt")
        MengenRechnerOperator.KARTESISCHES_PRODUKT -> setOf("produktmenge", "kartesisch")
        else -> emptySet()
    }

}
