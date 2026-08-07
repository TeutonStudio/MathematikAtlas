package de.TeutonStudio.MathematikKnoten

import de.TeutonStudio.KnotenKartenVerwalter.daten.*
import de.TeutonStudio.MathematikRechenSystem.kern.*

const val TENSOR_OPERATION_ID = "operationId"
const val TENSOR_SIGNATUR_FAMILIE = "signaturFamilie"
const val TENSOR_ACHSEN_EINGABE_MODUS = "achsenEingabeModus"
const val TENSOR_ACHSEN_SPEZIFIKATION = "achsenSpezifikation"
const val TENSOR_ACHSEN_IDS = "achsenIds"
const val TENSOR_ACHSEN_MIGRATIONSFEHLER = "achsenMigrationsFehler"
const val TENSOR_DYNAMISCHE_ACHSEN_ANZAHL = "dynamischeAchsenAnzahl"
const val TENSOR_OPERATION_PARAMETER = "operationParameter"

private const val LEGACY_TENSOR_OPERATOR = "operator"

data class TensorSignaturWechselDiagnose(
    val neueOperation: TensorOperationId,
    val erhalteneAnschlussIds: Set<AnschlussId>,
    val entfernteAnschlussIds: Set<AnschlussId>,
    val verbundeneEntfernteAnschlussIds: Set<AnschlussId>,
) {
    val trenntVerbindungen: Boolean get() = verbundeneEntfernteAnschlussIds.isNotEmpty()
}

fun TensorOperationDefinition.benoetigtAchsenEingabe(): Boolean =
    minimaleAchsenAnzahl > 0 && id.wert != TensorRechnerOperator.INDEXAUSWERTUNG.stabileId

fun aktuelleTensorOperationDefinition(knoten: KnotenDaten): TensorOperationDefinition {
    val operationId = knoten.parameter[TENSOR_OPERATION_ID]
        ?: knoten.parameter[LEGACY_TENSOR_OPERATOR]
        ?: TensorRechnerOperator.TENSORPRODUKT.stabileId
    return StandardTensorOperationen.registry.definition(operationId)
        ?: StandardTensorOperationen.registry.definition(
            TensorRechnerOperator.TENSORPRODUKT.alsTensorOperationId(),
        )!!
}

fun aktuellerAchsenEingabeModus(knoten: KnotenDaten): AchsenEingabeModus = runCatching {
    AchsenEingabeModus.valueOf(
        knoten.parameter[TENSOR_ACHSEN_EINGABE_MODUS]
            ?: AchsenEingabeModus.TUPEL.name,
    )
}.getOrDefault(AchsenEingabeModus.TUPEL)

fun konfiguriereTensorOperation(
    knoten: KnotenDaten,
    definition: TensorOperationDefinition,
    achsenModus: AchsenEingabeModus = aktuellerAchsenEingabeModus(knoten),
    dynamischeAchsenAnzahl: Int = definition.minimaleAchsenAnzahl.coerceAtLeast(1),
): KnotenDaten {
    require(knoten.art == TensorRechner.KNOTEN_ART)
    val achsenAnzahl = if (definition.benoetigtAchsenEingabe()) {
        dynamischeAchsenAnzahl.coerceAtLeast(definition.minimaleAchsenAnzahl)
    } else {
        vorhandeneAchsenIds(knoten).size
    }
    val gewuenscht = tensorOperationAnschluesse(definition, achsenModus, achsenAnzahl)
    val bestehend = knoten.anschlüsse.groupBy { it.richtung to it.name }
    val anschluesse = gewuenscht.map { neu ->
        val kandidaten = anschlussAliasNamen(neu.name, definition).flatMap { alias ->
            bestehend[neu.richtung to alias].orEmpty()
        }
        val alt = kandidaten.firstOrNull { es ->
            es.art == neu.art ||
                es.art == MathematikAnschlussArten.Objekt.id ||
                neu.art == MathematikAnschlussArten.Objekt.id
        }
        if (alt == null) neu else neu.copy(id = alt.id)
    }
    val achsenIds = stabileAchsenIds(knoten, achsenAnzahl)
    return knoten.copy(
        name = definition.titel,
        anschlüsse = anschluesse,
        parameter = knoten.parameter - LEGACY_TENSOR_OPERATOR - TENSOR_ACHSEN_MIGRATIONSFEHLER + mapOf(
            TENSOR_OPERATION_ID to definition.id.wert,
            TENSOR_SIGNATUR_FAMILIE to definition.familie.name,
            TENSOR_ACHSEN_EINGABE_MODUS to achsenModus.name,
            TENSOR_ACHSEN_SPEZIFIKATION to (knoten.parameter[TENSOR_ACHSEN_SPEZIFIKATION] ?: ""),
            TENSOR_ACHSEN_IDS to achsenIds.joinToString(",") { it.wert },
            TENSOR_DYNAMISCHE_ACHSEN_ANZAHL to achsenAnzahl.toString(),
            TENSOR_OPERATION_PARAMETER to (knoten.parameter[TENSOR_OPERATION_PARAMETER] ?: ""),
        ),
    )
}

