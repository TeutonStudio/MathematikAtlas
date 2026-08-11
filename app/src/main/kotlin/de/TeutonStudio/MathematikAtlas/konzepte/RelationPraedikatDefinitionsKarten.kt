package de.TeutonStudio.MathematikAtlas

import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussId
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussKante
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussVerweis
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphGröße
import de.TeutonStudio.KnotenKartenVerwalter.daten.GraphPunkt
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KartenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenId
import de.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungDaten
import de.TeutonStudio.KnotenKartenVerwalter.daten.VerbindungsId
import de.TeutonStudio.MathematikKnoten.PRAEDIKAT_SEITE_PARAMETER
import de.TeutonStudio.MathematikKnoten.PRAEDIKAT_SEITE_RELATIONEN
import de.TeutonStudio.MathematikKnoten.praedikatAnschluesse
import de.TeutonStudio.MathematikRechenSystem.kern.RelationsArgument
import de.TeutonStudio.MathematikRechenSystem.kern.RelationsArgumentArt
import de.TeutonStudio.MathematikRechenSystem.kern.RelationsOperatorDefinition
import de.TeutonStudio.MathematikRechenSystem.kern.RelationsOperatoren

/**
 * Liefert für jede Variante des kanonischen Relations-Prädikatknotens eine
 * Definitionskarte. Die Karte wird direkt aus [RelationsOperatorDefinition]
 * erzeugt, damit neu registrierte Relationen automatisch denselben
 * Definitionsdialog erhalten.
 */
internal fun relationPraedikatKonzept(knoten: KnotenDaten): KonzeptDefinition? {
    if (knoten.art != RelationsOperatoren.KNOTEN_ART) return null
    if (knoten.parameter[PRAEDIKAT_SEITE_PARAMETER] != PRAEDIKAT_SEITE_RELATIONEN) return null

    val operatorId = knoten.parameter[RelationsOperatoren.OPERATOR_PARAMETER]
    val definition = RelationsOperatoren.vonIdOderNull(operatorId) ?: return null
    val relationsKlassen = definition.relationsStruktur?.kompakteKlassen().orEmpty()

    return KonzeptDefinition(
        id = KonzeptId("relation-praedikat-${definition.stabileId.replace('.', '-')}") ,
        name = "Relation: ${definition.titel}",
        beschreibung = buildString {
            append("Definitionskarte der Relation ")
            append(definition.titel)
            append(". Die Eingänge entsprechen den Relationsargumenten; der Ausgang ist die daraus gebildete Aussage.")
            if (relationsKlassen.isNotEmpty()) {
                append(" Nachgewiesene Relationsklasse: ")
                append(relationsKlassen.joinToString { it.titel })
                append('.')
            }
        },
        pfad = listOf("Aussagen", "Prädikate", "Relationen", definition.kategorie),
        tags = definition.suchbegriffe + relationsKlassen.map { it.titel } + setOf(
            "Relation",
            "Prädikat",
            definition.titel,
            definition.stabileId,
        ),
        knotenArten = setOf(RelationsOperatoren.KNOTEN_ART),
        knotenParameter = mapOf(
            RelationsOperatoren.OPERATOR_PARAMETER to definition.stabileId,
            PRAEDIKAT_SEITE_PARAMETER to PRAEDIKAT_SEITE_RELATIONEN,
        ),
        reiter = listOf(
            KonzeptReiter(
                id = "definition",
                titel = definition.symbolLatex,
                rolle = KonzeptReiterRolle.Definition,
                karte = relationPraedikatDefinitionsKarte(definition),
            ),
        ),
    )
}

