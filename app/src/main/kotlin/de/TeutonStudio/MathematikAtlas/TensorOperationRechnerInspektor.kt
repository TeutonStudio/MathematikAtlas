package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
        var operatorMenue by remember(knoten.id, definition.id) { mutableStateOf(false) }
        var ausstehenderKnoten by remember(knoten.id) { mutableStateOf<KnotenDaten?>(null) }

        fun uebernehmeOderBestaetige(neu: KnotenDaten) {
            val neueIds = neu.anschlüsse.mapTo(linkedSetOf()) { it.id }
            val entfernt = knoten.anschlüsse.filter { it.id !in neueIds }
            if (entfernt.isEmpty()) aktionen.knoten(neu) else ausstehenderKnoten = neu
        }

        Text("Tensoroperation", style = MaterialTheme.typography.titleSmall)
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { operatorMenue = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(definition.titel, modifier = Modifier.weight(1f))
                Text(definition.familie.name, style = MaterialTheme.typography.labelSmall)
            }
            DropdownMenu(
                expanded = operatorMenue,
                onDismissRequest = { operatorMenue = false },
            ) {
                StandardTensorOperationen.registry.alle().forEach { auswaehlbar ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(auswaehlbar.titel)
                                Text(
                                    "${auswaehlbar.familie.name} · ${auswaehlbar.unterstuetzungsStatus.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        onClick = {
                            operatorMenue = false
                            uebernehmeOderBestaetige(
                                konfiguriereTensorOperation(
                                    knoten = knoten,
                                    definition = auswaehlbar,
                                    achsenModus = aktuellerAchsenEingabeModus(knoten),
                                    dynamischeAchsenAnzahl = dynamischeAchsenAnzahl(knoten, auswaehlbar),
                                ),
                            )
                        },
                    )
                }
            }
        }

        VertragsKarte(definition, knoten)

        if (definition.benoetigtAchsenEingabe()) {
            AchsenKonfiguration(
                knoten = knoten,
                definition = definition,
                aktionen = aktionen,
                knotenAendern = ::uebernehmeOderBestaetige,
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

        ausstehenderKnoten?.let { ziel ->
            val neueIds = ziel.anschlüsse.mapTo(linkedSetOf()) { it.id }
            val entfernt = knoten.anschlüsse.filter { it.id !in neueIds }
            AlertDialog(
                onDismissRequest = { ausstehenderKnoten = null },
                title = { Text("Signatur wirklich wechseln?") },
                text = {
                    Text(
                        "Die Handles ${entfernt.joinToString { it.name }} werden entfernt. " +
                            "Bestehende Verbindungen an diesen Handles werden dabei getrennt.",
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            aktionen.knoten(ziel)
                            ausstehenderKnoten = null
                        },
                    ) { Text("Wechseln") }
                },
                dismissButton = {
                    TextButton(onClick = { ausstehenderKnoten = null }) { Text("Abbrechen") }
                },
            )
        }
    }
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