fun diagnostiziereTensorSignaturWechsel(
    knoten: KnotenDaten,
    definition: TensorOperationDefinition,
    achsenModus: AchsenEingabeModus,
    dynamischeAchsenAnzahl: Int,
    verbundeneAnschlussIds: Set<AnschlussId>,
): TensorSignaturWechselDiagnose {
    val probe = konfiguriereTensorOperation(knoten, definition, achsenModus, dynamischeAchsenAnzahl)
    val alteIds = knoten.anschlüsse.mapTo(linkedSetOf()) { it.id }
    val neueIds = probe.anschlüsse.mapTo(linkedSetOf()) { it.id }
    val entfernt = alteIds - neueIds
    return TensorSignaturWechselDiagnose(
        neueOperation = definition.id,
        erhalteneAnschlussIds = alteIds.intersect(neueIds),
        entfernteAnschlussIds = entfernt,
        verbundeneEntfernteAnschlussIds = entfernt.intersect(verbundeneAnschlussIds),
    )
}

fun KartenDaten.migriereTensorOperationKnoten(): KartenDaten = copy(
    knoten = knoten.map { knoten ->
        when (knoten.art) {
            TensorRechner.KNOTEN_ART -> migriereBestehendenTensorRechner(knoten)
            TENSORPRODUKT_ART -> knoten.copy(
                art = TensorRechner.KNOTEN_ART,
                name = "Tensorprodukt",
                parameter = knoten.parameter + mapOf(
                    TENSOR_OPERATION_ID to TensorRechnerOperator.TENSORPRODUKT.stabileId,
                    TENSOR_SIGNATUR_FAMILIE to TensorSignaturFamilie.BINAER.name,
                    TENSOR_ACHSEN_EINGABE_MODUS to AchsenEingabeModus.TUPEL.name,
                    TENSOR_ACHSEN_SPEZIFIKATION to "",
                    TENSOR_ACHSEN_IDS to "",
                    TENSOR_DYNAMISCHE_ACHSEN_ANZAHL to "0",
                    TENSOR_OPERATION_PARAMETER to "",
                ),
            )
            else -> knoten
        }
    },
)

private fun migriereBestehendenTensorRechner(knoten: KnotenDaten): KnotenDaten {
    val definition = aktuelleTensorOperationDefinition(knoten)
    val (achsenSpezifikation, migrationsFehler) = migriereLegacyAchsen(knoten, definition)
    val operationParameter = knoten.parameter[TENSOR_OPERATION_PARAMETER]
        ?: when (definition.id.wert) {
            TensorRechnerOperator.ACHSENSCHNITT.stabileId,
            TensorRechnerOperator.INDEXAUSWERTUNG.stabileId,
            -> knoten.parameter["indizes"].orEmpty()
            else -> ""
        }
    val achsenAnzahl = if (definition.benoetigtAchsenEingabe()) {
        knoten.parameter[TENSOR_DYNAMISCHE_ACHSEN_ANZAHL]
            ?.toIntOrNull()
            ?.coerceAtLeast(definition.minimaleAchsenAnzahl)
            ?: definition.minimaleAchsenAnzahl
    } else {
        vorhandeneAchsenIds(knoten).size
    }
    val neueParameter = knoten.parameter - LEGACY_TENSOR_OPERATOR + mapOf(
        TENSOR_OPERATION_ID to definition.id.wert,
        TENSOR_SIGNATUR_FAMILIE to definition.familie.name,
        TENSOR_ACHSEN_EINGABE_MODUS to aktuellerAchsenEingabeModus(knoten).name,
        TENSOR_ACHSEN_SPEZIFIKATION to achsenSpezifikation,
        TENSOR_ACHSEN_IDS to stabileAchsenIds(knoten, achsenAnzahl).joinToString(",") { it.wert },
        TENSOR_DYNAMISCHE_ACHSEN_ANZAHL to achsenAnzahl.toString(),
        TENSOR_OPERATION_PARAMETER to operationParameter,
    )
    return knoten.copy(
        parameter = if (migrationsFehler == null) {
            neueParameter - TENSOR_ACHSEN_MIGRATIONSFEHLER
        } else {
            neueParameter + (TENSOR_ACHSEN_MIGRATIONSFEHLER to migrationsFehler)
        },
    )
}