private fun relationPraedikatDefinitionsKarte(definition: RelationsOperatorDefinition): KartenDaten {
    val suffix = definition.stabileId.replace('.', '-')
    val prefix = "definition-relation-$suffix"
    val schnittstelle = praedikatAnschluesse(definition)
    val eingänge = schnittstelle
        .filter { it.richtung == AnschlussRichtung.Eingang }
        .sortedBy { it.reihenfolge }
    val ausgang = schnittstelle.single { it.richtung == AnschlussRichtung.Ausgang }

    val regelAnschlüsse = schnittstelle.mapIndexed { index, anschluss ->
        anschluss.copy(id = AnschlussId("$prefix-regel-$index"))
    }
    val regel = KnotenDaten(
        id = KnotenId("$prefix-regel"),
        art = KonzeptKnotenArten.REGEL,
        name = definition.titel,
        position = GraphPunkt(390f, 70f),
        größe = GraphGröße(540f, maxOf(190f, 125f + 38f * eingänge.size)),
        anschlüsse = regelAnschlüsse,
        parameter = mapOf(
            "regel" to definition.symbolLatex,
            "regelInhaltArt" to DokumentationsInhaltArt.DISPLAY_LATEX.name,
            "definition" to definition.symbolLatex,
            "operator" to definition.stabileId,
            "relationsklassen" to definition.relationsStruktur
                ?.kompakteKlassen()
                .orEmpty()
                .joinToString { it.titel },
            "knotenArt" to RelationsOperatoren.KNOTEN_ART,
        ),
    )

    val eingangsKnoten = eingänge.mapIndexed { index, anschluss ->
        relationEingang(
            prefix = prefix,
            anschluss = anschluss,
            argument = definition.argumente[index],
            index = index,
        )
    }
    val ausgangsKnoten = relationAusgang(prefix, ausgang, eingänge.size)

    val regelEingänge = regel.anschlüsse
        .filter { it.richtung == AnschlussRichtung.Eingang }
        .sortedBy { it.reihenfolge }
    val regelAusgang = regel.anschlüsse.single { it.richtung == AnschlussRichtung.Ausgang }

    val verbindungen = buildList {
        eingangsKnoten.forEachIndexed { index, quelle ->
            add(
                VerbindungDaten(
                    id = VerbindungsId("$prefix-e-$index"),
                    von = AnschlussVerweis(quelle.id, quelle.anschlüsse.single().id),
                    zu = AnschlussVerweis(regel.id, regelEingänge[index].id),
                ),
            )
        }
        add(
            VerbindungDaten(
                id = VerbindungsId("$prefix-a"),
                von = AnschlussVerweis(regel.id, regelAusgang.id),
                zu = AnschlussVerweis(ausgangsKnoten.id, ausgangsKnoten.anschlüsse.single().id),
            ),
        )
    }

    return KartenDaten(
        id = KartenId(prefix),
        name = "Definition: ${definition.titel}",
        knoten = eingangsKnoten + regel + ausgangsKnoten,
        verbindungen = verbindungen,
    )
}

private fun relationEingang(
    prefix: String,
    anschluss: AnschlussDaten,
    argument: RelationsArgument,
    index: Int,
): KnotenDaten {
    val id = "$prefix-eingang-$index"
    return KnotenDaten(
        id = KnotenId(id),
        art = KonzeptKnotenArten.EINGANG,
        name = argument.rolle,
        position = GraphPunkt(30f, 70f + index * 120f),
        größe = GraphGröße(280f, 96f),
        anschlüsse = listOf(
            AnschlussDaten(
                id = AnschlussId("$id-wert"),
                name = "wert",
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = anschluss.art,
                zulässigeArten = anschluss.zulässigeArten,
            ),
        ),
        parameter = mapOf(
            "typ" to relationsArgumentTyp(argument),
            "typInhaltArt" to DokumentationsInhaltArt.TEXT.name,
            "rolle" to argument.rolle,
        ),
    )
}

private fun relationAusgang(
    prefix: String,
    anschluss: AnschlussDaten,
    eingangsAnzahl: Int,
): KnotenDaten {
    val id = "$prefix-ausgang"
    return KnotenDaten(
        id = KnotenId(id),
        art = KonzeptKnotenArten.AUSGANG,
        name = "Aussage",
        position = GraphPunkt(1010f, 70f + maxOf(0, eingangsAnzahl - 1) * 60f),
        größe = GraphGröße(240f, 96f),
        anschlüsse = listOf(
            AnschlussDaten(
                id = AnschlussId("$id-wert"),
                name = "wert",
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = anschluss.art,
                zulässigeArten = anschluss.zulässigeArten,
            ),
        ),
        parameter = mapOf(
            "typ" to "Aussage",
            "typInhaltArt" to DokumentationsInhaltArt.TEXT.name,
            "rolle" to "aussage",
        ),
    )
}

private fun relationsArgumentTyp(argument: RelationsArgument): String = when (argument.art) {
    RelationsArgumentArt.OBJEKT -> "Mathematisches Objekt"
    RelationsArgumentArt.ZAHL -> "Zahl"
    RelationsArgumentArt.MENGE -> "Menge"
    RelationsArgumentArt.GEOMETRIE_OBJEKT -> "Geometrisches Objekt"
    RelationsArgumentArt.PUNKT -> "Punkt"
    RelationsArgumentArt.GERADE -> "Gerade"
    RelationsArgumentArt.STRECKE -> "Strecke"
    RelationsArgumentArt.WINKEL -> "Winkel"
}
