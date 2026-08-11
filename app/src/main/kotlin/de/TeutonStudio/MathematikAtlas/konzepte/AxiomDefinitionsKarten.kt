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
import de.TeutonStudio.MathematikKnoten.AxiomKnotenVorlagen
import de.TeutonStudio.MathematikKnoten.PRAEDIKAT_SEITE_AXIOME
import de.TeutonStudio.MathematikKnoten.PRAEDIKAT_SEITE_PARAMETER
import de.TeutonStudio.MathematikRechenSystem.kern.AxiomArgumentArt
import de.TeutonStudio.MathematikRechenSystem.kern.AxiomOperatorDefinition
import de.TeutonStudio.MathematikRechenSystem.kern.AxiomOperatoren
import de.TeutonStudio.MathematikRechenSystem.kern.RelationsOperatoren

/**
 * Liefert für jedes im kanonischen Axiomregister bekannte Axiom-Prädikat eine
 * Definitionskarte. Der Langdruck auf Knoten läuft bereits über
 * [KnotenKonzeptDialog]; diese dynamische Definition schließt dort die bisherige
 * Lücke für die gemeinsame Prädikat-Knotenart.
 *
 * Die Karte wird aus [AxiomOperatorDefinition] erzeugt. Neue Axiome benötigen
 * deshalb keine zusätzliche UI-Registrierung und können nicht versehentlich
 * ohne Definitionskarte in den Prädikatdialog gelangen.
 */
internal fun axiomKonzeptFürKnoten(knoten: KnotenDaten): KonzeptDefinition? {
    if (knoten.art != RelationsOperatoren.KNOTEN_ART) return null
    val definition = AxiomOperatoren.vonIdOderNull(
        knoten.parameter[RelationsOperatoren.OPERATOR_PARAMETER],
    ) ?: return null
    return axiomKonzept(definition)
}

internal fun axiomKonzept(definition: AxiomOperatorDefinition): KonzeptDefinition = KonzeptDefinition(
    id = KonzeptId("axiom-${definition.stabileId.removePrefix("axiom.").replace('.', '-') }"),
    name = definition.titel,
    beschreibung = buildString {
        append("Definitionskarte des Axiom-Prädikats ")
        append(definition.titel)
        append(". Sie zeigt die formale Bedingung, alle benötigten Argumente und die resultierende Aussage.")
    },
    pfad = axiomPfad(definition),
    tags = definition.suchbegriffe + definition.systeme + setOf(
        "Axiom",
        "Prädikat",
        definition.titel,
        definition.kategorie,
        definition.stabileId,
    ),
    knotenArten = setOf(RelationsOperatoren.KNOTEN_ART),
    knotenParameter = mapOf(
        RelationsOperatoren.OPERATOR_PARAMETER to definition.stabileId,
        PRAEDIKAT_SEITE_PARAMETER to PRAEDIKAT_SEITE_AXIOME,
    ),
    reiter = listOf(
        KonzeptReiter(
            id = "definition",
            titel = "Definition: ${definition.symbolLatex}",
            rolle = KonzeptReiterRolle.Definition,
            karte = axiomDefinitionsKarte(definition),
        ),
    ),
)

