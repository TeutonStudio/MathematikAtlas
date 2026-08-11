package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenId
import de.TeutonStudio.MathematikKnoten.PRAEDIKAT_SEITE_AXIOME
import de.TeutonStudio.MathematikKnoten.PRAEDIKAT_SEITE_PARAMETER
import de.TeutonStudio.MathematikRechenSystem.kern.AxiomArgument
import de.TeutonStudio.MathematikRechenSystem.kern.AxiomArgumentArt
import de.TeutonStudio.MathematikRechenSystem.kern.AxiomOperatorDefinition
import de.TeutonStudio.MathematikRechenSystem.kern.AxiomOperatoren
import de.TeutonStudio.MathematikRechenSystem.kern.RelationsOperatoren

/**
 * Liefert für jeden kanonischen Axiom-Prädikatoperator eine Definitionskarte.
 *
 * Die Karte wird direkt aus [AxiomOperatorDefinition] erzeugt. Dadurch erhalten
 * auch später ergänzte Axiome automatisch dieselbe Definitionsdarstellung,
 * sofern sie in [AxiomOperatoren.alle] registriert sind.
 */
internal fun axiomPraedikatKonzept(knoten: KnotenDaten): KonzeptDefinition? {
    if (knoten.art != RelationsOperatoren.KNOTEN_ART) return null
    val operatorId = knoten.parameter[RelationsOperatoren.OPERATOR_PARAMETER] ?: return null
    val definition = AxiomOperatoren.vonIdOderNull(operatorId) ?: return null
    return axiomPraedikatKonzept(definition)
}

internal fun axiomPraedikatKonzept(definition: AxiomOperatorDefinition): KonzeptDefinition {
    val systemTitel = definition.systeme.sorted().map { systemId ->
        AxiomOperatoren.systeme.firstOrNull { it.stabileId == systemId }?.titel ?: systemId
    }
    val argumentBeschreibung = definition.argumente
        .joinToString { argument -> "${argument.rolle}: ${argument.typBeschreibung()}" }
        .ifBlank { "keine Eingangsparameter" }

    return KonzeptDefinition(
        id = KonzeptId("definition-${definition.stabileId}"),
        name = definition.titel,
        beschreibung = buildString {
            append("Formale Definition des Axiom-Prädikats „${definition.titel}“.")
            if (systemTitel.isNotEmpty()) {
                append(" Axiomensystem: ")
                append(systemTitel.joinToString(" · "))
                append('.')
            }
            append(" Eingänge: ")
            append(argumentBeschreibung)
            append('.')
        },
        pfad = definition.kategorie.split(" · ").filter(String::isNotBlank),
        tags = buildSet {
            add(definition.stabileId)
            add(definition.titel)
            add(definition.kategorie)
            addAll(definition.systeme)
            addAll(definition.suchbegriffe)
            definition.argumente.forEach { argument ->
                add(argument.rolle)
                add(argument.typBeschreibung())
            }
        },
        knotenArten = setOf(RelationsOperatoren.KNOTEN_ART),
        knotenParameter = mapOf(
            RelationsOperatoren.OPERATOR_PARAMETER to definition.stabileId,
            PRAEDIKAT_SEITE_PARAMETER to PRAEDIKAT_SEITE_AXIOME,
        ),
        reiter = listOf(
            KonzeptReiter(
                id = "definition",
                titel = "Definition",
                rolle = KonzeptReiterRolle.Definition,
                karte = axiomDefinitionsKarte(definition, systemTitel),
            ),
        ),
    )
}

private fun axiomDefinitionsKarte(
    definition: AxiomOperatorDefinition,
    systemTitel: List<String>,
): KartenDaten {
    val prefix = "definition-${definition.stabileId.replace('.', '-')}"
    val formel = KnotenDaten(
        id = KnotenId("$prefix-formel"),
        art = KonzeptKnotenArten.REGEL,
        name = definition.titel,
        position = GraphPunkt(300f, 55f),
        größe = GraphGröße(680f, 220f),
        parameter = mapOf(
            "regel" to definition.symbolLatex,
            "knotenArt" to definition.stabileId,
        ),
    )
    val system = KnotenDaten(
        id = KnotenId("$prefix-system"),
        art = KonzeptKnotenArten.REGEL,
        name = "Axiomensystem",
        position = GraphPunkt(40f, 75f),
        größe = GraphGröße(220f, 170f),
        parameter = mapOf(
            "regel" to systemTitel.joinToString("\n").ifBlank { definition.kategorie },
            "knotenArt" to definition.kategorie,
        ),
    )
    val eingänge = definition.argumente.mapIndexed { index, argument ->
        val spalte = index % 4
        val zeile = index / 4
        KnotenDaten(
            id = KnotenId("$prefix-eingang-${argument.rolle}"),
            art = KonzeptKnotenArten.EINGANG,
            name = argument.rolle,
            position = GraphPunkt(40f + spalte * 240f, 325f + zeile * 120f),
            größe = GraphGröße(215f, 92f),
            parameter = mapOf(
                "typ" to argument.typBeschreibung(),
                "rolle" to argument.rolle,
            ),
        )
    }

    return KartenDaten(
        id = KartenId(prefix),
        name = "Definition: ${definition.titel}",
        knoten = listOf(system, formel) + eingänge,
    )
}

private fun AxiomArgument.typBeschreibung(): String {
    val basis = when (art) {
        AxiomArgumentArt.OBJEKT -> "Objekt"
        AxiomArgumentArt.MENGE -> "Menge"
        AxiomArgumentArt.METHODE -> "Methode"
        AxiomArgumentArt.PRAEDIKAT -> "Prädikat"
    }
    return stelligkeit?.let { "$basis · $it-stellig" } ?: basis
}
