package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikKartenAdapter.BedingterWert
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKartenAdapter.MathematikAuswerterRegister
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val PRAEDIKAT_SEITE_PARAMETER = "praedikatSeite"
const val PRAEDIKAT_SEITE_RELATIONEN = "relationen"
const val PRAEDIKAT_SEITE_AXIOME = "axiome"

object PraedikatKnotenVorlagen {
    val standard: KnotenVorlage = vorlage(RelationsOperatoren.standard())

    fun vorlage(definition: RelationsOperatorDefinition): KnotenVorlage = KnotenVorlage(
        art = RelationsOperatoren.KNOTEN_ART,
        name = "Prädikat",
        kategorie = "Aussagen: Prädikate",
        beschreibung = "Kanonischer Prädikat-Knoten für Relationen und mathematische Axiome.",
        standardGröße = GraphGröße(280f, 145f),
        anschlüsse = praedikatAnschluesse(definition),
        standardParameter = mapOf(
            RelationsOperatoren.OPERATOR_PARAMETER to definition.stabileId,
            PRAEDIKAT_SEITE_PARAMETER to PRAEDIKAT_SEITE_RELATIONEN,
        ),
    )
}

private fun aussageAusgang() = AnschlussDaten(
    name = "aussage",
    richtung = AnschlussRichtung.Ausgang,
    kante = AnschlussKante.Rechts,
    art = MathematikAnschlussArten.Aussage.id,
)

fun praedikatAnschluesse(definition: RelationsOperatorDefinition): List<AnschlussDaten> =
    definition.argumente.mapIndexed { index, argument ->
        AnschlussDaten(
            name = argument.rolle,
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = argument.art.anschlussArt(),
            reihenfolge = index,
        )
    } + aussageAusgang()

fun praedikatAnschluesse(definition: AxiomOperatorDefinition): List<AnschlussDaten> =
    definition.argumente.mapIndexed { index, argument ->
        AnschlussDaten(
            name = argument.rolle,
            richtung = AnschlussRichtung.Eingang,
            kante = AnschlussKante.Links,
            art = argument.art.anschlussArt(),
            reihenfolge = index,
        )
    } + aussageAusgang()

fun konfigurierePraedikat(
    knoten: KnotenDaten,
    definition: RelationsOperatorDefinition,
): KnotenDaten {
    require(knoten.art == RelationsOperatoren.KNOTEN_ART)
    val anschluesse = erhaltePraedikatAnschlussIds(knoten.anschlüsse, praedikatAnschluesse(definition))
    return knoten.copy(
        name = normalisierterPraedikatName(knoten),
        anschlüsse = anschluesse,
        parameter = knoten.parameter + mapOf(
            RelationsOperatoren.OPERATOR_PARAMETER to definition.stabileId,
            PRAEDIKAT_SEITE_PARAMETER to PRAEDIKAT_SEITE_RELATIONEN,
        ),
    )
}

fun konfigurierePraedikat(
    knoten: KnotenDaten,
    definition: AxiomOperatorDefinition,
): KnotenDaten {
    require(knoten.art == RelationsOperatoren.KNOTEN_ART)
    val anschluesse = erhaltePraedikatAnschlussIds(knoten.anschlüsse, praedikatAnschluesse(definition))
    return knoten.copy(
        name = normalisierterPraedikatName(knoten),
        anschlüsse = anschluesse,
        parameter = knoten.parameter + mapOf(
            RelationsOperatoren.OPERATOR_PARAMETER to definition.stabileId,
            PRAEDIKAT_SEITE_PARAMETER to PRAEDIKAT_SEITE_AXIOME,
        ),
    )
}

private fun normalisierterPraedikatName(knoten: KnotenDaten): String {
    val bekannteNamen = RelationsOperatoren.alle.mapTo(mutableSetOf()) { it.titel } +
        AxiomOperatoren.alle.map { it.titel }
    return if (knoten.name == "Prädikat" || knoten.name in bekannteNamen) "Prädikat" else knoten.name
}

