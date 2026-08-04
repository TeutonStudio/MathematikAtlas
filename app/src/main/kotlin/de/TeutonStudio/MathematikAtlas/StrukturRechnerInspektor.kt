package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.AUSSAGESATZ_FORMEL_ID
import de.TeutonStudio.MathematikKnoten.LatexText
import de.TeutonStudio.MathematikKnoten.RECHNER_OPERATOR_PARAMETER
import de.TeutonStudio.MathematikKnoten.STRUKTUR_RECHNER_FORMEL_LATEX
import de.TeutonStudio.MathematikKnoten.STRUKTUR_RECHNER_FORMEL_VARIABLEN
import de.TeutonStudio.MathematikKnoten.StrukturRechnerKnotenFamilie
import de.TeutonStudio.MathematikKnoten.StrukturRechnerOperatorDefinition
import de.TeutonStudio.MathematikKnoten.StrukturRechnerOperatoren
import de.TeutonStudio.MathematikKnoten.konfiguriereStrukturRechner
import de.TeutonStudio.MathematikKnoten.konfiguriereStrukturRechnerFormel
import de.TeutonStudio.MathematikKnoten.ladeStrukturRechnerFormel
import de.TeutonStudio.MathematikKnoten.strukturOperatorAlsFormel
import de.TeutonStudio.MathematikRechenSystem.kern.AussagenSatzOperator
import de.TeutonStudio.MathematikRechenSystem.kern.TensorRechnerOperator

