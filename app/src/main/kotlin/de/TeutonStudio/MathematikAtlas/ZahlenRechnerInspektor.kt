package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.TeutonStudio.KnotenKartenVerwalter.daten.KnotenDaten
import de.TeutonStudio.MathematikKartenAdapter.KnotenAuswertungsErgebnis
import de.TeutonStudio.MathematikKnoten.*
import de.TeutonStudio.MathematikRechenSystem.kern.UniversellerZahlenOperator

internal object ZahlenRechnerInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        val operator = UniversellerZahlenOperator.vonId(
            knoten.parameter[ZAHLENRECHNER_OPERATOR],
        )
        var operatorMenü by remember(knoten.id, operator) { mutableStateOf(false) }

        Text("Operator", style = MaterialTheme.typography.titleSmall)
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { operatorMenü = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(operator.titel, modifier = Modifier.weight(1f))
                Text(operator.symbolLatex, style = MaterialTheme.typography.labelMedium)
            }
            DropdownMenu(
                expanded = operatorMenü,
                onDismissRequest = { operatorMenü = false },
            ) {
                UniversellerZahlenOperator.entries.forEach { auswählbar ->
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
                            aktionen.knoten(
                                konfiguriereZahlenRechner(
                                    knoten = knoten,
                                    operator = auswählbar,
                                ),
                            )
                        },
                    )
                }
            }
        }

        ergebnis?.warnungen.orEmpty().firstOrNull { it.startsWith("Definition:") }?.let { definition ->
            Text(
                definition,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        if (istVariadischerZahlenOperator(operator)) {
            val anzahl = knoten.parameter["festeEingänge"]?.toIntOrNull()?.coerceAtLeast(2) ?: 2
            Text("Feste Operanden", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        aktionen.knoten(
                            konfiguriereZahlenRechner(
                                knoten = knoten,
                                festeEingänge = (anzahl - 1).coerceAtLeast(2),
                            ),
                        )
                    },
                    enabled = anzahl > 2,
                ) { Text("−") }
                Text(anzahl.toString(), modifier = Modifier.weight(1f))
                OutlinedButton(
                    onClick = {
                        aktionen.knoten(
                            konfiguriereZahlenRechner(
                                knoten = knoten,
                                festeEingänge = anzahl + 1,
                            ),
                        )
                    },
                ) { Text("+") }
            }
        }

        if (istKomplexKonstruktor(operator)) {
            val modus = knoten.parameter[ZAHLENRECHNER_KOMPLEX_EINGABE]
                ?: ZAHLENRECHNER_KOMPLEX_SEPARIERT
            Text("Komplexe Eingabe", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = modus == ZAHLENRECHNER_KOMPLEX_SEPARIERT,
                    onClick = {
                        aktionen.knoten(
                            konfiguriereZahlenRechner(
                                knoten = knoten,
                                komplexEingabe = ZAHLENRECHNER_KOMPLEX_SEPARIERT,
                            ),
                        )
                    },
                    label = { Text("Getrennt") },
                )
                FilterChip(
                    selected = modus == ZAHLENRECHNER_KOMPLEX_TUPEL,
                    onClick = {
                        aktionen.knoten(
                            konfiguriereZahlenRechner(
                                knoten = knoten,
                                komplexEingabe = ZAHLENRECHNER_KOMPLEX_TUPEL,
                            ),
                        )
                    },
                    label = { Text("Tupel") },
                )
            }
        }

        if (verwendetGradWinkel(operator)) {
            val grad = knoten.parameter[ZAHLENRECHNER_GRADWINKEL].toBoolean()
            val auswerten = knoten.parameter[ZAHLENRECHNER_GRAD_AUSWERTEN] != "false"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Gradwinkel", modifier = Modifier.weight(1f))
                Switch(
                    checked = grad,
                    onCheckedChange = {
                        aktionen.parameter(ZAHLENRECHNER_GRADWINKEL, it.toString())
                    },
                )
            }
            if (grad) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Als x·π÷180 auswerten", modifier = Modifier.weight(1f))
                    Switch(
                        checked = auswerten,
                        onCheckedChange = {
                            aktionen.parameter(ZAHLENRECHNER_GRAD_AUSWERTEN, it.toString())
                        },
                    )
                }
                Text(
                    "° := π/180",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (operator in setOf(
                UniversellerZahlenOperator.INTEGRAL,
                UniversellerZahlenOperator.DIFFERENTIAL,
            )
        ) {
            val variable = knoten.parameter[ZAHLENRECHNER_VARIABLE] ?: "x"
            OutlinedTextField(
                value = variable,
                onValueChange = {
                    aktionen.parameter(
                        ZAHLENRECHNER_VARIABLE,
                        it.trim().ifBlank { "x" },
                    )
                },
                label = { Text("Variable") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        ergebnis?.warnungen.orEmpty()
            .filterNot { it.startsWith("Definition:") }
            .forEach { regel ->
                Text(
                    regel,
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
    }
}