private fun axiomDefinitionsKarte(definition: AxiomOperatorDefinition): KartenDaten {
    val prefix = "definition-${definition.stabileId.replace('.', '-')}"
    val schnittstelle = AxiomKnotenVorlagen.vorlage(definition).anschlüsse
    val eingänge = schnittstelle
        .filter { it.richtung == AnschlussRichtung.Eingang }
        .sortedBy { it.reihenfolge }
    val ausgänge = schnittstelle
        .filter { it.richtung == AnschlussRichtung.Ausgang }
        .sortedBy { it.reihenfolge }
    val regelAnschlüsse = schnittstelle.mapIndexed { index, anschluss ->
        anschluss.copy(id = AnschlussId("$prefix-regel-$index"))
    }
    val regel = KnotenDaten(
        id = KnotenId("$prefix-regel"),
        art = KonzeptKnotenArten.REGEL,
        name = definition.titel,
        position = GraphPunkt(390f, 55f),
        größe = GraphGröße(
            520f,
            maxOf(190f, 125f + 38f * maxOf(eingänge.size, ausgänge.size)),
        ),
        anschlüsse = regelAnschlüsse,
        parameter = mapOf(
            "regel" to definition.symbolLatex,
            "regelInhaltArt" to DokumentationsInhaltArt.DISPLAY_LATEX.name,
            "definition" to definition.symbolLatex,
            "axiomId" to definition.stabileId,
            "systeme" to definition.systeme.sorted().joinToString(),
            "knotenArt" to buildString {
                append("Axiom-Prädikat")
                if (definition.systeme.isNotEmpty()) {
                    append(" · ")
                    append(definition.systeme.sorted().joinToString())
                }
            },
        ),
    )
    val eingangsKnoten = eingänge.mapIndexed { index, anschluss ->
        axiomSchnittstelle(
            prefix = prefix,
            anschluss = anschluss,
            index = index,
            eingang = true,
            typ = axiomArgumentTyp(definition, index, anschluss),
        )
    }
    val ausgangsKnoten = ausgänge.mapIndexed { index, anschluss ->
        axiomSchnittstelle(
            prefix = prefix,
            anschluss = anschluss,
            index = index,
            eingang = false,
            typ = "Aussage",
        )
    }
    val regelEingänge = regel.anschlüsse
        .filter { it.richtung == AnschlussRichtung.Eingang }
        .sortedBy { it.reihenfolge }
    val regelAusgänge = regel.anschlüsse
        .filter { it.richtung == AnschlussRichtung.Ausgang }
        .sortedBy { it.reihenfolge }
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
        ausgangsKnoten.forEachIndexed { index, ziel ->
            add(
                VerbindungDaten(
                    id = VerbindungsId("$prefix-a-$index"),
                    von = AnschlussVerweis(regel.id, regelAusgänge[index].id),
                    zu = AnschlussVerweis(ziel.id, ziel.anschlüsse.single().id),
                ),
            )
        }
    }
    return KartenDaten(
        id = KartenId(prefix),
        name = "Definition: ${definition.titel}",
        knoten = eingangsKnoten + regel + ausgangsKnoten,
        verbindungen = verbindungen,
    )
}

private fun axiomSchnittstelle(
    prefix: String,
    anschluss: AnschlussDaten,
    index: Int,
    eingang: Boolean,
    typ: String,
): KnotenDaten {
    val id = "$prefix-${if (eingang) "eingang" else "ausgang"}-$index"
    return KnotenDaten(
        id = KnotenId(id),
        art = if (eingang) KonzeptKnotenArten.EINGANG else KonzeptKnotenArten.AUSGANG,
        name = anschluss.name,
        position = GraphPunkt(
            if (eingang) 35f else 1000f,
            55f + index * 125f,
        ),
        größe = GraphGröße(270f, 96f),
        anschlüsse = listOf(
            AnschlussDaten(
                id = AnschlussId("$id-wert"),
                name = "wert",
                richtung = if (eingang) AnschlussRichtung.Ausgang else AnschlussRichtung.Eingang,
                kante = if (eingang) AnschlussKante.Rechts else AnschlussKante.Links,
                art = anschluss.art,
                zulässigeArten = anschluss.zulässigeArten,
            ),
        ),
        parameter = mapOf(
            "typ" to typ,
            "rolle" to anschluss.name,
        ),
    )
}

private fun axiomArgumentTyp(
    definition: AxiomOperatorDefinition,
    index: Int,
    anschluss: AnschlussDaten,
): String {
    val argument = definition.argumente.getOrNull(index) ?: return anschluss.art.wert
    val basis = when (argument.art) {
        AxiomArgumentArt.OBJEKT -> "Objekt"
        AxiomArgumentArt.MENGE -> "Menge"
        AxiomArgumentArt.METHODE -> "Methode"
        AxiomArgumentArt.PRAEDIKAT -> "Prädikat"
    }
    return argument.stelligkeit?.let { "$basis · $it-stellig" } ?: basis
}

private fun axiomPfad(definition: AxiomOperatorDefinition): List<String> = when {
    definition.stabileId.startsWith("axiom.relation.") -> listOf("Logik", "Prädikate", "Relationsaxiome")
    definition.stabileId.startsWith("axiom.peano.") -> listOf("Arithmetik", "Natürliche Zahlen", "Peano-Axiome")
    definition.stabileId.startsWith("axiom.zf.") || definition.stabileId.startsWith("axiom.zfc.") ->
        listOf("Mengenlehre", "Axiome")
    definition.stabileId.startsWith("axiom.algebra.") -> listOf("Algebra", "Strukturen", "Axiome")
    else -> listOf("Logik", "Prädikate", "Axiome")
}