private fun migriereLegacyAchsen(
    knoten: KnotenDaten,
    definition: TensorOperationDefinition,
): Pair<String, String?> {
    knoten.parameter[TENSOR_ACHSEN_SPEZIFIKATION]?.let { return it to null }
    val alt = when (definition.id.wert) {
        TensorRechnerOperator.KONTRAKTION.stabileId,
        TensorRechnerOperator.ACHSENSCHNITT.stabileId,
        -> knoten.parameter["achsen"]
        TensorRechnerOperator.ACHSENPERMUTATION.stabileId,
        TensorRechnerOperator.TRANSPONIEREN.stabileId,
        -> knoten.parameter["permutation"]
        else -> null
    }.orEmpty().trim()
    if (alt.isEmpty()) return "" to null

    val werte = alt.split(',').map(String::trim)
    val zahlen = werte.map { it.toIntOrNull() }
    if (zahlen.any { it == null }) {
        return alt to "Die historische Achsenangabe '$alt' enthält keine reine Ganzzahlliste."
    }
    val konkret = zahlen.filterNotNull()
    if (konkret.any { it < 0 }) {
        return alt to "Historische negative Achsenindizes sind mehrdeutig und müssen im Inspector bestätigt werden."
    }
    return konkret.joinToString(",") { (it + 1).toString() } to null
}

private fun vorhandeneAchsenIds(knoten: KnotenDaten): List<TensorAchsenId> =
    knoten.parameter[TENSOR_ACHSEN_IDS]
        .orEmpty()
        .split(',')
        .map(String::trim)
        .filter(String::isNotEmpty)
        .map(::TensorAchsenId)

private fun stabileAchsenIds(knoten: KnotenDaten, anzahl: Int): List<TensorAchsenId> {
    val vorhanden = vorhandeneAchsenIds(knoten)
    return List(anzahl) { index ->
        vorhanden.getOrNull(index) ?: TensorAchsenId("achse-${index + 1}")
    }
}

private fun tensorOperationAnschluesse(
    definition: TensorOperationDefinition,
    achsenModus: AchsenEingabeModus,
    dynamischeAchsenAnzahl: Int,
): List<AnschlussDaten> = buildList {
    definition.eingangsRollen.forEachIndexed { index, rolle ->
        add(
            AnschlussDaten(
                name = rolle.wert,
                richtung = AnschlussRichtung.Eingang,
                kante = AnschlussKante.Links,
                art = eingangsArt(rolle),
                reihenfolge = index,
            ),
        )
    }
    if (definition.benoetigtAchsenEingabe()) {
        when (achsenModus) {
            AchsenEingabeModus.TUPEL -> add(
                AnschlussDaten(
                    name = "achsen",
                    richtung = AnschlussRichtung.Eingang,
                    kante = AnschlussKante.Links,
                    art = MathematikAnschlussArten.Tupel.id,
                    reihenfolge = size,
                ),
            )
            AchsenEingabeModus.DYNAMISCHE_EINZELHANDLES -> {
                val anzahl = dynamischeAchsenAnzahl.coerceAtLeast(definition.minimaleAchsenAnzahl)
                repeat(anzahl) { index ->
                    add(
                        AnschlussDaten(
                            name = "achse.${index + 1}",
                            richtung = AnschlussRichtung.Eingang,
                            kante = AnschlussKante.Links,
                            art = MathematikAnschlussArten.Zahl.id,
                            reihenfolge = size,
                            dynamischErzeugt = index >= definition.minimaleAchsenAnzahl,
                            kannSichErweitern = definition.maximaleAchsenAnzahl == null,
                        ),
                    )
                }
            }
        }
    }
    definition.ausgangsRollen.forEachIndexed { index, rolle ->
        add(
            AnschlussDaten(
                name = rolle.wert,
                richtung = AnschlussRichtung.Ausgang,
                kante = AnschlussKante.Rechts,
                art = ausgangsArt(rolle),
                reihenfolge = index,
            ),
        )
    }
}

private fun eingangsArt(rolle: TensorHandleRolle): AnschlussArtId = when (rolle.wert) {
    "skalar" -> MathematikAnschlussArten.Zahl.id
    else -> MathematikAnschlussArten.Objekt.id
}

private fun ausgangsArt(rolle: TensorHandleRolle): AnschlussArtId = when (rolle.wert) {
    "wert", "rang" -> MathematikAnschlussArten.Zahl.id
    else -> MathematikAnschlussArten.Objekt.id
}

private fun anschlussAliasNamen(
    name: String,
    definition: TensorOperationDefinition,
): List<String> = when {
    name == "ergebnis" -> listOf("ergebnis", "wert")
    name == "skalar" && definition.id.wert == TensorRechnerOperator.SKALARMULTIPLIKATION.stabileId ->
        listOf("skalar", "links")
    name == "tensor" && definition.id.wert == TensorRechnerOperator.SKALARMULTIPLIKATION.stabileId ->
        listOf("tensor", "rechts")
    name == "tensor" -> listOf("tensor", "links")
    else -> listOf(name)
}
