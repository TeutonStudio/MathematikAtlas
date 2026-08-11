package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.*

object PraedikatKnotenVorlagen {
    val standard: KnotenVorlage = vorlage(RelationsOperatoren.standard())

    fun vorlage(definition: RelationsOperatorDefinition): KnotenVorlage = KnotenVorlage(
        art = RelationsOperatoren.KNOTEN_ART,
        name = "Prädikat",
        kategorie = "Aussagen: Relationen",
        beschreibung = "Kanonischer Relations-OperatorKnoten; die konkrete Relation wird im Inspector gewählt.",
        standardGröße = GraphGröße(280f, 145f),
        anschlüsse = praedikatAnschluesse(definition),
        standardParameter = mapOf(RelationsOperatoren.OPERATOR_PARAMETER to definition.stabileId),
    )
}

fun praedikatAnschluesse(definition: RelationsOperatorDefinition): List<AnschlussDaten> =
    definition.argumente.mapIndexed { index, argument ->
        AnschlussDaten(
            name = argument.rolle,
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = argument.art.anschlussArt(),
            reihenfolge = index,
        )
    } + AnschlussDaten(
        name = "aussage",
        richtung = AnschlussRichtung.Ausgang,
        kante = AnschlussKante.Rechts,
        art = MathematikAnschlussArten.Aussage.id,
    )

fun konfigurierePraedikat(
    knoten: KnotenDaten,
    definition: RelationsOperatorDefinition,
): KnotenDaten {
    require(knoten.art == RelationsOperatoren.KNOTEN_ART)
    val anschluesse = erhaltePraedikatAnschlussIds(knoten.anschlüsse, praedikatAnschluesse(definition))
    return knoten.copy(
        name = if (knoten.name == "Prädikat" || RelationsOperatoren.alle.any { it.titel == knoten.name }) {
            "Prädikat"
        } else knoten.name,
        anschlüsse = anschluesse,
        parameter = knoten.parameter + (RelationsOperatoren.OPERATOR_PARAMETER to definition.stabileId),
    )
}

internal fun MathematikAuswerterRegister.registrierePraedikatKnoten() {
    registriere(RelationsOperatoren.KNOTEN_ART) { kontext ->
        val definition = RelationsOperatoren.vonIdOderNull(
            kontext.knoten.parameter[RelationsOperatoren.OPERATOR_PARAMETER],
        ) ?: error("Unbekannter Relationsoperator.")
        val argumente = definition.argumente.associate { argument ->
            val wert = kontext.eingänge[argument.rolle]
                ?: error("Für '${definition.titel}' fehlt der Eingang '${argument.rolle}'.")
            argument.rolle to wert.objekt
        }
        val aussage = definition.werteAus(argumente)
        val annahmen = kontext.eingänge.values.flatMap { it.annahmen }.toSet()
        KnotenAuswertungsErgebnis(
            ausgaben = mapOf(
                "aussage" to BedingterWert(
                    objekt = aussage,
                    annahmen = annahmen,
                    latexDarstellung = aussage.zuLatex(),
                ),
            ),
            warnungen = buildList {
                definition.relationsStruktur?.kompakteKlassen()?.forEach { klasse ->
                    add("Relationsklasse: ${klasse.titel}")
                }
            },
            eingänge = kontext.eingänge,
        )
    }
}

object PraedikatKnotenMigration {
    private val direkteLegacyArten = mapOf(
        "mathematik.gleichheit" to "relation.gleichheit",
        "mathematik.ungleichheit" to "relation.ungleichheit",
        "mathematik.kleiner" to "relation.kleiner",
        "mathematik.größer" to "relation.groesser",
        "mathematik.groesser" to "relation.groesser",
        "mathematik.kleinerGleich" to "relation.kleinerGleich",
        "mathematik.größerGleich" to "relation.groesserGleich",
        "mathematik.groesserGleich" to "relation.groesserGleich",
        "mathematik.geometrie.inzidenz" to "geometrie.relation.inzidenz",
        "mathematik.geometrie.zwischen" to "geometrie.relation.zwischenlage",
        "mathematik.geometrie.kollinear" to "geometrie.relation.kollinear",
        "mathematik.geometrie.parallel" to "geometrie.relation.parallel",
        "mathematik.geometrie.orthogonal" to "geometrie.relation.orthogonal",
        "mathematik.geometrie.gleichheit" to "geometrie.relation.gleichheit",
        "mathematik.geometrie.streckenKongruenz" to "geometrie.relation.streckenkongruenz",
        "mathematik.geometrie.winkelKongruenz" to "geometrie.relation.winkelkongruenz",
    )

    val alteKnotenArten: Set<String> = direkteLegacyArten.keys +
        MengenRelationsMigration.alteKnotenArten.keys +
        MengenRelationRechner.KNOTEN_ART

