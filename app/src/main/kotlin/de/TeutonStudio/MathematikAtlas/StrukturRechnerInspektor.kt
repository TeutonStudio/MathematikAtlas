package de.TeutonStudio.MathematikAtlas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import de.TeutonStudio.MathematikRechenSystem.kern.TensorRechner
import de.TeutonStudio.MathematikRechenSystem.kern.TensorRechnerOperator

internal object StrukturRechnerInspektor : KnotenInspektor {
    @Composable
    override fun Inhalt(
        knoten: KnotenDaten,
        ergebnis: KnotenAuswertungsErgebnis?,
        aktionen: KnotenInspektorAktionen,
    ) {
        if (knoten.art == TensorRechner.KNOTEN_ART) {
            TensorOperationRechnerInspektor.Inhalt(knoten, ergebnis, aktionen)
            return
        }

        val familie = StrukturRechnerKnotenFamilie.fuerKnotenArt(knoten.art) ?: return
        val operatorId = knoten.parameter[RECHNER_OPERATOR_PARAMETER]
        val formelModus = operatorId == familie.formelOperatorId
        val bekannterOperator = StrukturRechnerOperatoren.fuer(familie).firstOrNull { it.id == operatorId }
        val operator = bekannterOperator ?: StrukturRechnerOperatoren.fuer(familie).first()
        var operatorDialog by remember(knoten.id, operatorId) { mutableStateOf(false) }
        var formelDialog by remember(knoten.id) { mutableStateOf(false) }
        var formelKandidat by remember(knoten.id, operatorId) { mutableStateOf<KnotenDaten?>(null) }
        var formelStart by remember(knoten.id, operatorId) {
            mutableStateOf(ladeStrukturRechnerFormel(knoten) ?: strukturOperatorAlsFormel(operator))
        }
        val operatorEinträge = remember(knoten, familie) {
            buildList {
                if (!formelModus && bekannterOperator == null && !operatorId.isNullOrBlank()) {
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
                StrukturRechnerOperatoren.fuer(familie).forEach { definition ->
                    add(
                        RechnerOperatorAuswahlEintrag(
                            id = definition.id,
                            titel = definition.titel,
                            symbolLatex = definition.symbolLatex,
                            kategorie = strukturOperatorKategorie(familie, definition),
                            beschreibung = "${definition.definitionsLatex}. Anschlüsse und Ergebnistyp folgen diesem Operatorvertrag.",
                            suchbegriffe = definition.eingänge.mapTo(linkedSetOf()) { it.typ.name },
                            kandidat = konfiguriereStrukturRechner(knoten, familie, definition.id),
                        ),
                    )
                }
                add(
                    RechnerOperatorAuswahlEintrag(
                        id = familie.formelOperatorId,
                        titel = "Eigene Formel",
                        symbolLatex = "f(\\ldots)",
                        kategorie = "Eigene Formeln",
                        beschreibung = "Erstellt oder bearbeitet einen typisierten Ausdruck im CAS-Formelbauer.",
                        suchbegriffe = setOf("CAS", "Formelbauer", familie.formelFamilie.name),
                        art = RechnerOperatorAuswahlArt.FORMEL,
                    ),
                )
            }
        }
        val dialogEinträge = operatorEinträge.map { eintrag ->
            if (eintrag.id == familie.formelOperatorId) {
                eintrag.copy(kandidat = formelKandidat)
            } else {
                eintrag
            }
        }

        Text("Operator", style = MaterialTheme.typography.titleSmall)
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { operatorDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    when {
                        formelModus -> "Formel"
                        bekannterOperator == null && !operatorId.isNullOrBlank() -> "Unbekannter gespeicherter Operator"
                        else -> operator.titel
                    },
                    modifier = Modifier.weight(1f),
                )
                Text(
                    when {
                        formelModus -> "f(…)"
                        bekannterOperator == null && !operatorId.isNullOrBlank() -> "?"
                        else -> operator.symbolLatex
                    },
                    style = MaterialTheme.typography.labelMedium,
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
                        operatorDialog = true
                        formelDialog = true
                    }) { Text("Formel bearbeiten") }
                }
            }
        } else if (bekannterOperator == null && !operatorId.isNullOrBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Unbekannter gespeicherter Operator", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "Die Operator-ID $operatorId ist nicht registriert. Öffne die Operatorauswahl, um einen gültigen Ersatz zu wählen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
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

        if (operatorDialog) {
            RechnerOperatorAuswahlDialog(
                familienTitel = familie.titel,
                einträge = dialogEinträge,
                aktuelleId = operatorId,
                auswirkungFür = { eintrag ->
                    eintrag.kandidat?.let(aktionen::vorschauKnotenErsetzen)
                },
                schließen = {
                    formelKandidat = null
                    operatorDialog = false
                },
                operatorÜbernehmen = { eintrag ->
                    eintrag.kandidat?.let(aktionen::knoten)
                    formelKandidat = null
                    operatorDialog = false
                },
                formelÖffnen = {
                    formelStart = ladeStrukturRechnerFormel(knoten) ?: strukturOperatorAlsFormel(operator)
                    formelDialog = true
                },
            )
        }

        if (formelDialog) {
            StrukturFormelBauerDialog(
                familie = familie.formelFamilie,
                start = formelStart,
                quantorVariable = knoten.parameter["variablenName"].orEmpty().ifBlank { "x" },
                schließen = { formelDialog = false },
                übernehmen = { wurzel ->
                    formelKandidat = konfiguriereStrukturRechnerFormel(
                        knoten = knoten,
                        familie = familie,
                        wurzel = wurzel,
                        quantorVariable = knoten.parameter["variablenName"].orEmpty().ifBlank { "x" },
                    )
                    formelDialog = false
                    operatorDialog = true
                },
            )
        }
    }
}

private fun strukturOperatorKategorie(
    familie: StrukturRechnerKnotenFamilie,
    definition: StrukturRechnerOperatorDefinition,
): String = when (familie) {
    StrukturRechnerKnotenFamilie.AUSSAGESATZ ->
        if ("quantor" in definition.id.lowercase()) "Quantoren" else "Verknüpfungen"
    StrukturRechnerKnotenFamilie.VEKTOR -> when {
        definition.id.endsWith("norm") || definition.id.endsWith("winkel") || definition.id.endsWith("projektion") ||
            definition.id.endsWith("normalisierung") -> "Norm und Geometrie"
        definition.id.contains("produkt") || definition.id.endsWith("hadamard") -> "Produkte"
        else -> "Grundoperationen"
    }
    StrukturRechnerKnotenFamilie.MATRIX -> when {
        definition.id.endsWith("determinante") || definition.id.endsWith("spur") || definition.id.endsWith("rang") ->
            "Kennwerte"
        definition.id.endsWith("transponieren") || definition.id.endsWith("inverse") ||
            definition.id.endsWith("hauptdiagonale") || definition.id.endsWith("nebendiagonale") -> "Transformationen"
        definition.id.contains("produkt") || definition.id.endsWith("hadamard") -> "Produkte"
        else -> "Grundoperationen"
    }
    StrukturRechnerKnotenFamilie.TENSOR -> "Tensoren"
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