internal fun MathematikAuswerterRegister.registrierePraedikatKnoten() {
    registriere(RelationsOperatoren.KNOTEN_ART) { kontext ->
        val operatorId = kontext.knoten.parameter[RelationsOperatoren.OPERATOR_PARAMETER]
        val relation = RelationsOperatoren.vonIdOderNull(operatorId)
        val axiom = AxiomOperatoren.vonIdOderNull(operatorId)
        require(relation != null || axiom != null) { "Unbekannter Prädikatoperator '$operatorId'." }

        val argumentRollen = relation?.argumente?.map { it.rolle } ?: axiom!!.argumente.map { it.rolle }
        val argumente = argumentRollen.associateWith { rolle ->
            kontext.eingänge[rolle]?.mathematischesObjekt(
                "Prädikatseingang '$rolle' für '${relation?.titel ?: axiom?.titel}'",
            ) ?: error("Für '${relation?.titel ?: axiom?.titel}' fehlt der Eingang '$rolle'.")
        }
        val aussage = relation?.werteAus(argumente) ?: axiom!!.werteAus(argumente)
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
                relation?.relationsStruktur?.kompakteKlassen()?.forEach { klasse ->
                    add("Relationsklasse: ${klasse.titel}")
                }
                (aussage as? AxiomInstanz)?.let { instanz ->
                    add("Axiom: ${instanz.titel}")
                    add("Axiomensystem: ${instanz.systeme.sorted().joinToString()}")
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
        knoten.art == RelationsOperatoren.KNOTEN_ART -> {
            val id = knoten.parameter[RelationsOperatoren.OPERATOR_PARAMETER]
            AxiomOperatoren.vonIdOderNull(id)?.stabileId ?: RelationsOperatoren.vonIdOderNull(id)?.stabileId
        }
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
        val relation = RelationsOperatoren.vonIdOderNull(operatorId)
        val axiom = AxiomOperatoren.vonIdOderNull(operatorId)
        if (relation == null && axiom == null) return@map knoten

        val erwarteteAnschlüsse = relation?.let(::praedikatAnschluesse) ?: praedikatAnschluesse(axiom!!)
        val erwarteteSeite = if (axiom != null) PRAEDIKAT_SEITE_AXIOME else PRAEDIKAT_SEITE_RELATIONEN
        if (
            knoten.art == RelationsOperatoren.KNOTEN_ART &&
            knoten.parameter[RelationsOperatoren.OPERATOR_PARAMETER] == operatorId &&
            praedikatSchnittstelleEntspricht(knoten, erwarteteAnschlüsse) &&
            knoten.parameter[PRAEDIKAT_SEITE_PARAMETER] == erwarteteSeite
        ) return@map knoten

        geändert = true
        knoten.copy(
            art = RelationsOperatoren.KNOTEN_ART,
            name = if (knoten.name in historischeRelationsStandardnamen || knoten.art != RelationsOperatoren.KNOTEN_ART) {
                "Prädikat"
            } else knoten.name,
            anschlüsse = erhaltePraedikatAnschlussIds(knoten.anschlüsse, erwarteteAnschlüsse),
            parameter = knoten.parameter + mapOf(
                RelationsOperatoren.OPERATOR_PARAMETER to operatorId,
                PRAEDIKAT_SEITE_PARAMETER to erwarteteSeite,
            ),
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
    erwartet: List<AnschlussDaten>,
): Boolean = knoten.anschlüsse.size == erwartet.size && knoten.anschlüsse.zip(erwartet).all { (ist, soll) ->
    ist.name == soll.name && ist.richtung == soll.richtung && ist.art == soll.art
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

private fun AxiomArgumentArt.anschlussArt(): AnschlussArtId = when (this) {
    AxiomArgumentArt.OBJEKT -> MathematikAnschlussArten.Objekt.id
    AxiomArgumentArt.MENGE -> MathematikAnschlussArten.Menge.id
    AxiomArgumentArt.METHODE,
    AxiomArgumentArt.PRAEDIKAT,
    -> MathematikAnschlussArten.Methode.id
}