    fun operatorId(knoten: KnotenDaten): String? = when {
        knoten.art == RelationsOperatoren.KNOTEN_ART ->
            RelationsOperatoren.vonIdOderNull(knoten.parameter[RelationsOperatoren.OPERATOR_PARAMETER])?.stabileId
        knoten.art == MengenRelationRechner.KNOTEN_ART ->
            MengenRelationsOperator.vonIdOderNull(knoten.parameter[MENGENRELATION_OPERATOR_PARAMETER])?.stabileId
        knoten.art in MengenRelationsMigration.alteKnotenArten ->
            MengenRelationsMigration.alteKnotenArten[knoten.art]?.stabileId
        else -> direkteLegacyArten[knoten.art]
    }
}

fun KartenDaten.migrierePraedikatKnoten(): KartenDaten {
    var geändert = false
    val neueKnoten = knoten.map { knoten ->
        val operatorId = PraedikatKnotenMigration.operatorId(knoten) ?: return@map knoten
        val definition = RelationsOperatoren.vonIdOderNull(operatorId) ?: return@map knoten
        if (
            knoten.art == RelationsOperatoren.KNOTEN_ART &&
            knoten.parameter[RelationsOperatoren.OPERATOR_PARAMETER] == definition.stabileId &&
            praedikatSchnittstelleEntspricht(knoten, definition)
        ) return@map knoten

        geändert = true
        val gewünschteAnschlüsse = praedikatAnschluesse(definition)
        knoten.copy(
            art = RelationsOperatoren.KNOTEN_ART,
            name = if (knoten.name in historischeRelationsStandardnamen || knoten.art != RelationsOperatoren.KNOTEN_ART) {
                "Prädikat"
            } else knoten.name,
            anschlüsse = erhaltePraedikatAnschlussIds(knoten.anschlüsse, gewünschteAnschlüsse),
            parameter = knoten.parameter + (RelationsOperatoren.OPERATOR_PARAMETER to definition.stabileId),
        )
    }
    return if (geändert) copy(knoten = neueKnoten) else this
}

private val historischeRelationsStandardnamen = setOf(
    "Gleichheit",
    "Ungleichheit",
    "Kleiner",
    "Größer",
    "Kleiner oder gleich",
    "Größer oder gleich",
    "Inzidenz",
    "Zwischenlage",
    "Kollinear",
    "Parallel",
    "Orthogonal",
    "Geometrische Gleichheit",
    "Streckenkongruenz",
    "Winkelkongruenz",
    "Mengenrelation",
    "Prädikat",
) + MengenRelationsOperator.entries.map { it.titel }

private fun praedikatSchnittstelleEntspricht(
    knoten: KnotenDaten,
    definition: RelationsOperatorDefinition,
): Boolean {
    val erwartet = praedikatAnschluesse(definition)
    return knoten.anschlüsse.size == erwartet.size && knoten.anschlüsse.zip(erwartet).all { (ist, soll) ->
        ist.name == soll.name && ist.richtung == soll.richtung && ist.art == soll.art
    }
}

private fun erhaltePraedikatAnschlussIds(
    bisher: List<AnschlussDaten>,
    gewünscht: List<AnschlussDaten>,
): List<AnschlussDaten> {
    val verbraucht = mutableSetOf<AnschlussId>()
    return gewünscht.map { soll ->
        val kandidat = bisher.firstOrNull {
            it.id !in verbraucht && it.richtung == soll.richtung && it.name == soll.name
        } ?: bisher.firstOrNull {
            it.id !in verbraucht &&
                it.richtung == soll.richtung &&
                it.reihenfolge == soll.reihenfolge
        }
        if (kandidat == null) soll else {
            verbraucht += kandidat.id
            soll.copy(id = kandidat.id)
        }
    }
}

private fun RelationsArgumentArt.anschlussArt(): AnschlussArtId = when (this) {
    RelationsArgumentArt.OBJEKT -> MathematikAnschlussArten.Objekt.id
    RelationsArgumentArt.ZAHL -> MathematikAnschlussArten.Zahl.id
    RelationsArgumentArt.MENGE -> MathematikAnschlussArten.Menge.id
    RelationsArgumentArt.GEOMETRIE_OBJEKT -> GeometrieAnschlussArten.Objekt.id
    RelationsArgumentArt.PUNKT -> GeometrieAnschlussArten.Punkt.id
    RelationsArgumentArt.GERADE -> GeometrieAnschlussArten.Gerade.id
    RelationsArgumentArt.STRECKE -> GeometrieAnschlussArten.Strecke.id
    RelationsArgumentArt.WINKEL -> GeometrieAnschlussArten.Winkel.id
}
