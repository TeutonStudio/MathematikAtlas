package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.AxiomArgument
import de.TeutonStudio.MathematikRechenSystem.kern.AxiomOperatorDefinition
import de.TeutonStudio.MathematikRechenSystem.kern.AxiomOperatoren
import de.TeutonStudio.MathematikRechenSystem.kern.RelationsOperatoren

/**
 * Liefert für jede Axiomvariante des kanonischen Prädikatknotens eine eigene
 * Definitionskarte. Der Knoten-Langdruck öffnet bereits den Konzeptdialog;
 * diese dynamische Definition schließt dort die bisher fehlende Axiomauflösung.
 */
internal fun axiomPraedikatKonzept(knoten: KnotenDaten): KonzeptDefinition? {
    if (knoten.art != RelationsOperatoren.KNOTEN_ART) return null
    val definition = AxiomOperatoren.vonIdOderNull(
        knoten.parameter[RelationsOperatoren.OPERATOR_PARAMETER],
    ) ?: return null
    val systemTitel = definition.systeme.map { systemId ->
        AxiomOperatoren.systeme.firstOrNull { it.stabileId == systemId }?.titel ?: systemId
    }.sorted()

    return KonzeptDefinition(
        id = KonzeptId("axiom-praedikat-${definition.kartenKennung()}"),
        name = definition.titel,
        beschreibung = buildString {
            append(definition.titel)
            append(" als parametrisiertes Axiom-Prädikat")
            if (systemTitel.isNotEmpty()) {
                append(" im Axiomensystem ")
                append(systemTitel.joinToString())
            }
            append('.')
        },
        pfad = listOf("Logik", "Prädikate", "Axiome"),
        tags = buildSet {
            add("Axiom")
            add("Prädikat")
            add(definition.titel)
            add(definition.stabileId)
            add(definition.symbolLatex)
            addAll(definition.systeme)
            addAll(systemTitel)
            addAll(definition.suchbegriffe)
            addAll(definition.argumente.map(AxiomArgument::rolle))
        },
        knotenArten = setOf(RelationsOperatoren.KNOTEN_ART),
        knotenParameter = mapOf(
            RelationsOperatoren.OPERATOR_PARAMETER to definition.stabileId,
            PRAEDIKAT_SEITE_PARAMETER to PRAEDIKAT_SEITE_AXIOME,
        ),
        reiter = listOf(
            KonzeptReiter(
                id = "definition",
                titel = definition.symbolLatex,
                rolle = KonzeptReiterRolle.Definition,
                karte = axiomPraedikatDefinitionsKarte(definition),
            ),
        ),
    )
}

