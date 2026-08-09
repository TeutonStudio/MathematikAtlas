package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.AnschlussRichtung
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.TENSOR_ACHSEN_IDS
import de.TeutonStudio.MathematikKnoten.TENSOR_ACHSEN_MIGRATIONSFEHLER
import de.TeutonStudio.MathematikKnoten.TENSOR_ACHSEN_SPEZIFIKATION
import de.TeutonStudio.MathematikKnoten.TENSOR_OPERATION_PARAMETER
import de.TeutonStudio.MathematikKnoten.aktuelleTensorOperationDefinition
import de.TeutonStudio.MathematikKnoten.aktuellerAchsenEingabeModus
import de.TeutonStudio.MathematikKnoten.benoetigtAchsenEingabe
import de.TeutonStudio.MathematikKnoten.konfiguriereTensorOperation
import de.TeutonStudio.MathematikRechenSystem.kern.AchsenEingabeModus
import de.TeutonStudio.MathematikRechenSystem.kern.StandardTensorOperationen
import de.TeutonStudio.MathematikRechenSystem.kern.TensorOperationDefinition

internal object TensorOperationRechnerInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        val definition = aktuelleTensorOperationDefinition(knoten)
        val operatorId = knoten.parameter[de.TeutonStudio.MathematikKnoten.RECHNER_OPERATOR_PARAMETER]
            ?: knoten.parameter[de.TeutonStudio.MathematikKnoten.TENSOR_OPERATION_ID]
            ?: definition.id.wert
        var operatorDialog by remember(knoten.id, definition.id) { mutableStateOf(false) }
        var ausstehenderKnoten by remember(knoten.id) { mutableStateOf<KnotenDaten?>(null) }
        val operatorEinträge = remember(knoten) {
            buildList {
                if (StandardTensorOperationen.registry.definition(operatorId) == null) {
                    add(
                        RechnerOperatorAuswahlEintrag(
                            id = operatorId,
                            titel = "Unbekannter gespeicherter Operator",
                            symbolLatex = "?",
                            kategorie = "Nicht verfügbar",
                            beschreibung = "Die gespeicherte Operator-ID $operatorId ist nicht registriert.",
                            art = RechnerOperatorAuswahlArt.UNBEKANNT,
                        ),
                    )
                }
                StandardTensorOperationen.registry.alle().forEach { auswählbar ->
                    add(RechnerOperatorAuswahlEintrag(
                    id = auswählbar.id.wert,
                    titel = auswählbar.titel,
                    symbolLatex = "\\operatorname{${auswählbar.titel}}",
                    kategorie = tensorOperatorKategorie(auswählbar),
                    beschreibung = buildString {
                        append("Tensoroperation der Signaturfamilie ${auswählbar.familie.name.lowercase()}.")
                        if (auswählbar.parameter.isNotEmpty()) {
                            append(" Zusatzparameter: ")
                            append(auswählbar.parameter.joinToString { it.id })
                            append('.')
                        }
                    },
                    suchbegriffe = buildSet {
                        add(auswählbar.familie.name)
                        add(auswählbar.unterstuetzungsStatus.name)
                        addAll(auswählbar.eingangsRollen.map { it.wert })
                        addAll(auswählbar.ausgangsRollen.map { it.wert })
                        addAll(auswählbar.parameter.map { it.id })
                    },
                    status = tensorStatusTitel(auswählbar),
                    kandidat = konfiguriereTensorOperation(
                        knoten = knoten,
                        definition = auswählbar,
                        achsenModus = aktuellerAchsenEingabeModus(knoten),
                        dynamischeAchsenAnzahl = dynamischeAchsenAnzahl(knoten, auswählbar),
                    ),
                    ))
                }
            }
        }

        fun übernehmeAchsenÄnderung(neu: KnotenDaten) {
            if (aktionen.vorschauKnotenErsetzen(neu).trenntVerbindungen) {
                ausstehenderKnoten = neu
            } else {
                aktionen.knoten(neu)
            }
        }

        Text("Tensoroperation", style = MaterialTheme.typography.titleSmall)
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { operatorDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(definition.titel, modifier = Modifier.weight(1f))
                Text(definition.familie.name, style = MaterialTheme.typography.labelSmall)
            }
        }

        VertragsKarte(definition, knoten)

        if (definition.benoetigtAchsenEingabe()) {
            AchsenKonfiguration(
                knoten = knoten,
                definition = definition,
                aktionen = aktionen,
                knotenAendern = ::übernehmeAchsenÄnderung,
            )
        }
        if (definition.parameter.isNotEmpty()) {
            OutlinedTextField(
                value = knoten.parameter[TENSOR_OPERATION_PARAMETER].orEmpty(),
                onValueChange = { aktionen.parameter(TENSOR_OPERATION_PARAMETER, it) },
                label = { Text(definition.parameter.joinToString { it.id }) },
                supportingText = {
                    Text("Operationsparameter, kommagetrennt. Achsen werden getrennt und sichtbar einsbasiert gespeichert.")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        ergebnis?.warnungen.orEmpty().forEach { warnung ->
            Text(
                warnung,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ergebnis?.fehler?.let { fehler ->
            Text(fehler, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        if (operatorDialog) {
            RechnerOperatorAuswahlDialog(
                familienTitel = "Tensorrechner",
                einträge = operatorEinträge,
                aktuelleId = operatorId,
                auswirkungFür = { eintrag ->
                    eintrag.kandidat?.let(aktionen::vorschauKnotenErsetzen)
                },
                schließen = { operatorDialog = false },
                operatorÜbernehmen = { eintrag ->
                    eintrag.kandidat?.let(aktionen::knoten)
                    operatorDialog = false
                },
                formelÖffnen = {},
            )
        }

        ausstehenderKnoten?.let { ziel ->
            val auswirkung = aktionen.vorschauKnotenErsetzen(ziel)
            AlertDialog(
                onDismissRequest = { ausstehenderKnoten = null },
                title = { Text("Achseneingabe wirklich wechseln?") },
                text = {
                    Text(
                        "${auswirkung.entfallendeVerbindungen.size} bestehende Verbindung(en) an " +
                            "${auswirkung.entfallendeAnschlüsse.joinToString { it.name }} werden getrennt.",
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            aktionen.knoten(ziel)
                            ausstehenderKnoten = null
                        },
                    ) { Text("Wechseln und Verbindungen trennen") }
                },
                dismissButton = {
                    TextButton(onClick = { ausstehenderKnoten = null }) { Text("Abbrechen") }
                },
            )
        }
    }
}

private fun tensorOperatorKategorie(definition: TensorOperationDefinition): String = when (definition.familie) {
    de.TeutonStudio.MathematikRechenSystem.kern.TensorSignaturFamilie.BINAER -> "Binäre Operationen"
    de.TeutonStudio.MathematikRechenSystem.kern.TensorSignaturFamilie.UNAER -> "Unäre Operationen"
    de.TeutonStudio.MathematikRechenSystem.kern.TensorSignaturFamilie.ACHSENABHAENGIG -> "Achsenoperationen"
    de.TeutonStudio.MathematikRechenSystem.kern.TensorSignaturFamilie.INDEXIERUNG -> "Indexierung"
    de.TeutonStudio.MathematikRechenSystem.kern.TensorSignaturFamilie.KONSTRUKTION -> "Konstruktion"
    de.TeutonStudio.MathematikRechenSystem.kern.TensorSignaturFamilie.ZERLEGUNG,
    de.TeutonStudio.MathematikRechenSystem.kern.TensorSignaturFamilie.MEHRFACHAUSGANG,
    -> "Zerlegungen"
    de.TeutonStudio.MathematikRechenSystem.kern.TensorSignaturFamilie.VARIADISCH -> "Variadische Operationen"
}

private fun tensorStatusTitel(definition: TensorOperationDefinition): String = when (definition.unterstuetzungsStatus) {
    de.TeutonStudio.MathematikRechenSystem.kern.TensorUnterstuetzungsStatus.KONKRET_IMPLEMENTIERT -> "Konkret implementiert"
    de.TeutonStudio.MathematikRechenSystem.kern.TensorUnterstuetzungsStatus.SYMBOLISCH_IMPLEMENTIERT -> "Symbolisch implementiert"
    de.TeutonStudio.MathematikRechenSystem.kern.TensorUnterstuetzungsStatus.REGISTRIERT -> "Registriert"
}

@Composable
private fun VertragsKarte(
    definition: TensorOperationDefinition,
    knoten: KnotenDaten,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text("Signaturvertrag", style = MaterialTheme.typography.labelLarge)
            Text("Familie: ${definition.familie.name}", style = MaterialTheme.typography.bodySmall)
            Text(
                "Eingänge: ${definition.eingangsRollen.joinToString { it.wert }}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "Ausgänge: ${definition.ausgangsRollen.joinToString { it.wert }}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "Unterstützung: ${definition.unterstuetzungsStatus.name}",
                style = MaterialTheme.typography.bodySmall,
            )
            knoten.parameter[TENSOR_ACHSEN_IDS]?.takeIf(String::isNotBlank)?.let { ids ->
                Text("Stabile Achsen-IDs: $ids", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "Aktuelle Handles: ${knoten.anschlüsse.joinToString { "${it.name}:${it.id.wert}" }}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            knoten.parameter[TENSOR_ACHSEN_MIGRATIONSFEHLER]?.let { fehler ->
                Text(fehler, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun AchsenKonfiguration(
    knoten: KnotenDaten,
    definition: TensorOperationDefinition,
    aktionen: KnotenInspektorAktionen,
    knotenAendern: (KnotenDaten) -> Unit,
) {
    val modus = aktuellerAchsenEingabeModus(knoten)
    val dynamischeAnzahl = dynamischeAchsenAnzahl(knoten, definition)

    Text("Achseneingabe", style = MaterialTheme.typography.titleSmall)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = modus == AchsenEingabeModus.TUPEL,
            onClick = {
                knotenAendern(
                    konfiguriereTensorOperation(
                        knoten,
                        definition,
                        AchsenEingabeModus.TUPEL,
                        dynamischeAnzahl,
                    ),
                )
            },
            label = { Text("Tupel") },
        )
        FilterChip(
            selected = modus == AchsenEingabeModus.DYNAMISCHE_EINZELHANDLES,
            onClick = {
                knotenAendern(
                    konfiguriereTensorOperation(
                        knoten,
                        definition,
                        AchsenEingabeModus.DYNAMISCHE_EINZELHANDLES,
                        dynamischeAnzahl,
                    ),
                )
            },
            label = { Text("Einzelhandles") },
        )
    }

    OutlinedTextField(
        value = knoten.parameter[TENSOR_ACHSEN_SPEZIFIKATION].orEmpty(),
        onValueChange = { neu ->
            aktionen.knoten(
                knoten.copy(
                    parameter = knoten.parameter - TENSOR_ACHSEN_MIGRATIONSFEHLER +
                        (TENSOR_ACHSEN_SPEZIFIKATION to neu),
                ),
            )
        },
        label = { Text("Gespeicherte Achsen") },
        supportingText = {
            Text("1 = erste Achse, -1 = letzte Achse, 0 ist ungültig. Verbundene Handles überschreiben diese Werte.")
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )

    if (modus == AchsenEingabeModus.DYNAMISCHE_EINZELHANDLES && definition.maximaleAchsenAnzahl == null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = {
                    knotenAendern(
                        konfiguriereTensorOperation(
                            knoten,
                            definition,
                            modus,
                            (dynamischeAnzahl - 1).coerceAtLeast(definition.minimaleAchsenAnzahl),
                        ),
                    )
                },
                enabled = dynamischeAnzahl > definition.minimaleAchsenAnzahl,
            ) { Text("Achse entfernen") }
            OutlinedButton(
                onClick = {
                    knotenAendern(
                        konfiguriereTensorOperation(
                            knoten,
                            definition,
                            modus,
                            dynamischeAnzahl + 1,
                        ),
                    )
                },
            ) { Text("Achse hinzufügen") }
        }
    }

    Text(
        "Ein Modus- oder Signaturwechsel ist eine Knotenaktion. Semantisch fremde Handles werden nicht unter derselben ID wiederverwendet.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun dynamischeAchsenAnzahl(
    knoten: KnotenDaten,
    definition: TensorOperationDefinition,
): Int = knoten.anschlüsse.count {
    it.richtung == AnschlussRichtung.Eingang && it.name.startsWith("achse.")
}.coerceAtLeast(definition.minimaleAchsenAnzahl.coerceAtLeast(1))
