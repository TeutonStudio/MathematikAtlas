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
        val operatorId = knoten.parameter[ZAHLENRECHNER_OPERATOR]
        val standardOperator = UniversellerZahlenOperator.entries.firstOrNull { operator ->
            operatorId == operator.stabileId || operatorId.equals(operator.name, ignoreCase = true)
        }
        val erweiterterOperator = ErweiterterZahlenOperator.vonId(operatorId)
        val formelModus = operatorId == ZAHLENRECHNER_FORMEL_ID
        var operatorMenü by remember(knoten.id, operatorId) { mutableStateOf(false) }
        var formelDialog by remember(knoten.id) { mutableStateOf(false) }

        val titel = when {
            formelModus -> "Formel"
            erweiterterOperator != null -> erweiterterOperator.titel
            else -> standardOperator?.titel ?: "Addition"
        }
        val symbol = when {
            formelModus -> "f(x)"
            erweiterterOperator != null -> erweiterterOperator.symbolLatex
            else -> standardOperator?.symbolLatex ?: "+"
        }

        Text("Operator", style = MaterialTheme.typography.titleSmall)
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { operatorMenü = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(titel, modifier = Modifier.weight(1f))
                Text(symbol, style = MaterialTheme.typography.labelMedium)
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
                            aktionen.knoten(konfiguriereStandardZahlenRechner(knoten, auswählbar))
                        },
                    )
                }
                HorizontalDivider()
                ErweiterterZahlenOperator.entries.forEach { auswählbar ->
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
                            aktionen.knoten(konfiguriereErweitertenZahlenRechner(knoten, auswählbar))
                        },
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("Formel")
                            Text(
                                "CAS-Formelbauer öffnen",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        operatorMenü = false
                        formelDialog = true
                    },
                )
            }
        }

        if (formelModus) {
            val latex = knoten.parameter[ZAHLENRECHNER_FORMEL_LATEX].orEmpty().ifBlank { "x" }
            Surface(
                Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Gespeicherte Formel", style = MaterialTheme.typography.labelLarge)
                    LatexText(latex, style = MaterialTheme.typography.titleMedium)
                    Text(
                        knoten.parameter[ZAHLENRECHNER_FORMEL_VARIABLEN]
                            ?.takeIf { it.isNotBlank() }
                            ?.let { "Eingänge: $it" }
                            ?: "Konstante Formel ohne freie Eingänge",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = { formelDialog = true }) { Text("Formel bearbeiten") }
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

        if (standardOperator != null && istVariadischerZahlenOperator(standardOperator)) {
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

        if (standardOperator != null && istKomplexKonstruktor(standardOperator)) {
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

        val verwendetGrad = standardOperator?.let(::verwendetGradWinkel) == true ||
            erweiterterOperator?.let(::verwendetGradWinkel) == true
        if (verwendetGrad) {
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
                    onCheckedChange = { aktionen.parameter(ZAHLENRECHNER_GRADWINKEL, it.toString()) },
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

        if (standardOperator in setOf(
                UniversellerZahlenOperator.INTEGRAL,
                UniversellerZahlenOperator.DIFFERENTIAL,
            )
        ) {
            val variable = knoten.parameter[ZAHLENRECHNER_VARIABLE] ?: "x"
            OutlinedTextField(
                value = variable,
                onValueChange = {
                    aktionen.parameter(ZAHLENRECHNER_VARIABLE, it.trim().ifBlank { "x" })
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

        if (formelDialog) {
            FormelBauerDialog(
                startLatex = knoten.parameter[ZAHLENRECHNER_FORMEL_LATEX].orEmpty().ifBlank { "x" },
                schließen = { formelDialog = false },
                übernehmen = { latex ->
                    aktionen.knoten(konfiguriereZahlenRechnerFormel(knoten, latex))
                    formelDialog = false
                },
            )
        }
    }
}