private fun axiomPraedikatDefinitionsKarte(definition: AxiomOperatorDefinition): KartenDaten {
    val prefix = "definition-axiom-${definition.kartenKennung()}"
    val erwarteteAnschlüsse = praedikatAnschluesse(definition).map { anschluss ->
        anschluss.copy(id = AnschlussId("$prefix-praedikat-${anschluss.name.sichereKennung()}-${anschluss.richtung.name.lowercase()}"))
    }
    val prädikatEingänge = erwarteteAnschlüsse
        .filter { it.richtung == AnschlussRichtung.Eingang }
        .sortedBy { it.reihenfolge }
    val prädikatAusgang = erwarteteAnschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }
    val zeilenAbstand = 132f
    val prädikatY = if (prädikatEingänge.isEmpty()) {
        80f
    } else {
        45f + (prädikatEingänge.lastIndex * zeilenAbstand) / 2f
    }

    val prädikatId = KnotenId("$prefix-praedikat")
    val prädikat = KnotenDaten(
        id = prädikatId,
        art = RelationsOperatoren.KNOTEN_ART,
        name = "Prädikat · ${definition.titel}",
        position = GraphPunkt(390f, prädikatY),
        größe = GraphGröße(320f, maxOf(155f, 105f + prädikatEingänge.size * 28f)),
        anschlüsse = erwarteteAnschlüsse,
        parameter = mapOf(
            RelationsOperatoren.OPERATOR_PARAMETER to definition.stabileId,
            PRAEDIKAT_SEITE_PARAMETER to PRAEDIKAT_SEITE_AXIOME,
        ),
    )

    val knoten = mutableListOf<KnotenDaten>()
    val verbindungen = mutableListOf<VerbindungDaten>()

    definition.argumente.zip(prädikatEingänge).forEachIndexed { index, (argument, ziel) ->
        val rolle = argument.rolle.sichereKennung()
        val eingangId = KnotenId("$prefix-eingang-$rolle")
        val eingangAusgangId = AnschlussId("$prefix-eingang-$rolle-wert")
        knoten += KnotenDaten(
            id = eingangId,
            art = KonzeptKnotenArten.EINGANG,
            name = argument.rolle,
            position = GraphPunkt(45f, 45f + index * zeilenAbstand),
            größe = GraphGröße(235f, 96f),
            anschlüsse = listOf(
                AnschlussDaten(
                    id = eingangAusgangId,
                    name = "wert",
                    richtung = AnschlussRichtung.Ausgang,
                    kante = AnschlussKante.Rechts,
                    art = ziel.art,
                ),
            ),
            parameter = mapOf(
                "typ" to argument.typBeschreibung(ziel.art),
                "rolle" to argument.rolle,
            ),
        )
        verbindungen += VerbindungDaten(
            id = VerbindungsId("$prefix-kante-$rolle"),
            von = AnschlussVerweis(eingangId, eingangAusgangId),
            zu = AnschlussVerweis(prädikatId, ziel.id),
        )
    }

    knoten += prädikat

    val ausgangId = KnotenId("$prefix-ausgang")
    val ausgangEingangId = AnschlussId("$prefix-ausgang-aussage")
    knoten += KnotenDaten(
        id = ausgangId,
        art = KonzeptKnotenArten.AUSGANG,
        name = "Aussage",
        position = GraphPunkt(790f, prädikatY + 25f),
        größe = GraphGröße(220f, 96f),
        anschlüsse = listOf(
            AnschlussDaten(
                id = ausgangEingangId,
                name = "wert",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = prädikatAusgang.art,
            ),
        ),
        parameter = mapOf(
            "typ" to prädikatAusgang.art.wert,
            "rolle" to "aussage",
        ),
    )
    verbindungen += VerbindungDaten(
        id = VerbindungsId("$prefix-kante-aussage"),
        von = AnschlussVerweis(prädikatId, prädikatAusgang.id),
        zu = AnschlussVerweis(ausgangId, ausgangEingangId),
    )

    val formelY = maxOf(330f, 85f + definition.argumente.size * zeilenAbstand)
    knoten += KnotenDaten(
        id = KnotenId("$prefix-formel"),
        art = KonzeptKnotenArten.REGEL,
        name = "Axiomformel",
        position = GraphPunkt(340f, formelY),
        größe = GraphGröße(460f, 150f),
        parameter = mapOf(
            "regel" to definition.symbolLatex,
            "regelInhaltArt" to "DISPLAY_LATEX",
            "knotenArt" to RelationsOperatoren.KNOTEN_ART,
        ),
    )

    return KartenDaten(
        id = KartenId(prefix),
        name = "Definition: ${definition.titel}",
        knoten = knoten,
        verbindungen = verbindungen,
    )
}

private fun AxiomOperatorDefinition.kartenKennung(): String =
    stabileId.removePrefix("axiom.").sichereKennung()

private fun String.sichereKennung(): String =
    lowercase().replace(Regex("[^a-z0-9_-]+"), "-").trim('-').ifBlank { "wert" }

private fun AxiomArgument.typBeschreibung(anschlussArt: AnschlussArtId): String = buildString {
    append(anschlussArt.wert)
    stelligkeit?.let { append(" · ").append(it).append("-stellig") }
}