internal object StrukturRechnerInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        val familie = StrukturRechnerKnotenFamilie.fuerKnotenArt(knoten.art) ?: return
        val operatorId = knoten.parameter[RECHNER_OPERATOR_PARAMETER]
        val formelModus = operatorId == familie.formelOperatorId
        val operator = StrukturRechnerOperatoren.finde(familie, operatorId)
        var operatorMenü by remember(knoten.id, operatorId) { mutableStateOf(false) }
        var formelDialog by remember(knoten.id) { mutableStateOf(false) }
        var formelStart by remember(knoten.id, operatorId) {
            mutableStateOf(ladeStrukturRechnerFormel(knoten) ?: strukturOperatorAlsFormel(operator))
        }

        Text("Operator", style = MaterialTheme.typography.titleSmall)
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { operatorMenü = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (formelModus) "Formel" else operator.titel, modifier = Modifier.weight(1f))
                Text(
                    if (formelModus) "f(…)" else operator.symbolLatex,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            DropdownMenu(
                expanded = operatorMenü,
                onDismissRequest = { operatorMenü = false },
            ) {
                StrukturRechnerOperatoren.fuer(familie).forEach { auswählbar ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(auswählbar.titel)
                                Text(
                                    auswählbar.symbolLatex,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        onClick = {
                            operatorMenü = false
                            aktionen.knoten(konfiguriereStrukturRechner(knoten, familie, auswählbar.id))
                        },
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("Formel")
                            Text(
                                "Typisierten CAS-Formelbauer öffnen",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        operatorMenü = false
                        formelStart = ladeStrukturRechnerFormel(knoten) ?: strukturOperatorAlsFormel(operator)
                        formelDialog = true
                    },
                )
            }
        }

        if (formelModus) {
            val latex = knoten.parameter[STRUKTUR_RECHNER_FORMEL_LATEX].orEmpty().ifBlank { "f(\\ldots)" }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Gespeicherte Strukturformel", style = MaterialTheme.typography.labelLarge)
                    LatexText(latex, style = MaterialTheme.typography.titleMedium)
                    Text(
                        knoten.parameter[STRUKTUR_RECHNER_FORMEL_VARIABLEN]
                            ?.takeIf(String::isNotBlank)
                            ?.let { "Eingänge: $it" }
                            ?: "Konstante Formel ohne freie Eingänge",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = {
                        formelStart = ladeStrukturRechnerFormel(knoten) ?: strukturOperatorAlsFormel(operator)
                        formelDialog = true
                    }) { Text("Formel bearbeiten") }
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Dynamische Definition", style = MaterialTheme.typography.labelLarge)
                    LatexText(operator.definitionsLatex, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Anschlüsse und Ergebnistyp folgen diesem Operatorvertrag.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (familie == StrukturRechnerKnotenFamilie.AUSSAGESATZ) {
            AussagenParameter(knoten, operator, aktionen)
        }
        if (familie == StrukturRechnerKnotenFamilie.TENSOR) {
            TensorParameter(knoten, operatorId, aktionen)
        }

        ergebnis?.warnungen.orEmpty().forEach { warnung ->
            Text(
                warnung,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ergebnis?.fehler?.let { fehler ->
            Text(
                fehler,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (formelDialog) {
            StrukturFormelBauerDialog(
                familie = familie.formelFamilie,
                start = formelStart,
                quantorVariable = knoten.parameter["variablenName"].orEmpty().ifBlank { "x" },
                schließen = { formelDialog = false },
                übernehmen = { wurzel ->
                    aktionen.knoten(
                        konfiguriereStrukturRechnerFormel(
                            knoten = knoten,
                            familie = familie,
                            wurzel = wurzel,
                            quantorVariable = knoten.parameter["variablenName"].orEmpty().ifBlank { "x" },
                        ),
                    )
                    formelDialog = false
                },
            )
        }
    }
}

@Composable
private fun AussagenParameter(
    knoten: KnotenDaten,
    operator: StrukturRechnerOperatorDefinition,
    aktionen: KnotenInspektorAktionen,
) {
    val quantor = operator.id in setOf(
        AussagenSatzOperator.ALLQUANTOR.stabileId,
        AussagenSatzOperator.EXISTENZQUANTOR.stabileId,
        AussagenSatzOperator.EINDEUTIGER_EXISTENZQUANTOR.stabileId,
    )
    if (!quantor && knoten.parameter[RECHNER_OPERATOR_PARAMETER] != AUSSAGESATZ_FORMEL_ID) return

    Text("Variablenbindung", style = MaterialTheme.typography.titleSmall)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = knoten.parameter["variablenName"].orEmpty().ifBlank { "x" },
            onValueChange = { aktionen.parameter("variablenName", it.trim().ifBlank { "x" }) },
            label = { Text("Sichtbarer Name") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = knoten.parameter["praedikatName"].orEmpty().ifBlank { "P" },
            onValueChange = { aktionen.parameter("praedikatName", it.trim().ifBlank { "P" }) },
            label = { Text("Prädikat") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TensorParameter(
    knoten: KnotenDaten,
    operatorId: String?,
    aktionen: KnotenInspektorAktionen,
) {
    when (operatorId) {
        TensorRechnerOperator.KONTRAKTION.stabileId -> ParameterZeile(
            "Achsen",
            knoten.parameter["achsen"].orEmpty(),
        ) { aktionen.parameter("achsen", it) }

        TensorRechnerOperator.ACHSENPERMUTATION.stabileId,
        TensorRechnerOperator.TRANSPONIEREN.stabileId,
        -> ParameterZeile(
            "Permutation",
            knoten.parameter["permutation"].orEmpty(),
        ) { aktionen.parameter("permutation", it) }

        TensorRechnerOperator.ACHSENSCHNITT.stabileId -> {
            ParameterZeile("Achsen", knoten.parameter["achsen"].orEmpty()) {
                aktionen.parameter("achsen", it)
            }
            ParameterZeile("Indizes", knoten.parameter["indizes"].orEmpty()) {
                aktionen.parameter("indizes", it)
            }
        }

        TensorRechnerOperator.INDEXAUSWERTUNG.stabileId -> ParameterZeile(
            "Indizes",
            knoten.parameter["indizes"].orEmpty(),
        ) { aktionen.parameter("indizes", it) }
    }
}

@Composable
private fun ParameterZeile(
    titel: String,
    wert: String,
    geändert: (String) -> Unit,
) {
    OutlinedTextField(
        value = wert,
        onValueChange = geändert,
        label = { Text(titel) },
        supportingText = { Text("Kommagetrennte, nullbasierte Indizes") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
